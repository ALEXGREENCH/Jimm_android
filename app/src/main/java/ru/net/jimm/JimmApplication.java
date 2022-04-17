package ru.net.jimm;

import android.app.Application;

public class JimmApplication extends Application {

    public static JimmApplication application;

    @Override
    public void onCreate() {
        super.onCreate();
        application = this;
    }
}
