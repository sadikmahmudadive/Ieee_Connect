package com.example.ieeeconnect;

import android.app.Application;

import com.example.ieeeconnect.data.remote.CloudinaryManager;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CloudinaryManager.init(this);
    }
}
