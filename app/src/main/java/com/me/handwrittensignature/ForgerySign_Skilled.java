package com.me.handwrittensignature;
// RealSign

//import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;

public class ForgerySign_Skilled extends AppCompatActivity {
    private TextView modeText;
    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 20;   // 실제 서명으로 등록할 횟수
    public static String name;
    public static String targetName;
    public static String targetFile;

    private int checkInit = 0;
    private final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver2/";
    private String strFilePath;
    private String targetSignatureFolderPath;   // intent 로 practice 에서 전달받기


    // 타이머 관련 변수
    TimerTask timerTask;
    Timer timer = new Timer();
    private int timeLimit = 10;   // 제한 시간 설정
    TextView timerText;

    TimerTask captureTimerTask;

    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.skilled_forgery_sign);

        Button loadButton = (Button)findViewById(R.id.loadButton);
        Button startButton = (Button)findViewById(R.id.button_start);
        Button saveButton = (Button)findViewById(R.id.button_save);
        Button clearButton = (Button)findViewById(R.id.button_restart);

        TextView modeText = (TextView)findViewById(R.id.modeText);
        TextView countText = (TextView)findViewById(R.id.countText);
        TextView finishText = (TextView)findViewById(R.id.finishText);
        timerText = (TextView)findViewById(R.id.timerText);

        modeText.setVisibility(View.VISIBLE);

        timerText.setText("제한시간 : " + timeLimit + " 초");
        iv = findViewById(R.id.image1);

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        name = intent.getStringExtra("text");
        targetName = intent.getStringExtra("targetName");
        targetSignatureFolderPath = intent.getStringExtra("targetSignatureFolderPath");

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

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
                //불러오기 버튼 숨기기
                loadButton.setEnabled(false);
                // Practice 모드에서 랜덤으로 불러온 이미지를 띄우기
                try {
//                    File storageDir = new File(targetPath);
//                    String loadImgName = targetFile;
                    File file = new File(targetSignatureFolderPath);
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                    iv.setImageBitmap(bitmap);

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), targetName + "   " + targetFile, Toast.LENGTH_SHORT).show();   // intent로 넘긴 값 확인용
                    Toast.makeText(getApplicationContext(), "이미지 로드 실패", Toast.LENGTH_SHORT).show();
                }

            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.setEnabled(true);

                loadButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                clearButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);

                startButton.setEnabled(false);

                startTimerTask();

            }

        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //write code for saving the signature here
                captureView(signaturePad);

                countNum += 1;

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(ForgerySign_Skilled.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                // 기록 저장 후에도 초기화 실행
                signaturePad.clear();

                // 또 다시 시작 버튼 누르고 -> 기록 저장 / 초기화 버튼으로 구분할 것인지?
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
//                    Toast.makeText(RealSign.this, "Complete Signature Saved", Toast.LENGTH_SHORT).show();
                    startButton.setText("등록 완료");
                    startButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // 일정 횟수 채울 시작 버튼 -> 완료 버튼 -> 위조 서명 중 skilled/unskilled 다시 선택 페이지로
                            Intent intent = new Intent(getApplicationContext(), SelectStatus.class);
                            startActivity(intent);
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
            }
        });

    }

    public void captureView(View View) {
        Intent intent = getIntent();   // 전달한 데이터를 받을 Intent
        name = intent.getStringExtra("text");

        // 저장소 영역  ->  위조하는 대상의 디렉토리에 해당 서명 캡처 이미지 저장!!!
//        final String rootPath = "/storage/self/primary/Pictures/Signature/";
        signaturePad.destroyDrawingCache();
        signaturePad.setDrawingCacheEnabled(true);
        signaturePad.buildDrawingCache();
        Bitmap bitmap = signaturePad.getDrawingCache();   // Bitmap 가져오기

        FileOutputStream fos;

        // TODO 위조 서명이 저장될 경로는, 위조 대상의 디렉토리 내 생성된 unskiiled 표시가 붙은 디렉토리 => targetSignaturePath
        if (checkInit == 0) {
            strFilePath = targetSignatureFolderPath + "/" + targetName + '_' + "unskilled_forgery_" + System.currentTimeMillis() + ".png";   // strFilePath: 이미지 저장 경로
        } else {
            strFilePath = targetSignatureFolderPath + "/" + targetName + '_' + "unskilled_forgery_" + System.currentTimeMillis() + "_init_" + ".png";   // strFilePath: 이미지 저장 경로
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