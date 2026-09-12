package com.recnaile.masterdata.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vendors")
public class Vendor {

    @Id
    private String id;

    private String name;

    private String email;
    private String category;
    private String gstNumber;
    private String notes;
    private String orderSource;
    private String webUrl;

    private MasterDataStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}
