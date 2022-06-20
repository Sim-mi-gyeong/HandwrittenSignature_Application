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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // rootPath 를 Environment.getExternalStorageDirectory() + "/Pictures/"로 하고 이 안에 Signature 폴더가 있는지 확인 후 없으면 생성하도록!
                // -> MainActivity 에 추가
                final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/";
                File directory = new File(rootPath);
                File[] files = directory.listFiles();
                List<String> filesDirList = new ArrayList<>();

                for (int i=0; i< files.length; i++) {
                    filesDirList.add(files[i].getName());
                }

                final String rootName = "Signature";
                final String rootName2 = "Signature_ver2";
//                if (filesDirList.contains(rootName)) {
//                    Toast.makeText(getApplicationContext(), "이미 Signature 폴더가 존재합니.", Toast.LENGTH_SHORT).show();
//                }
                if (filesDirList.contains(rootName2)) {
                    Toast.makeText(getApplicationContext(), "이미 Signature_ver2 폴더가 존재합니다.", Toast.LENGTH_SHORT).show();
                }
                else {
                    // Signature 디렉토리 생성
//                    String strFolderPath = rootPath + rootName;
                    String strFolderPath = rootPath + rootName2;
                    File folder = new File(strFolderPath);
                    try {
                        folder.mkdir();   //폴더 생성
//                        Toast.makeText(getApplicationContext(), "Signature 폴더 생성", Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(), "Signature_ver2 폴더 생성", Toast.LENGTH_SHORT).show();
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