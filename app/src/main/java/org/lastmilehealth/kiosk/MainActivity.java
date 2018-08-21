package org.lastmilehealth.kiosk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import static android.app.Dialog.OnClickListener;


public class MainActivity extends Activity {

    private int count = 0;
    public PackageManager PM;
    private long startMillis = 0;
    public GridView allowedApplist;
    private ImageView ivBackground;
    public List<AppDetail> alowedApps;
    private List blockedKeys = new ArrayList(Arrays.asList(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP));
    private static boolean wasServiceStarted;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (blockedKeys.contains(event.getKeyCode())) {
            return true;
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    protected void onPause() {
        super.onPause();

        boolean kioskModeActive = PrefUtils.isKioskModeActive(this);
        if (kioskModeActive) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    KioskModeUtil.handleKioskMode(getApplicationContext());
                }
            }, 300);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!KioskModeUtil.isMyLauncherDefault(getApplicationContext()) && PrefUtils.isKioskModeActive(this)) {
            Toast.makeText(this, "Please set the Kiosk-App as default.", Toast.LENGTH_LONG).show();
            KioskModeUtil.triggerLauncherChooser(getApplicationContext());
        }

        if (PrefUtils.isKioskModeActive(getApplicationContext())) {
            KioskModeUtil.preventStatusBarExpansion(getApplicationContext());
            KioskModeUtil.applyDevicePolicy(getApplicationContext());
        } else {
            KioskModeUtil.removeStatusbarOverlay(getApplicationContext());
            KioskModeUtil.cancelDevicePolicy(getApplicationContext());
        }

        loadApps();
        loadListView();
        addClickListener();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("Kiosk", "OnCREATE1");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (!isPermissionSet()) {
                Toast.makeText(this, "Please add the permission for 'Android Kiosk' and restart the App.", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                //finish();  // Commented, because this broke non-android studio installs.
            }
        }

        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //PrefUtils.setKioskModeActive(true, getApplicationContext());
        setContentView(R.layout.activity_main);

        ImageView logo = (ImageView) findViewById(R.id.logo);
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder Builder = new AlertDialog.Builder(MainActivity.this);
                Builder.setTitle("Unlock Device");
                Builder.setMessage("Please enter Unlock Code:");
                final EditText input = new EditText(MainActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                AlertDialog.Builder ok = Builder.setPositiveButton("OK", new OnClickListener() {
                    public void onClick(final DialogInterface dialog, int whichButton) {

                        String SetUnlockCode = input.getText().toString();
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        final String password = preferences.getString("pref_UnlockCode", "1234");

                        if (password.equals(SetUnlockCode)) {
                            startActivity(new Intent(MainActivity.this, AppPreferences.class));
                            input.getText().clear();
                        } else {
                            Toast.makeText(getApplicationContext(), "Invalid code!", Toast.LENGTH_SHORT).show();
                        }

                    }

                });

                final AlertDialog dlg = Builder.create();
                dlg.setView(input);
                dlg.show();
            }
        });
        if (KioskModeUtil.isMyLauncherDefault(getApplicationContext()) && !PrefUtils.isKioskModeActive(this)) {
            getPackageManager().clearPackagePreferredActivities(getPackageName());
        }

        Button btn = (Button) findViewById(R.id.settings);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BluetoothActivity.class);
                startActivity(intent);
            }
        });
        Button craash = (Button) findViewById(R.id.craaaash);
        craash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throw new NullPointerException();
            }
        });

        if (!wasServiceStarted) {
            startKioskService();
            wasServiceStarted = true;
        }
    }

    private void startKioskService() { // ... and this method
        startService(new Intent(this, KioskService.class));
    }

    private void loadApps() {
        PM = getPackageManager();
        alowedApps = new ArrayList<>();

        if (PrefUtils.isKioskModeActive(this)) {
            Set<String> allowedAppPackages = PrefUtils.getAllowedApps(this);
            Set<String> hiddenAppPackages = PrefUtils.getAppsWithHiddenIcon(this);

            for (String packageName : allowedAppPackages) {
                if (hiddenAppPackages.contains(packageName))
                    continue; // skip

                AppDetail appDetail = buildAppInfoByPackage(packageName);
                appDetail.displayOrder = PrefUtils.getOrderForApp(this, packageName);
                alowedApps.add(appDetail);
            }

            // sorting the app based on display order previously set
            Collections.sort(alowedApps, new AppOrderComparator());

        } else {
            // add all
            SortedSet<PackageInfo> installedApps = InstalledAppsUtil.getInstalledApps(this);
            for (PackageInfo each : installedApps) {
                alowedApps.add(buildAppInfoByPackage(each.packageName));
            }
        }
    }

    private AppDetail buildAppInfoByPackage(String packageName) {
        AppDetail app = new AppDetail();
        try {
            ApplicationInfo ai = this.getPackageManager().getApplicationInfo(packageName, 0);
            app.label = PM.getApplicationLabel(ai);
            app.icon = PM.getApplicationIcon(ai);
            app.name = ai.packageName;
        } catch (final PackageManager.NameNotFoundException e) {
            Log.i("Kiosk", "name not found for package: " + packageName);
        }
        return app;
    }

    private void loadListView() {
        allowedApplist = (GridView) findViewById(R.id.apps_list);

        if (PrefUtils.isBackgroundSet(this))
            setBackground();

        ArrayAdapter<AppDetail> adapter = new ArrayAdapter<AppDetail>(this, R.layout.show_allowed_apps, alowedApps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.show_allowed_apps, null);
                }

                ImageView appIcon = (ImageView) convertView.findViewById(R.id.item_app_icon);
                appIcon.setImageDrawable(alowedApps.get(position).icon);

                TextView appLabel = (TextView) convertView.findViewById(R.id.item_app_label);
                appLabel.setText(alowedApps.get(position).label);

                return convertView;
            }
        };
        allowedApplist.setAdapter(adapter);
    }

    private void setBackground() {
        ivBackground = (ImageView) findViewById(R.id.iv_background);
        ivBackground.setImageBitmap(KioskModeUtil.getBackground(this));
    }

    private void addClickListener() {
        allowedApplist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos,
                                    long id) {
                Intent i = PM.getLaunchIntentForPackage(alowedApps.get(pos).name.toString());
                startActivity(i);
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        loadApps();
        loadListView();
        addClickListener();

//        if (!hasFocus) {
//            // Close every kind of system dialog
//            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
//            //sendBroadcast(closeDialog);
//        }
    }

    @Override
    public void onBackPressed() {
        // nothing to do here
        // … really
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//
//        if (1 == 1)
//            return false;
//
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        final String DeffUnlockCode = preferences.getString("pref_UnlockCode", "7777");
//        final String GetTouches = preferences.getString("pref_ClickAttempts", "5");
//        final String DeffTimeToUnlock = preferences.getString("pref_TimeToUnlock", "3");
//
//
//        Integer TouchCount = Integer.parseInt(GetTouches);
//        final Integer TouchTime = Integer.parseInt(DeffTimeToUnlock);
//
//        int eventaction = event.getAction();
//        if (eventaction == MotionEvent.ACTION_UP) {
//
//
//            //get system current milliseconds
//            long time = System.currentTimeMillis();
//
//
//            //if it is the first time, or if it has been more than 3 seconds since the first tap ( so it is like a new try), we reset everything
//            if (startMillis == 0 || (time - startMillis > (TouchTime * 1000))) {
//                startMillis = time;
//                count = 1;
//            }
//            //it is not the first, and it has been  less than 3 seconds since the first
//            else { //  time-startMillis< PeriodToUnlock/1000
//                count++;
//
//            }
//
//            if (count == TouchCount) {
//                AlertDialog.Builder Builder = new AlertDialog.Builder(this);
//                Builder.setTitle("Unlock Device");
//                Builder.setMessage("Please enter Unlock Code:");
//                final EditText input = new EditText(this);
//                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//
//
//                if (DeffUnlockCode.equals("7777")) {
//                    startActivity(new Intent(MainActivity.this, AppPreferences.class));
//                    Toast.makeText(getApplicationContext(), "Don't forget do define your Unlock code for future usage!", Toast.LENGTH_SHORT).show();
//
//                } else {
//
//                    AlertDialog.Builder ok = Builder.setPositiveButton("OK", new OnClickListener() {
//
//
//                        public void onClick(final DialogInterface dialog, int whichButton) {
//
//                            String SetUnlockCode = input.getText().toString();
//
//                            if (DeffUnlockCode.equals(SetUnlockCode)) {
//                                startActivity(new Intent(MainActivity.this, AppPreferences.class));
//                                input.getText().clear();
//
//                            } else {
//
//                                Toast.makeText(getApplicationContext(), "Wrong Unlock Code!", Toast.LENGTH_SHORT).show();
//
//                            }
//
//                        }
//
//                    });
//
//                    final AlertDialog dlg = Builder.create();
//
//                    dlg.setView(input);
//                    dlg.show();
//
//
//                    dlg.setOnKeyListener(new DialogInterface.OnKeyListener() {
//                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
//
//                            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
//                                onBackPressed();
//                                return true;
//                            } else {
//
//                                Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
//                                sendBroadcast(closeDialog);
//                            }
//
//
//                            return false;
//                        }
//                    });
//
//                    final Timer t = new Timer();
//                    t.schedule(new TimerTask() {
//                        public void run() {
//                            dlg.dismiss(); // when the task active then close the dialog
//                            t.cancel(); // also just top the timer thread, otherwise, you may receive a crash report
//                        }
//                    }, 3000); // after 5 second (or 5000 miliseconds), the task will be active
//                }
//                return true;
//            }
//        }
//        return false;
//    }

    private boolean isPermissionSet() {
        try {
            PackageManager packageManager = getApplicationContext().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getApplicationContext().getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }


}

