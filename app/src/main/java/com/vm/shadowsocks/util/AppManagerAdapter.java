package com.vm.shadowsocks.util;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;
import com.vm.shadowsocks.R;
import com.vm.shadowsocks.core.AppProxyManager;

public class AppManagerAdapter extends RecyclerView.Adapter<AppViewHolder> implements SectionTitleProvider {
    
    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AppViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_apps_item, parent, false));
    }
    
    @Override
    public void onBindViewHolder(AppViewHolder holder, int position) {
        AppInfo appInfo = AppProxyManager.Instance.mlistAppInfo.get(position);
        holder.bind(appInfo);
    }
    
    @Override
    public int getItemCount() {
        return AppProxyManager.Instance.mlistAppInfo.size();
    }
    
    @Override
    public String getSectionTitle(int position) {
        AppInfo appInfo = AppProxyManager.Instance.mlistAppInfo.get(position);
        return appInfo.getAppLabel();
    }
}
