package com.me.handwrittensignature;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

public class InputName extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.input_name);

        Button confirm_button = (Button) findViewById(R.id.confirm_button);
        Button start_button = (Button) findViewById(R.id.start_button);
//        String[] nameArray = new String[1000000];
        EditText nameText = (EditText) findViewById(R.id.nameText);

        confirm_button.setOnClickListener(new View.OnClickListener() {

            // 버튼 클릭 시 기존 사용자 데이터와 확인
            // (실제 서명이) 이미 등록된 사용자이면 -> Toast Message로 '이미 등록된 사용자입니다'를 알리고 "등록 시작" 버튼 클릭 시 -> SetStatus 페이지로
            // (실제 서명이) 등록되지 않은 사용자이면 -> Toast Message로 '등록되지 않은 사용자입니다'를 알리고 "등록 시작" 버튼 클릭 시 -> SetMode 페이지로
            @Override
            public void onClick(View v) {

                if (nameText.getText().toString().length() == 0) {
                    Toast.makeText(InputName.this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                    nameText.requestFocus();
//                    confirm_button.setEnabled(false);
                }

                // 이미 등록된 사용자인 경우
                Toast.makeText(getApplicationContext(), "이미 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();

                // 아직 등록되지 않은 사용자인 경우
                Toast.makeText(getApplicationContext(), "아직 등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT).show();

            }
        });

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                while (nameText.getText().toString().length() == 0) {
//                    Toast.makeText(InputName.this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
//                    nameText.requestFocus();
//                }

                if (nameText.getText().toString().length() == 0) {
                    Toast.makeText(InputName.this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                    nameText.requestFocus();
//                    start_button.setEnabled(false);
                }
//                // 이미 등록된 사용자인 경우
                  // 사용자 이름을 입력하지 않은 경우 인텐트 비활성화 하기
//                Intent intent1 = new Intent(getApplicationContext(), SelectStatus.class);
//                startActivity(intent1);

                // 아직 등록되지 않은 사용자인 경우
                // 우선 SelectMode로 연결되도록
                // 사용자 이름을 입력하지 않은 경우 인텐트 비활성화 하기
                Intent intent2 = new Intent(getApplicationContext(), SelectMode.class);
                startActivity(intent2);
            }
        });
    }

}
