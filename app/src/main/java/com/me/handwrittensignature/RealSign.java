package com.me.handwrittensignature;
// RealSign

import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.github.gcacace.signaturepad.views.SignaturePad;

import static android.os.SystemClock.sleep;

public class RealSign extends AppCompatActivity {
    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 5;   // 실제 서명으로 등록할 횟수

//    TimerTask timerTask;
//    Timer timer = new Timer();

    // 타이머 관련 변수
    private TextView timerText;
    private int timeLimit = 10;   // 제한 시간 설정
    private int status = 0;   // o: 종료/초기화(기록 시작 전 상태, 기록 시작 -> 초기화 상태) , 1: 시작(기록 시작 후 상태) , 2: 일시 정지(기록 시작 -> 기록 저장 상태)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.real_sign);

        Button startButton = (Button)findViewById(R.id.button_start);
        Button saveButton = (Button)findViewById(R.id.button_save);
        Button clearButton = (Button)findViewById(R.id.button_clear);

        TextView countText = (TextView)findViewById(R.id.countText);
        TextView finishText = (TextView)findViewById(R.id.finishText);
        TextView timerText = (TextView)findViewById(R.id.timerText);

        // 타이머를 위한 핸들러 인스턴스 변수
        TimerHandler timer = new TimerHandler();


//        saveButton.setEnabled(false)
//        clearButton.setEnabled(false);

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

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
                clearButton.setEnabled(true);
                saveButton.setEnabled(true);
            }

            @Override
            public void onClear() {
                //Event triggered when the pad is cleared
                clearButton.setEnabled(false);
                saveButton.setEnabled(false);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                signaturePad.setEnabled(true);   // 서명 패드 활성화

                startButton.setVisibility(View.GONE);   // 시작 버튼 숨기기
                clearButton.setVisibility(View.VISIBLE);   // 초기화 버튼 나타나게
                saveButton.setVisibility(View.VISIBLE);   // 저장 버튼 나타나게
                startButton.setEnabled(false);   // 시작 버튼 비활성화

                // 시작 버튼 클릭 시 CountDown Timer 실행   ->   어플 종료되는 현상 발생
//                if (status == 0) {
//                    status = 1;   // 종료 상태를 -> 시작 상태로
//                    timer.sendEmptyMessage(0);
//                }

            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //write code for saving the signature here

                // 사용자 이름 + autoIncre + 서명 녹화 영상 저장

                countNum += 1;

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(RealSign.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                // 기록 저장 후에도 초기화 실행
                signaturePad.clear();

                // 또 다시 시작 버튼 누르고 -> 기록 저장 / 초기화 버튼으로 구분할 것인지?
                signaturePad.setEnabled(false);

//                sleep(1000);

                startButton.setVisibility(View.VISIBLE);
                clearButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);

                startButton.setEnabled(true);

                if(countNum == countComplete) {
                    finishText.setVisibility(View.VISIBLE);
//                    Toast.makeText(RealSign.this, "Complete Signature Saved", Toast.LENGTH_SHORT).show();
                    startButton.setText("등록 완료");
                    startButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getApplicationContext(), SelectStatus.class);
                            startActivity(intent);
                        }
                    });
                }

                // 타이머 멈추도록 설정(일시 정지 후 초기화)
                // 시작 상태 -> 일시 정지(2번) -> sleep -> 초기화(0번)
                if (status == 1) {
                    status = 2;   // 타이머 동작 중 상태를 -> 일시 정지 상태로
                    sleep(1000);
                    status = 0;
                    timer.sendEmptyMessage(1);   // 1번 메세지(타이머 일시정지)
                }

            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.clear();
                saveButton.setEnabled(true);
                clearButton.setEnabled(true);

                // 타이머 초기화
                if (status == 1) {
                    status = 0;
                    timer.sendEmptyMessage(2);   // 2번 메세지(정지 후 타이머 초기화)
                }

            }
        });

    }

    // 타이머 핸들러 클래스
    class TimerHandler extends Handler {

        int timeLimit = 10;   // 제한 시간 설정

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

//            this.removeMessages(0);
//            Boolean Timer_state = false;
//            if(Timer_state == true) {
//                this.sendEmptyMessageDelayed(0, 1000);
//
//            }

            switch (msg.what) {
                case 0:   // 시작
                    if (timeLimit == 0) {
                        timerText.setText("제한 시간 : " + timeLimit);
                        removeMessages(0);
                        break;
                    }
                    timerText.setText("제한 시간 : " + timeLimit--);
                    sendEmptyMessageDelayed(0, 1000);

                    break;

                case 1:   // 일시 정지
                    removeMessages(0);   // 타이머 메세지 삭제
                    timerText.setText("제한 시간 : " + timeLimit);   // 현재 시간 표시

                    break;

                case 2:   // 정지 후 타이머 초기화
                    removeMessages(0);   // 타이머 메세지 삭제
                    timeLimit = 10;
                    timerText.setText("제한 시간 : " + timeLimit);

                    break;

            }


        }
    }
}