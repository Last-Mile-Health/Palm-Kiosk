package org.lastmilehealth.kiosk;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import net.xpece.android.support.preference.AppCompatPreferenceActivity;

import java.io.FileNotFoundException;

/**
 * Created by Andreas Schrade on 17.09.2015.
 */
public class AppPreferences extends AppCompatPreferenceActivity {

    private static final int ACTION_REQUEST_GALLERY = 101;
    private Preference chooseBackgroundPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        chooseBackgroundPref = findPreference("pref_Wallpaper");

        chooseBackgroundPref.setSummary(PrefUtils.isBackgroundSet(this) ? R.string.already_set : R.string.not_set);

        chooseBackgroundPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");

                Intent chooser = Intent.createChooser(intent, "Choose a Picture");
                startActivityForResult(chooser, ACTION_REQUEST_GALLERY);

                return true;
            }
        });

        findPreference("pref_ExitLog").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(AppPreferences.this);
                dialogBuilder.setTitle("Exit Log");
                dialogBuilder.setItems(LogUtils.readLogs(AppPreferences.this), null);
                dialogBuilder.setPositiveButton(R.string.ok, null);
                dialogBuilder.setNegativeButton(R.string.clear, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogUtils.clearLogs(AppPreferences.this);
                    }
                });
                dialogBuilder.create().show();
                return true;
            }
        });

        Preference exitButton = findPreference("pref_Exit");

        exitButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {

                // write to log
                LogUtils.writeLog(AppPreferences.this);

                PrefUtils.setKioskModeActive(false, getApplicationContext());
                KioskModeUtil.removeStatusbarOverlay(getApplicationContext());
                KioskModeUtil.cancelDevicePolicy(getApplicationContext());
                Toast.makeText(getApplicationContext(), "Please set the regular Home Screen as default.", Toast.LENGTH_LONG).show();
                KioskModeUtil.triggerLauncherChooser(getApplicationContext());
                finish();
                return true;
            }
        });


        Preference onOffToggleButton = findPreference("pref_kiosk_mode");

        onOffToggleButton.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean isActive = (Boolean) newValue;

                if (!isActive) {
                    // has been deactivated
                    Toast.makeText(getApplicationContext(), "Deactivated", Toast.LENGTH_LONG).show();
                    KioskModeUtil.removeStatusbarOverlay(getApplicationContext());
                    // triggerLauncherChooser();
                } else {
                    // has been activated
                    if (!KioskModeUtil.isMyLauncherDefault(getApplicationContext())) {
                        Toast.makeText(getApplicationContext(), "Activated, please set the Kiosk App as default home screen.", Toast.LENGTH_LONG).show();
                        KioskModeUtil.triggerLauncherChooser(getApplicationContext());
//                        if (Build.VERSION.SDK_INT >= 23) {
//                            if (!Settings.canDrawOverlays(AppPreferences.this)) {
//                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                                        Uri.parse("package:" + getPackageName()));
//                                startActivity(intent);
//                            }
//                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Activated", Toast.LENGTH_LONG).show();
                    }
                    // triggerLauncherChooser();

                }
                return true;
            }
        });

        Preference AllowedApps = findPreference("pref_AllowedApps");

        AllowedApps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getApplicationContext(), AppChooserActivity.class);
                startActivity(intent);
                return true;
            }
        });

        Preference OrderAllowedApps = findPreference("pref_OrderAllowedApps");

        OrderAllowedApps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(getApplicationContext(), AppOrderActivity.class);
                startActivity(intent);
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_REQUEST_GALLERY && resultCode == RESULT_OK) {
            try {
                KioskModeUtil.saveBackground(this, getContentResolver().openInputStream(data.getData()));
                chooseBackgroundPref.setSummary(R.string.already_set);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}