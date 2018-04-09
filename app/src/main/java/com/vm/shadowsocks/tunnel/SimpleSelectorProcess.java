package com.vm.shadowsocks.tunnel;

import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class SimpleSelectorProcess implements Runnable{
    SocketChannel local;
    SocketChannel remote;
    ICrypt crypt;
    ByteBuffer buffer;
    
    Selector selector;
    
    public SimpleSelectorProcess(SocketChannel local, SocketChannel remote, ICrypt crypt) throws Exception{
        this.local = local;
        this.remote = remote;
        this.crypt = crypt;
        
        buffer = ByteBuffer.allocate(20000);
        selector = Selector.open();
    }
    
    @Override
    public void run() {
        try {
            local.configureBlocking(false);
            remote.configureBlocking(false);
            local.register(selector, SelectionKey.OP_READ);
            remote.register(selector, SelectionKey.OP_READ);
        
            while (!Thread.interrupted()) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                for (SelectionKey key : keys) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    
                    buffer.clear();
                    channel.read(buffer);
                    buffer.flip();
                        
                    byte[] content = new byte[buffer.limit()];
                    buffer.get(content);
                    buffer.clear();
                    
                    if (channel == local) {
                        buffer.put(crypt.encrypt(content));
                        buffer.flip();
    
                        while (buffer.hasRemaining())
                            remote.write(buffer);
                    }
                    else {
                        buffer.put(crypt.decrypt(content));
                        buffer.flip();
                        
                        while (buffer.hasRemaining())
                            local.write(buffer);
                    }
                    
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            CommonMethods.close(local);
            CommonMethods.close(remote);
        }
    }
}
