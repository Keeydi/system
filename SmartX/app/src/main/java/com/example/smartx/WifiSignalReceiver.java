package com.example.smartx;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WifiSignalReceiver extends BroadcastReceiver {

    private static final String WIFI_SIGNAL_CHANNEL_ID = "wifi_signal_channel";
    private static final int WIFI_SIGNAL_NOTIFICATION_ID = 2;
    private static final int RSSI_THRESHOLD = -75; // dBm

    @Override
    public void onReceive(Context context, Intent intent) {
        if (WifiManager.RSSI_CHANGED_ACTION.equals(intent.getAction())) {
            FirebaseDatabase.getInstance().getReference("wifi/presence").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String presence = dataSnapshot.getValue(String.class);
                    if ("HOME".equals(presence)) {
                        int signalStrength = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -127);
                        if (signalStrength < RSSI_THRESHOLD) {
                            sendNotification(context, "You're getting out of range", "Your Wi-Fi signal is weak.");
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });
        }
    }

    private void sendNotification(Context context, String title, String message) {
        createNotificationChannel(context);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, WIFI_SIGNAL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_info) 
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(WIFI_SIGNAL_NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Wi-Fi Signal";
            String description = "Notifications for Wi-Fi signal strength";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(WIFI_SIGNAL_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
