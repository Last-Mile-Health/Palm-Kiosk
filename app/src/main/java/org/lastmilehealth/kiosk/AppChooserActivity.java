package org.lastmilehealth.kiosk;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.HashSet;
import java.util.SortedSet;

/**
 * Created by Andreas Schrade on 17.09.2015.
 */
public class AppChooserActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_chooser);

        final ListView appList = (ListView) findViewById(R.id.appsListView);
        Button saveButton = (Button) findViewById(R.id.saveBtn);

        final SortedSet<PackageInfo> filteredPackages = InstalledAppsUtil.getInstalledApps(getApplicationContext());
        final ListAdapter adapter = new ListAdapter(this, filteredPackages, getPackageManager());
        adapter.setAllowedPackages(new HashSet<>(PrefUtils.getAllowedApps(getApplicationContext()))); // make shallow copy
        adapter.setAppPackagesWithHiddenIcon(new HashSet<>(PrefUtils.getAppsWithHiddenIcon(getApplicationContext())));
        appList.setAdapter(adapter);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefUtils.setAllowedApps(adapter.getAllowedPackages(), getApplicationContext());
                PrefUtils.setAppsWithHiddenIcon(adapter.getAppPackagesWithHiddenIcon(), getApplicationContext());
                finish();
            }
        });
    }
}