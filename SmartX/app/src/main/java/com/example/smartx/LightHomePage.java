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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

// Change AppCompatActivity to BaseActivity
public class LightHomePage extends BaseActivity {

    private TextView consumptionValue;
    private TextView consumptionUnit;
    private Button wattsButton;
    private Button phpButton;
    private TextView userNameText;
    private TextView lightArea;
    private boolean isDisplayingWatts = true;
    private DatabaseReference pzemRef;

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String USER_NAME = "user_name";
    public static final String LIGHT_AREA = "light_area";
    private static final double KWH_TO_PHP_RATE = 11.7;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_home_page);

        consumptionValue = findViewById(R.id.consumption_value);
        consumptionUnit = findViewById(R.id.consumption_unit);
        wattsButton = findViewById(R.id.watts_button);
        phpButton = findViewById(R.id.php_button);
        userNameText = findViewById(R.id.user_name);
        lightArea = findViewById(R.id.light_area);

        pzemRef = FirebaseDatabase.getInstance().getReference("pzem/light");

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

        TextView lightLabel = findViewById(R.id.light_label);

        View.OnClickListener lightPageClickListener = v -> startActivity(new Intent(LightHomePage.this, LightPage.class));
        lightLabel.setOnClickListener(lightPageClickListener);
        lightArea.setOnClickListener(lightPageClickListener);

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

        ImageButton homepageButton = findViewById(R.id.homepage_button);
        homepageButton.setOnClickListener(v -> { /* Already in Home Activity */ });

        ImageButton lightButton = findViewById(R.id.light_button);
        lightButton.setOnClickListener(v -> startActivity(new Intent(LightHomePage.this, LightPage.class)));

        View.OnClickListener userActivityListener = v -> startActivity(new Intent(LightHomePage.this, LightUserActivity.class));

        ImageButton userButton = findViewById(R.id.user_button);
        userButton.setOnClickListener(userActivityListener);

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(v -> startActivity(new Intent(LightHomePage.this, LightSettingsActivity.class)));

        ImageView infoIcon = findViewById(R.id.info_icon);
        infoIcon.setOnClickListener(v -> startActivity(new Intent(LightHomePage.this, LightGuideActivity.class)));
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
        String userName = sharedPreferences.getString(USER_NAME, "WattaPips");
        userNameText.setText("Hello, " + userName + "!");
        lightArea.setText(sharedPreferences.getString(LIGHT_AREA, "Area"));
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
