package com.example.smartx;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Tracks whether any activity is visible. If no activity is visible for
 * BACKGROUND_DELAY_MS, a broadcast is sent so ConnectionStateService can
 * proactively mark phone_connected = false — instead of waiting ~2 minutes
 * for Firebase's server-side onDisconnect to fire.
 */
public class SmartXApplication extends Application {

    private static final String TAG = "SmartXApplication";

    public static final String ACTION_APP_BACKGROUND = "com.example.smartx.APP_BACKGROUND";
    public static final String ACTION_APP_FOREGROUND  = "com.example.smartx.APP_FOREGROUND";

    /** How long the app must stay in the background before we signal disconnect. */
    private static final long BACKGROUND_DELAY_MS = 20_000; // 20 seconds

    private int startedActivities = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable backgroundRunnable;

    @Override
    public void onCreate() {
        super.onCreate();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivities++;
                if (startedActivities == 1) {
                    // At least one activity is now visible — app is in foreground.
                    if (backgroundRunnable != null) {
                        handler.removeCallbacks(backgroundRunnable);
                        backgroundRunnable = null;
                    }
                    Log.d(TAG, "App entered FOREGROUND — signalling connected.");
                    sendBroadcast(new Intent(ACTION_APP_FOREGROUND));
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivities--;
                if (startedActivities == 0) {
                    // No activity is visible — schedule the background signal.
                    Log.d(TAG, "App entered BACKGROUND — scheduling disconnect in "
                            + (BACKGROUND_DELAY_MS / 1000) + "s.");
                    backgroundRunnable = () -> {
                        Log.d(TAG, "Background delay elapsed — broadcasting APP_BACKGROUND.");
                        sendBroadcast(new Intent(ACTION_APP_BACKGROUND));
                    };
                    handler.postDelayed(backgroundRunnable, BACKGROUND_DELAY_MS);
                }
            }

            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityResumed(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }
}
