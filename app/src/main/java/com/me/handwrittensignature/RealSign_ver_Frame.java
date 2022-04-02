package com.me.handwrittensignature;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;

public class RealSign_ver_Frame extends AppCompatActivity {
    public EditText nameText;
    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 20;   // 실제 서명으로 등록할 횟수
    private String name;
    private TextView nameView;
    private Uri filePath;

    private int checkInit = 1;
    private final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver2/";
    private String strFilePath;
    private int signatureCnt;
    private int newSignatureCnt;
    private String targetSignatureFolderPath;
    private File signatureFolder;

    // 타이머 관련 변수
    TimerTask timerTask;
    Timer timer = new Timer();
    private int timeLimit = 10;   // 제한 시간 설정
    TextView timerText;

    TimerTask captureTimerTask;
    TimerTask initTimerTask;

    Handler handler = new Handler();

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
        name = intent.getStringExtra("text");

        nameView.setText(name);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MODE_PRIVATE);

        /**
         * 1단계 : MediaProjectionManager 는 getSystemService 를 통해 service를 생성하고
         *     -> 사용자에게 권한 요구
         */

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

                createSignatureDir();   // 해당 서명이 저장될 디렉토리 생성 후

                startTimerTask();
                iterableCaptureView();

            }
        });

        // 기록 시작 버튼 누르고 -> 저장버튼 누르면 해당 영역 캡처 사진 저장
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                captureView(signaturePad);   // 스크린 캡처 메서드 반복 실행

                countNum += 1;   // 파일 이름은 name + '_' + countNum

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(RealSign_ver_Frame.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                iterableCaptureViewSave();

                // 기록 저장 후에도 초기화 실행
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        signaturePad.clear();
                    }
                }, 100);

                signaturePad.setEnabled(false);

                sleep(1000);

                // 타이머 세팅
                saveStopTimerTask();
                timerText.setText("제한시간 : " + timeLimit + " 초");

//                iterableCaptureViewSave();

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
                // 초기화 시 init 표시를 추가해 스크린 캡처
                checkInit = 0;
                /**
                 * clearButton 클릭 이벤트 발생 시 이미지 캡처 - init 표시 후에는 init 표시 제거되도록
                 * clearButton 을 누른 순간 -> initCheck = 0 -> 0.1초(특정 시간) delay 후 initCheck = 1 상태로 돌리기
                 */
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkInit = 1;
                    }
                }, 100);

            }
        });

    }

    public void captureView(View View) {
        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        name = intent.getStringExtra("text");

        // TODO 저장소 영역 : ./name + 서명 저장 개수 => targetSignatureFolderPath + name_System.currentTimeMillis().png

//        final String rootPath = "/storage/self/primary/Pictures/Signature/";
        signaturePad.destroyDrawingCache();
        signaturePad.setDrawingCacheEnabled(true);
        signaturePad.buildDrawingCache();
        Bitmap bitmap = signaturePad.getDrawingCache();   // Bitmap 가져오기

        FileOutputStream fos;

        if (checkInit == 1) {
            strFilePath = targetSignatureFolderPath + "/" + name + "_" + System.currentTimeMillis() + ".png";   // strFilePath: 이미지 저장 경로
        } else {
            strFilePath = targetSignatureFolderPath + "/" + name + "_" + System.currentTimeMillis() + "_init_" + ".png";
        }

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
     * 각 서명 하나의 프레임들이 저장될 디렉토리 생성메서드
     */
    private void createSignatureDir() {
        // TODO 사용자 디렉토리 안에 있는 각 서명 디렉토리 파일 리스트 개수 -> 개수 + 1 로 새로운 디렉토리 생성

//        final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver2/";
        String targetFolderPath = rootPath + name;

        File signatureDir = new File(targetFolderPath);
        File[] files = signatureDir.listFiles();

        // name_signatureCnt + 1 의 이름으로 폴더 생성
        signatureCnt = files.length;
        newSignatureCnt = signatureCnt + 1;
        targetSignatureFolderPath = targetFolderPath + '/' + name + '_' + String.valueOf(newSignatureCnt);
        signatureFolder = new File(targetSignatureFolderPath);
        try {
            signatureFolder.mkdir();   // 서명 한 개의 프레임들이 저장될 폴더 생성
            Toast.makeText(getApplicationContext(), "서명 폴더 생성", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.getStackTrace();
        }

    }

    /**
     * captureView() 메서드를 반복해서 처리할 핸들러 구현
     */
    private void iterableCaptureView() {
        iterableCaptureViewSave();
        captureTimerTask = new TimerTask() {

            @Override
            public void run() {
                signaturePad.post(new Runnable() {
                    @Override
                    public void run() {
                        captureView(signaturePad);
                    }
                });
            }
        };
        timer.schedule(captureTimerTask, 0, 100);
    }
    // 기록 저장 버튼 클릭 시 스크린 캡처 Timer 종료
    private void iterableCaptureViewSave() {
        if (captureTimerTask != null) {
            captureTimerTask.cancel();
            captureTimerTask = null;
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

}
