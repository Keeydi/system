package com.example.smartx;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// Dapat itong AppCompatActivity, HINDI BaseActivity, para hindi mag-infinite loop.
public class NoInternetActivity extends AppCompatActivity {

    private static final String TAG = "NoInternetActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        // *** ITO ANG MAHALAGANG DAGDAG ***
        // Dahil nasa screen na ito, siguradong walang internet. Isulat agad sa Firebase.
        Log.d(TAG, "NoInternetActivity created. Setting phone_connected to FALSE in Firebase.");
        DatabaseReference phoneConnectedRef = FirebaseDatabase.getInstance().getReference("device/phone_connected");
        phoneConnectedRef.setValue(false);

        Button retryButton = findViewById(R.id.retry_button);
        retryButton.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                Toast.makeText(this, "Internet connection restored!", Toast.LENGTH_SHORT).show();
                // If internet is back, go to MainActivity to restart the app flow properly.
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "No internet connection yet.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        try {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        } catch (Exception e) {
            return false;
        }
    }

    // Prevent the user from simply pressing back to bypass this screen.
    @Override
    public void onBackPressed() {
        // Do nothing, force user to use the retry button.
    }
}
