package com.me.handwrittensignature;

import android.os.CountDownTimer;
import android.widget.TextView;

public class MyTimer extends CountDownTimer {
    TextView timerText;

    /**
     * @param millisInFuture : 타이머가 동작하는 총 시간 - 밀리세컨드(ms) 단위
     * @param countDownInterval : 카운트다운 되는 시간
     */
   public MyTimer(long millisInFuture, long countDownInterval)
    {
        super(millisInFuture, countDownInterval);
    }

    /**
     * onTick() : 생성자 인수 countDownInterval로 지정된 시간 간격마다 호출되는 함수
     * @param millisUntilFinished : 현재 타이머의 남은 시간
     */
    @Override
    public void onTick(long millisUntilFinished) {
        timerText.setText("제한시간 : " + millisUntilFinished/1000 + " 초");
    }

    /**
     * onFinish() : 타이머가 끝났을 때 호출되는 함수
     */
    @Override
    public void onFinish() {
        timerText.setText("제한시간 : 0 초");

    }
}
