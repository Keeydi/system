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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// Change AppCompatActivity to BaseActivity
public class LightPage extends BaseActivity {

    // --- Data Class para sa UI, katulad ng sa Socket pages ---
    private static class LightAreaUI {
        final Button button;
        final TextView textView;
        final String areaName;

        LightAreaUI(Button button, TextView textView) {
            this.button = button;
            this.textView = textView;
            this.areaName = textView.getText().toString();
        }
    }

    // --- Views ---
    private TextView connectionStatus;
    private Button onButton;
    private Button offButton;
    private ProgressBar connectionProgress;
    private TextView lightPageSubtitle;

    // --- State ---
    private final List<LightAreaUI> lightAreaUIs = new ArrayList<>();
    private LightAreaUI selectedArea = null;

    // --- Firebase ---
    private DatabaseReference lightRef;
    private ValueEventListener lightStateListener;

    // --- Android System ---
    private AlarmManager alarmManager;
    private PendingIntent lightReminderPendingIntent;

    // --- Constants ---
    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String LIGHT_AREA = "light_area";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_page);

        // 1. Initialize Firebase reference sa bagong structure
        lightRef = FirebaseDatabase.getInstance().getReference("lights/l1");

        // 2. Initialize views
        connectionStatus = findViewById(R.id.connection_status);
        onButton = findViewById(R.id.on_button);
        offButton = findViewById(R.id.off_button);
        connectionProgress = findViewById(R.id.connection_progress);
        lightPageSubtitle = findViewById(R.id.light_page_subtitle);

        // 3. Ibalik ang Area Selection logic
        setupAreaSelection();

        // 4. Load saved data at mag-listen sa Firebase
        loadData();
        setupFirebaseListener();

        // 5. Set button listeners
        onButton.setOnClickListener(v -> {
            if (selectedArea != null) {
                lightRef.child("status").setValue("ON");
            }
        });

        offButton.setOnClickListener(v -> {
            if (selectedArea != null) {
                lightRef.child("status").setValue("OFF");
            }
        });

        // 6. Setup Navigation
        setupNavigation();

        ApplianceReminderReceiver.createNotificationChannel(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lightRef != null && lightStateListener != null) {
            lightRef.removeEventListener(lightStateListener);
        }
    }

    private void setupFirebaseListener() {
        lightStateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String status = dataSnapshot.child("status").exists() ? dataSnapshot.child("status").getValue(String.class) : "OFF";
                updateUIForLightStatus(status);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        };
        lightRef.addValueEventListener(lightStateListener);
    }

    private void setupAreaSelection() {
        int[] buttonIds = {R.id.select_button_1, R.id.select_button_2, R.id.select_button_3, R.id.select_button_4, R.id.select_button_5, R.id.select_button_6};
        int[] textViewIds = {R.id.bedroom_1_label, R.id.bedroom_2_label, R.id.living_room_label, R.id.dining_area_label, R.id.kitchen_area_label, R.id.comfort_room_label};

        for (int i = 0; i < buttonIds.length; i++) {
            Button button = findViewById(buttonIds[i]);
            TextView textView = findViewById(textViewIds[i]);

            if (button != null && textView != null) {
                LightAreaUI ui = new LightAreaUI(button, textView);
                lightAreaUIs.add(ui);
                button.setOnClickListener(v -> onAreaSelected(ui));
                textView.setOnClickListener(v -> showDetailsDialog(ui.areaName, "220-240 V", "60 Watts", "60 Hz"));
            }
        }
    }

    private void onAreaSelected(LightAreaUI tappedUI) {
        if (selectedArea != null && selectedArea.button == tappedUI.button) {
            // Deselecting the current area
            selectedArea = null;
            lightRef.child("status").setValue("OFF");
            lightRef.child("appliance_name").setValue("None");
        } else {
            // Selecting a new area
            selectedArea = tappedUI;
            lightRef.child("status").setValue("OFF"); // Start with OFF state
            lightRef.child("appliance_name").setValue(tappedUI.areaName);
        }
        String nameToSave = selectedArea != null ? selectedArea.areaName : "Area";
        saveData(nameToSave);
        updateDisplay();
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String savedAreaName = sharedPreferences.getString(LIGHT_AREA, "Area");

        selectedArea = null; // Reset
        for (LightAreaUI ui : lightAreaUIs) {
            if (ui.areaName.equals(savedAreaName)) {
                selectedArea = ui;
                break;
            }
        }
        updateDisplay();
    }

    private void updateDisplay() {
        String currentAreaName = selectedArea != null ? selectedArea.areaName : "Area";
        lightPageSubtitle.setText(currentAreaName);

        for (LightAreaUI ui : lightAreaUIs) {
            setButtonState(ui.button, selectedArea != null && ui.button == selectedArea.button);
        }

        onButton.setEnabled(selectedArea != null);
        offButton.setEnabled(selectedArea != null);
    }

    private void updateUIForLightStatus(String status) {
        boolean isConnected = "ON".equals(status);
        connectionStatus.setText(isConnected ? "Connected" : "Not connected");
        setButtonActive(onButton, isConnected);
        setButtonActive(offButton, !isConnected);
        connectionProgress.setIndeterminate(isConnected);
        connectionProgress.setIndeterminateTintList(ColorStateList.valueOf(Color.parseColor(isConnected ? "#FFA500" : "#888888")));

        if (isConnected && selectedArea != null) {
            scheduleLightReminder("l1");
        } else {
            cancelLightReminder();
        }
    }

    private void setupNavigation() {
        findViewById(R.id.info_icon).setOnClickListener(v -> startActivity(new Intent(this, LightGuideActivity.class)));
        findViewById(R.id.settings_icon).setOnClickListener(v -> startActivity(new Intent(this, LightSettingsActivity.class)));
        findViewById(R.id.homepage_button).setOnClickListener(v -> startActivity(new Intent(this, LightHomePage.class)));
        findViewById(R.id.light_button).setOnClickListener(v -> {});
        findViewById(R.id.user_button).setOnClickListener(v -> startActivity(new Intent(this, LightUserActivity.class)));
    }

    private void showDetailsDialog(String name, String voltage, String wattage, String frequency) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_details, null);
        builder.setView(dialogView);

        TextView detailName = dialogView.findViewById(R.id.detail_name);
        TextView detailVoltage = dialogView.findViewById(R.id.detail_voltage);
        TextView detailWattage = dialogView.findViewById(R.id.detail_wattage);
        TextView detailFrequency = dialogView.findViewById(R.id.detail_frequency);

        detailName.setText(name);
        detailVoltage.setText("Voltage: " + voltage);
        detailWattage.setText("Wattage: " + wattage);
        detailFrequency.setText("Frequency: " + frequency);

        builder.create().show();
    }

    private void setButtonState(Button button, boolean isSelected) {
        button.setSelected(isSelected);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(isSelected ? "#FFA500" : "#333333")));
        button.setTextColor(isSelected ? Color.BLACK : Color.WHITE);
    }

    private void setButtonActive(Button button, boolean isActive) {
        setButtonState(button, isActive);
    }

    private void saveData(String areaName) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        sharedPreferences.edit().putString(LIGHT_AREA, areaName).apply();
    }

    private void scheduleLightReminder(String lightId) {
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, ApplianceReminderReceiver.class);
        intent.putExtra("socket_id", lightId);
        lightReminderPendingIntent = PendingIntent.getBroadcast(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerTime = SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR * 3;
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, AlarmManager.INTERVAL_HOUR * 3, lightReminderPendingIntent);
    }

    private void cancelLightReminder() {
        if (alarmManager != null && lightReminderPendingIntent != null) {
            alarmManager.cancel(lightReminderPendingIntent);
        }
    }
}
