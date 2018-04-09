package com.vm.shadowsocks.util;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.vm.shadowsocks.R;
import com.vm.shadowsocks.ui.SessionDetail;
import com.vm.shadowsocks.ui.TrafficStatus;

public class SessionViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private ImageView appIcon;
    private TextView overview;
    private SessionContent content;
    
    public SessionViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
        
        appIcon = itemView.findViewById(R.id.itemicon);
        overview = itemView.findViewById(R.id.session_overview);
    }
    
    public void bind(SessionContent content) {
        this.content = content;
        if (content.getAppIcon() != null)
            appIcon.setImageDrawable(content.getAppIcon());
    
        String data = content.getDomain() + ":" + content.getLocalPort() + "\n"
                + "RX: " + content.getBytesReceived() + " TX: " + content.getBytesSent();
        overview.setText(data);
    }
    
    @Override
    public void onClick(View v) {
        Bundle bundle = new Bundle();
        bundle.putInt("port", content.getLocalPort());
    
        Intent intent = new Intent(TrafficStatus.Instance, SessionDetail.class);
        intent.putExtras(bundle);
        
        TrafficStatus.Instance.startNewActivity(intent);
    }
}
