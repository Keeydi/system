package com.example.smartx;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String SERVICE_KEEPER_WORK_TAG = "ServiceKeeperWork";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAndRequestBatteryOptimizationPermission();
        setupPeriodicWorker();

        Button startButton = findViewById(R.id.start_button);

        startButton.setOnClickListener(v -> {
            startConnectionService(); // Start the background service

            if (isNetworkAvailable()) {
                Intent modeIntent = new Intent(MainActivity.this, ModeActivity.class);
                startActivity(modeIntent);
                // finish(); // DO NOT call finish() here. This was causing the app to crash.
            } else {
                // Write 'false' to Firebase because there is no internet.
                DatabaseReference phoneConnectedRef = FirebaseDatabase.getInstance().getReference("device/phone_connected");
                phoneConnectedRef.setValue(false);

                // Show the NoInternetActivity.
                Intent noInternetIntent = new Intent(MainActivity.this, NoInternetActivity.class);
                startActivity(noInternetIntent);
            }
        });
    }

    private void setupPeriodicWorker() {
        PeriodicWorkRequest keepAliveRequest = new PeriodicWorkRequest.Builder(
                ServiceKeepAliveWorker.class, 15, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                SERVICE_KEEPER_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                keepAliveRequest);
    }

    private void checkAndRequestBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                        .setTitle("Background Operation Permission")
                        .setMessage("To ensure SmartX can monitor your connection status, please allow it to run in the background without battery restrictions.")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
            }
        }
    }

    private void startConnectionService() {
        Intent serviceIntent = new Intent(this, ConnectionStateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    // Restore the self-contained network check method
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
