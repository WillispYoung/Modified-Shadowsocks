package com.vm.shadowsocks.ui;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;

import com.futuremind.recyclerviewfastscroll.FastScroller;
import com.vm.shadowsocks.R;
import com.vm.shadowsocks.util.AppInfo;
import com.vm.shadowsocks.core.AppProxyManager;
import com.vm.shadowsocks.util.AppManagerAdapter;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class AppManager extends Activity{
    private View loadingView;
    private RecyclerView appListView;
    private FastScroller fastScroller;
    
    private AppManagerAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.layout_apps);

        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        loadingView = findViewById(R.id.loading);
        appListView = findViewById(R.id.list);
        fastScroller = findViewById(R.id.fastscroller);
        
        appListView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        appListView.setItemAnimator(new DefaultItemAnimator());

        Observable<List<AppInfo>> observable = Observable.create(new ObservableOnSubscribe<List<AppInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<AppInfo>> appInfo) throws Exception {
                queryAppInfo();
                adapter = new AppManagerAdapter();
                appInfo.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        
        Observer<List<AppInfo>> observer = new Observer<List<AppInfo>>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onNext(List<AppInfo> aLong) {}

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onComplete() {
                appListView.setAdapter(adapter);
                fastScroller.setRecyclerView(appListView);
                
                long shortAnimTime = 1;
                
                appListView.setAlpha(0);
                appListView.setVisibility(View.VISIBLE);
                appListView.animate().alpha(1).setDuration(shortAnimTime);
                loadingView.animate().alpha(0).setDuration(shortAnimTime).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animator) {}

                    @Override
                    public void onAnimationEnd(Animator animator) {
                        loadingView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animator) {}

                    @Override
                    public void onAnimationRepeat(Animator animator) {}
                });
            }
        };
        
        observable.subscribe(observer);
    }

    public void queryAppInfo() {
        PackageManager pm = this.getPackageManager(); // 获得PackageManager对象
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);
        Collections.sort(resolveInfos, new ResolveInfo.DisplayNameComparator(pm));
        
        if (AppProxyManager.Instance.mlistAppInfo != null) {
            AppProxyManager.Instance.mlistAppInfo.clear();
            for (ResolveInfo reInfo : resolveInfos) {
                String pkgName = reInfo.activityInfo.packageName; // 获得应用程序的包名
                String appLabel = (String) reInfo.loadLabel(pm); // 获得应用程序的Label
                Drawable icon = reInfo.loadIcon(pm); // 获得应用程序图标
                
                AppInfo appInfo = new AppInfo();
                appInfo.setAppLabel(appLabel);
                appInfo.setPkgName(pkgName);
                appInfo.setAppIcon(icon);
                
                if (!appInfo.getPkgName().equals("com.vm.shadowsocks"))//App本身会强制加入代理列表
                    AppProxyManager.Instance.mlistAppInfo.add(appInfo);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
