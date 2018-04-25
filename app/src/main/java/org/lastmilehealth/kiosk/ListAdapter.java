package org.lastmilehealth.kiosk;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * Adapter to show apps.
 */
public class ListAdapter extends BaseAdapter {

    private Activity context;
    private List<PackageInfo> packageList;
    private PackageManager packageManager;
    private HashSet<String> appPackagesWithHiddenIcon;
    private Set<String> allowedPackages = new HashSet<>();
    private Set<String> cameraPackages = new HashSet<>();
    public ListAdapter(Activity context, SortedSet<PackageInfo> packageList, PackageManager packageManager) {
        super();
        this.context = context;
        this.packageList = new ArrayList<>(packageList);
        this.packageManager = packageManager;

        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        List<ResolveInfo> listCam = packageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo res : listCam) {
            cameraPackages.add(res.activityInfo.packageName);
        }
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        LayoutInflater inflater = context.getLayoutInflater();

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_item, null);
            holder = new ViewHolder();

            holder.appName = (TextView) convertView.findViewById(R.id.appNameTxtView);
            holder.packageName = (TextView) convertView.findViewById(R.id.packageNameTxtView);
            holder.allowedChk = (CheckBox) convertView.findViewById(R.id.allowedChk);
            holder.hiddenIconChk = (CheckBox) convertView.findViewById(R.id.hiddenIconChk);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        PackageInfo packageInfo = (PackageInfo) getItem(position);

        Drawable appIcon = packageManager.getApplicationIcon(packageInfo.applicationInfo);
        String appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString();
        String packageName = packageInfo.packageName;
        appIcon.setBounds(0, 0, 40, 40);

        holder.appName.setCompoundDrawables(appIcon, null, null, null);
        holder.appName.setCompoundDrawablePadding(15);
        holder.appName.setText(appName);

        if(cameraPackages.contains(packageName)) {
            holder.appName.setText(appName + " - Camera");
        }

        holder.packageName.setText(packageName);

        if(appPackagesWithHiddenIcon.contains(holder.packageName.getText().toString())) {
            appIcon.setAlpha(50);
        } else {
            appIcon.setAlpha(255);
        }

        holder.allowedChk.setChecked(allowedPackages.contains(packageName));
        holder.allowedChk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.allowedChk.isChecked()) {
                    allowedPackages.add(holder.packageName.getText().toString());
                } else {
                    allowedPackages.remove(holder.packageName.getText().toString());
                }
                notifyDataSetChanged();
            }
        });

        holder.hiddenIconChk.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.hiddenIconChk.isChecked()) {
                    appPackagesWithHiddenIcon.add(holder.packageName.getText().toString());
                } else {
                    appPackagesWithHiddenIcon.remove(holder.packageName.getText().toString());
                }
                notifyDataSetChanged();
            }
        });

        if(allowedPackages.contains(holder.packageName.getText().toString())) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.allowed_bg));
            holder.hiddenIconChk.setVisibility(View.VISIBLE);
            holder.hiddenIconChk.setChecked(appPackagesWithHiddenIcon.contains(holder.packageName.getText().toString()));
        } else {
            convertView.setBackgroundResource(0);
            holder.hiddenIconChk.setVisibility(View.INVISIBLE);
            holder.hiddenIconChk.setChecked(false);
        }
        return convertView;
    }

    private class ViewHolder {
        TextView appName;
        TextView packageName;
        CheckBox allowedChk;
        CheckBox hiddenIconChk;
    }

    public int getCount() {
        return packageList.size();
    }

    public Object getItem(int position) {
        return packageList.get(position);
    }

    public Set<String> getAllowedPackages() {
        return allowedPackages;
    }

    public void setAllowedPackages(Set<String> allowedPackages) {
        this.allowedPackages = allowedPackages;
    }

    public void setAppPackagesWithHiddenIcon(HashSet<String> appPackagesWithHiddenIcon) {
        this.appPackagesWithHiddenIcon = appPackagesWithHiddenIcon;
    }

    public HashSet<String> getAppPackagesWithHiddenIcon() {
        return appPackagesWithHiddenIcon;
    }

    public long getItemId(int position) {
        return 0;
    }
}
