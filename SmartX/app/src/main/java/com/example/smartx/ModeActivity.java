package com.example.smartx;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.Button;
import android.widget.TextView;

// Change AppCompatActivity to BaseActivity
public class ModeActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);

        // Color the 'X' in SMRTX
        TextView title = findViewById(R.id.title);
        SpannableString spannableString = new SpannableString("SMRTX");
        spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#FFA500")),
                4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(spannableString);

        Button appliancesButton = findViewById(R.id.appliances_button);
        appliancesButton.setOnClickListener(v -> {
            Intent intent = new Intent(ModeActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        Button lightsButton = findViewById(R.id.lights_button);
        lightsButton.setOnClickListener(v -> {
            Intent intent = new Intent(ModeActivity.this, LightHomePage.class);
            startActivity(intent);
        });
    }
}