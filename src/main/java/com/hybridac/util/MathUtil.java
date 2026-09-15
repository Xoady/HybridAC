package com.hybridac.util;

import java.text.DecimalFormat;
import java.util.List;

public final class MathUtil {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.000");

    private MathUtil() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static double safeDivide(double numerator, double denominator) {
        return denominator == 0.0D ? 0.0D : numerator / denominator;
    }

    public static double mean(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0D;
        }

        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
    }

    public static double stdDev(List<Double> values, double mean) {
        if (values.size() < 2) {
            return 0.0D;
        }

        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2.0D))
                .average()
                .orElse(0.0D);
        return Math.sqrt(variance);
    }

    public static double angleDistance(double first, double second) {
        double diff = Math.abs(first - second) % 360.0D;
        return diff > 180.0D ? 360.0D - diff : diff;
    }

    public static double wrapAngleTo180(double angle) {
        double wrapped = angle % 360.0D;
        if (wrapped >= 180.0D) {
            wrapped -= 360.0D;
        }
        if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }

    public static double signChangeRatio(List<Double> values) {
        if (values.size() < 2) {
            return 0.0D;
        }

        int changes = 0;
        for (int i = 1; i < values.size(); i++) {
            if (Math.signum(values.get(i - 1)) != Math.signum(values.get(i))) {
                changes++;
            }
        }
        return safeDivide(changes, values.size() - 1);
    }

    public static String format(double value) {
        synchronized (DECIMAL_FORMAT) {
            return DECIMAL_FORMAT.format(value);
        }
    }
}
