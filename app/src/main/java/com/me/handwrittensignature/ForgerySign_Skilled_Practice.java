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

    private final String videoRootPath = Environment.getExternalStorageDirectory() + "/Movies/Signature_ver_Record/";
    private final String imageRootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver_Record/";
    private String targetImageDirPath;   // 위조할 대상의 디렉토리
    private String targetImageSignature;   // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택된 서명
    private String targetImageSignaturePath;   // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택된 서명 이미지 경로 => 기록 부분으로 넘겨주기

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.skilled_forgery_sign_practice);

        modeText = findViewById(R.id.modeText);
        Button loadButton = findViewById(R.id.loadButton);
        Button startButton = findViewById(R.id.button_start);
        Button restartButton = findViewById(R.id.button_restart);
        Button endButton = findViewById(R.id.button_end);

        modeText.setVisibility(View.VISIBLE);

        iv = findViewById(R.id.image1);

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
                restartButton.setEnabled(true);
                endButton.setEnabled(true);
            }

            public void onClear() {
                //Event triggered when the pad is cleared
                restartButton.setEnabled(false);
                endButton.setEnabled(false);
            }
        });

        loadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                try {
                    targetImageSignaturePath = loadTargetSignature();
                    File file = new File(targetImageSignaturePath);
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                    iv.setImageBitmap(bitmap);

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "이미지 로드 실패", Toast.LENGTH_SHORT).show();
                }

                loadButton.setEnabled(false);

            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.setEnabled(true);

                loadButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                restartButton.setVisibility(View.VISIBLE);
                endButton.setVisibility(View.VISIBLE);
                startButton.setEnabled(false);
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
                // Skilled 연습 페이지 종료 시 skilled 위조 서명 등록 화면으로
                Intent intent2 = new Intent(getApplicationContext(), ForgerySign_Skilled.class);
                intent2.putExtra("text", name);
                intent2.putExtra("targetName", targetName);
                intent2.putExtra("targetPath", targetImageDirPath);
                intent2.putExtra("targetSignaturePath", targetImageSignaturePath);
                startActivity(intent2);
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

}