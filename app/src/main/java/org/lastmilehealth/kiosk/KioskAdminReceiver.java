package org.lastmilehealth.kiosk;

import android.annotation.SuppressLint;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class KioskAdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "KioskAdminReceiver";

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "Received intent: " + intent.toString());
    }
}
