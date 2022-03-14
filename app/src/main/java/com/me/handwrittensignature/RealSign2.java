package com.me.handwrittensignature;
// RealSign

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

public class RealSign2 extends AppCompatActivity {
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
    private static int IMAGES_PRODUCED;
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                                                    | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static MediaProjection sMediaProjection;

    /**
     * 화면 녹화 관련 member object
     */
    private MediaProjectionManager mProjectionManager;
    private ImageReader mImageReader;
    private ImageReader imageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private OrientationChangeCallback mOrientationChangeCallback;
    private MediaProjectionStopCallback sMediaProjectionStopCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.real_sign);

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
         * 1단계 : MediaProjectionManager 는 getSystemService 를 통해 service를 생성하고
         *     -> 사용자에게 권한 요구
         */
        // TODO 서비스 받아오기 -> 멤버변수 MediaProjectionManager 로 들어감
//        mProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
          MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//        // TODO 실제 권한을 사용자에게 통보하고 권한 요구
//        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);

        if (mpm != null) {
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_CODE);
        } else {
            Toast.makeText(this, "권한 획득 실패", Toast.LENGTH_SHORT).show();
        }


        /**
         * mHandler 생성
         * 새로운 스레드를 하나 만들고 -> Handler 를 만들어 -> prepare()로 메세지 큐가 준비되면,
         * -> Handler를 만들고 -> loop로 무한정 기다리게 되는 상태
         * -> 해당 스레드를 사용할 때는 성능상 문제가 없도록 구현 필요 - 강제 종료를 시키지 않으면, 메모리를 계속 차지하고 있기 때문
         */
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

//                startProjection();
//                onActivityResult(REQUEST_CODE, Activity.RESULT_OK, mProjectionManager.createScreenCaptureIntent());



            }
        });

        // 기록 시작 버튼 누르고 -> 저장버튼 누르면 해당 영역 캡처 사진 저장
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                captureView(signaturePad);

                countNum += 1;   // 파일 이름은 name + '_' + countNum

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(RealSign2.this, "Signature Saved", Toast.LENGTH_SHORT).show();

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
//                stopProjection();
//                startProjection();
//                sMediaProjectionStopCallback.onStop();
//                onActivityResult(REQUEST_CODE, Activity.RESULT_OK, mProjectionManager.createScreenCaptureIntent());
            }
        });

    }

    @SuppressLint("WrongConstant")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO 권한 요청 및 임의의 REQUEST_CODE
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {

            // 사용자가 권한을 허용해주었는지에 대한 처리
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "권한 획득 실패", Toast.LENGTH_SHORT).show();
                return;   // 권한을 허용하지 않았을 때
            }

            // resultCode 와 Intent 를 getMediaProjection 에 넘겨주고 -> sMediaProjection 에 들어가는 object 생성
            // 권한 부여 받고는 끝이므로 -> static object 로 넣은 것?
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
            int dpi = displayMetrics.densityDpi;
            int width = displayMetrics.widthPixels;
            int height = displayMetrics.heightPixels;

            // TODO VirtualDisplay 생성
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

            VirtualDisplay virtualDisplay = sMediaProjection.createVirtualDisplay("ScreenCapture",
                                    width, height, dpi,
                                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                    imageReader.getSurface(), null, null);


            imageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);


//            if (sMediaProjection != null) {
//                // getMediaProjection 으로 생성된 object 를 저장할 directory 생성
////                STORE_DIRECTORY = Environment.getExternalStorageDirectory() + "/data/capture/test";
//                STORE_DIRECTORY = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver2/";
//
//                File storeDirectory = new File(STORE_DIRECTORY);
//                if (!storeDirectory.exists()) {
//                    boolean success = storeDirectory.mkdirs();
//                    Toast.makeText(getApplicationContext(), "새로운 디렉토리 생성", Toast.LENGTH_SHORT).show();
//                    if (!success) {
//                        return;
//                    }
//                }
/*
                // TODO 현재 디스플레이의 density dpi 가져오기
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                mDisplay = getWindowManager().getDefaultDisplay();

                // TODO createVirtual() 호출 -> virtualDisplay 생성
//                sMediaProjection.createVirtualDisplay();
                createVirtualDisplay();

                // TODO orientation callback 등록 부분 - 감지할 수 있으면, enable()로 감지할 수 있도록
                mOrientationChangeCallback = new OrientationChangeCallback(this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    mOrientationChangeCallback.enable();
                }

                // TODO getMediaProjection 으로 가져온 object 에 register 등록 + Handler
                // -> mHandler 를 여기서 등록하면, null 값을 주어도 됨 ? => developer 참고
                sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
*/

//            }

        }
    }

//    private void startProjection(){
//        // TODO 사용자 허가 요청
//        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
//    }

//    private void stopProjection() {
//        // Projection 종료
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                if (sMediaProjection != null) {
//                    sMediaProjection.stop();
//                }
//            }
//        });
//    }
 /*
    // TODO 가상 디스플레이 생성
    @SuppressLint("WrongConstant")
    private void createVirtualDisplay() {
        // 가로/세로 사이즈 다시 설정
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;

        // TODO ImageReader 새로운 사이즈의 인스턴스 생성 by createVirtualDisplay -> 이미지를 처리할 ImageAvailableListener 등록
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity
                , VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);

        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);

    }

  */

    public void captureView(View View) {
        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        String name = intent.getStringExtra("text");

        // 저장소 영역
//        final String rootPath = "/storage/self/primary/Pictures/Signature/";
        final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature/";
        final String CAPTURE_PATH = name;
//        Toast.makeText(getApplicationContext(), name + "의 새 폴더 생성 시도 ", Toast.LENGTH_SHORT).show();   // name null값 여부 확인
        signaturePad.destroyDrawingCache();
        signaturePad.setDrawingCacheEnabled(true);
        signaturePad.buildDrawingCache();
        Bitmap bitmap = signaturePad.getDrawingCache();   // Bitmap 가져오기

        FileOutputStream fos;

//        String strFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + CAPTURE_PATH;
        String strFolderPath = rootPath + CAPTURE_PATH;

        String strFilePath = strFolderPath + "/" + name + '_' + System.currentTimeMillis() + ".png";   // strFilePath: 이미지 저장 경로
        File fileCacheItem = new File(strFilePath);

        try {
            fos = new FileOutputStream(fileCacheItem, false);
            // 해당 Bitmap 으로 만든 이미지를 png 파일 형태로 만들기
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "스크린샷 저장 실패", Toast.LENGTH_SHORT).show();
        }

    }
    /**
     * MediaProjection API 를 활용한 화면 캡처 녹화
     */



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

    /**
     * ImageReader 에서 Image를 처리할 class
     */
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader imageReader) {
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

            try {
                // 가장 최신 이미지 가져오기
//                image = mImageReader.acquireLatestImage();
                image = imageReader.acquireLatestImage();
                if (image != null) {

                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();   // 이미지 버퍼 정보
                    // 픽셀 + rowStride -> rowPadding = rowStride - pixxelStride * mWidth
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // createBitmap() -> bitmap 파일을 만들고 -> 위의 이미지 buffer로 이미지를 가져옴
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    // 그 다음 저장하는 부분
//                    fos = new FileOutputStream(STORE_DIRECTORY + " /myscreen_" +  IMAGES_PRODUCED + ".jpg");
                    fos = new FileOutputStream(STORE_DIRECTORY + " /myscreen_" +  System.currentTimeMillis() + ".jpg");
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();

//                    IMAGES_PRODUCED++;

                }
            } catch (Exception e) {
                e.printStackTrace();
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

    /**
     * Rotation 시 사용할 클래스 + change 되는 부분이 있는지
     */


    private class OrientationChangeCallback extends OrientationEventListener {
        public OrientationChangeCallback(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            synchronized (this) {   // 화면 전환으로 인해 virtual Display 를 새로 만드는 과정 -> 동기화 필요
                final int rotation = mDisplay.getRotation();
                // rotation 값이 다르다면
                if (rotation != mRotation) {
                    mRotation = rotation;
                    try {
                        // TODO virtual Display 를 release 해주고 -> imageReader 의 이벤트를 빼고,
                        if (mVirtualDisplay != null) mVirtualDisplay.release();
                        if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);

//                        createVirtualDisplay();   // virtualdisplay 새로 생성

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();

                    sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }
}