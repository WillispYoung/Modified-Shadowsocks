package com.vm.shadowsocks.util;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;

public class SessionContent {
    private ArrayList<PacketEntry> content;
    private String      domain;
    private Drawable    appIcon;
    
    private int       localPort;
    private long        bytesSent;
    private long        bytesReceived;
    
    public SessionContent(ArrayList<PacketEntry> entries, String domain, Drawable appIcon,
                          int localPort, long bytesSent, long bytesReceived) {
        this.content = entries;
        this.domain = domain;
        this.appIcon = appIcon;
        
        this.localPort = localPort;
        this.bytesSent = bytesSent;
        this.bytesReceived=  bytesReceived;
    }
    
    public ArrayList<PacketEntry> getContent() {
        return content;
    }
    
    public String getDomain() {
        return domain;
    }
    
    public Drawable getAppIcon() {
        return appIcon;
    }
    
    public int getLocalPort() {
        return localPort;
    }
    
    public long getBytesSent() {
        return bytesSent;
    }
    
    public long getBytesReceived() {
        return bytesReceived;
    }
}
