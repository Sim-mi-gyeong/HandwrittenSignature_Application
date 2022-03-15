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
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.media.tv.TvInputService;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

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


class RecorderService extends Service {

    private int resultCode;
    private Intent resultData;
    private MediaProjectionManager mpm;
    private MediaProjection mediaProjection;
    private AudioPlaybackCaptureConfiguration audioPlaybackCaptureConfiguration;
    private final String NOTIFICATION_CHANNEL_ID = "my_channel_01";
    private final String NOTIFICATION_TICKER = "testTicker";
    private final String NOTIFICATION_CHANNEL_NAME = "testNotificationChannel";
    private final String NOTIFICATION_CHANNEL_DESC = "testNotification";



    public void onCreate() {
        super.onCreate();
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

        resultCode = intent.getIntExtra("resultCode", -1);
        resultData = intent.getParcelableExtra("data");
        Log.i(TAG, "onStartCommand: " + resultCode);
        Log.i(TAG, "onStartCommand: " + resultData);
//        notification();
        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpm.getMediaProjection(resultCode, resultData);
        Log.i(TAG, "onStartCommand: " + mediaProjection);
        AudioPlaybackCaptureConfiguration.Builder builder = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection);
        builder.addMatchingUid(AudioAttributes.USAGE_ALARM);
        builder.addMatchingUid(AudioAttributes.USAGE_MEDIA);
        builder.addMatchingUid(AudioAttributes.USAGE_GAME);
        audioPlaybackCaptureConfiguration = builder.build();
//        generateAudioRecord();

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
        startForeground(0, notification);

        return super.onStartCommand(intent, flags, startId);
    }

//    private void generateAudioRecord() {
//    }

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

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopSelf();

        super.onDestroy();
    }
}
