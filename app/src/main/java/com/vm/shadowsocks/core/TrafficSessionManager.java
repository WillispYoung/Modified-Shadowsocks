package com.vm.shadowsocks.core;

import com.vm.shadowsocks.util.SessionContent;

import java.util.ArrayList;

public class TrafficSessionManager {
    public static long OriginBytesSent;
    public static long OriginBytesReceived;
    public static long OriginHTTPBytesSent;
    public static long OriginHTTPBytesReceived;
    public static long EncryptedBytesSent;
    public static long EncryptedBytesReceived;
    public static long VideoBytesSent;
    public static long VideoBytesReceived;
    
    private static ArrayList<SessionContent> allSessions = new ArrayList<>();
    
    public static SessionContent get(int index) {
        return allSessions.get(index);
    }
    
    public static int getSize() {
        return allSessions.size();
    }
    
    public static void addSession(SessionContent content) {
        allSessions.add(content);
    }
    
    public static void initialization() {
        OriginBytesSent = 0;
        OriginBytesReceived = 0;
        OriginHTTPBytesSent = 0;
        OriginHTTPBytesReceived = 0;
        EncryptedBytesSent = 0;
        EncryptedBytesReceived = 0;
        VideoBytesSent = 0;
        VideoBytesReceived = 0;
        
        allSessions.clear();
    }
    
    public static SessionContent getByPort(int port) {
        for (SessionContent content : allSessions)
            if (content.getLocalPort() == port)
                return content;
        return null;
    }
}
