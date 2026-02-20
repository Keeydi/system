package com.example.smartx;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

// Change AppCompatActivity to BaseActivity
public class LightSettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_settings);

        Button controlFunctionButton = findViewById(R.id.control_function_button);
        controlFunctionButton.setOnClickListener(v -> startActivity(new Intent(LightSettingsActivity.this, LightControlFunctionActivity.class)));

        Button notificationButton = findViewById(R.id.notification_button);
        notificationButton.setOnClickListener(v -> startActivity(new Intent(LightSettingsActivity.this, LightNotificationActivity.class)));

        Button consumptionButton = findViewById(R.id.consumption_button);
        consumptionButton.setOnClickListener(v -> startActivity(new Intent(LightSettingsActivity.this, LightConsumptionYearActivity.class)));

        Button goToAppliancesButton = findViewById(R.id.go_to_appliances_button);
        goToAppliancesButton.setOnClickListener(v -> {
            Intent intent = new Intent(LightSettingsActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}
