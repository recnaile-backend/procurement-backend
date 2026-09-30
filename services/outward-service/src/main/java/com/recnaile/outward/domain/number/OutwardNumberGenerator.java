package com.recnaile.outward.domain.number;


public final class OutwardNumberGenerator {

    private static final String PREFIX = "OUT%2F";

    private OutwardNumberGenerator() {
    }

    public static String generate(int fiscalYearStart, long sequence) {
        if (fiscalYearStart < 2000 || fiscalYearStart > 9999) {
            throw new IllegalArgumentException("fiscalYearStart must be a four-digit year");
        }
        if (sequence < 1 || sequence > 999999) {
            throw new IllegalArgumentException("sequence must be between 1 and 999999");
        }

        int fiscalYearEnd = (fiscalYearStart + 1) % 100;
        return PREFIX
                + fiscalYearStart % 100
                + "-"
                + String.format("%02d", fiscalYearEnd)
                + "%2F"
                + String.format("%03d", sequence);
    }
}
