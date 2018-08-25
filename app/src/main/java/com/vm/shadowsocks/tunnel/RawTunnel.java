package com.vm.shadowsocks.tunnel;

import android.os.Build;
import android.util.Log;

import com.vm.shadowsocks.core.LocalVpnService;
import com.vm.shadowsocks.core.MalwareInfo;
import com.vm.shadowsocks.core.NatSession;
import com.vm.shadowsocks.core.NatSessionManager;
import com.vm.shadowsocks.core.TrafficSessionManager;
import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tcpip.HTTPRequestHeader;
import com.vm.shadowsocks.test.ProtoPortionTest;
import com.vm.shadowsocks.ui.TrafficStatus;
import com.vm.shadowsocks.util.PacketEntry;
import com.vm.shadowsocks.util.SessionContent;

import org.apache.http.HttpRequest;
import org.bouncycastle.util.Pack;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class RawTunnel extends Tunnel {
    private boolean isLocal = true;
    private boolean isBypassed = false;
    private boolean isReadable = true;
    
    private ArrayList<PacketEntry> capturedPackets;
    
    public void setLocal(boolean local) {
        isLocal = local;
    }
    
    public void setBypassed(boolean bypassed) {
        isBypassed = bypassed;
    }
    
    public RawTunnel(InetSocketAddress serverAddress, Selector selector) throws Exception {
        super(serverAddress, selector);
        capturedPackets = new ArrayList<>();
    }

    public RawTunnel(SocketChannel innerChannel, Selector selector) {
        super(innerChannel, selector);
        capturedPackets = new ArrayList<>();
    }

    @Override
    protected void onConnected(ByteBuffer buffer) throws Exception {
        onTunnelEstablished();
    }

    @Override
    protected void beforeSend(ByteBuffer buffer, boolean isPartial) throws Exception {
        // 只处理本地接收的HTTP响应数据包
        if (isReadable && isLocal && !isPartial) {
            String content = new String(buffer.array()).trim();
            long time = System.currentTimeMillis();

            capturedPackets.add(new PacketEntry(content, time, false));

            TrafficSessionManager.OriginHTTPBytesReceived += content.length();

            if (isBypassed)
                TrafficSessionManager.VideoBytesReceived += content.length();
        }
    }
    
    @Override
    protected void afterReceived(ByteBuffer buffer) throws Exception {
        // 只处理本地发出的HTTP请求数据包
        if (isReadable && isLocal) {
            String content = new String(buffer.array()).trim();
            try {
                HTTPRequestHeader header = new HTTPRequestHeader(content);
                if (header.getValue("Host") != null) {
                    if (CommonMethods.checkMaliciousDomain(header.getValue("Host"))) {
                        // TODO 访问恶意域名的处理
                        String blockResponse = "HTTP/1.1 200 OK\n" +
                                "Server: nginx/1.4.6 (Ubuntu)\n" +
                                "Date: Fri, 29 Jun 2018 07:32:20 GMT\n" +
                                "Content-Type: text/html; charset=utf-8\n" +
                                "Connection: keep-alive\n" +
                                "\n" +
                                "Malicious Domain!";
                        this.m_InnerChannel.write(ByteBuffer.wrap(blockResponse.getBytes()));
                        this.dispose();

                        return;
                    }
                }

                if (!isBypassed && header.getUrl() != null) {
                    String url = header.getUrl();
                    if (CommonMethods.checkBypassUrl(url)) {
                        System.out.println("Bypass url: " + url);
                        this.getBrotherTunnel().closeSelf();

                        // 生成目的主机的地址
                        short portKey = (short) getInnerChannel().socket().getPort();
                        NatSession session = NatSessionManager.getSession(portKey);
                        InetSocketAddress address = new InetSocketAddress(getInnerChannel().socket().getInetAddress(), session.RemotePort & 0xFFFF);

                        // 保护，然后连接
                        SocketChannel channel = SocketChannel.open();
                        LocalVpnService.Instance.protect(channel.socket());
                        channel.connect(address);
                        channel.configureBlocking(false);

                        RawTunnel tunnel = new RawTunnel(channel, this.getSelector());
                        tunnel.setLocal(false);
                        tunnel.setBypassed(true);

                        // 互相设置兄弟然后注册
                        this.setBrotherTunnel(tunnel);
                        tunnel.setBrotherTunnel(this);
                        channel.register(this.getSelector(), SelectionKey.OP_READ, tunnel);
                        isBypassed = true;
                    }
                }
                long time = System.currentTimeMillis();
                capturedPackets.add(new PacketEntry(content, time, true));

                TrafficSessionManager.OriginHTTPBytesSent += content.length();

                if (isBypassed)
                    TrafficSessionManager.VideoBytesSent += content.length();
            }
            catch (Exception e) {
                if (!e.getMessage().equals("Data is not in HTTP request format"))
                    e.printStackTrace();
                else {
                    isReadable = false;
                }
            }
        }
    }

    @Override
    protected boolean isTunnelEstablished() { return true; }

    @Override
    protected void onDispose() {
        if (isLocal && isReadable) {
            String domain = "";
            int port = getInnerChannel().socket().getPort();
            NatSession session = NatSessionManager.getSession((short) port);
            if (session != null)
                domain = session.RemoteHost;

            if (capturedPackets.size() > 0) {
                SessionContent content = new SessionContent(capturedPackets, domain, null, port, bytesRead, bytesWritten);
                TrafficSessionManager.addSession(content);
            }
        }
    }
}
