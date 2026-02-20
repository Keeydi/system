package com.example.smartx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final int NO_INTERNET_NOTIFICATION_ID = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        DatabaseReference phoneSsidRef = FirebaseDatabase.getInstance().getReference("wifi/phone_ssid");

        if (isConnected) {
            NotificationManagerCompat.from(context).cancel(NO_INTERNET_NOTIFICATION_ID);
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // Connected to a Wi-Fi network, so update the SSID in Firebase
                String phoneSSID = getPhoneSsid(context);
                phoneSsidRef.setValue(phoneSSID);
                compareSsids(context, phoneSSID);
            } else {
                // Connected to cellular or other network, so clear the SSID and update presence
                phoneSsidRef.setValue(null);
                updatePresence(context, "AWAY");
            }
        } else {
            // Not connected to any network
            phoneSsidRef.setValue(null);
            updatePresence(context, "AWAY");
            showNoInternetNotification(context);
        }
    }

    private void compareSsids(Context context, String phoneSSID) {
        if (phoneSSID == null) {
            updatePresence(context, "AWAY");
            return;
        }

        DatabaseReference esp32SsidRef = FirebaseDatabase.getInstance().getReference("wifi/esp32_ssid");
        esp32SsidRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String esp32SSID = dataSnapshot.getValue(String.class);
                if (esp32SSID != null && phoneSSID.equals(esp32SSID)) {
                    updatePresence(context, "HOME");
                    NotificationManagerCompat.from(context).cancel(4); // Cancel remote notification
                } else {
                    updatePresence(context, "AWAY");
                    sendConnectedNetworkNotification(context, phoneSSID);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                updatePresence(context, "AWAY");
            }
        });
    }

    private void updatePresence(Context context, String status) {
        DatabaseReference presenceRef = FirebaseDatabase.getInstance().getReference("wifi/presence");
        presenceRef.setValue(status);

        SharedPreferences.Editor editor = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit();
        editor.putBoolean("is_at_home", "HOME".equals(status));
        editor.apply();
    }

    private String getPhoneSsid(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                if (ssid != null && !ssid.isEmpty() && !ssid.equals("<unknown ssid>")) {
                    return ssid.replace("\"", "");
                }
            }
        }
        return null;
    }

    private void sendConnectedNetworkNotification(Context context, String ssid) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "connected_network_channel")
                .setSmallIcon(R.drawable.ic_info)
                .setContentTitle("Remote Connection Active")
                .setContentText("Connected to " + ssid)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        NotificationManagerCompat.from(context).notify(4, builder.build());
    }

    private void showNoInternetNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "no_internet_channel")
                .setSmallIcon(R.drawable.ic_info)
                .setContentTitle("No Internet Connection")
                .setContentText("Please check your internet connection.")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat.from(context).notify(NO_INTERNET_NOTIFICATION_ID, builder.build());
    }
}
