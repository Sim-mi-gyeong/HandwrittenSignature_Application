package com.me.handwrittensignature;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.github.gcacace.signaturepad.views.SignaturePad;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;

public class RealSign_ver_Record extends AppCompatActivity {

    private static final String TAG = "RealSign_ver_Record";
    private Context mAppContext;
    private Handler mMainHandler;
    private Handler mWorkerHandler;
    private ViewRecorder mViewRecorder;
    private boolean mRecording = false;

    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 20;   // 실제 서명으로 등록할 횟수
    private String name;
    private TextView nameView;

    private final String videoRootPath = Environment.getExternalStorageDirectory() + "/Movies/Signature_ver_Record/";
    private final String imageRootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver_Record/";
    private String userVideoFolderPath, userImageFolderPath;   // 각 사용자의 서명 녹화 영상/이미지 저장 경로
    private String strFilePath;   // 각 사용자의 서명 이미지 저장 경로 + 파일명
    private Bitmap bitmap;

    // 타이머 관련 변수
    TimerTask timerTask;
    Timer timer = new Timer();
    private int timeLimit = 60;   // 제한 시간 설정
    TextView timerText;

    Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.real_sign);

        mAppContext = getApplicationContext();

        Button startButton = findViewById(R.id.button_start);
        Button saveButton = findViewById(R.id.button_save);
        Button clearButton = findViewById(R.id.button_clear);

        TextView countText = findViewById(R.id.countText);
        TextView finishText = findViewById(R.id.finishText);
        timerText = findViewById(R.id.timerText);
        nameView = findViewById(R.id.nameView);

        timerText.setText("제한시간 : " + timeLimit + " 초");

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        name = intent.getStringExtra("text");
        nameView.setText(name);

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

        userVideoFolderPath = videoRootPath + name;
        userImageFolderPath = imageRootPath + name;

        checkPermission();

        mMainHandler = new Handler();
        HandlerThread ht = new HandlerThread("viewRecorder");
        ht.start();
        mWorkerHandler = new Handler(ht.getLooper());

        startButton.setOnClickListener(mRecordOnClickListener);

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

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                signaturePad.setEnabled(true);   // 서명 패드 활성화

                startButton.setVisibility(View.GONE);   // 시작 버튼 숨기기
                clearButton.setVisibility(View.VISIBLE);   // 초기화 버튼 나타나게
                saveButton.setVisibility(View.VISIBLE);   // 저장 버튼 나타나게
                startButton.setEnabled(false);   // 시작 버튼 비활성화

                startTimerTask();

                startRecord();

            }
        });

        // 기록 시작 버튼 누르고 -> 저장 버튼 누르면 해당 영역 캡처 사진 저장
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                captureView(signaturePad);

                stopRecord();

                countNum += 1;   // 파일 이름은 name + '_' + countNum

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(RealSign_ver_Record.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                // 기록 저장 후에도 서명 패드 초기화 실행
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

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                signaturePad.clear();
                saveButton.setEnabled(true);
                clearButton.setEnabled(true);

                // TODO 초기화 시 이전 녹화 영상을 저장하지 않고 다시 녹화 시작
                startRecord();

            }
        });

    }

    public void captureView(View View) {

        // 저장소 영역  ->  위조하는 대상의 디렉토리에 해당 서명 캡처 이미지 저장!!!
        View.destroyDrawingCache();
        View.setDrawingCacheEnabled(true);
        View.buildDrawingCache();
        bitmap = View.getDrawingCache();   // Bitmap 가져오기

        FileOutputStream fos;

        strFilePath = userImageFolderPath + "/" + name + "_" + System.currentTimeMillis() + ".png";

        File fileCacheItem = new File(strFilePath);

        try {
            fos = new FileOutputStream(fileCacheItem, false);
            bitmap.createBitmap(View.getWidth(), View.getHeight(), Bitmap.Config.ARGB_8888);
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
            timeLimit = 60;
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
            timeLimit = 60;
        }

    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "권한 체크 필요", Toast.LENGTH_SHORT).show();

                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 2);

            } else {

            }
        }

    }

    private final View.OnClickListener mRecordOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mViewRecorder.setRecordedView(signaturePad);
            mWorkerHandler.post(new Runnable() {
                @Override
                public void run() {
                    startRecord();
                }
            });
        }
    };

    private final MediaRecorder.OnErrorListener mOnErrorListener = new MediaRecorder.OnErrorListener() {

        @Override
        public void onError(MediaRecorder mr, int what, int extra) {
            Log.e(TAG, "MediaRecorder Error: Type = " + what + ", Code = " + extra);
            mViewRecorder.reset();
            mViewRecorder.release();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if (mRecording) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopRecord();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startRecord() {
        File directory = mAppContext.getExternalCacheDir();
        if (directory != null) {
            directory.mkdirs();
            if (!directory.exists()) {
                Log.w(TAG, "StartRecord Failed: " + directory + " Does Not Exist!");
                return;
            }
        }

        mViewRecorder = new ViewRecorder();
//        mViewRecorder.setAudioSource(MediaRecorder.AudioSource.MIC); // uncomment this line if audio required
        mViewRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mViewRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mViewRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mViewRecorder.setVideoFrameRate(60); // 60fps
        mViewRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mViewRecorder.setVideoSize(720, 1280);
        mViewRecorder.setVideoEncodingBitRate(2000 * 1000);
        mViewRecorder.setOutputFile(userVideoFolderPath + "/" + name + "_" + + System.currentTimeMillis() + ".mp4");
        mViewRecorder.setOnErrorListener(mOnErrorListener);

        mViewRecorder.setRecordedView(signaturePad);

        try {
            mViewRecorder.prepare();
            mViewRecorder.start();
        } catch (IOException e) {
            Log.e(TAG, "StartRecord Failed", e);
            return;
        }

        Log.d(TAG, "StartRecord Successfully!");
        mRecording = true;
    }

    private void stopRecord() {
        try {
            mViewRecorder.stop();
            mViewRecorder.reset();
            mViewRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mRecording = false;
        Log.d(TAG, "StopRecord Successfully!");
    }
}