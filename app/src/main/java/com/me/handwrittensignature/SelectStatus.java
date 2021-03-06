package com.me.handwrittensignature;
// Uskilled OR Skilled 선택 페이지

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SelectStatus extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_status);

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        String name = intent.getStringExtra("text");

        Button button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent2 = new Intent(getApplicationContext(), ForgerySign_Unskilled.class);
//                Intent intent2 = new Intent(getApplicationContext(), SubActivity.class);
                intent2.putExtra("text", name);
                startActivity(intent2);
            }
        });
        Button button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent3 = new Intent(getApplicationContext(), ForgerySign_Skilled_Practice.class);
                intent3.putExtra("text", name);
                startActivity(intent3);
            }
        });

    }

}