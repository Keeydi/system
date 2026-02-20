package com.example.smartx;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// Change AppCompatActivity to BaseActivity
public class Socket1Activity extends BaseActivity {

    // --- Data Class for Robustness ---
    private static class ApplianceUI {
        final Button button;
        final TextView textView;
        final String applianceName;

        ApplianceUI(Button button, TextView textView) {
            this.button = button;
            this.textView = textView;
            this.applianceName = textView.getText().toString();
        }
    }

    // --- Views ---
    private TextView connectionStatus;
    private Button onButton;
    private Button offButton;
    private ProgressBar connectionProgress;
    private TextView socketSubtitle;

    // --- State ---
    private final List<ApplianceUI> applianceUIs = new ArrayList<>();
    private ApplianceUI selectedAppliance = null;

    // --- Firebase ---
    private DatabaseReference socketRef;
    private ValueEventListener socketStateListener;

    // --- Android System ---
    private AlarmManager alarmManager;
    private PendingIntent applianceReminderPendingIntent;

    // --- Constants ---
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String SOCKET1_APPLIANCE = "socket1_appliance";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket1);

        // 1. Initialize Firebase reference
        socketRef = FirebaseDatabase.getInstance().getReference("sockets/s1");

        // 2. Initialize views
        connectionStatus = findViewById(R.id.connection_status);
        onButton = findViewById(R.id.on_button);
        offButton = findViewById(R.id.off_button);
        connectionProgress = findViewById(R.id.connection_progress);
        socketSubtitle = findViewById(R.id.socket_1_subtitle);

        // 3. Setup UI safely
        setupApplianceSelection();

        // 4. Load data and set up listener
        loadData();
        setupFirebaseListener();

        // 5. Set button listeners
        onButton.setOnClickListener(v -> {
            if (selectedAppliance != null) {
                socketRef.child("status").setValue("ON");
            }
        });

        offButton.setOnClickListener(v -> {
            if (selectedAppliance != null) {
                socketRef.child("status").setValue("OFF");
            }
        });

        // 6. Setup Navigation
        setupNavigation();

        ApplianceReminderReceiver.createNotificationChannel(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (socketRef != null && socketStateListener != null) {
            socketRef.child("status").removeEventListener(socketStateListener);
        }
    }

    private void setupFirebaseListener() {
        // Listen to the 'status' child for ON/OFF state
        socketStateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.exists() ? dataSnapshot.getValue(String.class) : "OFF";
                updateUIForSocketStatus(status);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        socketRef.child("status").addValueEventListener(socketStateListener);
    }

    private void setupApplianceSelection() {
        int[] buttonIds = {R.id.select_button_1, R.id.select_button_2, R.id.select_button_3, R.id.select_button_4, R.id.select_button_5};
        int[] textViewIds = {R.id.appliance_text_1, R.id.appliance_text_2, R.id.appliance_text_3, R.id.appliance_text_4, R.id.appliance_text_5};

        for (int i = 0; i < buttonIds.length; i++) {
            Button button = findViewById(buttonIds[i]);
            TextView textView = findViewById(textViewIds[i]);

            if (button != null && textView != null) {
                ApplianceUI ui = new ApplianceUI(button, textView);
                applianceUIs.add(ui);

                button.setOnClickListener(v -> onApplianceSelected(ui));
                textView.setOnClickListener(v -> showDetailsDialog(ui.applianceName));
            }
        }
    }

    private void onApplianceSelected(ApplianceUI tappedUI) {
        // *** ITO ANG MAJOR NA PAGBABAGO ***

        if (selectedAppliance != null && selectedAppliance.button == tappedUI.button) {
            // User is deselecting the current appliance
            selectedAppliance = null;
            socketRef.child("status").setValue("OFF"); // Automatically turn off socket
            socketRef.child("appliance_name").setValue("None"); // Update appliance name in Firebase
        } else {
            // User is selecting a new appliance
            selectedAppliance = tappedUI;
            socketRef.child("status").setValue("OFF"); // Ensure it's off initially
            socketRef.child("appliance_name").setValue(tappedUI.applianceName); // Update appliance name in Firebase
        }

        // Save to local storage for persistence
        String nameToSave = selectedAppliance != null ? selectedAppliance.applianceName : "Not Selected";
        saveData(nameToSave);
        updateDisplay();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String savedAppName = sharedPreferences.getString(SOCKET1_APPLIANCE, "Not Selected");

        selectedAppliance = null; // Reset
        for (ApplianceUI ui : applianceUIs) {
            if (ui.applianceName.equals(savedAppName)) {
                selectedAppliance = ui;
                break;
            }
        }
        updateDisplay();
    }

    private void updateDisplay() {
        String currentAppName = selectedAppliance != null ? selectedAppliance.applianceName : "Not Selected";
        socketSubtitle.setText(currentAppName);

        for (ApplianceUI ui : applianceUIs) {
            setButtonState(ui.button, selectedAppliance != null && ui.button == selectedAppliance.button);
        }

        onButton.setEnabled(selectedAppliance != null);
        offButton.setEnabled(selectedAppliance != null);

        if (selectedAppliance == null) {
            onButton.setEnabled(false);
            offButton.setEnabled(false);
        }
    }

    private void updateUIForSocketStatus(String status) {
        boolean isConnected = "ON".equals(status);
        connectionStatus.setText(isConnected ? "Connected" : "Not connected");
        setButtonActive(onButton, isConnected);
        setButtonActive(offButton, !isConnected);
        connectionProgress.setIndeterminate(isConnected);
        connectionProgress.setIndeterminateTintList(ColorStateList.valueOf(Color.parseColor(isConnected ? "#FFA500" : "#888888")));

        if (isConnected && selectedAppliance != null) {
            scheduleApplianceReminder("s1");
        } else {
            cancelApplianceReminder();
        }
    }

    private void setupNavigation() {
        findViewById(R.id.info_icon).setOnClickListener(v -> startActivity(new Intent(this, GuideActivity.class)));
        findViewById(R.id.settings_icon).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));

        TabLayout tabLayout = findViewById(R.id.socket_tabs);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    startActivity(new Intent(Socket1Activity.this, Socket2Activity.class));
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        findViewById(R.id.homepage_button).setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.socket_button).setOnClickListener(v -> {
        });
        findViewById(R.id.user_button).setOnClickListener(v -> startActivity(new Intent(this, UserActivity.class)));
    }

    private void showDetailsDialog(String name) {
        String voltage = "220-240 V";
        String wattage = "-";
        String frequency = "60 Hz";

        if (name.contains("Fan 1")) wattage = "50 Watts";
        else if (name.contains("Fan 2")) wattage = "60 Watts";
        else if (name.contains("Fan 3")) wattage = "70 Watts";
        else if (name.contains("Television")) wattage = "80 Watts";
        else if (name.contains("Refrigerator")) wattage = "150 Watts";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_details, null);
        builder.setView(dialogView);

        ((TextView) dialogView.findViewById(R.id.detail_name)).setText(name);
        ((TextView) dialogView.findViewById(R.id.detail_voltage)).setText("Voltage: " + voltage);
        ((TextView) dialogView.findViewById(R.id.detail_wattage)).setText("Wattage: " + wattage);
        ((TextView) dialogView.findViewById(R.id.detail_frequency)).setText("Frequency: " + frequency);

        builder.create().show();
    }

    private void setButtonState(Button button, boolean isSelected) {
        button.setSelected(isSelected);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isSelected ? "#FFA500" : "#333333")));
        button.setTextColor(isSelected ? Color.BLACK : Color.WHITE);
    }

    private void setButtonActive(Button button, boolean isActive) {
        // This just reflects the ON/OFF state, distinct from selection
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isActive ? "#FFA500" : "#333333")));
        button.setTextColor(isActive ? Color.BLACK : Color.WHITE);
    }

    private void saveData(String applianceName) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        sharedPreferences.edit().putString(SOCKET1_APPLIANCE, applianceName).apply();
    }

    private void scheduleApplianceReminder(String socketId) {
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, ApplianceReminderReceiver.class);
        intent.putExtra("socket_id", socketId);
        applianceReminderPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerTime = SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR * 3;
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, AlarmManager.INTERVAL_HOUR * 3, applianceReminderPendingIntent);
    }

    private void cancelApplianceReminder() {
        if (alarmManager != null && applianceReminderPendingIntent != null) {
            alarmManager.cancel(applianceReminderPendingIntent);
        }
    }
}
