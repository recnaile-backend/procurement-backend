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
@Document(collection = "projects")
public class Project {

    @Id
    private String projectId;

    private String projectName;

    /** ID of the Team Lead assigned to the project. */
    @Indexed
    private String teamLeadId;

    private MasterDataStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    @Version
    private Long version;
}
