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

    @Indexed(unique = true)
    private String email;

    /**
     * BCrypt encoded password.
     * Never store the raw password here.
     */
    private String passwordHash;

    private String firstName;

    private String lastName;

    private String departmentId;

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
