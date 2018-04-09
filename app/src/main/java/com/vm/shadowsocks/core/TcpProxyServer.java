package com.vm.shadowsocks.core;

import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tcpip.HTTPRequestHeader;
import com.vm.shadowsocks.tunnel.SimpleInProcess;
import com.vm.shadowsocks.tunnel.SimpleOutProcess;
import com.vm.shadowsocks.tunnel.SimpleSelectorProcess;
import com.vm.shadowsocks.tunnel.TaskScheduler;
import com.vm.shadowsocks.tunnel.Tunnel;
import com.vm.shadowsocks.tunnel.shadowsocks.CryptFactory;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;
import com.vm.shadowsocks.tunnel.shadowsocks.ShadowsocksConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpProxyServer implements Runnable {
    
    public boolean Stopped;
    public short Port;
    
    Selector m_Selector;
    Thread m_ServerThread;
    ServerSocketChannel m_ServerSocketChannel;
    
    ExecutorService executor = Executors.newCachedThreadPool();
    
    public TcpProxyServer(int port) throws Exception {
        m_Selector = Selector.open();
        
        m_ServerSocketChannel = ServerSocketChannel.open();
        m_ServerSocketChannel.configureBlocking(false);
        m_ServerSocketChannel.socket().bind(new InetSocketAddress(port));
        m_ServerSocketChannel.register(m_Selector, SelectionKey.OP_ACCEPT);
        
        this.Port = (short) m_ServerSocketChannel.socket().getLocalPort();
        System.out.printf("AsyncTcpServer listen on %d success.\n", this.Port & 0xFFFF);
    }
    
    public void start() {
        m_ServerThread = new Thread(this);
        m_ServerThread.setName("TcpProxyServerThread");
        m_ServerThread.start();
    }
    
    public void stop() {
        this.Stopped = true;
        if (m_Selector != null) {
            try {
                m_Selector.close();
                m_Selector = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (m_ServerSocketChannel != null) {
            try {
                m_ServerSocketChannel.close();
                m_ServerSocketChannel = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdownNow();
        TaskScheduler.stopTasks();
    }
    
    // 未解析的地址 = 发送给VPN服务器
    // 解析过的地址 = 直接发送到目的机器（包过滤）
    InetSocketAddress getDestAddress(SocketChannel localChannel) {
        short portKey = (short) localChannel.socket().getPort();
        NatSession session = NatSessionManager.getSession(portKey);
        if (session != null) {
            if (ProxyConfig.Instance.needProxy(session.RemoteHost, session.RemoteIP)) {
//                System.out.printf("%d/%d:[PROXY] %s=>%s:%d\n", NatSessionManager.getSessionCount(), Tunnel.SessionCount, session.RemoteHost, CommonMethods.ipIntToString(session.RemoteIP), session.RemotePort & 0xFFFF);
                return InetSocketAddress.createUnresolved(session.RemoteHost, session.RemotePort & 0xFFFF);
            } else {
                return new InetSocketAddress(localChannel.socket().getInetAddress(), session.RemotePort & 0xFFFF);
            }
        }
        return null;
    }
    
    // 发送认证报文
    public void sendAuthentication(SocketChannel channel, ICrypt encryptor,
                                   InetSocketAddress address) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(2000);
        buffer.put((byte) 0x03);
        
        byte[] domainBytes = address.getHostName().getBytes();
        buffer.put((byte) domainBytes.length);
        buffer.put(domainBytes);
        buffer.putShort((short) address.getPort());
        buffer.flip();
        
        byte[] header = new byte[buffer.limit()];
        buffer.get(header);
        
        buffer.clear();
        buffer.put(encryptor.encrypt(header));
        buffer.flip();
        
        channel.write(buffer);
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                m_Selector.select();
                Iterator<SelectionKey> keyIterator = m_Selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid()) {
                        try {
                            if (key.isReadable()) {
                                ((Tunnel) key.attachment()).onReadable(key);
                            } else if (key.isWritable()) {
                                ((Tunnel) key.attachment()).onWritable(key);
                            } else if (key.isConnectable()) {
                                ((Tunnel) key.attachment()).onConnectable();
                            } else if (key.isAcceptable()) {
                                onAccepted();
                            }
                        } catch (Exception e) {
//                            e.printStackTrace();
                            key.cancel();
                            System.out.println(e.getMessage() + " during TcpServer processing");
                        }
                    }
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.stop();
            System.out.println("TcpServer thread exited.");
        }
    }
    
    // 初始设计，单线程SELECT调度
    private void onAccepted() {
        Tunnel localTunnel = null;
        try {
            SocketChannel localChannel = m_ServerSocketChannel.accept();
            localTunnel = TunnelFactory.wrap(localChannel, m_Selector);
            
            InetSocketAddress destAddress = getDestAddress(localChannel);
            if (destAddress != null) {
                Tunnel remoteTunnel = TunnelFactory.createTunnelByConfig(destAddress, m_Selector);
                remoteTunnel.setBrotherTunnel(localTunnel); //关联兄弟
                localTunnel.setBrotherTunnel(remoteTunnel); //关联兄弟
                remoteTunnel.connect(destAddress);          //开始连接：实际连接服务器
            } else {
                LocalVpnService.Instance.writeLog("Error: socket(%s:%d) target host is null.", localChannel.socket().getInetAddress().toString(), localChannel.socket().getPort());
                localTunnel.dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (localTunnel != null) {
                localTunnel.dispose();
            }
            LocalVpnService.Instance.writeLog("Error: remote socket create failed: %s", e.toString());
        }
    }
    
    // 简单的循环异步多线程，每个线程只处理读
    // 读取第一个数据包，检查其访问的URL是否需要分流
    // 问题：多并发页面会卡住，猜测是线程调度的问题
    private void onAcceptedSimpleAsync() {
        SocketChannel localChannel = null;
        SocketChannel remoteChannel = null;
        try {
            localChannel = m_ServerSocketChannel.accept();
            remoteChannel = SocketChannel.open();
            LocalVpnService.Instance.protect(remoteChannel.socket());
            
            ByteBuffer buffer = ByteBuffer.allocate(2000);
            localChannel.read(buffer);
            buffer.flip();
            
            boolean isReadable = true;
            HTTPRequestHeader header = null;
            NatSession session = NatSessionManager.getSession((short) localChannel.socket().getPort());
            ShadowsocksConfig config = (ShadowsocksConfig) ProxyConfig.Instance.getDefaultProxy();
            ICrypt encryptor = CryptFactory.get(config.EncryptMethod, config.Password);
            
            try {
                header = new HTTPRequestHeader(new String(buffer.array()).trim());
            } catch (Exception e) {
                isReadable = false;
            }
            
            // 分流：直接发送到目的服务器
            if (header != null) {
                if (header.getUrl().matches("/\\S*.(mp4|flv|f4v)(\\?\\S+)?")) { // 分流出去
                    InetSocketAddress address = new InetSocketAddress(localChannel.socket().getInetAddress(), session.RemotePort & 0xFFFF);
                    remoteChannel.connect(address);
                    remoteChannel.write(buffer);
                    
                    System.out.println("Bypass: " + header.getUrl());
                    executor.submit(new SimpleInProcess(localChannel, remoteChannel, isReadable));
                    executor.submit(new SimpleOutProcess(localChannel, remoteChannel, isReadable));
                    
                    return;
                }
            }
            
            // 否则，默认发送到VPN服务器
            InetSocketAddress address = InetSocketAddress.createUnresolved(session.RemoteHost, session.RemotePort & 0xFFFF);
            remoteChannel.connect(config.ServerAddress);
            sendAuthentication(remoteChannel, encryptor, address);
            
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            buffer.clear();
            buffer.put(encryptor.encrypt(bytes));
            buffer.flip();
            
            remoteChannel.write(buffer);
            
            executor.submit(new SimpleInProcess(localChannel, remoteChannel, isReadable, encryptor));
            executor.submit(new SimpleOutProcess(localChannel, remoteChannel, isReadable, encryptor));
            
        } catch (Exception e) {
            CommonMethods.close(localChannel);
            CommonMethods.close(remoteChannel);
            e.printStackTrace();
            System.out.println("Error accept local channel and setup remote channel");
        }
    }
    
    // 多线程，每个线程一个Selector，处理一组SocketChannel
    // 问题：多并发页面会崩溃，出现too many open files的错误
    private void onAcceptedSingleAsync() {
        SocketChannel local = null;
        SocketChannel remote = null;
        try {
            local = m_ServerSocketChannel.accept();
            remote = SocketChannel.open();
            LocalVpnService.Instance.protect(remote.socket());
            
            NatSession session = NatSessionManager.getSession((short) local.socket().getPort());
            ShadowsocksConfig config = (ShadowsocksConfig) ProxyConfig.Instance.getDefaultProxy();
            ICrypt encryptor = CryptFactory.get(config.EncryptMethod, config.Password);
            InetSocketAddress address = InetSocketAddress.createUnresolved(session.RemoteHost, session.RemotePort & 0xFFFF);
            
            remote.connect(config.ServerAddress);
            sendAuthentication(remote, encryptor, address);
            
            executor.submit(new SimpleSelectorProcess(local, remote, encryptor));
        } catch (Exception e) {
            e.printStackTrace();
            CommonMethods.close(local);
            CommonMethods.close(remote);
        }
    }
    
    // 多线程，每个线程一个Selector，处理多个SocketChannel
    // 根据每个线程处理的SocketChannel数目进行调度
    // 问题：Selector.wakeup和Selector.select会相互阻塞，导致效率极差
    private void onAcceptedScheduled() {
        try {
            SocketChannel localChannel = m_ServerSocketChannel.accept();
            InetSocketAddress address = getDestAddress(localChannel);
            
            TaskScheduler.addTask(localChannel, address);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
