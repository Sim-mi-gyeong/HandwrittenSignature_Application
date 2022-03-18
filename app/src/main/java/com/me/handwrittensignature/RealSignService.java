package com.me.handwrittensignature;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.me.handwrittensignature.consts.ActivityServiceMessage;
import com.me.handwrittensignature.consts.ExtraIntent;

import java.io.IOException;


public final class RealSignService extends Service {

    private static final int FPS = 30;
    private final String TAG = "ScreenCastService";

    private MediaProjectionManager mpm;
    private Handler handler;
    private Messenger crossProcessMessenger;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaCodec encoder;
    private MediaCodec.BufferInfo videoBufferInfo;


//    private final IBinder mBinder = new LocalBinder();

    class LocalBinder extends Binder {
        RealSignService getService() {
            return RealSignService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                Log.i(TAG, "Handler got message. what : " + msg.what);
                switch (msg.what) {
                    case ActivityServiceMessage.CONNECTED:   // 100
                    case ActivityServiceMessage.DISCONNECTED:
                        break;
                    case ActivityServiceMessage.STOP:
                        stopScreenCapture();
                        stopSelf();
                        break;
                }
                return false;
            }
        });
//        return mBinder;
        crossProcessMessenger = new Messenger(handler);
        return crossProcessMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(getApplicationContext(), "Service Created", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Service Created");

        mpm = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        stopScreenCapture();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            return START_NOT_STICKY;   // 2
        }

        final int resultCode = intent.getIntExtra(ExtraIntent.RESULT_CODE.toString(), -1);
        final Intent resultData = intent.getParcelableExtra(ExtraIntent.RESULT_DATA.toString());

        Log.i(TAG, "resultCode : " + resultCode + "resultData : " + resultData);

        if (resultCode == 0 || resultData == null) {
            return START_NOT_STICKY;
        }

        final String format = intent.getStringExtra(ExtraIntent.VIDEO_FORMAT.toString());
        final int screenWidth = intent.getIntExtra(ExtraIntent.SCREEN_WIDTH.toString(), 640);
        final int screenHeight = intent.getIntExtra(ExtraIntent.SCREEN_HEIGHT.toString(), 360);
        final int screenDpi = intent.getIntExtra(ExtraIntent.SCREEN_DPI.toString(), 96);

        Log.i(TAG, "Start Casting with format : " + format + "screen : " + screenWidth + " x " + screenHeight + " @ " + screenDpi);

        startScreenCapture(resultCode, resultData, format, screenWidth, screenHeight, screenDpi);

        return START_STICKY;
    }

    private void startScreenCapture(int resultCode, Intent resultData, String format, int screenWidth, int screenHeight, int screenDpi) {
        this.mediaProjection = mpm.getMediaProjection(resultCode, resultData);

        Log.d(TAG, "startRecording ... ");

        this.videoBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(format, screenWidth, screenHeight);

        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FPS);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 0);
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);

//        try {
//
//        } catch (IOException e) {
//            Log.e(TAG, "Failed to initial encoder, e : " + e);
//            releaseEncoders();
//        }
    }

    private void stopScreenCapture() {
        releaseEncoders();

        if (virtualDisplay == null) {
            return;
        }
        virtualDisplay.release();
        virtualDisplay = null;
    }

    private void releaseEncoders() {
        if (encoder != null) {
            encoder.stop();
            encoder.release();
            encoder = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        videoBufferInfo = null;
    }
}