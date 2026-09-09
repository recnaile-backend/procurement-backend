package com.recnaile.auth.repository;

import com.recnaile.auth.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    // =========================================================
    // EMPLOYEE ID
    // =========================================================

    boolean existsByEmployeeId(String employeeId);

    Optional<User> findByEmployeeId(String employeeId);


    // =========================================================
    // TEAM MANAGEMENT
    // =========================================================

    List<User> findByTeamLeaderId(String teamLeaderId);

    Optional<User> findByEmployeeIdAndTeamLeaderId(
        String employeeId,
        String teamLeaderId
    );
}