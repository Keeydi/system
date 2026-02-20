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
import androidx.core.content.ContextCompat;

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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Change AppCompatActivity to BaseActivity
public class LightConsumptionMonthActivity extends BaseActivity implements View.OnClickListener {

    private BarChart monthChart;
    private DatabaseReference pzemRef;
    private TextView monthTitle, phpValue, kwhValue;
    private Map<TextView, String> monthMap = new HashMap<>();
    private TextView selectedMonthView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_consumption_month);

        monthChart = findViewById(R.id.month_chart);
        pzemRef = FirebaseDatabase.getInstance().getReference("pzem/light");
        monthTitle = findViewById(R.id.month_title);
        phpValue = findViewById(R.id.php_value);
        kwhValue = findViewById(R.id.kwh_value);

        setupChart();
        setupMonthButtons();

        // Set initial view to current month
        Calendar cal = Calendar.getInstance();
        for (Map.Entry<TextView, String> entry : monthMap.entrySet()) {
            if (entry.getValue().endsWith(String.format("-%02d", cal.get(Calendar.MONTH) + 1))) {
                setSelectedMonth(entry.getKey());
                break;
            }
        }

        LinearLayout monthSelector = findViewById(R.id.month_selector);
        monthSelector.setOnClickListener(this::showTimeframeDialog);

        // Navigation
        findViewById(R.id.info_icon).setOnClickListener(v -> startActivity(new Intent(this, LightGuideActivity.class)));
        findViewById(R.id.settings_icon).setOnClickListener(v -> startActivity(new Intent(this, LightSettingsActivity.class)));
        findViewById(R.id.homepage_button).setOnClickListener(v -> startActivity(new Intent(this, LightHomePage.class)));
        findViewById(R.id.light_button).setOnClickListener(v -> startActivity(new Intent(this, LightPage.class)));
        findViewById(R.id.user_button).setOnClickListener(v -> startActivity(new Intent(this, LightUserActivity.class)));
    }

    private void setupMonthButtons() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        monthMap.put(findViewById(R.id.jan_button), year + "-01");
        monthMap.put(findViewById(R.id.feb_button), year + "-02");
        monthMap.put(findViewById(R.id.mar_button), year + "-03");
        monthMap.put(findViewById(R.id.apr_button), year + "-04");
        monthMap.put(findViewById(R.id.may_button), year + "-05");
        monthMap.put(findViewById(R.id.jun_button), year + "-06");
        monthMap.put(findViewById(R.id.jul_button), year + "-07");
        monthMap.put(findViewById(R.id.aug_button), year + "-08");
        monthMap.put(findViewById(R.id.sep_button), year + "-09");
        monthMap.put(findViewById(R.id.oct_button), year + "-10");
        monthMap.put(findViewById(R.id.nov_button), year + "-11");
        monthMap.put(findViewById(R.id.dec_button), year + "-12");

        for (TextView monthView : monthMap.keySet()) {
            monthView.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (monthMap.containsKey(v)) {
            setSelectedMonth((TextView) v);
        }
    }

    private void setSelectedMonth(TextView monthView) {
        if (selectedMonthView != null) {
            selectedMonthView.setTextColor(Color.WHITE);
        }
        monthView.setTextColor(ContextCompat.getColor(this, R.color.orange));
        selectedMonthView = monthView;

        String monthKey = monthMap.get(monthView);
        monthTitle.setText(monthView.getText().toString().toUpperCase());
        loadMonthlyData(monthKey);
    }

    private void setupChart() {
        monthChart.setDrawBarShadow(false);
        monthChart.setDrawValueAboveBar(true);
        monthChart.getDescription().setEnabled(false);
        monthChart.setPinchZoom(false);
        monthChart.setDrawGridBackground(false);
        monthChart.getXAxis().setDrawGridLines(false);
        monthChart.getAxisLeft().setTextColor(Color.WHITE);
        monthChart.getAxisRight().setEnabled(false);
        monthChart.getLegend().setEnabled(false);

        XAxis xAxis = monthChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGranularity(1f);
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            labels.add("Week " + i);
        }
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
    }

    private void loadMonthlyData(String monthKey) {
        if (monthKey == null) return;

        pzemRef.child("history").child(monthKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<BarEntry> entries = new ArrayList<>();
                float[] weeklyConsumption = new float[5];
                double totalMonthKwh = 0;

                for (DataSnapshot daySnapshot : dataSnapshot.getChildren()) {
                    if (!daySnapshot.getKey().equals("totalEnergy")) {
                        try {
                            int dayOfMonth = Integer.parseInt(daySnapshot.getKey());
                            Double dailyEnergy = daySnapshot.getValue(Double.class);
                            if (dailyEnergy != null) {
                                int weekOfMonth = (dayOfMonth - 1) / 7;
                                if (weekOfMonth < 5) {
                                    weeklyConsumption[weekOfMonth] += dailyEnergy.floatValue();
                                }
                                totalMonthKwh += dailyEnergy;
                            }
                        } catch (NumberFormatException e) {
                            // Ignore non-day keys like 'totalEnergy'
                        }
                    }
                }

                phpValue.setText(String.format("P %.2f", totalMonthKwh * 11.7));
                kwhValue.setText(String.format("%.2f kwh", totalMonthKwh));

                for (int i = 0; i < 5; i++) {
                    entries.add(new BarEntry(i, weeklyConsumption[i]));
                }

                BarDataSet dataSet = new BarDataSet(entries, "Weekly Consumption");
                dataSet.setColor(ContextCompat.getColor(LightConsumptionMonthActivity.this, R.color.orange));
                dataSet.setValueTextColor(Color.WHITE);

                BarData barData = new BarData(dataSet);
                monthChart.setData(barData);
                monthChart.invalidate();
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

        monthOption.setTextColor(ContextCompat.getColor(this, R.color.orange));
        yearOption.setTextColor(Color.WHITE);

        monthOption.setOnClickListener(view1 -> dialog.dismiss());

        yearOption.setOnClickListener(view1 -> {
            startActivity(new Intent(this, LightConsumptionYearActivity.class));
            finish();
            dialog.dismiss();
        });

        dialog.show();
    }
}
