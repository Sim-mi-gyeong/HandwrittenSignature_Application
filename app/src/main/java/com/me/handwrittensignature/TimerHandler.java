//package com.me.handwrittensignature;
//
//import android.os.Message;
//
//import androidx.annotation.NonNull;
//
//public class  TimerHandler {
//
//    int timeLimit = 10;   // 제한 시간 설정
//
//        @Override
//        public void handleMessage(@NonNull Message msg) {
//            super.handleMessage(msg);
//
////            this.removeMessages(0);
////            Boolean Timer_state = false;
////            if(Timer_state == true) {
////                this.sendEmptyMessageDelayed(0, 1000);
////
////            }
//
//            switch (msg.what) {
//                case 0:   // 시작
//                    if (timeLimit == 0) {
//                        timerText.setText("제한 시간 : " + timeLimit);
//                        removeMessages(0);
//                        break;
//                    }
//                    timerText.setText("제한 시간 : " + timeLimit--);
//                    sendEmptyMessageDelayed(0, 1000);
//
//                    break;
//
//                case 1:   // 일시 정지
//                    removeMessages(0);   // 타이머 메세지 삭제
//                    timerText.setText("제한 시간 : " + timeLimit);   // 현재 시간 표시
//
//                    break;
//
//                case 2:   // 정지 후 타이머 초기화
//                    removeMessages(0);   // 타이머 메세지 삭제
//                    timeLimit = 10;
//                    timerText.setText("제한 시간 : " + timeLimit);
//
//                    break;
//
//            }
//
//
//        }
//    }
//}
