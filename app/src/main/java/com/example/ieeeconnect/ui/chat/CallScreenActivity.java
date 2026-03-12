package com.example.ieeeconnect.ui.chat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.ieeeconnect.R;
import com.example.ieeeconnect.databinding.ActivityCallScreenBinding;
import com.example.ieeeconnect.util.StorageImageLoader;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;

public class CallScreenActivity extends AppCompatActivity {

    private static final String TAG = "CallScreenActivity";
    private static final int PERMISSION_REQ_ID = 22;
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
    };

    private ActivityCallScreenBinding binding;
    private RtcEngine mRtcEngine;
    private boolean isMuted = false;
    private boolean isVideoDisabled = false;

    private String channelName;
    private String remoteUserName;
    private String remoteUserImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        channelName = getIntent().getStringExtra("roomId");
        remoteUserName = getIntent().getStringExtra("roomName");
        remoteUserImage = getIntent().getStringExtra("roomImage");

        if (channelName == null) {
            finish();
            return;
        }

        setupUI();

        if (checkSelfPermission()) {
            initAgoraEngineAndJoinChannel();
        } else {
            ActivityCompat.requestPermissions(this, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID);
        }
    }

    private void setupUI() {
        binding.callerName.setText(remoteUserName);
        StorageImageLoader.load(binding.callerImage, remoteUserImage, null, null, R.drawable.ic_profile_placeholder);

        binding.btnEndCall.setOnClickListener(v -> endCall());
        binding.btnMute.setOnClickListener(v -> toggleMute());
        binding.btnSwitchCamera.setOnClickListener(v -> mRtcEngine.switchCamera());
    }

    private boolean checkSelfPermission() {
        return ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, REQUESTED_PERMISSIONS[1]) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAgoraEngineAndJoinChannel();
            } else {
                Toast.makeText(this, "Permissions needed for calling", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void initAgoraEngineAndJoinChannel() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getBaseContext();
            config.mAppId = getString(R.string.agora_app_id);
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            Log.e(TAG, "Agora init failed", e);
            finish();
            return;
        }

        setupVideoConfig();
        setupLocalVideo();
        mRtcEngine.joinChannel(null, channelName, "", 0);
    }

    private void setupVideoConfig() {
        mRtcEngine.enableVideo();
        mRtcEngine.setVideoEncoderConfiguration(new io.agora.rtc2.video.VideoEncoderConfiguration(
                io.agora.rtc2.video.VideoEncoderConfiguration.VD_640x360,
                io.agora.rtc2.video.VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                io.agora.rtc2.video.VideoEncoderConfiguration.STANDARD_BITRATE,
                io.agora.rtc2.video.VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));
    }

    private void setupLocalVideo() {
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        binding.localVideoViewContainer.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            runOnUiThread(() -> {
                binding.callStatus.setText("Connected");
                // Hide profile info once connected to show full video
                binding.callerInfo.setVisibility(View.GONE);
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            runOnUiThread(() -> setupRemoteVideo(uid));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> onRemoteUserLeft());
        }
    };

    private void setupRemoteVideo(int uid) {
        SurfaceView surfaceView = new SurfaceView(getBaseContext());
        binding.remoteVideoViewContainer.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void onRemoteUserLeft() {
        binding.remoteVideoViewContainer.removeAllViews();
        endCall();
    }

    private void toggleMute() {
        isMuted = !isMuted;
        mRtcEngine.muteLocalAudioStream(isMuted);
        binding.btnMute.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
        binding.btnMute.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                isMuted ? getColor(R.color.purple_500) : Color.parseColor("#4DFFFFFF")));
    }

    private void endCall() {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
        }
        RtcEngine.destroy();
        mRtcEngine = null;
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
        }
    }
}
