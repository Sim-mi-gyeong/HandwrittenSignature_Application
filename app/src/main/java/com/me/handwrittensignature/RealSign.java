package com.me.handwrittensignature;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.gcacace.signaturepad.views.SignaturePad;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;

public class RealSign extends AppCompatActivity {

    public static Context context;

    public EditText nameText;
    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 20;   // 실제 서명으로 등록할 횟수
    private String name;
    private TextView nameView;
    private Uri filePath;

    // 타이머 관련 변수
    TimerTask timerTask;
    Timer timer = new Timer();
    private int timeLimit = 10;   // 제한 시간 설정
    TextView timerText;

    /**
     * 화면 녹화 관련 static object
     */
    private static final int REQUEST_CODE = 100;
    private static String STORE_DIRECTORY;   // 기존 Signature 폴더 위치에 SignatureVideo 폴더 생성해서 저장
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

    /**
     * 화면 녹화 관련 member object
     */
    private static final int REQUEST_SCREENCAST=59706;
    private static MediaProjectionManager mpm;
    private MediaProjection mediaProjection;
    private int resultCode;
    private Intent resultData;

    private ImageReader mImageReader;
    private ImageReader imageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
//    private OrientationChangeCallback mOrientationChangeCallback;
//    private MediaProjectionStopCallback sMediaProjectionStopCallback;

    private static final String videoFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/MediaProjection.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.real_sign);

//        context = this;

        Button startButton = (Button)findViewById(R.id.button_start);
        Button saveButton = (Button)findViewById(R.id.button_save);
        Button clearButton = (Button)findViewById(R.id.button_clear);

        TextView countText = (TextView)findViewById(R.id.countText);
        TextView finishText = (TextView)findViewById(R.id.finishText);
        timerText = (TextView)findViewById(R.id.timerText);
        TextView nameView = (TextView)findViewById(R.id.nameView);

        timerText.setText("제한시간 : " + timeLimit + " 초");

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        String name = intent.getStringExtra("text");

        nameView.setText(name);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MODE_PRIVATE);

        /**
         * 1단계 : MediaProjectionManager 는 getSystemService 를 통해 service 를 생성하고
         *     -> 사용자에게 권한 요구
         */
        // TODO 서비스 받아오기 -> 멤버변수 MediaProjectionManager 로 들어감
        // TODO 실제 권한을 사용자에게 통보하고 권한 요구
//        startProjection();
//        mpm = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
//        Intent passIntent = mpm.createScreenCaptureIntent();
//        passIntent.putExtra("resultCode", -1);
//        passIntent.putExtra("data", passIntent);


        mpm = (MediaProjectionManager)getApplicationContext()
                .getSystemService(MEDIA_PROJECTION_SERVICE);
        if (mpm != null) {
            Intent captureIntent = mpm.createScreenCaptureIntent();
//            startActivityForResult(captureIntent, REQUEST_CODE);
//            activityResult.launch(captureIntent);
//            startActivity(captureIntent);
//            registerForActivityResult(new ActivityResultContracts.StartActivityForResult()).launch(captureIntent);
        }

//            Intent intent2 = new Intent(this, RecorderService.class);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(intent2);
//            } else {
//                startService(intent2);
//            }

//        new Thread() {
//            @Override
//            public void run() {
//                Looper.prepare();
//                mHandler = new Handler();
//                Looper.loop();
//            }
//        }.start();

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {

            @Override
            public void onStartSigning() {

            }

            @Override
            public void onSigned() {
                clearButton.setEnabled(true);
                saveButton.setEnabled(true);
            }

            @Override
            public void onClear() {
                clearButton.setEnabled(false);
                saveButton.setEnabled(false);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                signaturePad.setEnabled(true);   // 서명 패드 활성화

                startButton.setVisibility(View.GONE);   // 시작 버튼 숨기기
                clearButton.setVisibility(View.VISIBLE);   // 초기화 버튼 나타나게
                saveButton.setVisibility(View.VISIBLE);   // 저장 버튼 나타나게
                startButton.setEnabled(false);   // 시작 버튼 비활성화

                startTimerTask();

                ((RecorderService)RecorderService.recorderServiceContext).screenRecorder(Activity.RESULT_OK, resultData);

//                startProjection();
//                onActivityResult(REQUEST_CODE, Activity.RESULT_OK, mProjectionManager.createScreenCaptureIntent());

            }
        });

        // 기록 시작 버튼 누르고 -> 저장버튼 누르면 해당 영역 캡처 사진 저장
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                captureView(signaturePad);
                ((RecorderService)RecorderService.recorderServiceContext).screenRecorder(Activity.RESULT_OK, resultData);

                countNum += 1;   // 파일 이름은 name + '_' + countNum

                countText.setText((countNum + "/" + countComplete).toString());
//                Toast.makeText(RealSign.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                // 기록 저장 후에도 초기화 실행
                signaturePad.clear();

                signaturePad.setEnabled(false);

                sleep(1000);

                // 타이머 세팅
                saveStopTimerTask();
                timerText.setText("제한시간 : " + timeLimit + " 초");

                startButton.setVisibility(View.VISIBLE);
                clearButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);

                startButton.setEnabled(true);

                if(countNum == countComplete) {
                    finishText.setVisibility(View.VISIBLE);
                    startButton.setText("등록 완료");
                    startButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // 일정 횟수 채울 시작 버튼 -> 완료 버튼 -> 위조 서명 중 skilled/unskilled 선택 페이지로
                            Intent intent2 = new Intent(getApplicationContext(), SelectStatus.class);
                            startActivity(intent2);
                        }
                    });
                }

            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.clear();
                saveButton.setEnabled(true);
                clearButton.setEnabled(true);

                // TODO 초기화 시 이전 녹화 영상을 저장하지 않고 다시 녹화 시작
                ((RecorderService)RecorderService.recorderServiceContext).screenRecorder(Activity.RESULT_OK, resultData);
//                sMediaProjectionStopCallback.onStop();
//                onActivityResult(REQUEST_CODE, Activity.RESULT_OK, mProjectionManager.createScreenCaptureIntent());
            }
        });

    }


    /**
     * 서명 기록 시작 / 초기화 / 저장 / 제한 시간 종료 시 타이머 설정 메서드
     */
    private void startTimerTask() {
        stopTimerTask();
        timerTask = new TimerTask() {

            @Override
            public void run() {
                timeLimit --;
                timerText.post(new Runnable() {
                    @Override
                    public void run() {
                        if (timeLimit == 0) {
                            timerTask.cancel();
                            signaturePad.setEnabled(false);
                            Toast.makeText(getApplicationContext(), "제한시간 종료", Toast.LENGTH_SHORT).show();
                        }
                        timerText.setText("제한시간 : " + timeLimit + " 초");
                    }
                });

            }
        };
        timer.schedule(timerTask, 0, 1000);

    }

    private void stopTimerTask() {
        if (timerTask != null) {
            timeLimit = 10;
            timerText.setText("제한시간 : " + timeLimit + " 초");
            timerTask.cancel();
            timerTask = null;
        }

    }
    // 저장 버튼 클릭했을 때 남은 시간에서 멈추고 -> 타이머 다시 시작
    private void saveStopTimerTask() {
        if (timerTask != null) {
            timerText.setText("제한시간 : " + timeLimit + " 초");
            timerTask.cancel();
            timerTask = null;
            timeLimit = 10;
        }

    }

//    private void startProjection() {
//        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
//        Intent passIntent = mpm.createScreenCaptureIntent();
//        passIntent.putExtra("resultCode", -1);
//        passIntent.putExtra("data", passIntent);
//
//        if (mpm != null) {
//            startActivityForResult(passIntent, REQUEST_CODE);
//
//            Intent intent = new Intent(this, RecorderService.class);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                startForegroundService(intent);
//            } else {
//                startService(intent);
//            }
//
//        } else {
//            Toast.makeText(RealSign.this, "mpm is null", Toast.LENGTH_SHORT).show();
//        }
//
//    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO 권한 요청 및 임의의 REQUEST_CODE
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==REQUEST_CODE) {
            if (resultCode==RESULT_OK) {
//                Intent i=
//                        new Intent(this, RecorderService.class)
//                                .putExtra(RecorderService.EXTRA_RESULT_CODE, resultCode)
//                                .putExtra(RecorderService.EXTRA_RESULT_INTENT, data);
//
//                startService(i);

//                mediaProjection = data.getParcelableExtra("data");
                resultData = data.getParcelableExtra("data");

//                Intent intent = new Intent(this, RecorderService.class);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    startForegroundService(intent);
//                } else {
//                    startService(intent);
//                }

//                DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
//                mediaProjection.createVirtualDisplay(
//                        "sample",
//                        displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//                        screenRecorder.getSurface(), null, null);
/*
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
                int dpi = displayMetrics.densityDpi;
                int width = displayMetrics.widthPixels;
                int height = displayMetrics.heightPixels;

                // TODO VirtualDisplay 생성
                imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

                VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                        width, height, dpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader.getSurface(), null, null);

                imageReader.setOnImageAvailableListener(new RealSign.ImageAvailableListener(), null);

 */
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
/*
        finish();

        if (requestCode == REQUEST_CODE) {
            Toast.makeText(RealSign.this, "권한 획득 성공", Toast.LENGTH_SHORT).show();

            // 사용자가 권한을 허용해주었는지에 대한 처리
            if (resultCode != RESULT_OK) {
                Toast.makeText(RealSign.this, "권한 획득 실패 상태입니다.", Toast.LENGTH_SHORT).show();
                return;   // 권한을 허용하지 않았을 때
            }

            // resultCode 와 Intent 를 getMediaProjection 에 넘겨주고 -> sMediaProjection 에 들어가는 object 생성
            // 권한 부여 받고는 끝이므로 -> static object 로 넣은 것?
            // TODO 이 부분에서 오류 발생!!!!!!!!
//            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            try {
                mProjection = mpm.getMediaProjection(resultCode, data);

                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
                int dpi = displayMetrics.densityDpi;
                int width = displayMetrics.widthPixels;
                int height = displayMetrics.heightPixels;

                // TODO VirtualDisplay 생성
                imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

                VirtualDisplay virtualDisplay = mProjection.createVirtualDisplay("ScreenCapture",
                        width, height, dpi,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        imageReader.getSurface(), null, null);

                imageReader.setOnImageAvailableListener(new RealSign.ImageAvailableListener(), null);

            } catch (IllegalStateException e) {
                e.printStackTrace();
                Toast.makeText(RealSign.this, "getMediaProjection() 실패", Toast.LENGTH_SHORT).show();
            }

        }
        else {
            Toast.makeText(RealSign.this, "requestCode != REQUEST_CODE 상태입니다.", Toast.LENGTH_SHORT).show();
        }

 */

    }


    /**
     * ImageReader 에서 Image 를 처리할 class
     */
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {

        @Override
        public void onImageAvailable(ImageReader imageReader) {

            Toast.makeText(RealSign.this, "ImageAvailableListener 까지 온 상태", Toast.LENGTH_SHORT).show();

            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
            String name = intent.getStringExtra("text");

            try {
                // 가장 최신 이미지 가져오기
                image = imageReader.acquireLatestImage();
                if (image != null) {

                    Toast.makeText(getApplicationContext(), "이미지 가져오기 성공", Toast.LENGTH_SHORT).show();

                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();   // 이미지 버퍼 정보
                    // 픽셀 + rowStride -> rowPadding = rowStride - pixelStride * mWidth
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // createBitmap() -> bitmap 파일을 만들고 -> 위의 이미지 buffer 로 이미지를 가져옴
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver2/";
                    final String CAPTURE_PATH = name;
                    STORE_DIRECTORY = rootPath + CAPTURE_PATH;
//
//                    signaturePad.destroyDrawingCache();
//                    signaturePad.setDrawingCacheEnabled(true);
//                    signaturePad.buildDrawingCache();
//                    Bitmap bitmap = signaturePad.getDrawingCache();   // Bitmap 가져오기
//
//                    FileOutputStream fos;

                    String strFilePath = STORE_DIRECTORY + "/" + name + '_' + System.currentTimeMillis() + ".png";   // strFilePath: 이미지 저장 경로
                    File fileCacheItem = new File(strFilePath);
                    fos = new FileOutputStream(fileCacheItem, false);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();

                    Toast.makeText(getApplicationContext(), "스크린샷 저장 성공", Toast.LENGTH_SHORT).show();

                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "스크린샷 저장 실패", Toast.LENGTH_SHORT).show();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                if (bitmap != null) {
                    bitmap.recycle();
                }
                if (image != null) {
                    image.close();
                }
            }
        }

    }

    private MediaRecorder createRecorder() {
        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(videoFile);
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        mediaRecorder.setVideoSize(displayMetrics.widthPixels, displayMetrics.heightPixels);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mediaRecorder.setVideoFrameRate(30);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mediaRecorder;
    }

    @Override
    protected void onDestroy() {
        // ?뱁솕以묒씠硫?醫낅즺?섍린
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        super.onDestroy();
    }



}
