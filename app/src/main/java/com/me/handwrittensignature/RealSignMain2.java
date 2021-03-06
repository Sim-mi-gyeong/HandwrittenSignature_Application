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
import android.content.pm.PackageManager;
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
import android.os.PersistableBundle;
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.gcacace.signaturepad.views.SignaturePad;
import com.me.handwrittensignature.consts.ActivityServiceMessage;
import com.me.handwrittensignature.consts.ExtraIntent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.MODE_PRIVATE;
import static android.os.SystemClock.sleep;


public class RealSignMain2 extends AppCompatActivity {
    private ActivityResultLauncher<Intent> resultLauncher;

    private static final String TAG = "RealSignMain";
    public EditText nameText;
    private SignaturePad signaturePad;
    private int countNum = 0;   // ????????? ????????? ?????? ??????
    private int countComplete = 20;   // ?????? ???????????? ????????? ??????
    private String name;
    private TextView nameView;
    private Uri filePath;

    // ????????? ?????? ??????
    TimerTask timerTask;
    Timer timer = new Timer();
    private int timeLimit = 10;   // ?????? ?????? ??????
    TextView timerText;

    private String[] permissions = new String[] {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQ_PERMISSIONS = 1;
    private static final int REQ_PROJECTION = 2;

    private Intent mProjData;
    private boolean isStarted;

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
        timerText = (TextView)findViewById(R.id.timerText);
        TextView nameView = (TextView)findViewById(R.id.nameView);

        timerText.setText("???????????? : " + timeLimit + " ???");

        Intent intent = getIntent(); // ????????? ???????????? ?????? Intent
        String name = intent.getStringExtra("text");

        nameView.setText(name);

        reqPermission();

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

                signaturePad.setEnabled(true);   // ?????? ?????? ?????????

                startButton.setVisibility(View.GONE);   // ?????? ?????? ?????????
                clearButton.setVisibility(View.VISIBLE);   // ????????? ?????? ????????????
                saveButton.setVisibility(View.VISIBLE);   // ?????? ?????? ????????????
                startButton.setEnabled(false);   // ?????? ?????? ????????????

                startTimerTask();

                if (isStarted) {

                    stopScreenCapture();
                    isStarted = false;

                } else {

                    startScreenCapture();
                    isStarted = true;
                }


            }
        });

        // ?????? ?????? ?????? ????????? -> ???????????? ????????? ?????? ?????? ?????? ?????? ??????
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                captureView(signaturePad);

                countNum += 1;   // ?????? ????????? name + '_' + countNum

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(RealSignMain2.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                // ?????? ?????? ????????? ????????? ??????
                signaturePad.clear();

                signaturePad.setEnabled(false);

                sleep(1000);

                // ????????? ??????
                saveStopTimerTask();
                timerText.setText("???????????? : " + timeLimit + " ???");

                startButton.setVisibility(View.VISIBLE);
                clearButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);

                startButton.setEnabled(true);

                if(countNum == countComplete) {
                    finishText.setVisibility(View.VISIBLE);
                    startButton.setText("?????? ??????");
                    startButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // ?????? ?????? ?????? ?????? ?????? -> ?????? ?????? -> ?????? ?????? ??? skilled/unskilled ?????? ????????????
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

                // TODO ????????? ??? ?????? ?????? ????????? ???????????? ?????? ?????? ?????? ??????

            }
        });

    }

    private void startScreenCapture() {

        if (mProjData == null) {
            // create ca[ture intent with media projection manager
            Intent capture_intent = ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE)).createScreenCaptureIntent();
            startActivityForResult(capture_intent, REQ_PROJECTION);
            return;
        }
        Intent serviceIntent = new Intent(this, RealSignService2.class);
        serviceIntent.setAction("start");
        serviceIntent.putExtra("con.ns.pData", mProjData);
        startService(serviceIntent);
    }

    private void stopScreenCapture() {
        Intent serviceIntent = new Intent(this, RealSignService2.class);
        serviceIntent.setAction("stop");
        startService(serviceIntent);

    }

    private void reqPermission() {
        if (ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSIONS);
        }
    }

//    private void requestPermission() {
//        if (ActivityCompat.checkSelfPermission(this, permissions[0]) +
//                ActivityCompat.checkSelfPermission(this, permissions[1]) != PackageManager.PERMISSION_GRANTED) {
//            Log.d(TAG, "requestPermission: requesting for permissions" );
//            ActivityCompat.requestPermissions(this, permissions, REQ_PERMISSIONS);
//        }
//
//        Log.d(TAG, "requestPermission: permission accepted - " + permissions[0] + " " + permissions[1]);
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permission, grantResults);

//        if (requestCode == REQ_PERMISSIONS) {
//            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                finish();
//            }
//        }
        if (requestCode == REQ_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                return;
            }
            finish();
            System.exit(0);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (REQ_PROJECTION == requestCode) {
            if (resultCode == RESULT_OK) {
                mProjData = data;
                startScreenCapture();
            }
        }
    }

    /**
     * ?????? ?????? ?????? / ????????? / ?????? / ?????? ?????? ?????? ??? ????????? ?????? ?????????
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
                            Toast.makeText(getApplicationContext(), "???????????? ??????", Toast.LENGTH_SHORT).show();
                        }
                        timerText.setText("???????????? : " + timeLimit + " ???");
                    }
                });

            }
        };
        timer.schedule(timerTask, 0, 1000);

    }

    private void stopTimerTask() {
        if (timerTask != null) {
            timeLimit = 10;
            timerText.setText("???????????? : " + timeLimit + " ???");
            timerTask.cancel();
            timerTask = null;
        }

    }
    // ?????? ?????? ???????????? ??? ?????? ???????????? ????????? -> ????????? ?????? ??????
    private void saveStopTimerTask() {
        if (timerTask != null) {
            timerText.setText("???????????? : " + timeLimit + " ???");
            timerTask.cancel();
            timerTask = null;
            timeLimit = 10;
        }

    }

    /**
     * Service ?????? ?????? ?????????
     */

}