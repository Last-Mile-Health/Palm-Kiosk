package org.lastmilehealth.kiosk;

/**
 * Created by Andreas Schrade on 22.09.2015.
 */

import java.lang.Thread.UncaughtExceptionHandler;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static android.content.ContentValues.TAG;

public class DefaultExceptionHandler implements UncaughtExceptionHandler {

    private UncaughtExceptionHandler defaultUEH;
    Context ctx;
    private UncaughtExceptionHandler crashlyticsExceptionHandler;

    public DefaultExceptionHandler(Context ctx, UncaughtExceptionHandler crashlyticsExceptionHandler) {
        this.ctx = ctx;
        this.crashlyticsExceptionHandler = crashlyticsExceptionHandler;
    }


    /**
     * Automatically restart app on crash!
     *
     * @param thread
     * @param ex
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        crashlyticsExceptionHandler.uncaughtException(thread, ex);

        Log.e(TAG, "uncaughtException: ", ex);
        Intent intent = new Intent(ctx, MainActivity.class);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, intent, intent.getFlags());

        AlarmManager mgr = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 750,
                pendingIntent);
        System.exit(2);
    }
}