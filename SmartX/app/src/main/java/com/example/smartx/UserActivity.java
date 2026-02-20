package com.example.smartx;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

// Change AppCompatActivity to BaseActivity
public class UserActivity extends BaseActivity implements View.OnClickListener {

    public static final String SHARED_PREFS = "sharedPrefs";
    public static final String USER_EMAIL = "user_email";
    public static final String USER_NAME = "user_name";

    private TextView userEmailText;
    private TextView userNameText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        userEmailText = findViewById(R.id.user_email_text);
        userNameText = findViewById(R.id.user_name_text);

        loadUserData();

        // Set click listeners
        findViewById(R.id.info_icon).setOnClickListener(this);
        findViewById(R.id.settings_icon).setOnClickListener(this);
        findViewById(R.id.email_button).setOnClickListener(this);
        findViewById(R.id.name_button).setOnClickListener(this);
        findViewById(R.id.change_password_button).setOnClickListener(this);
        findViewById(R.id.login_security_button).setOnClickListener(this);
        findViewById(R.id.logout_button).setOnClickListener(this);
        findViewById(R.id.homepage_button).setOnClickListener(this);
        findViewById(R.id.socket_button).setOnClickListener(this);
        findViewById(R.id.user_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.logout_button) {
            logout();
        } else if (id == R.id.login_security_button) {
            showCustomDialog(R.layout.dialog_login_security);
        } else if (id == R.id.email_button) {
            showSetEmailDialog();
        } else if (id == R.id.name_button) {
            showSetNameDialog();
        } else if (id == R.id.change_password_button) {
            showSetPasswordDialog();
        } else if (id == R.id.info_icon) {
            startActivity(new Intent(this, GuideActivity.class));
        } else if (id == R.id.settings_icon) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.homepage_button) {
            startActivity(new Intent(this, HomeActivity.class));
        } else if (id == R.id.socket_button) {
            startActivity(new Intent(this, Socket1Activity.class));
        }
    }

    private void showCustomDialog(int layoutResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(layoutResId, null);
        builder.setView(view);
        builder.create().show();
    }

    private void showSetEmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_set_email, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText emailInput = view.findViewById(R.id.email_input);
        view.findViewById(R.id.set_button).setOnClickListener(v -> {
            String newEmail = emailInput.getText().toString();
            if (!newEmail.isEmpty()) {
                saveString(USER_EMAIL, newEmail);
                userEmailText.setText(newEmail);
            }
            dialog.dismiss();
        });
    }

    private void showSetNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_set_name, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        EditText nameInput = view.findViewById(R.id.name_input);
        view.findViewById(R.id.set_button).setOnClickListener(v -> {
            String newName = nameInput.getText().toString();
            if (!newName.isEmpty()) {
                saveString(USER_NAME, newName);
                userNameText.setText(newName);
            }
            dialog.dismiss();
        });
    }

    private void showSetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_set_password, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        view.findViewById(R.id.set_button).setOnClickListener(v -> {
            Toast.makeText(this, "Password Set!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        view.findViewById(R.id.change_button).setOnClickListener(v -> {
            dialog.dismiss();
            showChangePasswordDialog();
        });
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();

        view.findViewById(R.id.set_button).setOnClickListener(v -> {
            Toast.makeText(this, "Password Changed!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    private void saveString(String key, String value) {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void loadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        userEmailText.setText(sharedPreferences.getString(USER_EMAIL, "wattapips@gmail.com"));
        userNameText.setText(sharedPreferences.getString(USER_NAME, "WATTAPIPS"));
    }

    private void logout() {
        Intent intent = new Intent(UserActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
