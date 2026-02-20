package com.example.smartx;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

// Change AppCompatActivity to BaseActivity
public class LightNotificationActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_notification);

        RelativeLayout manageNotificationsButton = findViewById(R.id.manage_notifications_button);
        manageNotificationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        });

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        ImageView infoIcon = findViewById(R.id.info_icon);
        infoIcon.setOnClickListener(v -> startActivity(new Intent(LightNotificationActivity.this, LightGuideActivity.class)));

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(v -> startActivity(new Intent(LightNotificationActivity.this, LightSettingsActivity.class)));
    }
}
