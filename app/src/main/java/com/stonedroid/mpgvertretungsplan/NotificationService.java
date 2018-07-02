package com.stonedroid.mpgvertretungsplan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

public class NotificationService extends Service
{
    private String channelID = getPackageName() + ".notification";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Example title")
                .setContentText("Example text")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        createNotificationChannel();

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        manager.notify(0, builder.build());

        return Service.START_NOT_STICKY;
    }

    // Creates a required notification channel for Android > 8.0
    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String name = "Channel";
            String description = "Example channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            channel.setDescription(description);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
