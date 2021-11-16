package com.me.handwrittensignature;
// ForgerySign_Skilled

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.gcacace.signaturepad.views.SignaturePad;

public class ForgerySign_Skilled_Practice extends AppCompatActivity {
    private TextView modeText;
    private SignaturePad signaturePad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.skilled_forgery_sign_practice);

        TextView modeText = (TextView)findViewById(R.id.modeText);
        Button startButton = (Button)findViewById(R.id.button_start);
        Button restartButton = (Button)findViewById(R.id.button_restart);
        Button endButton = (Button)findViewById(R.id.button_end);

        modeText.setVisibility(View.VISIBLE);

//        saveButton.setVisibility(false)
//        clearButton.setVisibility(false);

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);

        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {

            @Override
            public void onStartSigning() {
                //Event triggered when the pad is touched

//                clearButton.setVisibility(true);
//                saveButton.setVisibility(true);
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

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signaturePad.setEnabled(true);   // 서명 패드 활성화

                startButton.setVisibility(View.GONE);   // 시작 버튼 숨기기
                restartButton.setVisibility(View.VISIBLE);   // 초기화 버튼 나타나게
                endButton.setVisibility(View.VISIBLE);   // 종료 버튼 나타나게
                startButton.setEnabled(false);   // 시작 버튼 비활성화
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signaturePad.clear();
                restartButton.setEnabled(true);
                endButton.setEnabled(true);
            }
        });

        endButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Skilled 연습 페이지 종료 시  skilled 위조 서명  등록 화면으로
                Intent intent = new Intent(getApplicationContext(), ForgerySign_Skilled.class);
                startActivity(intent);
            }
        });

    }

}