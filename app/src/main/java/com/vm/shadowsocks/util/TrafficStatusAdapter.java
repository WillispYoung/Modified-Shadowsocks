package com.vm.shadowsocks.util;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;
import com.vm.shadowsocks.R;
import com.vm.shadowsocks.core.TrafficSessionManager;

public class TrafficStatusAdapter extends RecyclerView.Adapter<SessionViewHolder> implements SectionTitleProvider {
    
    @Override
    public SessionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SessionViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_session_item, parent, false));
    }
    
    @Override
    public void onBindViewHolder(SessionViewHolder holder, int position) {
        SessionContent content = TrafficSessionManager.get(position);
        holder.bind(content);
    }
    
    @Override
    public int getItemCount() {
        return TrafficSessionManager.getSize();
    }
    
    @Override
    public String getSectionTitle(int position) {
        SessionContent content = TrafficSessionManager.get(position);
        return content.getDomain();
    }
}
