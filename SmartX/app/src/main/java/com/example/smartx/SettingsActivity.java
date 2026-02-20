package com.example.smartx;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

// Change AppCompatActivity to BaseActivity
public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button controlFunctionButton = findViewById(R.id.control_function_button);
        controlFunctionButton.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, ControlFunctionActivity.class)));

        Button notificationButton = findViewById(R.id.notification_button);
        notificationButton.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, NotificationActivity.class)));

        Button consumptionButton = findViewById(R.id.consumption_button);
        consumptionButton.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, ConsumptionYearActivity.class)));

        Button goToLightPageButton = findViewById(R.id.go_to_light_page_button);
        goToLightPageButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, LightHomePage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }
}
