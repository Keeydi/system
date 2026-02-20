package com.example.smartx;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.database.FirebaseDatabase;

public class ApplianceReminderActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String socketId = intent.getStringExtra("socket_id");
        int notificationId = intent.getIntExtra("notification_id", 0);

        if ("YES_ACTION".equals(intent.getAction())) {
            // User is still using the appliance, so we just dismiss the notification.
        } else if ("NO_ACTION".equals(intent.getAction())) {
            if (socketId != null) {
                FirebaseDatabase.getInstance().getReference("sockets").child(socketId).setValue("OFF");
            }
        }

        // Dismiss the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
    }
}