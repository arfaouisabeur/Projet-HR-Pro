package edu.RhPro.utils;

import java.math.BigDecimal;

public final class TaxPolicy {

    private TaxPolicy(){}

    // You can change these anytime
    public static BigDecimal taxRateForContinent(String continent) {
        if (continent == null) return BigDecimal.valueOf(0.20);

        switch (continent.toUpperCase()) {
            case "EUROPE":  return BigDecimal.valueOf(0.25);
            case "AFRICA":  return BigDecimal.valueOf(0.15);
            case "AMERICA": return BigDecimal.valueOf(0.20);
            case "ASIA":    return BigDecimal.valueOf(0.18);
            case "OCEANIA": return BigDecimal.valueOf(0.22);
            default:        return BigDecimal.valueOf(0.50);
        }
    }
}