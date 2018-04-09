package com.vm.shadowsocks.ui;

import android.animation.Animator;
import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.futuremind.recyclerviewfastscroll.FastScroller;
import com.vm.shadowsocks.R;
import com.vm.shadowsocks.core.TrafficSessionManager;
import com.vm.shadowsocks.util.PacketEntry;
import com.vm.shadowsocks.util.SessionContent;
import com.vm.shadowsocks.util.SessionDetailAdapter;
import com.vm.shadowsocks.util.TrafficStatusAdapter;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class SessionDetail extends Activity {
    private View loadingView;
    private RecyclerView packetList;
    private FastScroller fastScroller;
    
    private SessionDetailAdapter adapter;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_session_detail);
        
        // 设置此数据流的概述
        Bundle bundle = this.getIntent().getExtras();
        int port = bundle.getInt("port");
        final SessionContent content = TrafficSessionManager.getByPort(port);
        
        TextView textView = findViewById(R.id.session_overview);
        String overview = content.getDomain() + ":" + content.getLocalPort() + "\n"
                + "RX: " + content.getBytesReceived() + ", TX: " + content.getBytesSent();
        textView.setText(overview);
    
        // 左上添加返回图标
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
        
        loadingView = findViewById(R.id.loading);
        packetList = findViewById(R.id.packet_list);
        fastScroller = findViewById(R.id.scroller);
        
        // 设置ITEM的展示方式
        packetList.setLayoutManager(new LinearLayoutManager(this, LinearLayout.VERTICAL, false));
        packetList.setItemAnimator(new DefaultItemAnimator());
    
        Observable<List<SessionContent>> observable = Observable.create(new ObservableOnSubscribe<List<SessionContent>>() {
            @Override
            public void subscribe(ObservableEmitter<List<SessionContent>> emitter) throws Exception {
                adapter = new SessionDetailAdapter(content);
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    
        Observer<List<SessionContent>> observer = new Observer<List<SessionContent>>() {
            @Override
            public void onSubscribe(Disposable d) { }
        
            @Override
            public void onNext(List<SessionContent> value) { }
        
            @Override
            public void onError(Throwable e) { }
        
            @Override
            public void onComplete() {
                packetList.setAdapter(adapter);
                fastScroller.setRecyclerView(packetList);
            
            
                long animTime = 1;
            
                packetList.setAlpha(0);
                packetList.setVisibility(View.VISIBLE);
                packetList.animate().alpha(1).setDuration(animTime);
            
                loadingView.animate().alpha(0).setDuration(animTime).setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) { }
                
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        loadingView.setVisibility(View.GONE);
                    }
                
                    @Override
                    public void onAnimationCancel(Animator animation) { }
                
                    @Override
                    public void onAnimationRepeat(Animator animation) { }
                });
            }
        };
    
        observable.subscribe(observer);
    
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
