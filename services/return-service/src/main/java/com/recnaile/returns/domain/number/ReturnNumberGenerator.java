package com.recnaile.returns.domain.number;

public final class ReturnNumberGenerator {
    private static final String PREFIX = "RETURN%2F";
    private ReturnNumberGenerator() {}
    public static String generate(int fiscalYearStart, long sequence) {
        if (fiscalYearStart < 2000 || fiscalYearStart > 9999) throw new IllegalArgumentException("fiscalYearStart must be a four-digit year");
        if (sequence < 1 || sequence > 999999) throw new IllegalArgumentException("sequence must be between 1 and 999999");
        int fiscalYearEnd = (fiscalYearStart + 1) % 100;
        return PREFIX + String.format("%02d-%02d", fiscalYearStart % 100, fiscalYearEnd) + "%2F" + String.format("%03d", sequence);
    }
}
