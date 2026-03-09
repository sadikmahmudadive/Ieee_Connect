package com.example.ieeeconnect;

import android.app.Application;

import com.example.ieeeconnect.data.remote.CloudinaryManager;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CloudinaryManager.init(this);
        
        // Enable Firebase Realtime Database offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
