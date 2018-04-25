package org.lastmilehealth.kiosk;

import android.app.Application;

/**
 * Created by Andreas Schrade on 17.09.2015.
 */
public class AppContext extends Application {

    private AppContext instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

}
