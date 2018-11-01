package org.lastmilehealth.kiosk;

import android.app.ActivityManager;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by Andreas Schrade on 18.09.2015.
 */
public class KioskModeUtil {
    private static final String TAG = "KioskModeUtil";
    public static String activePackage = "";
    public static long whiteListTimeStamp = -1;
    public static StatusBarOverlayView statusbarOverlayView;

    private static final String BG_FILENAME = "bg_img";

    public static void handleKioskMode(Context ctx) {
        if (isWhiteListed()) {
            return;
        }

        // is Kiosk Mode active?
        if (PrefUtils.isKioskModeActive(ctx)) {
            // is App in background?
            activePackage = getNameOfRunningPackageName(ctx);

            if(activePackage == null){
                Log.d(TAG, "activePackage: null, skipping");
            }

            if (isInBackground(ctx)) {
                restoreApp(ctx);
            }
        }

    }

    private static boolean isWhiteListed() {
        if (whiteListTimeStamp > 0) {
            return System.currentTimeMillis() < whiteListTimeStamp;
        }
        return false;
    }


    private static boolean isInBackground(Context ctx) {
        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        //sendBroadcast(closeDialog);

        return (!ctx.getApplicationContext().getPackageName().equals(activePackage));
    }

    private static String getNameOfRunningPackageName(Context ctx) {
        String topPackageName = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1){
            UsageStatsManager mUsageStatsManager = (UsageStatsManager) ctx.getSystemService(Service.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 1000 * 3600, time);
            UsageEvents.Event event = new UsageEvents.Event();
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event);
                if(event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    topPackageName = event.getPackageName();
                }
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            //noinspection ResourceType
            UsageStatsManager mUsageStatsManager = (UsageStatsManager) ctx.getSystemService("usagestats");
            long time = System.currentTimeMillis();
            // We get usage stats for the last 10 seconds
            List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 200, time);
            // Sort the stats by the last time used
            if (stats != null) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : stats) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    topPackageName = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }
        } else {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            topPackageName = componentInfo.getPackageName();
        }
        Log.e("Kiosk", "Running App: " + topPackageName);

        return topPackageName;
    }

    private static void restoreApp(Context ctx) {
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Set<String> myAps = prefs.getStringSet("allowed_apps", Collections.<String>emptySet());

        if (myAps != null) {

            if (!myAps.contains(activePackage)) {
                Intent h = new Intent(ctx, MainActivity.class);
                h.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(h);
            } else {
                Log.v("This is cool", "This is cool");
            }
        }
    }

    public static void whiteListPackageForSpecificTime(String s, long l) {
        whiteListTimeStamp = System.currentTimeMillis() + l;
    }

    public static void triggerLauncherChooser(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final ComponentName componentName = new ComponentName(context, FakeLauncherActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        whiteListPackageForSpecificTime("android", TimeUnit.SECONDS.toMillis(10));
        context.startActivity(selector);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
            }
        }, 500);
    }

    public static boolean isMyLauncherDefault(Context ctx) {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);

        final String myPackageName = ctx.getPackageName();
        List<ComponentName> activities = new ArrayList<ComponentName>();
        final PackageManager packageManager = (PackageManager) ctx.getPackageManager();

        packageManager.getPreferredActivities(filters, activities, null);

        for (ComponentName activity : activities) {
            if (myPackageName.equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public static void applyDevicePolicy(Context context) {
        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        if (manager != null) {
            if (isDeviceAdmin(context)) {
                try {
                    ComponentName name = new ComponentName(context, KioskAdminReceiver.class);
                    //TODO all calls below require Device/Profile owner, remove it until EMM is implemented
                    manager.addUserRestriction(name, UserManager.DISALLOW_ADD_USER);
                    manager.addUserRestriction(name, UserManager.DISALLOW_APPS_CONTROL);
                    manager.addUserRestriction(name, UserManager.DISALLOW_CONFIG_CREDENTIALS);
                    manager.addUserRestriction(name, UserManager.DISALLOW_FACTORY_RESET);
                    manager.addUserRestriction(name, UserManager.DISALLOW_INSTALL_APPS);
                    manager.addUserRestriction(name, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
                    manager.addUserRestriction(name, UserManager.DISALLOW_MODIFY_ACCOUNTS);
                    manager.addUserRestriction(name, UserManager.DISALLOW_REMOVE_USER);
                    manager.addUserRestriction(name, UserManager.DISALLOW_SAFE_BOOT);
                    manager.addUserRestriction(name, UserManager.DISALLOW_UNINSTALL_APPS);
                    manager.setStatusBarDisabled(name, true);
                } catch (SecurityException e) {
                    Log.e(TAG, "Can't set device owner policy. Is the app the device owner?");
                }
            }
        }
    }

    private static boolean isDeviceAdmin(Context context) {
        final Intent deviceAdminIntent = new Intent("android.app.action.DEVICE_ADMIN_ENABLED", null);
        final List<ResolveInfo> appsList = context.getPackageManager().queryBroadcastReceivers(deviceAdminIntent, 0);
        for (ResolveInfo info : appsList) {
            String packageName = info.activityInfo.applicationInfo.packageName;
            if (packageName.equals(BuildConfig.APPLICATION_ID)) {
                return true;
            }
        }
        return false;
    }

    public static void cancelDevicePolicy(Context context) {
        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        if (manager != null) {
            try {
                if (isDeviceAdmin(context)) {
                    ComponentName name = new ComponentName(context, KioskAdminReceiver.class);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_ADD_USER);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_APPS_CONTROL);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_CONFIG_CREDENTIALS);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_FACTORY_RESET);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_INSTALL_APPS);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_MODIFY_ACCOUNTS);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_REMOVE_USER);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_SAFE_BOOT);
                    manager.clearUserRestriction(name, UserManager.DISALLOW_UNINSTALL_APPS);
                    manager.setStatusBarDisabled(name, false);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Can't cancel policy");
            }
        }
    }

    public static void preventStatusBarExpansion(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            return;
        }
        if (statusbarOverlayView != null)
            return;

        WindowManager manager = ((WindowManager) context.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE));

        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.TOP;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |

                // this is to enable the notification to recieve touch events
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |

                // Draws over status bar
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        localLayoutParams.height = (int) (50 * context.getResources()
                .getDisplayMetrics().scaledDensity);
        localLayoutParams.format = PixelFormat.TRANSPARENT;

        statusbarOverlayView = new StatusBarOverlayView(context);
        if (manager != null) {
            manager.addView(statusbarOverlayView, localLayoutParams);
        }
    }

    public static void removeStatusbarOverlay(Context context) {
        if (statusbarOverlayView != null) {
            WindowManager manager = ((WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
            try {
                manager.removeView(statusbarOverlayView);
            } catch (Exception e) {
                Log.i("Tag", "catch excpetion, error during remove ");
                //Toast.makeText(context, "BASDFSDFESF", Toast.LENGTH_LONG).show();
            }
        }
    }

    public static class StatusBarOverlayView extends ViewGroup {

        public StatusBarOverlayView(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            Log.v("customViewGroup", "**********Intercepted");
            return true;
        }
    }

    public static Bitmap getBackground(Context context) {
        return BitmapFactory.decodeFile(bgFile(context).getAbsolutePath());
    }

    public static void saveBackground(Context context, InputStream inputStream) {
        copyFile(inputStream, bgFile(context));
        PrefUtils.setIsBackground(true, context);
    }

    private static File bgFile(Context context) {
        return new File(ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_PICTURES)[0].getAbsolutePath(), BG_FILENAME);
    }

    private static void copyFile(InputStream sourceFileStream, File destFile) {
        try {
            BufferedInputStream bis = new BufferedInputStream(sourceFileStream);
            FileOutputStream fos = new FileOutputStream(destFile);
            int bufferSize = 8192;
            byte[] res = new byte[bufferSize];
            int got;
            while ((got = bis.read(res)) != -1) {
                fos.write(res, 0, got);
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
