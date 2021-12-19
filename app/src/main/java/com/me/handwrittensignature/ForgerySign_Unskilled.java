package com.me.handwrittensignature;
// ForgerySign_Unskilled
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;

public class ForgerySign_Unskilled extends AppCompatActivity {

    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 5;   // 실제 서명으로 등록할 횟수
    public static String name;

    // 타이머 관련 변수
//    private TextView timerText;
//    private int timeLimit = 10;   // 제한 시간 설정
//    private int status = 0;   // o: 종료/초기화(기록 시작 전 상태, 기록 시작 -> 초기화 상태) , 1: 시작(기록 시작 후 상태) , 2: 일시 정지(기록 시작 -> 기록 저장 상태)

    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unskilled_forgery_sign);

        Button loadButton = (Button)findViewById(R.id.loadButton);
        Button startButton = (Button)findViewById(R.id.button_start);
        Button saveButton = (Button)findViewById(R.id.button_save);
        Button clearButton = (Button)findViewById(R.id.button_clear);

        TextView countText = (TextView)findViewById(R.id.countText);
        TextView finishText = (TextView)findViewById(R.id.finishText);
        TextView timerText = (TextView)findViewById(R.id.timerText);

        iv = findViewById(R.id.image1);

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        String name = intent.getStringExtra("text");

        // 타이머를 위한 핸들러 인스턴스 변수
//        RealSign.TimerHandler timer = new ForgerySign_Unskilled.TimerHandler();

//        saveButton.setEnabled(false)
//        clearButton.setEnabled(false);

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

                final String rootPath = "/storage/self/primary/Pictures/Signature/";
                File directory = new File(rootPath);
                File[] files = directory.listFiles();
                List<String> filesDirList = new ArrayList<>();

                for (int i=0; i< files.length; i++) {
                    filesDirList.add(files[i].getName());
                }
                // 위조할 타겟 대상의 디렉토리 선택
                double randomValue = Math.random();
                int ran1 = (int)(randomValue * filesDirList.size()) -1;
                String targetName = filesDirList.get(ran1);

                // 위조할 타켓 대상의 디렉토리 내 서명 선택
                final String targetPath = "/storage/self/primary/Pictures/Signature/" + targetName;
                File fileDirectory = new File(targetPath);
                File[] targetFiles = fileDirectory.listFiles();
                List<String> filesList = new ArrayList<>();

                for (int i=0; i< files.length; i++) {
                    filesList.add(targetFiles[i].getName());
                }

                int ran2 = (int)(randomValue * filesList.size()) -1;
                String targetFile = filesList.get(ran2);

                FileInputStream fis;   // 없어도 되는가?

                try{
                    String loadImgPath = targetPath + targetFile;
                    Bitmap bm = BitmapFactory.decodeFile(loadImgPath);
                    iv.setImageBitmap(bm);
                    Toast.makeText(getApplicationContext(), "이미지 로드 성공", Toast.LENGTH_SHORT).show();
                }
                catch(Exception e){
                    Toast.makeText(getApplicationContext(), "이미지 로드 실패", Toast.LENGTH_SHORT).show();
                }

//                // Cloud Storage 연결 설정 테스트!!
//                //firebaseStorage 인스턴스 생성
//                //하나의 Storage와 연동되어 있는 경우, getInstance()의 파라미터는 공백으로 두어도 됨
//                //하나의 앱이 두개 이상의 Storage와 연동이 되어있 경우, 원하는 저장소의 스킴을 입력
//                //getInstance()의 파라미터는 firebase console에서 확인 가능('gs:// ... ')
//                FirebaseStorage storage = FirebaseStorage.getInstance();
//
//                //생성된 FirebaseStorage를 참조하는 storage 생성
//                StorageReference storageRef = storage.getReference();
//
//                //Storage 내부의 images 폴더 안의 image.jpg 파일명을 가리키는 참조 생성
//                StorageReference pathReference = storageRef.child("images/image.jpg");
//
//                pathReference = storageRef.child("dog.jpg");
//
//                if (pathReference != null) {
//                    // 참조 객체로부터 이미지 다운로드 url을 얻어오기
//                    pathReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
//                        @Override
//                        public void onSuccess(Uri uri) {
//                            // 다운로드 URL이 파라미터로 전달되어 옴.
//                            Glide.with(ForgerySign_Unskilled.this).load(uri).into(iv);
//
//                        }
//                    }).addOnFailureListener(new OnFailureListener() {
//                        @Override
//                        public void onFailure(@NonNull Exception e) {
//                            Toast.makeText(getApplicationContext(), "다운로드 실패", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//
//                }
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

//                // 시작 버튼 클릭 시 CountDown Timer 실행   ->   어플 종료되는 현상 발생
//                final Timer ssmmss = new Timer();
//                final Handler timerhandler = new Handler() {
//                    public void handleMessage(Message msg) {
////                        timeLimit = 10;
//                        timeLimit --;
//                        if (timeLimit == 0) {
//                            ssmmss.cancel();
//                        }
//                        timerText.setText("제한 시간 : " + timeLimit + " 초");
//
//                    }
//                };
//
//                final TimerTask outputtime = new TimerTask() {
//                    @Override
//                    public void run() {
//                        Message msg = timerhandler.obtainMessage() ;
//                        timerhandler.sendMessage(msg);
//                    }
//
//                };
//                ssmmss.schedule(outputtime, 0, 1000);
//
//                if (timeLimit == 0) {
//                    outputtime.cancel();
//                    timerText.setText("제한 시간 : " + timeLimit + " 초");
//                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 사용자 이름 + autoIncre + 서명 녹화 영상 저장

                countNum += 1;

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(ForgerySign_Unskilled.this, "Signature Saved", Toast.LENGTH_SHORT).show();

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
//                    Toast.makeText(RealSign.this, "Complete Signature Saved", Toast.LENGTH_SHORT).show();
                    startButton.setText("등록 완료");
                    startButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getApplicationContext(), SelectStatus.class);
                            startActivity(intent);
                        }
                    });

                }
                // 타이머 멈추도록 설정(일시 정지 후 초기화)
                // 시작 상태 -> 일시 정지(2번) -> sleep -> 초기화(0번)

//                    //write code for saving the signature here
//                Toast.makeText(ForgerySign_Unskilled.this, "Signature Saved", Toast.LENGTH_SHORT).show();
            }

        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.clear();
                saveButton.setEnabled(true);
                clearButton.setEnabled(true);

                // 타이머 초기화

            }
        });

    }

}