package com.me.handwrittensignature;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InputName extends AppCompatActivity {
    private EditText nameText;
    private String pathName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_name);

        Button confirm_button = (Button) findViewById(R.id.confirm_button);
        Button start_button = (Button) findViewById(R.id.start_button);

        EditText nameText = (EditText) findViewById(R.id.nameText);

        start_button.setEnabled(false);

        // 내부 저장소 영역

//        final String rootPath = "/storage/self/primary/Pictures/Signature/";
//        final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature/";
        final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver2/";

        File directory = new File(rootPath);
//        File directory = new File(Environment.getExternalStorageDirectory(), "/Pictures");
//        File directory = new File(Environment.getExternalStorageDirectory().DIRECTORY_PICTURES);
        File[] files = directory.listFiles();
        List<String> filesDirList = new ArrayList<>();

        for (int i=0; i< files.length; i++) {
            filesDirList.add(files[i].getName());
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
                    if (filesDirList.contains(name)) {
                        Toast.makeText(getApplicationContext(), "이미 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();
                        start_button.setEnabled(true);

                    } else {
                        // 아직 등록되지 않은 사용자인 경우
                        Toast.makeText(getApplicationContext(), "아직 등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT).show();
                        // 디렉토리 생성
                        String strFolderPath = rootPath + name;
                        File folder = new File(strFolderPath);
                        try{
                            folder.mkdir();   //폴더 생성
                            Toast.makeText(getApplicationContext(), "새 폴더 생성", Toast.LENGTH_SHORT).show();
                        }
                        catch(Exception e){
                            e.getStackTrace();
                        }

                        start_button.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "등록되었습니다.", Toast.LENGTH_SHORT).show();
                    }

                }

            }

        });

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameText.getText().toString();

                // 이미 등록된 사용자인 경우
                // 사용자 이름을 입력하지 않은 경우 인텐트 비활성화 하기
//                Intent intent1 = new Intent(getApplicationContext(), SelectStatus.class);
//                startActivity(intent1);

                // 사용자 이름을 입력하지 않은 경우 인텐트 비활성화 하기
                Intent intent2 = new Intent(getApplicationContext(), SelectMode.class);
                intent2.putExtra("text", name);
                startActivity(intent2);
            }
        });
    }

}