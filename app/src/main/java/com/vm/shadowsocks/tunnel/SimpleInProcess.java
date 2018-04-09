package com.vm.shadowsocks.tunnel;

import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tunnel.shadowsocks.ICrypt;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SimpleInProcess implements Runnable{
    public static int bruteThreadCount = 0;
    
    private ByteBuffer buffer = ByteBuffer.allocate(10000);
    
    private SocketChannel localChannel;
    private SocketChannel remoteChannel;
    private boolean isReadable;
    
    private boolean needEncryption;
    private ICrypt encryptor;
    
    public SimpleInProcess(SocketChannel localChannel, SocketChannel remoteChannel,
                           boolean isReadable) {
        this.localChannel = localChannel;
        this.remoteChannel = remoteChannel;
        this.isReadable = isReadable;

        this.needEncryption = false;
        this.encryptor = null;
        
        bruteThreadCount += 1;
        System.out.println("Brute Thread Count: " + bruteThreadCount);
    }
    
    public SimpleInProcess(SocketChannel localChannel, SocketChannel remoteChannel,
                           boolean isReadable, ICrypt encryptor) {
        this.localChannel = localChannel;
        this.remoteChannel = remoteChannel;
        this.isReadable = isReadable;
        
        this.needEncryption = true;
        this.encryptor = encryptor;
    
        bruteThreadCount += 1;
        System.out.println("Brute Thread Count: " + bruteThreadCount);
    }
    
    @Override
    public void run() {
        try {
            while (true) {
                buffer.clear();
                
                int bytesRead = remoteChannel.read(buffer);
                buffer.flip();
                if (bytesRead < 0)
                    return;
                
                if (isReadable) {
                    //
                }
                
                if (needEncryption) {
                    byte[] content = new byte[buffer.limit()];
                    buffer.get(content);
                    buffer.clear();
                    buffer.put(encryptor.decrypt(content));
                    buffer.flip();
                }
                
                while (buffer.hasRemaining())
                    localChannel.write(buffer);
            }
        }
        catch (Exception e) {
        
        }
        finally {
            CommonMethods.close(localChannel);
            CommonMethods.close(remoteChannel);
            
            bruteThreadCount -= 1;
        }
    }
}
