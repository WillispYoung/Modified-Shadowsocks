package com.vm.shadowsocks.util;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.vm.shadowsocks.R;

public class PacketItemHolder extends RecyclerView.ViewHolder {
    private static int COLOR_LIGHT_BLUE = Color.parseColor("#99CCFF");
    private static int COLOR_LIGHT_GREEN = Color.parseColor("#CCFFFF");
    
    private TextView item;
    
    public PacketItemHolder(View itemView) {
        super(itemView);
        
        item = itemView.findViewById(R.id.item);
    }
    
    public void bind(PacketEntry entry) {
        item.setText(entry.content);
        if (entry.isRequest)
            item.setBackgroundColor(COLOR_LIGHT_BLUE);
        else
            item.setBackgroundColor(COLOR_LIGHT_GREEN);
    }
}
