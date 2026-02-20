package com.example.smartx;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ApplianceReminderReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "appliance_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String socketId = intent.getStringExtra("socket_id");
        int notificationId = 3; // Unique ID for this notification

        Intent yesIntent = new Intent(context, ApplianceReminderActionReceiver.class);
        yesIntent.setAction("YES_ACTION");
        yesIntent.putExtra("socket_id", socketId);
        yesIntent.putExtra("notification_id", notificationId);
        PendingIntent yesPendingIntent = PendingIntent.getBroadcast(context, 0, yesIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent noIntent = new Intent(context, ApplianceReminderActionReceiver.class);
        noIntent.setAction("NO_ACTION");
        noIntent.putExtra("socket_id", socketId);
        noIntent.putExtra("notification_id", notificationId);
        PendingIntent noPendingIntent = PendingIntent.getBroadcast(context, 1, noIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Appliance still running")
                .setContentText("Are you still using this appliance?")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(R.drawable.ic_action_yes, "Yes", yesPendingIntent)
                .addAction(R.drawable.ic_action_no, "No", noPendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, builder.build());
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Appliance Reminder";
            String description = "Channel for appliance usage reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
