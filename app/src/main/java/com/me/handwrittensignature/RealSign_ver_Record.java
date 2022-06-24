package com.me.handwrittensignature;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.gcacace.signaturepad.views.SignaturePad;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;

public class RealSign_ver_Record extends AppCompatActivity implements HBRecorderListener {

    private static final int SCREEN_RECORD_REQUEST_CODE = 100;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 101;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 102;
    HBRecorder hbRecorder;
    boolean hasPermissions;
    ContentValues contentValues;
    ContentResolver resolver;
    Uri mUri;

    public EditText nameText;
    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 20;   // 실제 서명으로 등록할 횟수
    private String name;
    private TextView nameView;
    private Uri filePath;

    private int checkInit = 0;
    private final String rootPath = Environment.getExternalStorageDirectory() + "/Movies/Signature_ver_Record/";
    private String userFolderPath;
    private String strFilePath;
    private int signatureCnt;
    private int newSignatureCnt;
    private String signatureFolderPath;
    private File signatureFolder;
    private Bitmap bitmap;

    // 타이머 관련 변수
    TimerTask timerTask;
    Timer timer = new Timer();
    private int timeLimit = 60;   // 제한 시간 설정
    TextView timerText;

    TimerTask captureTimerTask;
    TimerTask initTimerTask;

    Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
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
        name = intent.getStringExtra("text");
        nameView.setText(name);

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

        hbRecorder = new HBRecorder(this, this);
        hbRecorder.enableCustomSettings();
        hbRecorder.setScreenDimensions(signaturePad.getMeasuredHeight(), signaturePad.getMeasuredWidth());
        Log.d("signaturePad Size : ", signaturePad.getHeight() + "  " + signaturePad.getWidth());

        userFolderPath = rootPath + name;
        hbRecorder.setOutputPath(userFolderPath);
        hbRecorder.setFileName(name + "_" + System.currentTimeMillis());


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

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {

                signaturePad.setEnabled(true);   // 서명 패드 활성화

                startButton.setVisibility(View.GONE);   // 시작 버튼 숨기기
                clearButton.setVisibility(View.VISIBLE);   // 초기화 버튼 나타나게
                saveButton.setVisibility(View.VISIBLE);   // 저장 버튼 나타나게
                startButton.setEnabled(false);   // 시작 버튼 비활성화

                startTimerTask();

                // 권한 체크
                // 녹화 시작
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //first check if permissions was granted
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
                        hasPermissions = true;
                    }
                    if (hasPermissions) {

                        startRecordingScreen();

                    }
                } else {
                    //showLongToast("This library requires API 21>");
                }


            }
        });

        // 기록 시작 버튼 누르고 -> 저장버튼 누르면 해당 영역 캡처 사진 저장
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                captureView(signaturePad);

                // 영상 저장
                hbRecorder.setFileName(name + "_" + System.currentTimeMillis());
                hbRecorder.stopScreenRecording();

                countNum += 1;   // 파일 이름은 name + '_' + countNum

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(RealSign_ver_Record.this, "Signature Saved", Toast.LENGTH_SHORT).show();

                // 기록 저장 후에도 서명 패드 초기화 실행
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

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                signaturePad.clear();
                saveButton.setEnabled(true);
                clearButton.setEnabled(true);

                // TODO 초기화 시 이전 녹화 영상을 저장하지 않고 다시 녹화 시작
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //first check if permissions was granted
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
                        hasPermissions = true;
                    }
                    if (hasPermissions) {
                        startRecordingScreen();
                    }
                } else {
                    //showLongToast("This library requires API 21>");
                }

            }
        });

    }

    public void captureView(View View) {

        Intent intent = getIntent();   // 전달한 데이터를 받을 Intent
        name = intent.getStringExtra("text");

        // 저장소 영역  ->  위조하는 대상의 디렉토리에 해당 서명 캡처 이미지 저장!!!
        View.destroyDrawingCache();
        View.setDrawingCacheEnabled(true);
        View.buildDrawingCache();
        bitmap = signaturePad.getDrawingCache();   // Bitmap 가져오기

        FileOutputStream fos;

        // TODO 위조 서명이 저장될 경로는, 위조 대상의 디렉토리 내 생성된 unskiiled 표시가 붙은 디렉토리 => targetSignaturePath
        strFilePath = rootPath + "/" + name + System.currentTimeMillis() + ".png";

        File fileCacheItem = new File(strFilePath);

        try {
            fos = new FileOutputStream(fileCacheItem, false);
            bitmap.createBitmap(View.getWidth(), View.getHeight(), Bitmap.Config.ARGB_8888);
            // 해당 Bitmap 으로 만든 이미지를 png 파일 형태로 만들기
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "스크린샷 저장 실패", Toast.LENGTH_SHORT).show();
        }

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
            timeLimit = 60;
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
            timeLimit = 60;
        }

    }

    @Override
    public void HBRecorderOnStart() {
        Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void HBRecorderOnComplete() {
        Toast.makeText(this, "Completed", Toast.LENGTH_SHORT).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //Update gallery depending on SDK Level
            if (hbRecorder.wasUriSet()) {
                updateGalleryUri();
            }else{
                refreshGalleryFile();
            }
        }
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {
        Toast.makeText(this, errorCode+": "+reason, Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startRecordingScreen() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                //Start screen recording
                hbRecorder.startScreenRecording(data, resultCode, this);

            }
        }
    }

    //For Android 10> we will pass a Uri to HBRecorder
    //This is not necessary - You can still use getExternalStoragePublicDirectory
    //But then you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    //IT IS IMPORTANT TO SET THE FILE NAME THE SAME AS THE NAME YOU USE FOR TITLE AND DISPLAY_NAME

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setOutputPath() {

        String filename = generateFileName();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "SpeedTest/" + "SpeedTest");
            contentValues.put(MediaStore.Video.Media.TITLE, filename);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            //FILE NAME SHOULD BE THE SAME
            hbRecorder.setFileName(filename);
            hbRecorder.setOutputUri(mUri);
        } else{
            createFolder();
            hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) +"/HBRecorder");
        }
    }

    //Check if permissions was granted
    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    private void updateGalleryUri(){
        contentValues.clear();
        contentValues.put(MediaStore.Video.Media.IS_PENDING, 0);
        getContentResolver().update(mUri, contentValues, null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void refreshGalleryFile() {
        MediaScannerConnection.scanFile(this,
                new String[]{hbRecorder.getFilePath()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }

    // Generate a timestamp to be used as a file name
    private String generateFileName() {

        userFolderPath = rootPath + name;

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());

        return formatter.format(curDate).replace(" ", "");

    }

    // drawable to byte[]
    private byte[] drawable2ByteArray(@DrawableRes int drawableId) {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), drawableId);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    //Create Folder
    //Only call this on Android 9 and lower (getExternalStoragePublicDirectory is deprecated)
    //This can still be used on Android 10> but you will have to add android:requestLegacyExternalStorage="true" in your Manifest
    private void createFolder() {
        File f1 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "SpeedTest");
        if (!f1.exists()) {
            if (f1.mkdirs()) {
                Log.i("Folder ", "created");
            }
        }
    }
}