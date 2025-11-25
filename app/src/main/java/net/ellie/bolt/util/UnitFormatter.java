package net.ellie.bolt.util;

import java.text.DecimalFormat;

public class UnitFormatter {
    public static String formatFrequency(int frequency) {
        if (frequency < 0) {
            return "0 Hz";
        }
        if (frequency < 1000) {
            return frequency + " Hz";
        }

        double value = frequency;
        String[] units = {"Hz", "kHz", "MHz", "GHz"};
        int unitIndex = 0;

        while (value >= 1000 && unitIndex < units.length - 1) {
            value /= 1000;
            unitIndex++;
        }

        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(value) + " " + units[unitIndex];
    }
}
