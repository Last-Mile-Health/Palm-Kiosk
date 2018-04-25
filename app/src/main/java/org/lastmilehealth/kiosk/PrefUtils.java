package org.lastmilehealth.kiosk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashSet;
import java.util.Set;

/**
 * Utils to access preferences (Shared Preferences)
 */
public class PrefUtils {
    private static final String PREF_ALLOWED_APPS = "allowed_apps";
    private static final String PREF_KIOSK_MODE = "pref_kiosk_mode";
    private static final String PREF_ORDER_PREFIX_ = "order_of_";
    private static final String PREF_HIDDEN_ICON_APPS = "pref_hidden_icon_apps";
    private static final String PREF_BACKGROUND = "pref_background";

    public static boolean isKioskModeActive(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_KIOSK_MODE, false);
    }

    public static void setKioskModeActive(final boolean active, final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_KIOSK_MODE, active).commit();
    }

    public static boolean isBackgroundSet(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_BACKGROUND, false);
    }

    public static void setIsBackground(boolean bgSet, Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_BACKGROUND, bgSet).apply();
    }

    public static Set<String> getAllowedApps(final Context context) {
        return readStringSetProperty(PREF_ALLOWED_APPS, context);
    }

    public static void setAllowedApps(final Set<String> allowedApps, final Context context) {
        writeStringSetProperty(PREF_ALLOWED_APPS, allowedApps, context);
    }

    public static Set<String> getAppsWithHiddenIcon(final Context context) {
        return readStringSetProperty(PREF_HIDDEN_ICON_APPS, context);
    }

    public static void setAppsWithHiddenIcon(final Set<String> hiddenAppIcons, final Context context) {
        writeStringSetProperty(PREF_HIDDEN_ICON_APPS, hiddenAppIcons, context);
    }

    public static void setOrderForApp(Context context, String packageName, int order) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(PREF_ORDER_PREFIX_ + packageName, order).apply();
    }

    public static int getOrderForApp(Context context, String packageName) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(PREF_ORDER_PREFIX_ + packageName, Integer.MAX_VALUE);
    }

    private static void writeStringSetProperty(String field, Set<String> value, final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            sp.edit().putStringSet(field, value).apply();
        } else {
            sp.edit().putString(field, new JSONArray(value).toString()).apply();
        }
    }

    private static Set<String> readStringSetProperty(String field, final Context context) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { //
            return sp.getStringSet(field, new HashSet<String>());
        } else {
            JSONArray jsonArray;
            final Set<String> result = new HashSet<>();
            try {
                jsonArray = new JSONArray(sp.getString(field, "null"));
                for (int i = 0; i < jsonArray.length(); ++i)
                    result.add(jsonArray.getString(i));

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return result;
        }
    }
}
