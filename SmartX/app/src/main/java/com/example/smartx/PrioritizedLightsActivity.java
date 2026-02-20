package com.example.smartx;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

// Change AppCompatActivity to BaseActivity
public class PrioritizedLightsActivity extends BaseActivity {

    public static final String SHARED_PREFS = "sharedPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prioritized_lights);

        setupSwitch(R.id.switch_bedroom_1, "Bedroom 1");
        setupSwitch(R.id.switch_bedroom_2, "Bedroom 2");
        setupSwitch(R.id.switch_living_room, "Living Room");
        setupSwitch(R.id.switch_dining_area, "Dining Area");
        setupSwitch(R.id.switch_kitchen_area, "Kitchen Area");
        setupSwitch(R.id.switch_comfort_room, "Comfort Room");

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private String getFirebaseKey(String displayName) {
        return displayName.replace(" ", "_").toLowerCase();
    }

    private void setupSwitch(int switchId, final String areaName) {
        SwitchCompat switchCompat = findViewById(switchId);
        String firebaseKey = getFirebaseKey(areaName);
        DatabaseReference prioritizedRef = FirebaseDatabase.getInstance().getReference("prioritized_lights").child(firebaseKey);

        prioritizedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    switchCompat.setChecked(dataSnapshot.getValue(Boolean.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prioritizedRef.setValue(isChecked);
        });
    }
}
