package com.example.smartx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";
    private ConnectivityReceiver connectivityReceiver;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Instantiating ConnectivityReceiver.");
        connectivityReceiver = new ConnectivityReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Registering connectivity receiver.");
        registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        Log.d(TAG, "onResume: Receiver REGISTERED.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Unregistering connectivity receiver.");
        unregisterReceiver(connectivityReceiver);
        Log.d(TAG, "onPause: Receiver UNREGISTERED.");
    }

    // This is a static class. It is only active when an activity is in the foreground
    // because we register and unregister it in onResume/onPause.
    public static class ConnectivityReceiver extends BroadcastReceiver {
        private static final String RECEIVER_TAG = "ConnectivityReceiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {

                if (!isNetworkAvailable(context)) {
                    Log.w(RECEIVER_TAG, "Foreground network loss detected!");

                    // 1. Write 'false' to Firebase immediately.
                    DatabaseReference phoneConnectedRef = FirebaseDatabase.getInstance().getReference("device/phone_connected");
                    phoneConnectedRef.setValue(false);

                    // 2. Show the NoInternetActivity directly.
                    // FLAG_ACTIVITY_NEW_TASK is required because we are starting it from a receiver.
                    // FLAG_ACTIVITY_CLEAR_TASK ensures the user can't go back to a broken state.
                    Intent noInternetIntent = new Intent(context, NoInternetActivity.class);
                    noInternetIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(noInternetIntent);
                }
            }
        }

        private boolean isNetworkAvailable(Context context) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            try {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            } catch (Exception e) {
                Log.e(RECEIVER_TAG, "Error checking network availability", e);
                return false;
            }
        }
    }
}
