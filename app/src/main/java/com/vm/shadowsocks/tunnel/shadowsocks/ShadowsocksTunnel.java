package com.vm.shadowsocks.tunnel.shadowsocks;

import android.util.Log;

import com.vm.shadowsocks.core.TrafficSessionManager;
import com.vm.shadowsocks.tunnel.Tunnel;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;

public class ShadowsocksTunnel extends Tunnel {

    private ICrypt m_Encryptor;
    private ShadowsocksConfig m_Config;
    private boolean m_TunnelEstablished;

    public ShadowsocksTunnel(ShadowsocksConfig config, Selector selector) throws Exception {
        super(config.ServerAddress, selector);
        m_Config = config;
        m_Encryptor = CryptFactory.get(m_Config.EncryptMethod, m_Config.Password);

    }

    @Override
    protected void onConnected(ByteBuffer buffer) throws Exception {
        buffer.clear();
        // https://shadowsocks.org/en/spec/protocol.html

        buffer.put((byte) 0x03);//domain
        byte[] domainBytes = m_DestAddress.getHostName().getBytes();
        buffer.put((byte) domainBytes.length);  //domain length;
        buffer.put(domainBytes);                // domain
        buffer.putShort((short) m_DestAddress.getPort());   // port
        buffer.flip();
        byte[] _header = new byte[buffer.limit()];
        buffer.get(_header);

        byte[] encryptedHeader = m_Encryptor.encrypt(_header);
        buffer.clear();
        buffer.put(encryptedHeader);
        buffer.flip();
    
        TrafficSessionManager.EncryptedBytesSent += encryptedHeader.length;
        
        if (write(buffer, true)) {
            m_TunnelEstablished = true;
            onTunnelEstablished();
        } else {
            m_TunnelEstablished = true;
            this.beginReceive();
        }
    }

    @Override
    protected boolean isTunnelEstablished() {
        return m_TunnelEstablished;
    }

    @Override
    protected void beforeSend(ByteBuffer buffer, boolean isPartial) throws Exception {

        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);

        byte[] newbytes = m_Encryptor.encrypt(bytes);

        buffer.clear();
        buffer.put(newbytes);
        buffer.flip();
        
        TrafficSessionManager.EncryptedBytesReceived += newbytes.length;
    }

    @Override
    protected void afterReceived(ByteBuffer buffer) throws Exception {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        
        byte[] newbytes = m_Encryptor.decrypt(bytes);
        
        buffer.clear();
        buffer.put(newbytes);
        buffer.flip();
        
        TrafficSessionManager.EncryptedBytesSent += newbytes.length;
    }

    @Override
    protected void onDispose() {
        m_Config = null;
        m_Encryptor = null;
    }

}
