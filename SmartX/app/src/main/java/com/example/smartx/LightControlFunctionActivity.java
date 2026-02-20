package com.example.smartx;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// Change AppCompatActivity to BaseActivity
public class LightControlFunctionActivity extends BaseActivity {

    private DatabaseReference settingsRef;
    private SwitchCompat switchAutomaticCutoff;
    private SwitchCompat switchManualCutoff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_control_function);

        settingsRef = FirebaseDatabase.getInstance().getReference("settings/lights");

        switchAutomaticCutoff = findViewById(R.id.switch_automatic_cutoff);
        switchManualCutoff = findViewById(R.id.switch_manual_cutoff);
        setupFirebaseSwitch(findViewById(R.id.switch_prioritized_lights), "prioritized_lights");

        setupCutoffSwitches();

        // Top navigation
        findViewById(R.id.info_icon).setOnClickListener(v -> startActivity(new Intent(this, LightGuideActivity.class)));
        findViewById(R.id.settings_icon).setOnClickListener(v -> startActivity(new Intent(this, LightSettingsActivity.class)));

        // Prioritized Lights Button
        findViewById(R.id.prioritized_lights_button).setOnClickListener(v -> startActivity(new Intent(this, PrioritizedLightsActivity.class)));

        // Bottom navigation
        findViewById(R.id.homepage_button).setOnClickListener(v -> startActivity(new Intent(this, LightHomePage.class)));
        findViewById(R.id.light_button).setOnClickListener(v -> startActivity(new Intent(this, LightPage.class)));
        findViewById(R.id.user_button).setOnClickListener(v -> startActivity(new Intent(this, LightUserActivity.class)));
    }

    private void setupCutoffSwitches() {
        DatabaseReference autoRef = settingsRef.child("automatic_cutoff");
        DatabaseReference manualRef = settingsRef.child("manual_cutoff");

        autoRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                switchAutomaticCutoff.setChecked(snapshot.exists() ? snapshot.getValue(Boolean.class) : true);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        manualRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                switchManualCutoff.setChecked(snapshot.exists() ? snapshot.getValue(Boolean.class) : false);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        switchAutomaticCutoff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                autoRef.setValue(isChecked);
                if (isChecked) manualRef.setValue(false);
            }
        });

        switchManualCutoff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                manualRef.setValue(isChecked);
                if (isChecked) autoRef.setValue(false);
            }
        });
    }

    private void setupFirebaseSwitch(SwitchCompat switchCompat, final String preferenceKey) {
        DatabaseReference switchRef = settingsRef.child(preferenceKey);
        switchRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    switchCompat.setChecked(dataSnapshot.getValue(Boolean.class));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> switchRef.setValue(isChecked));
    }
}
