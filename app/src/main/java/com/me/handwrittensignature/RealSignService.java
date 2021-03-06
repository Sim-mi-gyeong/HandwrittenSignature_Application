package com.me.handwrittensignature;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.me.handwrittensignature.consts.ActivityServiceMessage;
import com.me.handwrittensignature.consts.ExtraIntent;

import java.io.IOException;
import java.nio.ByteBuffer;


public final class RealSignService extends Service {

    private static final int FPS = 30;
    private final String TAG = "ScreenCastService";

    private MediaProjectionManager mpm;
    private Handler handler;
    private Messenger crossProcessMessenger;

    private MediaProjection mediaProjection;
    private Surface inputSurface;
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
                    case ActivityServiceMessage.DISCONNECTED:   // 101
                        break;
                    case ActivityServiceMessage.STOP:   // 999
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

        Log.i(TAG, "Service onStartCommand() is called");

//        startForeground();

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            startForeground(0, intent, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
//        }

        if (intent == null) {   // ???????????? null ?????? -> ???????????? ??????????????? ?????? ???????????? ??????
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
        final int bitrate = intent.getIntExtra(ExtraIntent.VIDEO_BITRATE.toString(), 1024000);


        Log.i(TAG, "Start Casting with format : " + format + "screen : " + screenWidth + " x " + screenHeight + " @ " + screenDpi +  " bitrate:" + bitrate);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                startScreenCapture(resultCode, resultData, format, screenWidth, screenHeight, screenDpi, bitrate);
            }
        });
        thread.start();

//        startScreenCapture(resultCode, resultData, format, screenWidth, screenHeight, screenDpi, bitrate);

//        return START_STICKY;
        return START_REDELIVER_INTENT;
    }

    private void startScreenCapture(int resultCode, Intent resultData, String format, int screenWidth, int screenHeight, int screenDpi, int bitrate) {
        this.mediaProjection = mpm.getMediaProjection(resultCode, resultData);

        Log.d(TAG, "startRecording ... ");

        this.videoBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(format, screenWidth, screenHeight);

        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FPS);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 0);
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);

        try {

            switch (format) {
                case MediaFormat.MIMETYPE_VIDEO_AVC:
                    // AVC
                    mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);

                    this.encoder = MediaCodec.createEncoderByType(format);
                    this.encoder.setCallback(new MediaCodec.Callback() {
                        @Override
                        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

                        }

                        @Override
                        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int outputBufferId, @NonNull MediaCodec.BufferInfo info) {
                            ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);

                            if (info.size > 0 && outputBuffer != null) {
                                outputBuffer.position(info.offset);
                                outputBuffer.limit(info.offset + info.size);
                                byte[] b = new byte[outputBuffer.remaining()];
                                outputBuffer.get(b);

                                saveData(null, b);
                            }

                            if (encoder != null) {
                                encoder.releaseOutputBuffer(outputBufferId, false);
                            }
                            if ((videoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                Log.i(TAG, "End of Stream");
                                stopScreenCapture();
                            }
                        }

                        @Override
                        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                            Log.i(TAG, "onOutputFormatChanged. CodecInfo : " + codec.getCodecInfo().toString() + " MediaFormat : " + format.toString());
                        }
                    });

                    break;

                default:
                    throw new RuntimeException("Unknown Media Format. You need to add mimetype to string.xml and else if statement");

            }

            this.encoder.configure(mediaFormat
                    , null   // surface
                    , null   // crypto(??????)
                    , MediaCodec.CONFIGURE_FLAG_ENCODE);

            this.inputSurface = this.encoder.createInputSurface();
            this.encoder.start();

        } catch (IOException e) {
            Log.e(TAG, "Failed to initial encoder, e : " + e);
            releaseEncoders();
        }

        this.virtualDisplay = this.mediaProjection.createVirtualDisplay("Recording Display", screenWidth, screenHeight, screenDpi, 0, this.inputSurface, null, null);
    }

    // TODO ????????? ?????? ?????????
   private void saveData(byte[] header, byte[] data) {

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
        if (inputSurface != null) {
            inputSurface.release();
            inputSurface = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        videoBufferInfo = null;
    }
}