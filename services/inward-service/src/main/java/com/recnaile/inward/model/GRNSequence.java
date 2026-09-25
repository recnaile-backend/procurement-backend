package com.recnaile.inward.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sequence_counters")
public class GRNSequence {

    @Id
    private String financialYear;

    private long sequence;

    protected GRNSequence() {
    }

    public GRNSequence(String financialYear, long sequence) {
        this.financialYear = financialYear;
        this.sequence = sequence;
    }

    public String getFinancialYear() {
        return financialYear;
    }

    public long getSequence() {
        return sequence;
    }
}
