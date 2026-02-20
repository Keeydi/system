package com.example.smartx;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

// Change AppCompatActivity to BaseActivity
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class HomeActivity extends BaseActivity {

    private TextView consumptionValue;
    private TextView consumptionUnit;
    private TextView socket1Device;
    private TextView socket2Device;
    private Button wattsButton;
    private Button phpButton;
    private TextView userNameText;
    private DatabaseReference pzemRef;
    private boolean isDisplayingWatts = true;

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String SOCKET1_APPLIANCE = "socket1_appliance";
    public static final String SOCKET2_APPLIANCE = "socket2_appliance";
    public static final String USER_NAME = "user_name";
    private static final double KWH_TO_PHP_RATE = 11.7;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        consumptionValue = findViewById(R.id.consumption_value);
        consumptionUnit = findViewById(R.id.consumption_unit);
        socket1Device = findViewById(R.id.socket_1_device);
        socket2Device = findViewById(R.id.socket_2_device);
        wattsButton = findViewById(R.id.watts_button);
        phpButton = findViewById(R.id.php_button);
        userNameText = findViewById(R.id.user_name);

        pzemRef = FirebaseDatabase.getInstance().getReference("pzem/appliance");

        pzemRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                PzemData pzemData = dataSnapshot.getValue(PzemData.class);
                if (pzemData != null) {
                    if (isDisplayingWatts) {
                        updateConsumptionView(pzemData.power, true);
                    } else {
                        updateConsumptionView(pzemData.energy, false);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });

        wattsButton.setOnClickListener(v -> {
            isDisplayingWatts = true;
            pzemRef.child("power").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Double watts = dataSnapshot.getValue(Double.class);
                    updateConsumptionView(watts, true);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
            setButtonActive(wattsButton, true);
            setButtonActive(phpButton, false);
        });

        phpButton.setOnClickListener(v -> {
            isDisplayingWatts = false;
            pzemRef.child("energy").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Double energy = dataSnapshot.getValue(Double.class);
                    updateConsumptionView(energy, false);
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
            setButtonActive(phpButton, true);
            setButtonActive(wattsButton, false);
        });

        TextView socket1Label = findViewById(R.id.socket_1_label);
        socket1Label.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, Socket1Activity.class)));

        TextView socket2Label = findViewById(R.id.socket_2_label);
        socket2Label.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, Socket2Activity.class)));

        ImageButton homepageButton = findViewById(R.id.homepage_button);
        homepageButton.setOnClickListener(v -> { /* Already in Home Activity */ });

        ImageButton socketButton = findViewById(R.id.socket_button);
        socketButton.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, Socket1Activity.class)));

        View.OnClickListener userActivityListener = v -> startActivity(new Intent(HomeActivity.this, UserActivity.class));

        ImageButton userButton = findViewById(R.id.user_button);
        userButton.setOnClickListener(userActivityListener);

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, SettingsActivity.class)));

        ImageView infoIcon = findViewById(R.id.info_icon);
        infoIcon.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, GuideActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void updateConsumptionView(Double value, boolean isWatts) {
        if (value == null) {
            consumptionValue.setText("0.00");
            return;
        }

        DecimalFormat df = new DecimalFormat("0.00");
        if (isWatts) {
            consumptionValue.setText(df.format(value));
            consumptionUnit.setText("WATTS");
        } else {
            consumptionValue.setText("P " + df.format(value * KWH_TO_PHP_RATE));
            consumptionUnit.setText("kWh / month");
        }
    }

    private void loadData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        String socket1Appliance = sharedPreferences.getString(SOCKET1_APPLIANCE, "Electric Fan");
        String socket2Appliance = sharedPreferences.getString(SOCKET2_APPLIANCE, "Laptop");
        String userName = sharedPreferences.getString(USER_NAME, "WattaPips");

        socket1Device.setText(socket1Appliance);
        socket2Device.setText(socket2Appliance);
        userNameText.setText("Hello, " + userName + "!");
    }

    private void setButtonActive(Button button, boolean isActive) {
        if (isActive) {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA500")));
            button.setTextColor(Color.BLACK);
        } else {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#333333")));
            button.setTextColor(Color.WHITE);
        }
    }
}
