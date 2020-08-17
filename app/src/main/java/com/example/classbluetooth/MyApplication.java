package com.example.classbluetooth;

import android.app.Application;

import com.sdwfqin.cbt.CbtManager;

public class MyApplication extends Application {
    public MyApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CbtManager
                .getInstance()
                // 初始化
                .init(this)
                // 是否打印相关日志
                .enableLog(true);
    }


}
