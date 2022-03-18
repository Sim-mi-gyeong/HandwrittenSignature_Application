package com.me.handwrittensignature;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.media.AudioAttributes;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.media.tv.TvInputService;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static android.content.ContentValues.TAG;

// 서비스 내부에 startForeground()를 처리
/*
class RecorderService extends IntentService {

    private final int FOREGROUND_SERVICE_ID = 1000;

    public RecorderService() {
        super("TestService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // Restore interrupt status.
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent testIntent = new Intent(this, TestService.class);
        startService(testIntent);
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
//        return START_STICKY;
    }
}

 */

/*
class RecorderService extends Service {
    private static final int NOTIFY_ID=9906;
    static final String EXTRA_RESULT_CODE="resultCode";
    static final String EXTRA_RESULT_INTENT="resultIntent";
    static final String ACTION_RECORD=
            BuildConfig.APPLICATION_ID+".RECORD";
    static final String ACTION_STOP=
            BuildConfig.APPLICATION_ID+".STOP";
    static final String ACTION_SHUTDOWN=
            BuildConfig.APPLICATION_ID+".SHUTDOWN";
    private boolean isForeground=false;
    private int resultCode;
    private Intent resultData;
    private boolean recordOnNextStart=false;
    private RecordingSession session=null;

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i.getAction()==null) {
            resultCode=i.getIntExtra(EXTRA_RESULT_CODE, 1337);
            resultData=i.getParcelableExtra(EXTRA_RESULT_INTENT);

            if (recordOnNextStart) {
                startRecorder();
            }

            foregroundify(!recordOnNextStart);
            recordOnNextStart=false;
        }
        else if (ACTION_RECORD.equals(i.getAction())) {
            if (resultData!=null) {
                foregroundify(false);
                startRecorder();
            }
            else {
                Intent ui=
                        new Intent(this, MainActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(ui);
                recordOnNextStart=true;
            }
        }
        else if (ACTION_STOP.equals(i.getAction())) {
            foregroundify(true);
            stopRecorder();
        }
        else if (ACTION_SHUTDOWN.equals(i.getAction())) {
            stopSelf();
        }

        return(START_NOT_STICKY);
    }

    @Override
    public void onDestroy() {
        stopRecorder();
        stopForeground(true);

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new IllegalStateException("go away");
    }

    private void foregroundify(boolean showRecord) {
        NotificationCompat.Builder b=
                new NotificationCompat.Builder(this);

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL);

        b.setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(getString(R.string.app_name));

        if (showRecord) {
            b.addAction(R.drawable.ic_videocam_white_24dp,
                    getString(R.string.notify_record), buildPendingIntent(ACTION_RECORD));
        }
        else {
            b.addAction(R.drawable.ic_stop_white_24dp,
                    getString(R.string.notify_stop), buildPendingIntent(ACTION_STOP));
        }

        b.addAction(R.drawable.ic_eject_white_24dp,
                getString(R.string.notify_shutdown), buildPendingIntent(ACTION_SHUTDOWN));

        if (isForeground) {
            NotificationManager mgr=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);

            mgr.notify(NOTIFY_ID, b.build());
        }
        else {
            startForeground(NOTIFY_ID, b.build());
            isForeground=true;
        }
    }

    private PendingIntent buildPendingIntent(String action) {
        Intent i=new Intent(this, getClass());

        i.setAction(action);

        return(PendingIntent.getService(this, 0, i, 0));
    }

    synchronized private void startRecorder() {
        if (session==null) {
            MediaProjectionManager mgr=
                    (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            MediaProjection projection=
                    mgr.getMediaProjection(resultCode, resultData);

//            session=
//                    new RecordingSession(this, new RecordingConfig(this),
//                            projection);
            session=
                    new RecordingSession(this, new RecordingConfig(this),
                            projection);
            session.start();
        }
    }

    synchronized private void stopRecorder() {
        if (session!=null) {
            session.stop();
            session=null;
        }
    }
}

 */


public class RecorderService extends Service {

    public static Context recorderServiceContext;

    private int resultCode;
    private Intent resultData;
    private MediaProjectionManager mpm;
    private static MediaProjection mediaProjection;
    private AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration;
    private final String NOTIFICATION_CHANNEL_ID = "my_channel_01";
    private final String NOTIFICATION_TICKER = "testTicker";
    private final String NOTIFICATION_CHANNEL_NAME = "testNotificationChannel";
    private final String NOTIFICATION_CHANNEL_DESC = "testNotification";

    private static final String videoFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/MediaProjection.mp4";
    private static String STORE_DIRECTORY;
    private int mWidth;
    private int mHeight;


    public void onCreate() {
        super.onCreate();
        /**
         * 서비스는 한 번 실행되면 계속 실행된 상태로 존재
         * -> 서비스 특성상, intent 를 받아 처리하기 적합하지 않음
         * -> intent 에 대한 처리는 onStartCommand() 에서 처리 !
         */
    }

//    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand() is called");
        if (intent == null) {
            return Service.START_STICKY;   // 서비스가 종료되어도 다시 실행 요청
       }

        resultCode = intent.getIntExtra("resultCode", -1);
        resultData = intent.getParcelableExtra("data");
//        resultData = intent.getStringExtra("data");
        Log.i(TAG, "onStartCommand: " + resultCode);
        Log.i(TAG, "onStartCommand: " + resultData);
//        notification();

//        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpm.getMediaProjection(resultCode, resultData);
        Log.i(TAG, "onStartCommand: " + mediaProjection);


//        AudioPlaybackCaptureConfiguration.Builder builder = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection);
//        builder.addMatchingUid(AudioAttributes.USAGE_ALARM);
//        builder.addMatchingUid(AudioAttributes.USAGE_MEDIA);
//        builder.addMatchingUid(AudioAttributes.USAGE_GAME);
//        audioPlaybackCaptureConfiguration = builder.build();
//        generateAudioRecord();

//        Intent notificationIntent = new Intent(this, RecorderService.class);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
//                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground))
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .setContentTitle("Starting Service")
//                .setContentText("Starting monitoring service")
//                .setTicker(NOTIFICATION_TICKER)
//                .setContentIntent(pendingIntent);
//        Notification notification = notificationBuilder.build();

//        startForeground(0, notification);

        screenRecorder(resultCode, resultData);

        return super.onStartCommand(intent, flags, startId);
    }


    public void screenRecorder(int resultCode, @Nullable Intent data) {
        final MediaRecorder screenRecorder = createRecorder();

        MediaProjection.Callback callback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                if (screenRecorder != null) {
                    screenRecorder.stop();
                    screenRecorder.reset();
                    screenRecorder.release();
                }
                mediaProjection.unregisterCallback(this);
                mediaProjection = null;
            }
        };
        mediaProjection.registerCallback(callback, null);

        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        mediaProjection.createVirtualDisplay(
                "sample",
                displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                screenRecorder.getSurface(), null, null);

        if (mediaProjection != null) {
            mediaProjection.stop();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(videoFile), "video/mp4");
            startActivity(intent);
        }

//        final Button actionRec = findViewById(R.id.button_start);
//        actionRec.setText("STOP REC");
//        actionRec.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                actionRec.setText("START REC");
//                if (mediaProjection != null) {
//                    mediaProjection.stop();
//
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                    intent.setDataAndType(Uri.parse(videoFile), "video/mp4");
//                    startActivity(intent);
//                }
//            }
//        });
        screenRecorder.start();
    }

    private MediaRecorder createRecorder() {
        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(videoFile);
        DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
        mediaRecorder.setVideoSize(displayMetrics.widthPixels, displayMetrics.heightPixels);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncodingBitRate(512 * 1000);
        mediaRecorder.setVideoFrameRate(30);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mediaRecorder;
    }

    /**
     * ImageReader 에서 Image 를 처리할 class
     */
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {

        @Override
        public void onImageAvailable(ImageReader imageReader) {

            Toast.makeText(RecorderService.this, "ImageAvailableListener 까지 온 상태", Toast.LENGTH_SHORT).show();

            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;

//            Intent intent = getIntent(); // 전달한 데이터를 받을 Intent
//            String name = intent.getStringExtra("text");

            try {
                // 가장 최신 이미지 가져오기
                image = imageReader.acquireLatestImage();
                if (image != null) {

                    Toast.makeText(getApplicationContext(), "이미지 가져오기 성공", Toast.LENGTH_SHORT).show();

                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();   // 이미지 버퍼 정보
                    // 픽셀 + rowStride -> rowPadding = rowStride - pixelStride * mWidth
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;

                    // createBitmap() -> bitmap 파일을 만들고 -> 위의 이미지 buffer 로 이미지를 가져옴
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    final String rootPath = Environment.getExternalStorageDirectory() + "/Pictures/Signature_ver2/";
//                    final String CAPTURE_PATH = name;
                    STORE_DIRECTORY = rootPath;
//
//                    signaturePad.destroyDrawingCache();
//                    signaturePad.setDrawingCacheEnabled(true);
//                    signaturePad.buildDrawingCache();
//                    Bitmap bitmap = signaturePad.getDrawingCache();   // Bitmap 가져오기
//
//                    FileOutputStream fos;

                    String strFilePath = STORE_DIRECTORY + "/" + '_' + System.currentTimeMillis() + ".png";   // strFilePath: 이미지 저장 경로
                    File fileCacheItem = new File(strFilePath);
                    fos = new FileOutputStream(fileCacheItem, false);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.flush();
                    fos.close();

                    Toast.makeText(getApplicationContext(), "스크린샷 저장 성공", Toast.LENGTH_SHORT).show();

                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "스크린샷 저장 실패", Toast.LENGTH_SHORT).show();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
                if (bitmap != null) {
                    bitmap.recycle();
                }
                if (image != null) {
                    image.close();
                }
            }
        }

    }

/*
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void notification() {
        Log.i(TAG, "notification: " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent notificationIntent = new Intent(this, RecorderService.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("Starting Service")
                    .setContentText("Starting monitoring service")
                    .setTicker(NOTIFICATION_TICKER)
                    .setContentIntent(pendingIntent);

            Notification notification = notificationBuilder.build();
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(NOTIFICATION_CHANNEL_DESC);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);

            startForeground(0, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        }
    }
 */

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();

        super.onDestroy();
    }
}
