package com.me.handwrittensignature;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InputName extends AppCompatActivity {

    private Button confirm_button;
    private Button start_button;
    private EditText nameText;
    private List<String> videoFilesDirList, imageFilesDirList;   // 등록된 사용자 리스트
    private static final String videoRootPath = Environment.getExternalStorageDirectory() + "/Movies/Signature_ver_Record/";
    private static final String imageRootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver_Record/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_name);

        confirm_button = findViewById(R.id.confirm_button);
        start_button = findViewById(R.id.start_button);

        nameText = findViewById(R.id.nameText);

        start_button.setEnabled(false);

        File videoDirectory = new File(videoRootPath);
        File imageDirectory = new File(imageRootPath);

        File[] videoFiles = videoDirectory.listFiles();
        File[] imageFiles = imageDirectory.listFiles();

        videoFilesDirList = new ArrayList<>();
        imageFilesDirList = new ArrayList<>();

        for (int i = 0; i < videoFiles.length; i++) {
            videoFilesDirList.add(videoFiles[i].getName());
        }
        for (int i = 0; i < imageFiles.length; i++) {
            imageFilesDirList.add(imageFiles[i].getName());
        }

        confirm_button.setOnClickListener(new View.OnClickListener() {
            // 버튼 클릭 시 기존 사용자 데이터와 확인
            // (실제 서명이) 이미 등록된 사용자이면 -> Toast Message 로 '이미 등록된 사용자입니다'
            // (실제 서명이) 등록되지 않은 사용자이면 -> Toast Message 로 '등록되지 않은 사용자입니다'
            @Override
            public void onClick(View v) {
                String name = nameText.getText().toString();

                if(name.length() == 0) {
                    Toast.makeText(InputName.this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                    start_button.setEnabled(false);
                    nameText.requestFocus();
                }
                else {
                    if (videoFilesDirList.contains(name) && imageFilesDirList.contains(name)) {
                        Toast.makeText(getApplicationContext(), "이미 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();

                    } else if (!videoFilesDirList.contains(name) && imageFilesDirList.contains(name)) {

                        String strVideoFolderPath = videoRootPath + name;
                        File videoFolder = new File(strVideoFolderPath);

                        try{
                            videoFolder.mkdir();
                        }
                        catch(Exception e){
                            e.getStackTrace();
                        }

                    } else if (videoFilesDirList.contains(name) && !imageFilesDirList.contains(name)) {

                        String strImageFolderPath = imageRootPath + name;
                        File imageFolder = new File(strImageFolderPath);

                        try{
                            imageFolder.mkdir();
                        }
                        catch(Exception e){
                            e.getStackTrace();
                        }

                    } else {
                        // 아직 등록되지 않은 사용자인 경우
                        Toast.makeText(getApplicationContext(), "아직 등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT).show();

                        String strVideoFolderPath = videoRootPath + name;
                        String strImageFolderPath = imageRootPath + name;

                        File videoFolder = new File(strVideoFolderPath);
                        File imageFolder = new File(strImageFolderPath);

                        try{
                            // 사용자 디렉토리 생성
                            videoFolder.mkdir();
                            imageFolder.mkdir();
                            Toast.makeText(getApplicationContext(), "새 폴더 생성", Toast.LENGTH_SHORT).show();
                        }
                        catch(Exception e){
                            e.getStackTrace();
                        }
                        Toast.makeText(getApplicationContext(), "등록되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                    start_button.setEnabled(true);
                }
            }

        });

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameText.getText().toString();

                Intent intent = new Intent(getApplicationContext(), SelectMode.class);
                intent.putExtra("text", name);
                startActivity(intent);
            }
        });
    }

}