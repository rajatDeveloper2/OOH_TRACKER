package com.oohtracker;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.graphics.Color;
import android.os.Build;

import androidx.multidex.MultiDexApplication;

import com.anggrayudi.storage.SimpleStorage;
import com.oohtracker.room.FileDataViewModel;


public class MainApplication extends MultiDexApplication {

    public static final String PRIMARY_CHANNEL = "default";
    public static final String KEY_WAKELOCK = "WAKELOCK";
    static FileDataViewModel wordViewModel;

    static public FileDataViewModel getFileDataViewModel() {
        return wordViewModel;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        System.setProperty("http.keepAliveDuration", String.valueOf((30 * 60 * 1000)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerChannel();
        }
        wordViewModel = new FileDataViewModel(this);
        // Initialize FileX

    }

    @TargetApi(Build.VERSION_CODES.O)
    private void registerChannel() {
        NotificationChannel channel = new NotificationChannel(
                PRIMARY_CHANNEL, getString(R.string.channel_default), NotificationManager.IMPORTANCE_LOW
        );
        channel.setLightColor(Color.GREEN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
    }


}
