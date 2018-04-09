package com.vm.shadowsocks.util;

import java.util.ArrayList;

public class DomainEntry {
    public String head;
    public ArrayList<DomainEntry> tails = new ArrayList<>();
    
    public DomainEntry(String head) { this.head = head;}
    
    public void add(String item) { this.tails.add(new DomainEntry(item));}
    
    public DomainEntry get(String item) {
        for (DomainEntry entry : tails)
            if (entry.head.equals(item))
                return entry;
        return null;
    }
}
