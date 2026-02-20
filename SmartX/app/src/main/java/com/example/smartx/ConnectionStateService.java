package com.example.smartx;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
        Log.e(TAG, "!!! SERVICE DESTROYED !!! Attempting to restart...");
        
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

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_RESTART_SERVICE);
        broadcastIntent.setClass(this, ServiceRestarter.class);
        this.sendBroadcast(broadcastIntent);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.e(TAG, "!!! TASK REMOVED !!! Restarting service...");
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_RESTART_SERVICE);
        broadcastIntent.setClass(this, ServiceRestarter.class);
        this.sendBroadcast(broadcastIntent);
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
