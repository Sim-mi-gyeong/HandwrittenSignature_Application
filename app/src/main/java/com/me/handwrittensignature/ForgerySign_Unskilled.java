package com.me.handwrittensignature;
// ForgerySign_Unskilled
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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;

public class ForgerySign_Unskilled extends AppCompatActivity {

    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 20;   // 실제 서명으로 등록할 횟수
    public static String name;
    private String targetName;
    private String targetFile;
    private String pass_targetName;

    private int checkInit = 0;
    private final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver2/";
    private String strFilePath;
    private String targetSignature;
    private String targetSignaturePath;
    private int unskilledSignatureCnt;
    private int newUnskilledSignatureCnt;
    private String targetSignatureFolderPath;
    private File targetSignatureFolder;
    private String targetPath;

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
        setContentView(R.layout.unskilled_forgery_sign);

        Button loadButton = (Button)findViewById(R.id.loadButton);
        Button startButton = (Button)findViewById(R.id.button_start);
        Button saveButton = (Button)findViewById(R.id.button_save);
        Button clearButton = (Button)findViewById(R.id.button_clear);

        TextView countText = (TextView)findViewById(R.id.countText);
        TextView finishText = (TextView)findViewById(R.id.finishText);
        timerText = (TextView)findViewById(R.id.timerText);

        iv = findViewById(R.id.image1);

        timerText.setText("제한시간 : " + timeLimit + " 초");

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        name = intent.getStringExtra("text");

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
                loadButton.setEnabled(false);

                try {
                    loadTargetSignature();
                    File storageDir = new File(targetSignaturePath);   // 위조할 대상의 위조할 서명 디렉토리 path
                    String loadImgName = targetFile;   // 위조할 대상의 위조할 서명 디렉토리 path 내 보여줄 이미지(-1번째)
                    File file = new File(storageDir, loadImgName);
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                    iv.setImageBitmap(bitmap);

                } catch (Exception e) {
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
                startButton.setVisibility(View.GONE);
                clearButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);

                startButton.setEnabled(false);

                createSignatureDir();

                startTimerTask();
                iterableCaptureView();

            }

        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                captureView(signaturePad);

                countNum += 1;

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(ForgerySign_Unskilled.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                iterableCaptureViewSave();

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
            @Override
            public void onClick(View v) {
                signaturePad.clear();
                saveButton.setEnabled(true);
                clearButton.setEnabled(true);

                checkInit = 1;
                /**
                 * clearButton 클릭 이벤트 발생 시 이미지 캡처 - init 표시 후에는 init 표시 제거되도록
                 * clearButton 을 누른 순간 -> initCheck = 0 -> 0.1초(특정 시간) delay 후 initCheck = 1 상태로 돌리기
                 */
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkInit = 0;
                    }
                }, 100);
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
     * 위조 대상의 서명 데이터를 가져올 메서드 - targetFile = 위조 대상의 서명 이미지 경로(String)
     */
    private void loadTargetSignature() {
        File directory = new File(rootPath);
        File[] files = directory.listFiles();   // ~/Signature_ver2 디렉토리 내 파일 목록
        List<String> filesDirList = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            filesDirList.add(files[i].getName());
        }

        filesDirList.remove(name);   // 본인의 디렉토리(서명은)는 위조 대상에서 제외

        // 위조할 타겟 대상의 디렉토리 랜덤 선택
        int idx1 = new Random().nextInt(filesDirList.size());
        targetName = filesDirList.get(idx1);

        // TODO 위조할 타켓 대상의 디렉토리 내 서명 선택 - 각 서명 디렉토리 이름 중 unskilled or skilled 문자열 미포함 디렉토리 선택
        targetPath = rootPath + targetName;
        File targetPathFiles = new File(targetPath);
        File[] targetPathFileList = targetPathFiles.listFiles();   // 위조할 타켓 대상 디렉토리 내의 목록
        List<String> targetPathFolderList = new ArrayList<>();

        for (int i = 0; i < targetPathFileList.length; i++) {
            targetPathFolderList.add(targetPathFileList[i].getName());
        }

        for (int i = 0; i < targetPathFolderList.size(); i++) {
            if (targetPathFolderList.get(i).contains("unskilled") || targetPathFolderList.get(i).contains("skilled")) {
                targetPathFolderList.remove(targetPathFolderList.get(i));   // 위조 대상의 실제 서명 디렉토리들만 남기기
            }
        }

        // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택
        int idx2 = new Random().nextInt(targetPathFolderList.size());
        targetSignature = targetPathFolderList.get(idx2);
        targetSignaturePath = targetPath + "/" + targetSignature;

        // 위조할 서명 프레임들이 저장된 디렉토리 내에서 가장 마지막에서 두 번째 이미지 선택
        File targetSignatureFiles = new File(targetSignaturePath);
        File[] targetSignatureFrame = targetSignatureFiles.listFiles();
        List<String> targetSignatureFrameList = new ArrayList<>();

        for (int i = 0; i < targetSignatureFrame.length; i++) {
            targetSignatureFrameList.add(targetSignatureFrame[i].getName());
        }
//        targetFile = targetSignatureFrameList.get(-1);   // Java 는 이렇게 안 쓴다,, Python 만 이렇게 쓴다,,
        targetFile = targetSignatureFrameList.get(targetSignatureFrameList.size()-1);

    }


    /**
     * 각 서명 하나의 프레임들이 저장될 디렉토리 생성 메서드
     */
    private void createSignatureDir() {
        //TODO targetName 디렉토리 내에, unskilled 가 붙은 서명 디렉토리 개수 + 1 로 새로운 디렉토리 생성
        File unskilledSignatureDir = new File(targetPath);
        File[] files = unskilledSignatureDir.listFiles();
        unskilledSignatureCnt = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().contains("unskilled")) {   // skilled 위조 서명의 경우, 이름에 skilled 포함 여부 체크 + unskilled 는 포함되지 않도록 => 2개 조건 검사
                unskilledSignatureCnt++;
            }
        }
        newUnskilledSignatureCnt = unskilledSignatureCnt + 1;
        targetSignatureFolderPath = targetPath + "/" + targetName + "_unskilled_forgery_" + String.valueOf(newUnskilledSignatureCnt);
        targetSignatureFolder = new File(targetSignatureFolderPath);
        try {
            targetSignatureFolder.mkdir();
            Toast.makeText(getApplicationContext(), "위조 서명 폴더 생성", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
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