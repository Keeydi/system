package com.example.smartx;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PzemData {

    public double power;
    public double energy;
    public double voltage;
    public double current;

    public PzemData() {
        // Default constructor required for calls to DataSnapshot.getValue(PzemData.class)
    }

    public PzemData(double power, double energy, double voltage, double current) {
        this.power = power;
        this.energy = energy;
        this.voltage = voltage;
        this.current = current;
    }
}