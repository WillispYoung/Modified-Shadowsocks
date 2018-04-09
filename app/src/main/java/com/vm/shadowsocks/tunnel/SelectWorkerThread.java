package com.vm.shadowsocks.tunnel;

import com.vm.shadowsocks.core.TunnelFactory;
import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;

public class SelectWorkerThread implements Runnable {
    private static int ThreadCount = 0;
    private int threadID;
    private int socketCount;
  
    Selector selector;
    Thread thisThread;
    
    public SelectWorkerThread() throws IOException{
        ThreadCount += 1;
        this.threadID = ThreadCount;
        
        this.selector = Selector.open();
    }
    
    public void addTask(SocketChannel local, InetSocketAddress address) throws Exception {
        if (address != null) {
            socketCount += 1;
            
            Tunnel localTunnel = TunnelFactory.wrap(local, selector);
            Tunnel remoteTunnel = TunnelFactory.createTunnelByConfig(address, selector);
            localTunnel.setBrotherTunnel(remoteTunnel);
            remoteTunnel.setBrotherTunnel(localTunnel);
            
            selector.wakeup();
            remoteTunnel.connect(address);
            
            System.out.println("Thread " + threadID + " accept the task for " + local.getRemoteAddress().toString());
        }
    }
    
    public int getSocketCount() {
        return socketCount;
    }
    
    public int getThreadID() {
        return threadID;
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                int selected = selector.select();
                if (selected == 0) {
                    Thread.sleep(300);
                    continue;
                }
                System.out.println("Thread " + threadID + " selected  " + selected);
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid()) {
                        try {
                            if (key.isReadable()) {
                                ((Tunnel) key.attachment()).onReadable(key);
                            }
                            else if (key.isWritable()) {
                                ((Tunnel) key.attachment()).onWritable(key);
                            }
                            else if (key.isConnectable()) {
                                ((Tunnel) key.attachment()).onConnectable();
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage() + " during selector worker processing");
                        }
                    }
                    keyIterator.remove();
                }
            }
         }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            this.stop();
        }
    }
    
    public void start() {
        thisThread = new Thread(this);
        thisThread.setName("Worker Thread " + threadID);
        thisThread.start();
    }
    
    public void stop() {
        CommonMethods.close(selector);
    }
}
