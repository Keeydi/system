package com.example.smartx;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ServiceKeepAliveWorker extends Worker {

    private static final String TAG = "ServiceKeepAliveWorker";

    public ServiceKeepAliveWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Worker is running, ensuring service is started.");
        Intent serviceIntent = new Intent(getApplicationContext(), ConnectionStateService.class);
        try {
            // Siguraduhing tumatakbo ang ConnectionStateService
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(serviceIntent);
            } else {
                getApplicationContext().startService(serviceIntent);
            }
            Log.d(TAG, "Service start command sent successfully from Worker.");
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service from worker", e);
            // Kung mag-fail, ipaalam sa WorkManager para subukan ulit mamaya.
            return Result.retry();
        }
    }
}
