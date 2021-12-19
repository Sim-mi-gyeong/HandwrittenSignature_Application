package com.me.handwrittensignature;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class InputName extends AppCompatActivity {
    public EditText nameText;
    public String pathName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_name);

        Button confirm_button = (Button) findViewById(R.id.confirm_button);
        Button start_button = (Button) findViewById(R.id.start_button);

        EditText nameText = (EditText) findViewById(R.id.nameText);

        start_button.setEnabled(false);
        List<Uri> uris_ = new ArrayList<>();
        ArrayList<String> nameList = new ArrayList();

//        nameText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//
//                start_button.setEnabled(false);
//                start_button.setClickable(false);
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                String name = nameText.getText().toString();
//
//                if (name.length() > 0){
//                    confirm_button.setEnabled(true);
//                    start_button.setEnabled(true);
//                }
//                else {
//                    confirm_button.setEnabled(false);
//                    start_button.setEnabled(false);
//
//                }
//
//            }
//        });

        confirm_button.setOnClickListener(new View.OnClickListener() {
            // 버튼 클릭 시 기존 사용자 데이터와 확인
            // (실제 서명이) 이미 등록된 사용자이면 -> Toast Message로 '이미 등록된 사용자입니다'를 알리고 "등록 시작" 버튼 클릭 시 -> SetStatus 페이지로
            // (실제 서명이) 등록되지 않은 사용자이면 -> Toast Message로 '등록되지 않은 사용자입니다'를 알리고 "등록 시작" 버튼 클릭 시 -> SetMode 페이지로
            @Override
            public void onClick(View v) {
                String name = nameText.getText().toString();

                FirebaseStorage storage = FirebaseStorage.getInstance();

                //생성된 FirebaseStorage를 참조하는 storage 생성
                StorageReference storageRef = storage.getReference();

                //Storage 내부의 images 폴더 안의 image.jpg 파일명을 가리키는 참조 생성
//                StorageReference pathReference = storageRef.child("dog.jpg");
//                StorageReference pathReference = storageRef.child(name + '/');
                StorageReference pathReference = storageRef.child("signature");

                if(name.length() == 0) {
                    Toast.makeText(InputName.this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                    start_button.setEnabled(false);
                    nameText.requestFocus();
                }
                else {
                    if (nameList.contains(name)) {
                        // 이미 등록된 사용자인 경우
                        Toast.makeText(getApplicationContext(), "이미 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();
                        start_button.setEnabled(true);
                    } else {
                        // 아직 등록되지 않은 사용자인 경우
                        Toast.makeText(getApplicationContext(), "아직 등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT).show();
                        nameList.add(name);
                        start_button.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "등록되었습니다.", Toast.LENGTH_SHORT).show();
                    }

                }

//                    pathReference.listAll()
//                            .addOnSuccessListener(new OnSuccessListener<ListResult>() {
//                                @Override
//                                public void onSuccess(ListResult listResult) {
//                                    for (StorageReference prefix : listResult.getPrefixes()) {
//                                        // All the prefixes under listRef.
//                                        // You may call listAll() recursively on them.
//                                        if (prefix.getName() == "signature/" + name){
//                                            String pathName = prefix.getName();
//                                            // 이미 등록된 사용자인 경우
//                                            Toast.makeText(getApplicationContext(), "이미 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();
//                                        }
//                                        else {
//                                            pathReference.mkdirs();
//                                            // 아직 등록되지 않은 사용자인 경우
//                                            Toast.makeText(getApplicationContext(), "아직 등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT).show();
//                                        }
//                                    }
//
//                                    for (StorageReference item : listResult.getItems()) {
//                                        // All the items under listRef.
//                                    }
//                                }
//                            });
//                    // 이미 등록된 사용자인 경우
//                    Toast.makeText(getApplicationContext(), "이미 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();
//                    // 아직 등록되지 않은 사용자인 경우
//                    Toast.makeText(getApplicationContext(), "아직 등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT).show();
//                }

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

                // 아직 등록되지 않은 사용자인 경우
                // 우선 SelectMode로 연결되도록
                // 사용자 이름을 입력하지 않은 경우 인텐트 비활성화 하기
                Intent intent2 = new Intent(getApplicationContext(), SelectMode.class);
                intent2.putExtra("text", name);
                startActivity(intent2);
            }
        });
    }

}
