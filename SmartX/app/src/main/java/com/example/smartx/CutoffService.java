package com.example.smartx;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CutoffService extends Service {

    private static final long GRACE_PERIOD = 45 * 1000; // 45 seconds
    private Handler handler;
    private Runnable cutoffRunnable;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler = new Handler(Looper.getMainLooper());
        cutoffRunnable = () -> {
            // Handle Appliances
            turnOffAppliances();
            // Handle Lights
            turnOffLights();

            stopSelf();
        };
        handler.postDelayed(cutoffRunnable, GRACE_PERIOD);

        return START_NOT_STICKY;
    }

    private void turnOffAppliances() {
        DatabaseReference settingsRef = FirebaseDatabase.getInstance().getReference("settings/appliances");
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot settingsSnapshot) {
                if (settingsSnapshot.child("automatic_cutoff").exists() && settingsSnapshot.child("automatic_cutoff").getValue(Boolean.class)) {
                    DatabaseReference socketsRef = FirebaseDatabase.getInstance().getReference("sockets");
                    DatabaseReference prioritizedAppliancesRef = FirebaseDatabase.getInstance().getReference("prioritized_appliances");

                    socketsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot socketsSnapshot) {
                            prioritizedAppliancesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot prioritizedSnapshot) {
                                    for (DataSnapshot socketSnapshot : socketsSnapshot.getChildren()) {
                                        if ("ON".equals(socketSnapshot.getValue(String.class))) {
                                            String socketKey = socketSnapshot.getKey(); // s1, s2
                                            String applianceKey = "priority_" + socketKey; // priority_s1

                                            if (!prioritizedSnapshot.child(applianceKey).exists() || !prioritizedSnapshot.child(applianceKey).getValue(Boolean.class)) {
                                                socketSnapshot.getRef().setValue("OFF");
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) { }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void turnOffLights() {
        DatabaseReference settingsRef = FirebaseDatabase.getInstance().getReference("settings/lights");
        settingsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot settingsSnapshot) {
                if (settingsSnapshot.child("automatic_cutoff").exists() && settingsSnapshot.child("automatic_cutoff").getValue(Boolean.class)) {
                    DatabaseReference lightsRef = FirebaseDatabase.getInstance().getReference("lights");
                    DatabaseReference prioritizedLightsRef = FirebaseDatabase.getInstance().getReference("prioritized_lights");

                    lightsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot lightsSnapshot) {
                            prioritizedLightsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot prioritizedSnapshot) {
                                    for (DataSnapshot lightSnapshot : lightsSnapshot.getChildren()) {
                                        if ("ON".equals(lightSnapshot.getValue(String.class))) {
                                            String lightKey = lightSnapshot.getKey();
                                            if (!prioritizedSnapshot.child(lightKey).exists() || !prioritizedSnapshot.child(lightKey).getValue(Boolean.class)) {
                                                lightSnapshot.getRef().setValue("OFF");
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) { }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) { }
                    });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && cutoffRunnable != null) {
            handler.removeCallbacks(cutoffRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
