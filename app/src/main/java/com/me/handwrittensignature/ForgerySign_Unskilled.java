package com.me.handwrittensignature;
// ForgerySign_Unskilled
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
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.github.gcacace.signaturepad.views.SignaturePad;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.os.SystemClock.sleep;

public class ForgerySign_Unskilled extends AppCompatActivity implements HBRecorderListener {

    private static final int SCREEN_RECORD_REQUEST_CODE = 100;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 101;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = 102;
    HBRecorder hbRecorder;
    boolean hasPermissions;
    ContentValues contentValues;
    ContentResolver resolver;
    Uri mUri;

    private TextView modeText;
    private SignaturePad signaturePad;
    private int countNum = 0;   // 등록된 사용자 서명 횟수
    private int countComplete = 20;   // 실제 서명으로 등록할 횟수
    public static String name;
    public static String targetName;

    private final String videoRootPath = Environment.getExternalStorageDirectory() + "/Movies/Signature_ver_Record/";
    private final String imageRootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver_Record/";
    private String targetImageDirPath;   // 위조할 대상의 디렉토리
    private String targetImageSignature;   // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택된 서명
    private String targetImageSignaturePath;   // 위조 대상의 실제 서명 디렉토리 중 랜덤 선택된 서명 이미지 경로
    private String targetVideoSignaturePath;   // 위조한 서명 녹화 영상이 저장될 경로

    // 타이머 관련 변수
    TimerTask timerTask;
    Timer timer = new Timer();
    private int timeLimit = 60;   // 제한 시간 설정
    TextView timerText;

    ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unskilled_forgery_sign);

        Button loadButton = findViewById(R.id.loadButton);
        Button startButton = findViewById(R.id.button_start);
        Button saveButton = findViewById(R.id.button_save);
        Button clearButton = findViewById(R.id.button_clear);

        modeText = findViewById(R.id.modeText);
        TextView countText = findViewById(R.id.countText);
        TextView finishText = findViewById(R.id.finishText);
        timerText = findViewById(R.id.timerText);

        modeText.setVisibility(View.VISIBLE);

        iv = findViewById(R.id.image1);
        timerText.setText("제한시간 : " + timeLimit + " 초");

        Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
        name = intent.getStringExtra("text");

        signaturePad = (SignaturePad) findViewById(R.id.signaturePad);
        signaturePad.setEnabled(false);

        hbRecorder = new HBRecorder(this, this);
        hbRecorder.enableCustomSettings();
        hbRecorder.setScreenDimensions(signaturePad.getMeasuredHeight(), signaturePad.getMeasuredWidth());
        Log.d("signaturePad Size : ", signaturePad.getHeight() + "  " + signaturePad.getWidth());

//        targetVideoSignaturePath = videoRootPath + targetName;
//        hbRecorder.setOutputPath(targetVideoSignaturePath);
//        hbRecorder.setFileName(targetName + "_skilled_forgery_"  + System.currentTimeMillis());

        signaturePad.setOnSignedListener(new SignaturePad.OnSignedListener() {

            @Override
            public void onStartSigning() {

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

        loadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                try {
                    targetImageSignaturePath = loadTargetSignature();
                    File file = new File(targetImageSignaturePath);
                    Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
                    iv.setImageBitmap(bitmap);

                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "이미지 로드 실패", Toast.LENGTH_SHORT).show();
                }

                targetVideoSignaturePath = videoRootPath + targetName;
                hbRecorder.setOutputPath(targetVideoSignaturePath);
                hbRecorder.setFileName(targetName + "_skilled_forgery_"  + System.currentTimeMillis());

                loadButton.setEnabled(false);
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                signaturePad.setEnabled(true);

                loadButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                startButton.setVisibility(View.GONE);
                clearButton.setVisibility(View.VISIBLE);
                saveButton.setVisibility(View.VISIBLE);

                startButton.setEnabled(false);

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

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 영상 저장
                hbRecorder.setFileName(targetName + "_unskilled_forgery_" + System.currentTimeMillis());
                hbRecorder.stopScreenRecording();

                countNum += 1;

                countText.setText((countNum + "/" + countComplete).toString());
                Toast.makeText(ForgerySign_Unskilled.this, "Signature Saved", Toast.LENGTH_SHORT).show();

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
                            Intent intent = new Intent(getApplicationContext(), SelectStatus.class);
                            startActivity(intent);
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

    /**
     * 위조 대상의 서명 데이터를 가져올 메서드 - targetFile = 위조 대상의 서명 이미지 경로(String)
     */
    public String loadTargetSignature() {
        File directory = new File(imageRootPath);
        File[] files = directory.listFiles();   // 디렉토리 내 파일 목록
        List<String> imageFilesDirList = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            imageFilesDirList.add(files[i].getName());
        }

        imageFilesDirList.remove(name);   // 본인의 디렉토리(서명은)는 위조 대상에서 제외

        // 위조할 타겟 대상의 디렉토리 랜덤 선택
        int idx1 = new Random().nextInt(imageFilesDirList.size());
        targetName = imageFilesDirList.get(idx1);

        // TODO 위조할 타켓 대상의 디렉토리 내 서명 선택 - 각 서명 디렉토리 이름 중 unskilled or skilled 문자열 미포함 디렉토리 선택
        targetImageDirPath = imageRootPath + targetName;
        File targetImagePathFiles = new File(targetImageDirPath);
        File[] targetImagePathFileList = targetImagePathFiles.listFiles();   // 위조할 타켓 대상 디렉토리 내의 목록
        List<String> targetPathFolderList = new ArrayList<>();

        for (int i = 0; i < targetImagePathFileList.length; i++) {
            targetPathFolderList.add(targetImagePathFileList[i].getName());
        }

        for (int i = 0; i < targetPathFolderList.size(); i++) {
            if (targetPathFolderList.get(i).contains("unskilled") || targetPathFolderList.get(i).contains("skilled") || targetPathFolderList.get(i).contains("mp4") ) {
                targetPathFolderList.remove(targetPathFolderList.get(i));   // 위조 대상의 실제 서명 이미지들만 남기기
            }
        }

        // TODO 위조할 타겟 대상의 디렉토리 내 서명 - unskilled or skilled 문자열 미포함 -> png 파일 중 랜덤 선택
        int idx2 = new Random().nextInt(targetPathFolderList.size());
        targetImageSignature = targetPathFolderList.get(idx2);
        targetImageSignaturePath = targetImageDirPath + "/" + targetImageSignature;

        return targetImageSignaturePath;

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
            } else{
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