package com.me.handwrittensignature;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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

        EditText nameText = (EditText) findViewById(R.id.nameText);


        nameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                start_button.setEnabled(false);
                start_button.setClickable(false);

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String name = nameText.getText().toString();

                if (name.length() == 0){
                    confirm_button.setEnabled(false);
                    start_button.setEnabled(false);
                }
                else {
                    confirm_button.setEnabled(true);
                    start_button.setEnabled(true);

                }

            }
        });

        confirm_button.setOnClickListener(new View.OnClickListener() {
            // 버튼 클릭 시 기존 사용자 데이터와 확인
            // (실제 서명이) 이미 등록된 사용자이면 -> Toast Message로 '이미 등록된 사용자입니다'를 알리고 "등록 시작" 버튼 클릭 시 -> SetStatus 페이지로
            // (실제 서명이) 등록되지 않은 사용자이면 -> Toast Message로 '등록되지 않은 사용자입니다'를 알리고 "등록 시작" 버튼 클릭 시 -> SetMode 페이지로
            @Override
            public void onClick(View v) {
                String name = nameText.getText().toString();

                if(name.length() == 0) {
                    Toast.makeText(InputName.this, "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
                    start_button.setEnabled(false);
                    nameText.requestFocus();
                } else {
                    // 이미 등록된 사용자인 경우
                    Toast.makeText(getApplicationContext(), "이미 등록된 사용자입니다.", Toast.LENGTH_SHORT).show();

                    // 아직 등록되지 않은 사용자인 경우
                    Toast.makeText(getApplicationContext(), "아직 등록되지 않은 사용자입니다.", Toast.LENGTH_SHORT).show();
                }

            }

        });

        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 이미 등록된 사용자인 경우
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
