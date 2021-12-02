package com.me.handwrittensignature;
// Start 페이지(처음 화면) -> 이름 입력(InputName.java)로 이동

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(getApplicationContext(), InputName.class);
                Intent intent = new Intent(getApplicationContext(), TestActivity.class);
                startActivity(intent);

            }

        });

        // Cloud Storage 연결 설정 테스트
    }

}