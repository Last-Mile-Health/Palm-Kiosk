package org.lastmilehealth.kiosk;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.concurrent.TimeUnit;
/**
 * Created by Andreas Schrade on 17.09.2015.
 */
public class KioskService extends Service {
    public static String CHANNEL_NAME_FOREGROUND_SERVICE = "Palm Kiosk";
    public static final String CHANNEL_ID_FOREGROUND_SERVICE = "org.lastmilehealth.kiosk";
    public static final int NOTIFICATION_ID = 23;

    private static final long INTERVAL = TimeUnit.SECONDS.toMillis(4); // periodic interval to check in seconds -> 2
    private static final String TAG = KioskService.class.getSimpleName();

    private Thread t = null;
    private Context ctx = null;
    private boolean running = false;

    @Override
    public void onDestroy() {
        Log.i(TAG, "Stopping service 'KioskService'");
        running = false;
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Starting service 'KioskService' !!!!!!!!!!!!!");

        running = true;
        ctx = getApplicationContext();

        //start foreground
        startForeground(NOTIFICATION_ID, setUpNotificationForForegroundService(this, NOTIFICATION_ID));

        // start a thread that periodically checks if your app is in the foreground
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    KioskModeUtil.handleKioskMode(ctx);
                    try {
                        Thread.sleep(INTERVAL);
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Thread interrupted: 'KioskService'");
                    }
                } while (running);
                stopSelf();
            }
        });

        t.start();
        return Service.START_STICKY;
    }



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



    public static Notification setUpNotificationForForegroundService(Context context, int notificationForegroundId){
        //create channel for Android O
        String tempNotificationChannelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel notificationChannel = null;
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            CHANNEL_NAME_FOREGROUND_SERVICE = context.getString(R.string.app_name);

            //create channel
            notificationChannel = new NotificationChannel(CHANNEL_ID_FOREGROUND_SERVICE, CHANNEL_NAME_FOREGROUND_SERVICE, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setBypassDnd(true);
            notificationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            notificationChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(notificationChannel);

            tempNotificationChannelId = notificationChannel.getId();
        }

        //create intents
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK );
        PendingIntent pendingIntent = PendingIntent.getActivities(context, notificationForegroundId, new Intent[]{intent}, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, tempNotificationChannelId);
        builder
                .setContentTitle(CHANNEL_NAME_FOREGROUND_SERVICE)
                .setContentText(CHANNEL_NAME_FOREGROUND_SERVICE)
                .setContentIntent(pendingIntent)
                .setShowWhen(false)
        ;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setSmallIcon(R.mipmap.ic_launcher);
        } else {
            builder.setSmallIcon(R.mipmap.ic_launcher);
        }

        return builder.build();
    }
}