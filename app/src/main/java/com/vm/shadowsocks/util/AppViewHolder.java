package com.vm.shadowsocks.util;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;

import com.vm.shadowsocks.R;
import com.vm.shadowsocks.core.AppProxyManager;

public class AppViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private ImageView icon = (ImageView) itemView.findViewById(R.id.itemicon);
    private Switch check = (Switch) itemView.findViewById(R.id.itemcheck);
    private AppInfo item;
    private Boolean proxied = false;
    
    public AppViewHolder(View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
    }
    
    public void bind(AppInfo app) {
        this.item = app;
        proxied = AppProxyManager.Instance.isAppProxy(app.getPkgName());
        icon.setImageDrawable(app.getAppIcon());
        check.setText(app.getAppLabel());
        check.setChecked(proxied);
    }
    
    @Override
    public void onClick(View view) {
        if (proxied) {
            AppProxyManager.Instance.removeProxyApp(item.getPkgName());
            check.setChecked(false);
        } else {
            AppProxyManager.Instance.addProxyApp(item.getPkgName());
            check.setChecked(true);
        }
        proxied = !proxied;
    }
}

