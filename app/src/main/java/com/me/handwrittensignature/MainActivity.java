package com.me.handwrittensignature;
// Start 페이지(처음 화면) -> 이름 입력(InputName.java)로 이동

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private static final String videoTopRootPath = Environment.getExternalStorageDirectory() + "/Movies/";
    private static final String imageTopRootPath = Environment.getExternalStorageDirectory() + "/Pictures/";
    private static final String rootName = "Signature_ver_Record";;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // rootPath 를 Environment.getExternalStorageDirectory() + "/Pictures/"로 하고 이 안에 Signature 폴더가 있는지 확인 후 없으면 생성하도록!
                // -> MainActivity 에 추가
                File videoDirectory = new File(videoTopRootPath);
                File imageDirectory = new File(imageTopRootPath);

                File[] videoFiles = videoDirectory.listFiles();
                File[] imageFiles = imageDirectory.listFiles();

                List<String> videoFilesDirList = new ArrayList<>();
                List<String> imageFilesDirList = new ArrayList<>();

                for (int i = 0; i < videoFiles.length; i++) {
                    videoFilesDirList.add(videoFiles[i].getName());
                }
                for (int i = 0; i < imageFiles.length; i++) {
                    imageFilesDirList.add(imageFiles[i].getName());
                }

                if (videoFilesDirList.contains(rootName) && imageFilesDirList.contains(rootName)) {
                    Toast.makeText(getApplicationContext(), "이미 Signature_ver_Record 폴더가 존재합니다.", Toast.LENGTH_SHORT).show();

                } else if (!videoFilesDirList.contains(rootName) && imageFilesDirList.contains(rootName)) {

                    String strVideoFolderPath = videoTopRootPath + rootName;
                    File videoFolder = new File(strVideoFolderPath);

                    try {
                        videoFolder.mkdir();
                    } catch (Exception e) {
                        e.getStackTrace();
                    }

                } else if (videoFilesDirList.contains(rootName) && !imageFilesDirList.contains(rootName)) {

                    String strImageFolderPath = imageTopRootPath + rootName;
                    File imageFolder = new File(strImageFolderPath);

                    try {
                        imageFolder.mkdir();
                    } catch (Exception e) {
                        e.getStackTrace();
                    }

                } else {

                    String strVideoFolderPath = videoTopRootPath + rootName;
                    String strImageFolderPath = imageTopRootPath + rootName;

                    File videoFolder = new File(strVideoFolderPath);
                    File imageFolder = new File(strImageFolderPath);

                    try {
                        videoFolder.mkdir();
                        imageFolder.mkdir();
                        Toast.makeText(getApplicationContext(), "Signature_ver_Record 폴더 생성", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.getStackTrace();
                    }
                }

                Intent intent = new Intent(getApplicationContext(), InputName.class);
                startActivity(intent);

            }

        });

    }

}