package com.example.ieeeconnect.data.remote;

import android.content.Context;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

public class AgoraManager {
    private RtcEngine rtcEngine;

    public void init(Context context, String appId, IRtcEngineEventHandler handler) throws Exception {
        RtcEngineConfig config = new RtcEngineConfig();
        config.mContext = context;
        config.mAppId = appId;
        config.mEventHandler = handler;
        rtcEngine = RtcEngine.create(config);
    }

    public void joinChannel(String token, String channelName, int uid) {
        ChannelMediaOptions options = new ChannelMediaOptions();
        rtcEngine.joinChannel(token, channelName, uid, options);
    }

    public void leaveChannel() {
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
        }
    }

    public void destroy() {
        if (rtcEngine != null) {
            RtcEngine.destroy();
            rtcEngine = null;
        }
    }
}

