package com.vm.shadowsocks.tunnel;

import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleOutProcess implements Runnable{
    private ByteBuffer buffer = ByteBuffer.allocate(10000);
    
    private SocketChannel localChannel;
    private SocketChannel remoteChannel;
    private boolean isReadable;
    
    private boolean needEncryption;
    private ICrypt encryptor;
    
    public SimpleOutProcess(SocketChannel localChannel, SocketChannel remoteChannel,
                            boolean isReadable) {
        this.localChannel = localChannel;
        this.remoteChannel = remoteChannel;
        this.isReadable = isReadable;
        
        this.needEncryption = false;
        this.encryptor = null;
    }
    
    public SimpleOutProcess(SocketChannel localChannel, SocketChannel remoteChannel,
                            boolean isReadable, ICrypt encryptor) {
        this.localChannel = localChannel;
        this.remoteChannel = remoteChannel;
        this.isReadable = isReadable;
        
        this.needEncryption = true;
        this.encryptor = encryptor;
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                buffer.clear();
                
                int bytesRead = localChannel.read(buffer);
                buffer.flip();
                if (bytesRead < 0)
                    return;
                
                if (isReadable) {
                    // check malicious domain
                }
                
                if (needEncryption) {
                    byte[] bytes = new byte[buffer.limit()];
                    buffer.get(bytes);
                    buffer.clear();
                    buffer.put(encryptor.encrypt(bytes));
                    buffer.flip();
                }
                
                while (buffer.hasRemaining())
                    remoteChannel.write(buffer);
            }
        }
        catch (Exception e) {
        
        }
        finally {
            CommonMethods.close(localChannel);
            CommonMethods.close(remoteChannel);
        }
    }
}
