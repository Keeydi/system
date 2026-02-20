package com.example.smartx;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// Change AppCompatActivity to BaseActivity
public class ConsumptionYearActivity extends BaseActivity {

    private BarChart yearChart;
    private DatabaseReference pzemRef;
    private TextView phpValue, kwhValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consumption_year);

        yearChart = findViewById(R.id.year_chart);
        pzemRef = FirebaseDatabase.getInstance().getReference("pzem/appliance");
        phpValue = findViewById(R.id.php_value);
        kwhValue = findViewById(R.id.kwh_value);

        setupChart();
        loadYearlyData();

        LinearLayout yearSelector = findViewById(R.id.year_selector);
        yearSelector.setOnClickListener(this::showTimeframeDialog);

        // Navigation
        findViewById(R.id.info_icon).setOnClickListener(v -> startActivity(new Intent(this, GuideActivity.class)));
        findViewById(R.id.settings_icon).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.homepage_button).setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        findViewById(R.id.socket_button).setOnClickListener(v -> startActivity(new Intent(this, Socket1Activity.class)));
        findViewById(R.id.user_button).setOnClickListener(v -> startActivity(new Intent(this, UserActivity.class)));
    }

    private void setupChart() {
        yearChart.setDrawBarShadow(false);
        yearChart.setDrawValueAboveBar(true);
        yearChart.getDescription().setEnabled(false);
        yearChart.setPinchZoom(false);
        yearChart.setDrawGridBackground(false);
        yearChart.getXAxis().setDrawGridLines(false);
        yearChart.getAxisLeft().setTextColor(Color.WHITE);
        yearChart.getAxisRight().setEnabled(false);
        yearChart.getLegend().setEnabled(false);

        XAxis xAxis = yearChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGranularity(1f);
    }

    private void loadYearlyData() {
        pzemRef.child("history").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<BarEntry> entries = new ArrayList<>();
                List<String> labels = new ArrayList<>();
                int i = 0;
                double totalYearlyKwh = 0;
                for (DataSnapshot monthSnapshot : dataSnapshot.getChildren()) {
                    Double totalEnergy = monthSnapshot.child("totalEnergy").getValue(Double.class);
                    if (totalEnergy != null) {
                        entries.add(new BarEntry(i, totalEnergy.floatValue()));
                        labels.add(monthSnapshot.getKey());
                        totalYearlyKwh += totalEnergy;
                        i++;
                    }
                }

                // Update totals
                phpValue.setText(String.format("P %.2f", totalYearlyKwh * 11.7));
                kwhValue.setText(String.format("%.2f kwh", totalYearlyKwh));

                BarDataSet dataSet = new BarDataSet(entries, "Monthly Consumption");
                dataSet.setColor(Color.parseColor("#FFA500"));
                dataSet.setValueTextColor(Color.WHITE);

                BarData barData = new BarData(dataSet);
                yearChart.setData(barData);
                yearChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                yearChart.invalidate();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
            }
        });
    }

    private void showTimeframeDialog(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_timeframe_picker, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.TOP | Gravity.START;
            int[] location = new int[2];
            v.getLocationOnScreen(location);
            wlp.x = location[0];
            wlp.y = location[1] + v.getHeight();
            window.setAttributes(wlp);
        }

        TextView yearOption = view.findViewById(R.id.year_option);
        TextView monthOption = view.findViewById(R.id.month_option);

        // Set current view to orange
        yearOption.setTextColor(getResources().getColor(R.color.orange));
        monthOption.setTextColor(Color.WHITE);

        yearOption.setOnClickListener(view1 -> {
            // Already on Year page
            dialog.dismiss();
        });

        monthOption.setOnClickListener(view1 -> {
            startActivity(new Intent(this, ConsumptionMonthActivity.class));
            finish();
            dialog.dismiss();
        });

        dialog.show();
    }
}
