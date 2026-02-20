package com.example.smartx;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

// Change AppCompatActivity to BaseActivity
public class PrioritizedAppliancesActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prioritized_appliances);

        ListView appliancesListView = findViewById(R.id.appliances_list_view);

        List<Appliance> applianceList = new ArrayList<>();
        applianceList.add(new Appliance("Electric Fan 1", "priority_fan1"));
        applianceList.add(new Appliance("Electric Fan 2", "priority_fan2"));
        applianceList.add(new Appliance("Electric Fan 3", "priority_fan3"));
        applianceList.add(new Appliance("Television", "priority_tv"));
        applianceList.add(new Appliance("Refrigerator", "priority_fridge"));

        AppliancePriorityAdapter adapter = new AppliancePriorityAdapter(this, applianceList);
        appliancesListView.setAdapter(adapter);

        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private static class Appliance {
        String name;
        String firebaseKey;

        Appliance(String name, String firebaseKey) {
            this.name = name;
            this.firebaseKey = firebaseKey;
        }
    }

    private class AppliancePriorityAdapter extends ArrayAdapter<Appliance> {

        private final DatabaseReference prioritizedRef = FirebaseDatabase.getInstance().getReference("prioritized_appliances");

        public AppliancePriorityAdapter(Context context, List<Appliance> appliances) {
            super(context, 0, appliances);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_appliance_priority, parent, false);
            }

            Appliance currentAppliance = getItem(position);
            TextView applianceName = convertView.findViewById(R.id.appliance_name);
            SwitchCompat applianceSwitch = convertView.findViewById(R.id.appliance_switch);

            applianceName.setText(currentAppliance.name);

            DatabaseReference applianceRef = prioritizedRef.child(currentAppliance.firebaseKey);

            applianceRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        applianceSwitch.setChecked(dataSnapshot.getValue(Boolean.class));
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

            applianceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> applianceRef.setValue(isChecked));

            return convertView;
        }
    }
}
