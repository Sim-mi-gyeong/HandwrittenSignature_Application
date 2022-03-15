package com.me.handwrittensignature;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.ToneGenerator;
import android.media.projection.MediaProjection;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

class RecordingSession
        implements MediaScannerConnection.OnScanCompletedListener {
    static final int VIRT_DISPLAY_FLAGS=
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY |
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private RecordingConfig config;
    private final File output;
    private final Context ctxt;
    private final ToneGenerator beeper;
    private MediaRecorder recorder;
    private MediaProjection projection;
    private VirtualDisplay vdisplay;

    RecordingSession(Context ctxt, RecordingConfig config,
                     MediaProjection projection) {
        this.ctxt=ctxt.getApplicationContext();
        this.config=config;
        this.projection=projection;
        this.beeper=new ToneGenerator(
                AudioManager.STREAM_NOTIFICATION, 100);

        output=new File(ctxt.getExternalFilesDir(null), "andcorder.mp4");
        output.getParentFile().mkdirs();
    }

    void start() {
        recorder=new MediaRecorder();
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setVideoFrameRate(config.frameRate);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        recorder.setVideoSize(config.width, config.height);
        recorder.setVideoEncodingBitRate(config.bitRate);
        recorder.setOutputFile(output.getAbsolutePath());

        try {
            recorder.prepare();
            vdisplay=projection.createVirtualDisplay("andcorder",
                    config.width, config.height, config.density,
                    VIRT_DISPLAY_FLAGS, recorder.getSurface(), null, null);
            beeper.startTone(ToneGenerator.TONE_PROP_ACK);
            recorder.start();
        }
        catch (IOException e) {
            throw new RuntimeException("Exception preparing recorder", e);
        }
    }

    void stop() {
        projection.stop();
        recorder.stop();
        recorder.release();
        vdisplay.release();

        MediaScannerConnection.scanFile(ctxt,
                new String[]{output.getAbsolutePath()}, null, this);
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        beeper.startTone(ToneGenerator.TONE_PROP_NACK);
    }
}