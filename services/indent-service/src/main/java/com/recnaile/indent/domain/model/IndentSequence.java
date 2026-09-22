package com.recnaile.indent.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "indent_sequences")
public class IndentSequence {

    @Id
    private String financialYear;

    private long sequence;

    public IndentSequence() {
    }

    public IndentSequence(String financialYear, long sequence) {
        this.financialYear = financialYear;
        this.sequence = sequence;
    }

    public String getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }
}
