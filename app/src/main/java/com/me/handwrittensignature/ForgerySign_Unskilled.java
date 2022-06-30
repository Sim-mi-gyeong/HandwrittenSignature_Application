package com.me.handwrittensignature;
// ForgerySign_Unskilled
import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderCodecInfo;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;

public class ForgerySign_Unskilled extends AppCompatActivity {

    private static final String TAG = "RealSign_ver_Record";
    private Context mAppContext;
    private Handler mMainHandler;
    private Handler mWorkerHandler;
    private ViewRecorder mViewRecorder;
    private boolean mRecording = false;

    private TextView modeText;
    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 20;   // 실제 서명으로 등록할 횟수
    public static String name;
    public static String targetName;

    private final String videoRootPath = Environment.getExternalStorageDirectory() + "/Movies/Signature_ver_Record/";
    private final String imageRootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver_Record/";
    private String targetImageDirPath;   // 위조할 대상의 디렉토리
    private String targetImageSignature;   // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택된 서명
    private String targetImageSignaturePath;   // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택된 서명 이미지 경로
    private String targetVideoSignaturePath;   // 위조한 서명 녹화 영상이 저장될 경로

    // 타이머 관련 변수
    TimerTask timerTask;
    Timer timer = new Timer();
    private int timeLimit = 60;   // 제한 시간 설정
    TextView timerText;

    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unskilled_forgery_sign);

        mAppContext = getApplicationContext();

        Button loadButton = findViewById(R.id.loadButton);
        Button startButton = findViewById(R.id.button_start);
        Button saveButton = findViewById(R.id.button_save);
        Button clearButton = findViewById(R.id.button_clear);

        modeText = findViewById(R.id.modeText);
        TextView countText = findViewById(R.id.countText);
        TextView finishText = findViewById(R.id.finishText);
        timerText = findViewById(R.id.timerText);

        modeText.setVisibility(View.VISIBLE);

        iv = findViewById(R.id.image1);
        timerText.setText("제한시간 : " + timeLimit + " 초");

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        name = intent.getStringExtra("text");

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

        checkPermission();

        mMainHandler = new Handler();
        HandlerThread ht = new HandlerThread("bg_view_recorder");
        ht.start();
        mWorkerHandler = new Handler(ht.getLooper());

        startButton.setOnClickListener(mRecordOnClickListener);

        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {

            @Override
            public void onStartSigning() {

            }

            @Override
            public void onSigned() {
                //Event triggered when the pad is signed
                clearButton.setEnabled(true);
                saveButton.setEnabled(true);
            }

            @Override
            public void onClear() {
                //Event triggered when the pad is cleared
                clearButton.setEnabled(false);
                saveButton.setEnabled(false);
            }
        });

        loadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                try {
                    targetImageSignaturePath = loadTargetSignature();
                    File file = new File(targetImageSignaturePath);
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                    iv.setImageBitmap(bitmap);

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "이미지 로드 실패", Toast.LENGTH_SHORT).show();
                }

                targetVideoSignaturePath = videoRootPath + targetName;

                loadButton.setEnabled(false);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                signaturePad.setEnabled(true);

                loadButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                clearButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);

                startButton.setEnabled(false);

                startTimerTask();

                startRecord();

            }

        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopRecord();

                countNum += 1;

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(ForgerySign_Unskilled.this, "Signature Saved", Toast.LENGTH_SHORT).show();

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
                            Intent intent = new Intent(getApplicationContext(), SelectStatus.class);
                            startActivity(intent);
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

    /**
     * 위조 대상의 서명 데이터를 가져올 메서드 - targetFile = 위조 대상의 서명 이미지 경로(String)
     */
    public String loadTargetSignature() {
        File directory = new File(imageRootPath);
        File[] files = directory.listFiles();   // 디렉토리 내 파일 목록
        List<String> imageFilesDirList = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            imageFilesDirList.add(files[i].getName());
        }

        imageFilesDirList.remove(name);   // 본인의 디렉토리(서명은)는 위조 대상에서 제외

        // 위조할 타겟 대상의 디렉토리 랜덤 선택
        int idx1 = new Random().nextInt(imageFilesDirList.size());
        targetName = imageFilesDirList.get(idx1);

        // TODO 위조할 타켓 대상의 디렉토리 내 서명 선택 - 각 서명 디렉토리 이름 중 unskilled or skilled 문자열 미포함 디렉토리 선택
        targetImageDirPath = imageRootPath + targetName;
        File targetImagePathFiles = new File(targetImageDirPath);
        File[] targetImagePathFileList = targetImagePathFiles.listFiles();   // 위조할 타켓 대상 디렉토리 내의 목록
        List<String> targetPathFolderList = new ArrayList<>();

        for (int i = 0; i < targetImagePathFileList.length; i++) {
            targetPathFolderList.add(targetImagePathFileList[i].getName());
        }

        for (int i = 0; i < targetPathFolderList.size(); i++) {
            if (targetPathFolderList.get(i).contains("unskilled") || targetPathFolderList.get(i).contains("skilled") || targetPathFolderList.get(i).contains("mp4") ) {
                targetPathFolderList.remove(targetPathFolderList.get(i));   // 위조 대상의 실제 서명 이미지들만 남기기
            }
        }

        // TODO 위조할 타겟 대상의 디렉토리 내 서명 - unskilled or skilled 문자열 미포함 -> png 파일 중 랜덤 선택
        int idx2 = new Random().nextInt(targetPathFolderList.size());
        targetImageSignature = targetPathFolderList.get(idx2);
        targetImageSignaturePath = targetImageDirPath + "/" + targetImageSignature;

        return targetImageSignaturePath;

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
            Log.e(TAG, "MediaRecorder error: type = " + what + ", code = " + extra);
            mViewRecorder.reset();
            mViewRecorder.release();
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
//        mMainHandler.removeCallbacks(mUpdateTextRunnable);
        if (mRecording) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    stopRecord();
//                    updateRecordButtonText();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        mMainHandler.post(mUpdateTextRunnable);
//        updateRecordButtonText();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mWorkerHandler.getLooper().quit();
    }

    private void startRecord() {
        File directory = mAppContext.getExternalCacheDir();
        if (directory != null) {
            directory.mkdirs();
            if (!directory.exists()) {
                Log.w(TAG, "startRecord failed: " + directory + " does not exist!");
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
        mViewRecorder.setOutputFile(targetVideoSignaturePath  + "/" + targetName + "_unskilled_forgery_" + + System.currentTimeMillis() + ".mp4");
        mViewRecorder.setOnErrorListener(mOnErrorListener);

        mViewRecorder.setRecordedView(signaturePad);

        try {
            mViewRecorder.prepare();
            mViewRecorder.start();
        } catch (IOException e) {
            Log.e(TAG, "startRecord failed", e);
            return;
        }

        Log.d(TAG, "startRecord successfully!");
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
        Log.d(TAG, "stopRecord successfully!");
    }

}