package com.example.smartx;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ConnectionStateService extends Service {

    private static final String TAG = "ConnectionService";

    private DatabaseReference phoneConnectedRef;
    private DatabaseReference applicationConnectedRef;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver appStateReceiver;
    private boolean networkCallbackRegistered = false;

    private static final String CHANNEL_ID = "ConnectionStateServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_RESTART_SERVICE = "com.example.smartx.RESTART_SERVICE";

    @Override
    public void onCreate() {
        super.onCreate();
        phoneConnectedRef = FirebaseDatabase.getInstance().getReference("device/phone_connected");
        applicationConnectedRef = FirebaseDatabase.getInstance().getReference("device/application_connected");
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        Log.d(TAG, "Service Created. Setting application_connected to TRUE permanently.");
        applicationConnectedRef.setValue(true);

        createNotificationChannel();
        setupNetworkCallback();
        setupAppStateReceiver();
    }

    private void setupNetworkCallback() {
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                // Always re-arm the server-side onDisconnect so Firebase cleans up if
                // the client disappears again.
                phoneConnectedRef.onDisconnect().setValue(false);

                // Only mark phone as connected when the user actually has the app open.
                // If no activities are visible (service running after swipe-from-recents),
                // we must NOT set true — the user has left the app.
                if (SmartXApplication.getStartedActivities() > 0) {
                    Log.d(TAG, "Network AVAILABLE + app in foreground → phone_connected TRUE.");
                    phoneConnectedRef.setValue(true);
                } else {
                    Log.d(TAG, "Network AVAILABLE but no visible activities → keeping phone_connected FALSE.");
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                // No-op: cannot write to Firebase when there is no connection.
                // The server-side onDisconnect handler will set phone_connected = false.
                Log.d(TAG, "Network LOST. Relying on server-side onDisconnect to set phone_connected FALSE.");
            }
        };
    }

    private void setupAppStateReceiver() {
        appStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SmartXApplication.ACTION_APP_BACKGROUND.equals(intent.getAction())) {
                    Log.d(TAG, "APP_BACKGROUND received — setting phone_connected FALSE.");
                    phoneConnectedRef.setValue(false);
                } else if (SmartXApplication.ACTION_APP_FOREGROUND.equals(intent.getAction())) {
                    Log.d(TAG, "APP_FOREGROUND received — setting phone_connected TRUE and re-arming onDisconnect.");
                    phoneConnectedRef.onDisconnect().setValue(false);
                    phoneConnectedRef.setValue(true);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(SmartXApplication.ACTION_APP_BACKGROUND);
        filter.addAction(SmartXApplication.ACTION_APP_FOREGROUND);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(appStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(appStateReceiver, filter);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SmartX Service")
                .setContentText("Monitoring Internet Connection...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        // Check initial state
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        boolean isConnected = capabilities != null &&
                              (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                               capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        
        Log.d(TAG, "Initial network check. Is connected: " + isConnected
                + " | Visible activities: " + SmartXApplication.getStartedActivities());

        // Always re-arm Firebase's server-side onDisconnect so it cleans up if
        // the client disappears unexpectedly (process killed, WiFi lost, etc.).
        phoneConnectedRef.onDisconnect().setValue(false);

        // Only set phone_connected = true when the user is actually in the app.
        // If onStartCommand fires due to onTaskRemoved or START_STICKY restart
        // (no visible activities), we must NOT set true — keep it false.
        if (SmartXApplication.getStartedActivities() > 0) {
            phoneConnectedRef.setValue(isConnected);
        } else {
            Log.d(TAG, "Service (re)started with no visible activities — not setting phone_connected TRUE.");
        }

        // Guard against registering the callback multiple times (e.g. onTaskRemoved
        // causes onStartCommand to be called on the already-running service).
        if (!networkCallbackRegistered) {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
            networkCallbackRegistered = true;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "!!! SERVICE DESTROYED !!! Scheduling restart via AlarmManager...");

        if (networkCallback != null && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering network callback", e);
            }
        }
        networkCallbackRegistered = false;

        if (appStateReceiver != null) {
            try {
                unregisterReceiver(appStateReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering app state receiver", e);
            }
        }

        handler.removeCallbacksAndMessages(null);

        // Use AlarmManager so the restart survives process death.
        // On Android 12+, alarm-triggered broadcasts are explicitly allowed
        // to start foreground services — unlike regular sendBroadcast().
        scheduleServiceRestart(3_000);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.e(TAG, "!!! TASK REMOVED !!! Scheduling restart via AlarmManager...");
        // Schedule via AlarmManager — alarm is registered at the OS level so it
        // survives even if the process is killed immediately after task removal.
        scheduleServiceRestart(3_000);
    }

    /**
     * Schedules a service restart using AlarmManager.
     *
     * WHY AlarmManager instead of sendBroadcast():
     * - On Android 12+, starting a foreground service from a regular broadcast is
     *   blocked (ForegroundServiceStartNotAllowedException). Alarm-triggered
     *   broadcasts are explicitly exempted from this restriction.
     * - AlarmManager registers the alarm at the OS level, so it survives process
     *   death — unlike sendBroadcast() which is dropped if the process dies first.
     */
    private void scheduleServiceRestart(long delayMs) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(this, ServiceRestarter.class);
        intent.setAction(ACTION_RESTART_SERVICE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAt = SystemClock.elapsedRealtime() + delayMs;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+: check if exact alarms are permitted; fall back to inexact.
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
        } else {
            alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
        }

        Log.d(TAG, "Service restart scheduled via AlarmManager in " + (delayMs / 1000) + "s.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Connection State Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
