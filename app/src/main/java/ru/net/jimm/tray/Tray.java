package ru.net.jimm.tray;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.07.12 1:30
 *
 * @author vladimir
 */
public class Tray {

    private final Context context;
    private final NotificationManager mNM;

    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;

    private final Object[] mSetForegroundArgs = new Object[1];
    private final Object[] mStartForegroundArgs = new Object[2];
    private final Object[] mStopForegroundArgs = new Object[1];

    public Tray(Context context) {
        this.context = context;

        mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Class<?>[] mSetForegroundSignature = new Class[]{boolean.class};
            Class<?>[] mStartForegroundSignature = new Class[]{int.class, Notification.class};
            Class<?>[] mStopForegroundSignature = new Class[]{boolean.class};

            try {
                mStartForeground = context.getClass().getMethod("startForeground", mStartForegroundSignature);
                mStopForeground = context.getClass().getMethod("stopForeground", mStopForegroundSignature);
            } catch (NoSuchMethodException e) {
                // Running on an older platform.
                mStartForeground = mStopForeground = null;
            }
            try {
                mSetForeground = context.getClass().getMethod("setForeground", mSetForegroundSignature);
            } catch (NoSuchMethodException e) {
                mSetForeground = null;
            }
        }
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    public void startForegroundCompat(int id, Notification notification) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // If we have the new startForeground API, then use it.
            if (mStartForeground != null) {
                mStartForegroundArgs[0] = id;
                mStartForegroundArgs[1] = notification;
                invokeMethod(mStartForeground, mStartForegroundArgs);
                return;
            }

            // Fall back on the old API.
            mSetForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(mSetForeground, mSetForegroundArgs);
        }

        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    public void stopForegroundCompat(int id) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // If we have the new stopForeground API, then use it.
            if (mStopForeground != null) {
                mStopForegroundArgs[0] = Boolean.TRUE;
                invokeMethod(mStopForeground, mStopForegroundArgs);
                return;
            }
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            mSetForegroundArgs[0] = Boolean.FALSE;
            invokeMethod(mSetForeground, mSetForegroundArgs);
        }
    }

    void invokeMethod(Method method, Object[] args) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) return;

        try {
            method.invoke(context, args);
        } catch (Exception e) {
            // Should not happen.
        }
    }

    public void clear() {
        mNM.cancelAll();
    }
}
