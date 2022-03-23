package com.me.handwrittensignature;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.gcacace.signaturepad.views.SignaturePad;
import com.me.handwrittensignature.consts.ActivityServiceMessage;
import com.me.handwrittensignature.consts.ExtraIntent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.MODE_PRIVATE;
import static android.os.SystemClock.sleep;


public class RealSignMain extends AppCompatActivity {

    private static final String TAG = "RealSignMain";
    public EditText nameText;
    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 20;   // 실제 서명으로 등록할 횟수
    private String name;
    private TextView nameView;
    private Uri filePath;

    // 타이머 관련 변수
    TimerTask timerTask;
    Timer timer = new Timer();
    private int timeLimit = 10;   // 제한 시간 설정
    TextView timerText;

    /**
     * 화면 녹화 관련 static object
     */
    private static final int REQUEST_CODE = 100;
    private static String STORE_DIRECTORY;   // 기존 Signature 폴더 위치에 SignatureVideo 폴더 생성해서 저장
    private static int IMAGES_PRODUCED;
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
            | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static MediaProjection sMediaProjection;

    /**
     * 화면 녹화 관련 member object
     */
//    private MediaProjectionManager mpm;
    private ImageReader mImageReader;
    private ImageReader imageReader;
    private Handler mHandler;
    private Display mDisplay;
    private VirtualDisplay mVirtualDisplay;
    private int mDensity;
    private int mWidth;
    private int mHeight;
    private int mRotation;
//    private OrientationChangeCallback mOrientationChangeCallback;
//    private MediaProjectionStopCallback sMediaProjectionStopCallback;

    private ActivityResultLauncher<Intent> resultLauncher;
    private static final String PREFERENCE_KEY = "default";

    private Context context;
    private Messenger messenger;

    private MediaProjectionManager mpm;
    private ServiceConnection serviceConnection;
    private Messenger serviceMessenger;

    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "result_data";

    private int stateResultCode;
    private Intent stateResultData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.real_sign);

        Button startButton = (Button)findViewById(R.id.button_start);
        Button saveButton = (Button)findViewById(R.id.button_save);
        Button clearButton = (Button)findViewById(R.id.button_clear);

        TextView countText = (TextView)findViewById(R.id.countText);
        TextView finishText = (TextView)findViewById(R.id.finishText);
        timerText = (TextView)findViewById(R.id.timerText);
        TextView nameView = (TextView)findViewById(R.id.nameView);

        timerText.setText("제한시간 : " + timeLimit + " 초");

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        String name = intent.getStringExtra("text");

        nameView.setText(name);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MODE_PRIVATE);

        if (savedInstanceState != null) {
            this.stateResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            this.stateResultData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }

        this.context = this;
        // TODO 서비스 받아오기
        this.mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        this.messenger = new Messenger(new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.i(TAG, "Handler got message : " + msg.what);
                return false;
            }
        }));

        this.serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName nams, IBinder service) {
                Log.i(TAG, name + " service is connected");

//                RealSignService.LocalBinder binder=(RealSignService.LocalBinder)service;
//                Service recordService = binder.getService();
//
//                if(recordService != null) {
//                    Log.i("service-bind", "Service is bonded successfully!");
//
//                    serviceMessenger = new Messenger(service);
//                    Message msg = Message.obtain(null, ActivityServiceMessage.CONNECTED);
//                    msg.replyTo = messenger;
//                    try {
//                        serviceMessenger.send(msg);
//                    } catch (RemoteException e) {
//                        Log.e(TAG, "Failed to send message due to : " + e.toString());
//                        e.printStackTrace();
//                    }
//                }
                serviceMessenger = new Messenger(service);
                Message msg = Message.obtain(null, ActivityServiceMessage.CONNECTED);
                msg.replyTo = messenger;
                try {
                    serviceMessenger.send(msg);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to send message due to : " + e.toString());
                    e.printStackTrace();
                }

            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, name + " service is disconnected");
                serviceMessenger = null;
            }
        };

        // TODO resultLauncher 콜백 함수
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult()
                , new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent intent = result.getData();
//                            int CallType = intent.getIntExtra(STATE_RESULT_CODE, stateResultCode);
                            int CallType = intent.getIntExtra(ExtraIntent.RESULT_CODE.toString(), -1);


                        }

                    }
                }
        );

        /**
         * 1단계 : MediaProjectionManager 는 getSystemService 를 통해 service를 생성하고
         *     -> 사용자에게 권한 요구
         */
/*
        // TODO 서비스 받아오기 -> 멤버변수 MediaProjectionManager 로 들어감
//        mProjectionManager = (MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
       // TODO 실제 권한을 사용자에게 통보하고 권한 요구
        if (mpm != null) {
            startActivityForResult(mpm.createScreenCaptureIntent(), REQUEST_CODE);
//            Intent captureIntent = mpm.createScreenCaptureIntent();
//            registerForActivityResult(new ActivityResultContracts.StartActivityForResult()).launch(captureIntent);
        } else {
            Toast.makeText(RealSignMain.this, "권한 요청 실패", Toast.LENGTH_SHORT).show();
        }
 */
        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {

            @Override
            public void onStartSigning() {

            }

            @Override
            public void onSigned() {
                clearButton.setEnabled(true);
                saveButton.setEnabled(true);
            }

            @Override
            public void onClear() {
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

                startTimerTask();

//                startProjection();
//                onActivityResult(REQUEST_CODE, Activity.RESULT_OK, mProjectionManager.createScreenCaptureIntent());

                startCaptureScreen();

            }
        });

        // 기록 시작 버튼 누르고 -> 저장버튼 누르면 해당 영역 캡처 사진 저장
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                captureView(signaturePad);
                stopCaptureScreen();

                countNum += 1;   // 파일 이름은 name + '_' + countNum

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(RealSignMain.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                // 기록 저장 후에도 초기화 실행
                signaturePad.clear();

                signaturePad.setEnabled(false);

                sleep(1000);

                // 타이머 세팅
                saveStopTimerTask();
                timerText.setText("제한시간 : " + timeLimit + " 초");

                startButton.setVisibility(View.VISIBLE);
                clearButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);

                startButton.setEnabled(true);

                if(countNum == countComplete) {
                    finishText.setVisibility(View.VISIBLE);
                    startButton.setText("등록 완료");
                    startButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // 일정 횟수 채울 시작 버튼 -> 완료 버튼 -> 위조 서명 중 skilled/unskilled 선택 페이지로
                            Intent intent2 = new Intent(getApplicationContext(), SelectStatus.class);
                            startActivity(intent2);
                        }
                    });
                }

            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signaturePad.clear();
                saveButton.setEnabled(true);
                clearButton.setEnabled(true);

                // TODO 초기화 시 이전 녹화 영상을 저장하지 않고 다시 녹화 시작

                stopCaptureScreen();
                startCaptureScreen();

//                stopProjection();
//                startProjection();
//                sMediaProjectionStopCallback.onStop();
//                onActivityResult(REQUEST_CODE, Activity.RESULT_OK, mProjectionManager.createScreenCaptureIntent());
            }
        });

        startService();

    }

    /**
     * 서명 기록 시작 / 초기화 / 저장 / 제한 시간 종료 시 타이머 설정 메서드
     */
    private void startTimerTask() {
        stopTimerTask();
        timerTask = new TimerTask() {

            @Override
            public void run() {
                timeLimit --;
                timerText.post(new Runnable() {
                    @Override
                    public void run() {
                        if (timeLimit == 0) {
                            timerTask.cancel();
                            signaturePad.setEnabled(false);
                            Toast.makeText(getApplicationContext(), "제한시간 종료", Toast.LENGTH_SHORT).show();
                        }
                        timerText.setText("제한시간 : " + timeLimit + " 초");
                    }
                });

            }
        };
        timer.schedule(timerTask, 0, 1000);

    }

    private void stopTimerTask() {
        if (timerTask != null) {
            timeLimit = 10;
            timerText.setText("제한시간 : " + timeLimit + " 초");
            timerTask.cancel();
            timerTask = null;
        }

    }
    // 저장 버튼 클릭했을 때 남은 시간에서 멈추고 -> 타이머 다시 시작
    private void saveStopTimerTask() {
        if (timerTask != null) {
            timerText.setText("제한시간 : " + timeLimit + " 초");
            timerTask.cancel();
            timerTask = null;
            timeLimit = 10;
        }

    }

    private void startCaptureScreen() {
        if (stateResultCode != 0 && stateResultData != null) {
            startService();
        } else {
            Log.d(TAG, "Requesting confirmation");
//            startActivityForResult(mpm.createScreenCaptureIntent(),
//                    REQUEST_CODE);
            resultLauncher.launch(mpm.createScreenCaptureIntent());
        }
    }

    private void stopCaptureScreen() {
        if (serviceMessenger == null) {
            return;
        }

        Message msg = Message.obtain(null, ActivityServiceMessage.STOP);   // STOP : 999
        msg.replyTo = messenger;
        try {
            serviceMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException : " + e.toString());
            e.printStackTrace();
        }

    }

    /**
     * Service 실행 관련 메서드
     */

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (stateResultData != null) {
            outState.putInt(STATE_RESULT_CODE, stateResultCode);
            outState.putParcelable(STATE_RESULT_DATA, stateResultData);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO 권한 요청 및 임의의 REQUEST_CODE = 100
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.i(TAG, "User didn't allow.");
            } else {
                Log.d(TAG, "Starting screen capture");
                stateResultCode = resultCode;
                stateResultData = data;
                startCaptureScreen();
            }
        }

//        if (requestCode == REQUEST_CODE) {
//            if (resultCode == RESULT_OK) {
//                Toast.makeText(RealSignMain.this, "권한 획득 성공", Toast.LENGTH_SHORT).show();
//
//                super.onActivityResult(requestCode, resultCode, data);
//            }
//        }
    }


/*
    private void startCaptureScreen() {
        if (stateResultCode != 0 && stateResultData != null) {
            startService();
        } else {
            Log.d(TAG, "Requesting confirmation");
            startActivityForResult(
                    mpm.createScreenCaptureIntent(), REQUEST_CODE);
        }
    }

 */

    private void startService() {
        Log.i(TAG, "Starting Service");

        final Intent intent = new Intent(this, RealSignService.class);

        if (stateResultCode != 0 && stateResultData != null) {
            // string-array 에서 각 첫번째 원소 선택
            final String videoFormat = getResources().getStringArray(R.array.options_format_values)[0];
            final String[] videoResolutions = getResources().getStringArray(R.array.options_resolution_values)[0].split(",");

            final int screenWidth = Integer.parseInt(videoResolutions[0]);
            final int screenHeight = Integer.parseInt(videoResolutions[1]);
            final int screenDpi = Integer.parseInt(videoResolutions[2]);

            Log.i(TAG, "videoFormat : " + videoFormat);
            Log.i(TAG, "screenWidth : " + screenWidth);
            Log.i(TAG, "screenHeight : " + screenHeight);
            Log.i(TAG, "screenDpi : " + screenDpi);

            intent.putExtra(ExtraIntent.RESULT_CODE.toString(), stateResultCode);
            intent.putExtra(ExtraIntent.RESULT_DATA.toString(), stateResultData);
            intent.putExtra(ExtraIntent.VIDEO_FORMAT.toString(), videoFormat);
            intent.putExtra(ExtraIntent.SCREEN_WIDTH.toString(), screenWidth);
            intent.putExtra(ExtraIntent.SCREEN_HEIGHT.toString(), screenHeight);
            intent.putExtra(ExtraIntent.SCREEN_DPI.toString(), screenDpi);

        }

        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void unbindService() {
        if (serviceMessenger != null) {
            try {
                Message msg = Message.obtain(null, ActivityServiceMessage.DISCONNECTED);
                msg.replyTo = messenger;
                serviceMessenger.send(msg);
            } catch (RemoteException e) {
                Log.d(TAG, "Failed to send unregister message to service, e : " + e.toString());
                e.printStackTrace();
            }
            unbindService(serviceConnection);
        }
    }

}