package com.vm.shadowsocks.util;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;
import com.vm.shadowsocks.R;

public class SessionDetailAdapter extends RecyclerView.Adapter<PacketItemHolder> implements SectionTitleProvider {
    private SessionContent content;
    
    public SessionDetailAdapter(SessionContent content) {
        this.content = content;
    }
    
    @Override
    public PacketItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PacketItemHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_packet_item, parent, false));
    }
    
    @Override
    public void onBindViewHolder(PacketItemHolder holder, int position) {
        PacketEntry entry = content.getContent().get(position);
        holder.bind(entry);
        
    }
    
    @Override
    public int getItemCount() {
        return content.getContent().size();
    }
    
    @Override
    public String getSectionTitle(int position) {
        return null;
    }
}
