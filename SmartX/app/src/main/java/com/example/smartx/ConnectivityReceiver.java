package com.example.smartx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class ConnectivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Toast.makeText(context, "No internet connection", Toast.LENGTH_LONG).show();
            Intent serviceIntent = new Intent(context, CutoffService.class);
            context.startService(serviceIntent);
        } else {
            Intent serviceIntent = new Intent(context, CutoffService.class);
            context.stopService(serviceIntent);
        }
    }
}