package com.example.smartx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ServiceRestarter extends BroadcastReceiver {

    private static final String TAG = "ServiceRestarter";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast received, action: " + intent.getAction());
        // We are starting a foreground service, which is allowed in most cases for background execution.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, ConnectionStateService.class));
        } else {
            context.startService(new Intent(context, ConnectionStateService.class));
        }
    }
}
