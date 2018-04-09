package com.vm.shadowsocks.util;

public class PacketEntry {
    public String content;
    public long time;
    public boolean isRequest;
    
    public PacketEntry(String content, long time, boolean isRequest) {
        this.content = content;
        this.time = time;
        this.isRequest = isRequest;
    }
}
