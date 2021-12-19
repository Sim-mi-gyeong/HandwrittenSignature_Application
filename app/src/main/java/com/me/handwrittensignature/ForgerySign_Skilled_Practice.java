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
    private String pass_targetName;
    private String pass_targetFile;

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

        final String rootPath = "/storage/self/primary/Pictures/Signature/";
        File directory = new File(rootPath);
        File[] files = directory.listFiles();
        List<String> filesDirList = new ArrayList<>();

        for (int i=0; i< files.length; i++) {
            filesDirList.add(files[i].getName());
        }

        filesDirList.remove(name);   // 본인의 디렉토리(서명은)는 위조 대상에서 제외

        // 위조할 타겟 대상의 디렉토리 선택
        int idx1 = new Random().nextInt(filesDirList.size());
        String targetName = filesDirList.get(idx1);
        String pass_targetName = targetName;

        // 위조할 타켓 대상의 디렉토리 내 서명 선택
        final String targetPath = "/storage/self/primary/Pictures/Signature/" + targetName;
        File fileDirectory = new File(targetPath);
        File[] targetFiles = fileDirectory.listFiles();
        List<String> filesList = new ArrayList<>();

        for (int i=0; i< targetFiles.length; i++) {
            filesList.add(targetFiles[i].getName());
        }

        int idx2 = new Random().nextInt(filesList.size());
        String targetFile = filesList.get(idx2);
//                String targetFile = filesList.get(0);   // 임의의 파일 지정
        String pass_targetFile = targetFile;


        loadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                //불러오기 버튼 숨기기
                loadButton.setEnabled(false);

//                final String rootPath = "/storage/self/primary/Pictures/Signature/";
//                File directory = new File(rootPath);
//                File[] files = directory.listFiles();
//                List<String> filesDirList = new ArrayList<>();
//
//                for (int i=0; i< files.length; i++) {
//                    filesDirList.add(files[i].getName());
//                }
//
//                filesDirList.remove(name);   // 본인의 디렉토리(서명은)는 위조 대상에서 제외
//
//                // 위조할 타겟 대상의 디렉토리 선택
//                int idx1 = new Random().nextInt(filesDirList.size());
//                String targetName = filesDirList.get(idx1);
//                String pass_targetName = targetName;
//
//                // 위조할 타켓 대상의 디렉토리 내 서명 선택
//                final String targetPath = "/storage/self/primary/Pictures/Signature/" + targetName;
//                File fileDirectory = new File(targetPath);
//                File[] targetFiles = fileDirectory.listFiles();
//                List<String> filesList = new ArrayList<>();
//
//                for (int i=0; i< targetFiles.length; i++) {
//                    filesList.add(targetFiles[i].getName());
//                }
//
//                int idx2 = new Random().nextInt(filesList.size());
//                String targetFile = filesList.get(idx2);
////                String targetFile = filesList.get(0);   // 임의의 파일 지정
//                String pass_targetFile = targetFile;

                try {
                    File storageDir = new File(targetPath);
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
                intent2.putExtra("dirName", pass_targetName);
                intent2.putExtra("fileName", pass_targetFile);
                startActivity(intent2);
            }
        });

    }

}