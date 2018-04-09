package com.vm.shadowsocks.tcpip;

import java.util.HashMap;
import java.util.Map;

public class HTTPRequestHeader {
    private String method;
    private String url;
    private String version;
    
    private Map<String, String> entries;
    
    private String requestBody;
    
    public HTTPRequestHeader(String req) throws Exception{
        entries = new HashMap<>();
        
        String[] lines = req.split("\r\n");
        
        if (lines[0].matches(CommonMethods.HttpRequestLineFormat)) {
            String[] items = lines[0].split(" ");
            if (items.length != 3)
                throw new Exception("Data is not in HTTP request format");
            
            method = items[0];
            url = items[1];
            version = items[2];
            
            for (int i = 1;i < lines.length;i ++) {
                if (lines[i].length() == 0)
                    break;
                
                String[] kv = lines[i].split(": ");
                if (kv.length != 2)
                    throw new Exception("Data is not in HTTP request format");
                
                entries.put(kv[0], kv[1]);
            }
            
            int index = req.indexOf("\r\n\r\n") + 5;
            if (index > 0 && index < req.length())
                requestBody = req.substring(index);
        }
        else
            throw new Exception("Data is not in HTTP request format");
    }
    
    public String getValue(String key) {
        return entries.get(key);
    }
    
    public String getUrl() {
        return this.url;
    }
}
