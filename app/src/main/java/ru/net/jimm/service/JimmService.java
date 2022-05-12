package ru.net.jimm.service;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jimm.Jimm;
import jimm.cl.JimmModel;
import jimmui.model.chat.ChatModel;
import protocol.ProtocolHelper;
import ru.net.jimm.JimmActivity;
import ru.net.jimm.R;
import ru.net.jimm.tray.Tray;

public class JimmService extends Service {
    private static final String LOG_TAG = "JimmService";

    //private final Messenger messenger = new Messenger(new Handler(new IncomingMessageHandler()));
    private final Binder localBinder = new LocalBinder();

    //private MusicReceiver musicReceiver;
    private Tray tray = null;
    private WakeControl wakeLock;
    private JimmModel jimmModel;

    public static final int UPDATE_APP_ICON = 1;
    public static final int UPDATE_CONNECTION_STATUS = 2;
    public static final int CONNECT = 3;
    public static final int STARTED = 4;
    public static final int QUIT = 5;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "onStart();");
        wakeLock = new WakeControl(this);

        tray = new Tray(this);
        //Foreground Service
        //tray.startForegroundCompat(R.string.app_name, getNotification());

        //musicReceiver = new MusicReceiver(this);
        //this.registerReceiver(musicReceiver, musicReceiver.getIntentFilter());
        //scrobbling finished
    }


    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy();");
        wakeLock.release();
        //this.unregisterReceiver(musicReceiver);
        tray.stopForegroundCompat(R.string.app_name);
        jimmModel = null;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        System.out.println("service obBind " + arg0);
        //return messenger.getBinder();
        return localBinder;
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private Notification getNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            int unread = getPersonalUnreadMessageCount(false);
            int allUnread = getPersonalUnreadMessageCount(true);
            CharSequence stateMsg = "";

            boolean version2 = (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
            final int icon;
            if (0 < allUnread) {
                icon = version2 ? R.drawable.ic2_tray_msg : R.drawable.ic3_tray_msg;
            } else if (Jimm.getJimm().jimmModel.isConnected()) {
                icon = version2 ? R.drawable.ic2_tray_on : R.drawable.ic3_tray_on;
                stateMsg = getText(R.string.online);
            } else {
                icon = version2 ? R.drawable.ic2_tray_off : R.drawable.ic3_tray_off;
                if (Jimm.getJimm().jimmModel.isConnecting()) {
                    stateMsg = getText(R.string.connecting);
                } else {
                    stateMsg = getText(R.string.offline);
                }
            }

            final Notification notification = new Notification(icon, getText(R.string.app_name), 0);
            if (0 < allUnread) {
                notification.number = allUnread;
                stateMsg = String.format((String) getText(R.string.unreadMessages), allUnread);
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                // new
                notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0);
                notification.tickerText = stateMsg;
            } else {
                PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), 0);

                try {
                    Method deprecatedMethod = notification.getClass().getMethod("setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
                    deprecatedMethod.invoke(notification, this, getText(R.string.app_name), stateMsg, pendingIntent);
                } catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    Log.w("TAG", "Method not found", e);
                }
            }

            notification.defaults = 0;
            if (0 < unread) {
                notification.ledARGB = 0xff00ff00;
                notification.ledOnMS = 300;
                notification.ledOffMS = 1000;
                notification.flags |= Notification.FLAG_SHOW_LIGHTS;
            }
            return notification;
        } else {
            int allUnread = getPersonalUnreadMessageCount(true);
            CharSequence stateMsg = "";

            final int icon;
            if (0 < allUnread) {
                icon = R.drawable.ic3_tray_msg;
            } else if (Jimm.getJimm().jimmModel.isConnected()) {
                icon = R.drawable.ic3_tray_on;
                stateMsg = getText(R.string.online);
            } else {
                icon = R.drawable.ic3_tray_off;
                if (Jimm.getJimm().jimmModel.isConnecting()) {
                    stateMsg = getText(R.string.connecting);
                } else {
                    stateMsg = getText(R.string.offline);
                }
            }

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            String CHANNEL_ID = "jimm_channel";
            CharSequence name = "my_channel";
            String Description = "This is my channel";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mChannel);


            Notification.Builder notificationBuilder = new Notification.Builder(this, CHANNEL_ID);
            notificationBuilder.setSmallIcon(icon)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(stateMsg);
            //.setPriority(Notification.PRIORITY_MAX);

            Intent resultIntent = new Intent(this, JimmActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addParentStack(JimmActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.setContentIntent(resultPendingIntent);


            return notificationBuilder.build();

        }
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case UPDATE_CONNECTION_STATUS:
                tray.startForegroundCompat(R.string.app_name, getNotification());
                wakeLock.updateLock();
                break;
            case UPDATE_APP_ICON:
                tray.startForegroundCompat(R.string.app_name, getNotification());
                break;
            case CONNECT:
                ProtocolHelper.connect(jimmModel.protocols.elementAt(msg.arg1));
                break;
            case STARTED:
                jimmModel = Jimm.getJimm().jimmModel;
                tray.startForegroundCompat(R.string.app_name, getNotification());
                break;
            case QUIT:
                tray.clear();
                stopSelf();
                break;
            default:
        }
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public JimmService getService() {
            return JimmService.this;
        }
    }

    /*
    private class IncomingMessageHandler implements Handler.Callback {
        public boolean handleMessage(Message msg) {
            try {
                return JimmService.this.handleMessage(msg);
            } catch (Exception e) {
                return false;
            }
        }

     */

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private int getPersonalUnreadMessageCount(boolean all) {
        int count = 0;
        for (ChatModel c : jimmModel.chats) {
            if (all || c.isHuman() || !c.getContact().isSingleUserContact()) {
                count += c.getPersonalUnreadMessageCount();
            }
        }
        return count;
    }
}
