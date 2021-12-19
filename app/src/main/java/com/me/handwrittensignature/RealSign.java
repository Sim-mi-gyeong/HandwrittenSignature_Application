package com.me.handwrittensignature;
// RealSign

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;
import static java.nio.channels.Selector.open;

public class RealSign extends AppCompatActivity {
    public EditText nameText;
    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 5;   // 실제 서명으로 등록할 횟수
    private String name;
    private TextView nameView;
    private Uri filePath;

    TimerTask timerTask;
    Timer timer = new Timer();

    // 타이머 관련 변수
//    private TextView timerText;
    private int timeLimit = 10;   // 제한 시간 설정
    private int status = 0;   // o: 종료/초기화(기록 시작 전 상태, 기록 시작 -> 초기화 상태) , 1: 시작(기록 시작 후 상태) , 2: 일시 정지(기록 시작 -> 기록 저장 상태)

//    public TextView timerText;
//    public int timeLimit = 10;
//    public int status = 0;
    static TimerTask tt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.real_sign);

        Button startButton = (Button)findViewById(R.id.button_start);
        Button saveButton = (Button)findViewById(R.id.button_save);
        Button clearButton = (Button)findViewById(R.id.button_clear);

        TextView countText = (TextView)findViewById(R.id.countText);
        TextView finishText = (TextView)findViewById(R.id.finishText);
        TextView timerText = (TextView)findViewById(R.id.timerText);
        TextView nameView = (TextView)findViewById(R.id.nameView);

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        String name = intent.getStringExtra("text");

        nameView.setText(name);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MODE_PRIVATE);

        // 타이머를 위한 핸들러 인스턴스 변수
//        TimerHandler timer = new TimerHandler();

////        final TextView timerText = (TextView)findViewById(R.id.timerText);
//        final Timer ssmmss = new Timer();
//        final Handler timerhandler = new Handler() {
//            public void handleMessage(Message msg) {
//                timeLimit = 10;
//                timeLimit --;
//                timerText.setText("제한 시간 : " + timeLimit + " 초");
//
//            }
//        };
//
//        TimerTask outputtime = new TimerTask() {
//            @Override
//            public void run() {
//                Message msg = timerhandler.obtainMessage() ;
//                timerhandler.sendMessage(msg);
//            }
//
//        };
//        ssmmss.schedule(outputtime, 0, 1000);

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

                // 시작 버튼 클릭 시 CountDown Timer 실행   ->   어플 종료되는 현상 발생
//                if (status == 0) {
//                    status = 1;   // 종료 상태를 -> 시작 상태로
//
//                    timer.sendEmptyMessage(0);
//                }

//                timeLimit = 10;
//                timeLimit --;
//                timerText.setText("제한 시간 : " + timeLimit + " 초");
//                ssmmss.schedule(outputtime, 0, 1000);

//                ssmmss.schedule(outputtime, 0, 1000);

                //        final TextView timerText = (TextView)findViewById(R.id.timerText);
                final Timer ssmmss = new Timer();
                final Handler timerhandler = new Handler() {
                    public void handleMessage(Message msg) {
//                        timeLimit = 10;
                        timeLimit --;
                        if (timeLimit == 0) {
                            ssmmss.cancel();
                        }
                        timerText.setText("제한 시간 : " + timeLimit + " 초");

                    }
                };

                final TimerTask outputtime = new TimerTask() {
                    @Override
                    public void run() {
                        Message msg = timerhandler.obtainMessage() ;
                        timerhandler.sendMessage(msg);
                    }

                };
                ssmmss.schedule(outputtime, 0, 1000);

                if (timeLimit == 0) {
                    outputtime.cancel();
                    timerText.setText("제한 시간 : " + timeLimit + " 초");
                }

            }
        });

        // 기록 시작 버튼 누르고 -> 저장버튼 누르면 해당 영역 챕처 사진 저장
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //write code for saving the signature here

                // 사용자 이름 + auto + 서명 녹화 영상 저장
//                final String rootPath = "/storage/self/primary/Pictures/Signature/";
//                final String CAPTURE_PATH = name;
//                String strFolderPath = rootPath + CAPTURE_PATH;

                captureView(signaturePad);
//                captureActivity((Activity) getApplicationContext());

                countNum += 1;   // 파일 이름은 name + '_' + countNum

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(RealSign.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                // 기록 저장 후에도 초기화 실행
                signaturePad.clear();

                // 또 다시 시작 버튼 누르고 -> 기록 저장 / 초기화 버튼으로 구분할 것인지?
                signaturePad.setEnabled(false);

                sleep(1000);

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

                // 타이머 멈추도록 설정(일시 정지 후 초기화)
                // 시작 상태 -> 일시 정지(2번) -> sleep -> 초기화(0번)

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
        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        String name = intent.getStringExtra("text");
        // 캡쳐가 저장될 외부 저장소
//        final String CAPTURE_PATH = "/CAPTURE_TEST";
//        final String CAPTURE_PATH = '/' + name;

        // 내부 저장소 영역
        final String rootPath = "/storage/self/primary/Pictures/Signature/";
        final String CAPTURE_PATH = name;
//        Toast.makeText(getApplicationContext(), name + "의 새 폴더 생성 시도 ", Toast.LENGTH_SHORT).show();   // name null값 여부 확인
        signaturePad.setDrawingCacheEnabled(true);
        signaturePad.buildDrawingCache();
        Bitmap captureView = signaturePad.getDrawingCache();   // Bitmap 가져오기

        FileOutputStream fos;

//        String strFolderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + CAPTURE_PATH;
        String strFolderPath = rootPath + CAPTURE_PATH;
//        File folder = new File(strFolderPath);
//        if (!folder.exists()) {
//            try{
//                folder.mkdir();   //폴더 생성
//                Toast.makeText(getApplicationContext(), "새 폴더 생성", Toast.LENGTH_SHORT).show();
//            }
//            catch(Exception e){
//                e.getStackTrace();
//            }
//        } else {
//            Toast.makeText(getApplicationContext(), "이미 폴더가 생성되어 있습니다.", Toast.LENGTH_SHORT).show();
//        }

        String strFilePath = strFolderPath + "/" + System.currentTimeMillis() + ".png";   // strFilePath: 이미지 저장 경로
        File fileCacheItem = new File(strFilePath);

        try {
            fos = new FileOutputStream(fileCacheItem);
            // 해당 Bitmap 으로 만든 이미지를 png 파일 형태로 만들기
            captureView.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "스크린샷 저장 실패", Toast.LENGTH_SHORT).show();
        }


    }

}