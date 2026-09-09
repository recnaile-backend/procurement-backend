package com.recnaile.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    /**
     * Unique employee identifier.
     *
     * Example:
     * EMP1001
     * EMP1002
     * TL1001
     * MD1001
     */
    @Indexed(unique = true)
    private String employeeId;

    /**
     * BCrypt encoded password.
     * Never store the raw password here.
     */
    private String passwordHash;

    private String firstName;

    private String lastName;

    private String departmentName;

    /**
     * ID of the TL who manages this employee.
     *
     * Null for MD and TL accounts.
     */
    @Indexed
    private String teamLeaderId;

    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Builder.Default
    private Boolean enabled = true;

    @Builder.Default
    private Boolean accountLocked = false;

    @Builder.Default
    private Integer failedLoginAttempts = 0;

    private Instant createdAt;

    private Instant updatedAt;
}