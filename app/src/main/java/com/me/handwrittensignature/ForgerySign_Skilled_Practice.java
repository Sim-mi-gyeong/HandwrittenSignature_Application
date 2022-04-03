package com.me.handwrittensignature;
// ForgerySign_Skilled

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ForgerySign_Skilled_Practice extends AppCompatActivity {
    private TextView modeText;
    private SignaturePad signaturePad;
    ImageView iv;
    public static String name;
    public static String targetName;
    public static String targetFile;

    private final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver2/";
    private String targetPath;   // 위조할 대상의 디렉토리
    private String targetSignature;   // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택된 서명 디렉토리
    private String targetSignaturePath;   // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택된 서명 디렉토리 경로
    private String targetSignatureFolderPath;   // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택된 서명 파일(최종) 경로 => 기록 부분으로 넘겨주기


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.skilled_forgery_sign_practice);

        TextView modeText = (TextView)findViewById(R.id.modeText);
        Button loadButton = (Button)findViewById(R.id.loadButton);
        Button startButton = (Button)findViewById(R.id.button_start);
        Button restartButton = (Button)findViewById(R.id.button_restart);
        Button endButton = (Button)findViewById(R.id.button_end);

        modeText.setVisibility(View.VISIBLE);

        iv = findViewById(R.id.image1);

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        String name = intent.getStringExtra("text");

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {

            @Override
            public void onStartSigning() {

            }

            @Override
            public void onSigned() {
                //Event triggered when the pad is signed
                restartButton.setEnabled(true);
                endButton.setEnabled(true);
            }

            public void onClear() {
                //Event triggered when the pad is cleared
                restartButton.setEnabled(false);
                endButton.setEnabled(false);
            }
        });

/*
//        final String rootPath = "/storage/self/primary/Pictures/Signature/";
        final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature/";
        File directory = new File(rootPath);
        File[] files = directory.listFiles();
        List<String> filesDirList = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            filesDirList.add(files[i].getName());
        }

        filesDirList.remove(name);   // 본인의 디렉토리(서명은)는 위조 대상에서 제외

        // 위조할 타겟 대상의 디렉토리 선택
        int idx1 = new Random().nextInt(filesDirList.size());
        targetName = filesDirList.get(idx1);
        String pass_targetName = targetName;

        // 위조할 타켓 대상의 디렉토리 내 서명 선택
        final String targetPath = rootPath + targetName;
        File fileDirectory = new File(targetPath);
        File[] targetFiles = fileDirectory.listFiles();
        List<String> filesList = new ArrayList<>();

        for (int i=0; i< targetFiles.length; i++) {
            filesList.add(targetFiles[i].getName());
        }

//        int idx2 = new Random().nextInt(filesList.size());
//        String targetFile = filesList.get(idx2);
        targetFile = filesList.get(0);   // 임의의 파일 지정
        String pass_targetFile = targetFile;

 */
        loadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //불러오기 버튼 숨기기
                loadButton.setEnabled(false);

                try {
                    loadTargetSignature();
                    File storageDir = new File(targetSignaturePath);
                    String loadImgName = targetFile;
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
                signaturePad.setEnabled(true);   // 서명 패드 활성화

                loadButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);   // 시작 버튼 숨기기
                restartButton.setVisibility(View.VISIBLE);   // 초기화 버튼 나타나게
                endButton.setVisibility(View.VISIBLE);   // 종료 버튼 나타나게
                startButton.setEnabled(false);   // 시작 버튼 비활성화
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.clear();
                restartButton.setEnabled(true);
                endButton.setEnabled(true);
            }
        });

        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Skilled 연습 페이지 종료 시  skilled 위조 서명  등록 화면으로
                Intent intent2 = new Intent(getApplicationContext(), ForgerySign_Skilled.class);
                intent2.putExtra("text", name);
                intent2.putExtra("targetName", targetName);
                intent2.putExtra("targetSignatureFolderPath", targetSignatureFolderPath);
                startActivity(intent2);
            }
        });

    }
    /**
     * 위조 대상의 서명 데이터를 가져올 메서드 - targetSignaturePath(위조 대상의 서명 이미지 디렉토리) + targetFile(위조 대상의 서명 이미지 이름(String))
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

        targetFile = targetSignatureFrameList.get(targetSignatureFrameList.size()-1);

        targetSignatureFolderPath = targetSignaturePath + "/" + targetFile;

    }

}