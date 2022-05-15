package com.me.handwrittensignature;

import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RealSignService2 extends Service {

    private ServiceHandler handler;
    private Intent pData;

    public RealSignService2() {
        HandlerThread thread = new HandlerThread("handler thread");
        thread.start();

//        MediaProjection p = ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE)).getMediaProjection(-1, pData);
//        handler = new ServiceHandler(this, thread.getLooper(), p);
        handler = new ServiceHandler(this, thread.getLooper());
    }

    private static class ServiceHandler extends Handler {

        private MediaCodec encoder;
        private MediaMuxer muxer;

        private final String mimeType = MediaFormat.MIMETYPE_VIDEO_MPEG4;
        private final String videoUrl = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "Recorded video2.mp4";
        private VirtualDisplay virtualDisplay;
        private MediaProjection mediaProjection;

        private final int frameRate = 30;
        private final int bitRate = 6 * (int) Math.pow(2, 20) * 8;   // 6 mb
        private final int keyframeFreq = 10;   // 2 sec
        private final int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;

        private boolean isRecoding;
        private final RealSignService2 s;

        public ServiceHandler(RealSignService2 service, Looper looper) {
            super(looper);
//            mediaProjection = p;
            s = service;

        }
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                // Start Screen Capturing
                case 0: {

                    // TODO Create the encoder and muxer object
                    try {
                        encoder = MediaCodec.createEncoderByType(mimeType);
                        muxer = new MediaMuxer(videoUrl, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // TODO Set callback to run the encoder in async mode
                    encoder.setCallback(new MediaCodec.Callback() {

                        private String TAG = "encoder_callback";
                        int track;
                        long initialPts;   // initial presentation time

                        @Override
                        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

                        }

                        @Override
                        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                            //TODO if we encounter an end og stream buffer
                            if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                muxer.writeSampleData(track, codec.getOutputBuffer(index), info);
                                // release the muxer
                                muxer.stop();
                                muxer.release();

                                // release the encoder
                                encoder.stop();
                                encoder.release();

                                isRecoding = false;

                                return;
                            }

                            if (info.presentationTimeUs != 0) {
                                if (initialPts == 0) {
                                    initialPts = info.presentationTimeUs;
                                    info.presentationTimeUs = 100;   // micro seconds
                                } else {
                                    info.presentationTimeUs -= initialPts;
                                }
                            }

                            Log.d(TAG, "onOutputBufferAvailable : pts = " + info.presentationTimeUs);

                            ByteBuffer encodeBuffer = codec.getOutputBuffer(index);

                            // write this buffer data to muxer
                            muxer.writeSampleData(track, encodeBuffer, info);

                            // release this buffer
                            codec.releaseOutputBuffer(index, false);
                        }

                        @Override
                        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

                        }

                        @Override
                        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                            track = muxer.addTrack(format);
                            muxer.start();
                        }
                    });

                    // TODO Configure the encoder
                    DisplayMetrics m = Resources.getSystem().getDisplayMetrics();
                    MediaFormat videoFormat = MediaFormat.createVideoFormat(mimeType, m.widthPixels, m.heightPixels);
                    videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, frameRate);
                    videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                    videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, keyframeFreq);
                    videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);

                    encoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    Surface surface = encoder.createInputSurface();
//                    encoder.configure(videoFormat, surface, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

                    encoder.start();

                    mediaProjection = ((MediaProjectionManager) s.getSystemService(MEDIA_PROJECTION_SERVICE)).getMediaProjection(-1, s.pData);
                    assert mediaProjection != null;

                    virtualDisplay = mediaProjection.createVirtualDisplay("virtual display", m.widthPixels, m.heightPixels
                            , m.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null);

                } break;
                // Stop Screen Capturing
                case 1: {
                    encoder.signalEndOfInputStream();

                    while (isRecoding) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    mediaProjection.stop();
                    virtualDisplay.release();

                    // stop the service
                    s.stopSelf();

                } break;
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

//        String action = intent.getAction();
        switch (intent.getAction()) {
            case "start": {
                pData = intent.getParcelableExtra("con.ns.pData");
                Message m = handler.obtainMessage();
                m.what = 0;
                handler.sendMessage(m);
            } break;

            case "stop": {
                Message m = handler.obtainMessage();
                m.what = 1;
                handler.sendMessage(m);
            }
        }

//        return START_STICKY;
        return START_NOT_STICKY;
//        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
