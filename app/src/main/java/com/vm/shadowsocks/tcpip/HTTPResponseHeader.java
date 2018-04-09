package com.vm.shadowsocks.tcpip;

import java.util.HashMap;

public class HTTPResponseHeader {
    private String version;
    private String status;
    private String description;
    
    private HashMap<String, String> entries;
    
    private String body;
    
    public HTTPResponseHeader(String content) throws Exception {
        String[] lines = content.split("\r\n");
        if (lines[0].matches("HTTP/1\\.[01] \\d\\d\\d \\S+")) {
            String[] items = lines[0].split(" ");
            if (items.length != 3)
                throw new Exception("Wrong HTTP response status line");
            version = items[0];
            status = items[1];
            description = items[2];
            
            for (int i = 1;i < lines.length;i ++) {
                if (lines.length == 0)
                    break;
                
                String[] kv = lines[i].split(": ");
                if (kv.length != 2)
                    throw new Exception("Wrong HTTP response header in " + (i+1) + " line");
                entries.put(kv[0], kv[1]);
            }
            
            int index = content.indexOf("\r\n\r\n") + 5;
            if (index > 0 && index < content.length())
                body = content.substring(index);
        }
        else
            throw new Exception("Data is not in HTTP response format");
    }
    
    public String getVersion() { return version; }
    public String getStatus() { return status; }
    public String getDescription() { return description; }
    
    public String getValue(String key) { return entries.get(key); }
}
