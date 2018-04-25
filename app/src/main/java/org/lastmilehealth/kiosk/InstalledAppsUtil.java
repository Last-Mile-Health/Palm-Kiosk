package org.lastmilehealth.kiosk;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Utility to handle all installed apps.
 */
public class InstalledAppsUtil {

    public static SortedSet<PackageInfo> getInstalledApps(Context ctx) {


        final PackageManager packageManager = ctx.getPackageManager();



        final List<PackageInfo> allInstalledPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA);
        final SortedSet<PackageInfo> filteredPackages = new TreeSet(new Comparator() {
            @Override
            public int compare(Object lhs, Object rhs) {
                PackageInfo left = (PackageInfo)lhs;
                PackageInfo right = (PackageInfo)rhs;
                return packageManager.getApplicationLabel(left.applicationInfo).toString().compareTo(packageManager.getApplicationLabel(right.applicationInfo).toString());
            }
        });

        Drawable defaultActivityIcon = packageManager.getDefaultActivityIcon();

        for(PackageInfo each : allInstalledPackages) {
            if(ctx.getPackageName().equals(each.packageName)) {
                continue;  // skip own app
            }

            try {
                // add only apps with application icon
                Intent launchIntentForPackage = packageManager.getLaunchIntentForPackage(each.packageName);
                if(launchIntentForPackage == null)
                    continue;

                Drawable applicationIcon = packageManager.getActivityIcon(launchIntentForPackage);
                if(applicationIcon != null && !defaultActivityIcon.equals(applicationIcon)) {
                    filteredPackages.add(each);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.i("Kiosk", "Name not found " + each.packageName);
            }
        }

        return filteredPackages;

    }
}
