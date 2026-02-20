package com.example.smartx;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                String ssid = wifiInfo.getSSID().replace("\"", "");
                updateFirebase(context, ssid);
            }
        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(intent.getAction())) {
            int rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
            if (rssi < -80) { // This is an example threshold, you may need to adjust it
                sendOutOfRangeNotification(context);
            }
        }
    }

    private void updateFirebase(Context context, String ssid) {
        DatabaseReference wifiRef = FirebaseDatabase.getInstance().getReference("wifi");
        wifiRef.child("phone_ssid").setValue(ssid);

        wifiRef.child("esp32_ssid").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String esp32Ssid = dataSnapshot.getValue(String.class);
                if (esp32Ssid != null) {
                    if (esp32Ssid.equals(ssid)) {
                        wifiRef.child("presence").setValue("HOME");
                    } else {
                        wifiRef.child("presence").setValue("AWAY");
                        sendSsidChangeNotification(context, ssid);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void sendSsidChangeNotification(Context context, String ssid) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "wifi_signal_channel")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Wi-Fi Connection Change")
                .setContentText("You're connected to " + ssid)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(2, builder.build());
    }

    private void sendOutOfRangeNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "wifi_signal_channel")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle("Wi-Fi Signal Low")
                .setContentText("You\'re getting out of range")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(3, builder.build());
    }
}
