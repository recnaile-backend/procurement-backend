package com.recnaile.outward.repository;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "outward_sequences")
public class OutwardSequenceDocument {

    @Id
    private String id;

    private long sequence;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getSequence() { return sequence; }
    public void setSequence(long sequence) { this.sequence = sequence; }
}
