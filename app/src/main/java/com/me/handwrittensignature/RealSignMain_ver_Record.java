//package com.me.handwrittensignature;
//
//import android.graphics.Camera;
//import android.media.MediaPlayer;
//import android.media.MediaRecorder;
//import android.os.Bundle;
//import android.os.Environment;
//import android.util.Log;
//import android.view.Surface;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.view.View;
//import android.widget.Button;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//public class RealSignMain_ver_Record extends AppCompatActivity {
//
//    private final static String TAG = "RealSignMain_ver_Record";
//
//    private Button startButton;
//    private Button stopButton;
//    private Button playButton;
//
//
//    private SurfaceView surfaceView;
//    private SurfaceHolder surfaceHolder;
//
//    private MediaRecorder mediaRecorder;
//    private MediaPlayer mediaPlayer;
//    private Camera camera;
//
//    private boolean isRecording = false;
//    private boolean isPlaying = false;
//    private boolean hasVideo = false;
//
//    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.mp4";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.real_sign_record_test);
//
//        startButton = findViewById(R.id.startBtn);
//        stopButton = findViewById(R.id.stopBtn);
//        playButton = findViewById(R.id.playBtn);
//        surfaceView = findViewById(R.id.surfaceView);
//
//        MediaRecorder recorder = new MediaRecorder();
//
//        playButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (isPlaying == false) {
//                    try {
//                        mediaPlayer.setDataSource(path);
//                        if (hasVideo == true) {
//                            mediaPlayer.setDisplay(surfaceHolder);
//                            mediaPlayer.setOnCompletionListener(mListener);
//                        }
//                        mediaPlayer.prepare();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    mediaPlayer.start();
//
//                    isPlaying = true;
//                    startButton.setText("Stop Playing");
//                }
//                else {
//                    mediaPlayer.stop();
//
//                    isPlaying = false;
//                    playButton.setText("Start Playing");
//
//                }
//            }
//        });
//
//        mediaRecorder = new MediaRecorder();
//        mediaPlayer = new MediaPlayer();
//        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//            @Override
//            public void onCompletion(MediaPlayer mp) {
//                Log.d(TAG, "onCompletion");
//                isPlaying = false;
//                playButton.setText("Start Playing");
//            }
//        });
//
//        startButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                hasVideo = true;
//                initVideoRecorder();
//                startVideoRecorder();
//            }
//        });
//
//    }
//
//    MediaPlayer.OnCompletionListener mListener = new MediaPlayer.OnCompletionListener() {
//        @Override
//        public void onCompletion(MediaPlayer mp) {
//            playButton.setText("Start Playing");
//        }
//    };
//
//    void initVideoRecorder() {
//        camera = Camera.open();
//        camera.setDisplayOrientation(90);
//        surfaceHolder = surfaceView.getHolder();
//        surfaceHolder.addCallback(this);
//        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
//
//    }
//
//    void startVideoRecorder() {
//
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//
//        if(mediaPlayer != null) {
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//
//        if(mediaPlayer != null) {
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//    }
//
//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//
//        if (camera == null) {
//            try {
//                camera.setPreviewDisplay(surfaceHolder);
//                camera.startPreview();
//            }catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        camera.stopPreview();
//
//
//
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//
//    }
//}
