package com.recnaile.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recnaile.auth.dto.AddTeamMemberRequest;
import com.recnaile.auth.dto.LoginRequest;
import com.recnaile.auth.dto.LoginResponse;
import com.recnaile.auth.dto.RefreshTokenRequest;
import com.recnaile.auth.dto.RegisterRequest;
import com.recnaile.auth.dto.RegisterResponse;
import com.recnaile.auth.dto.TeamMemberResponse;
import com.recnaile.auth.dto.TeamMemberUpdateRequest;
import com.recnaile.auth.model.RefreshToken;
import com.recnaile.auth.model.Role;
import com.recnaile.auth.model.User;
import com.recnaile.auth.repository.RefreshTokenRepository;
import com.recnaile.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final long REFRESH_TOKEN_DAYS = 7L;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;


    // =========================================================
    // REGISTER
    // =========================================================

    public RegisterResponse register(
        RegisterRequest request
    ) {

        /*
         * Employee ID must be unique.
         */
        if (
            userRepository.existsByEmployeeId(
                request.employeeId()
            )
        ) {

            throw new IllegalArgumentException(
                "Employee ID already registered"
            );
        }

        /*
         * Every newly registered user is automatically
         * assigned the EMPLOYEE role.
         *
         * Users cannot choose their role during registration.
         * Only authorized management APIs can modify roles later.
         */
        Set<Role> roles = new HashSet<>();

        roles.add(Role.EMPLOYEE);

        Instant now = Instant.now();

        User user = User.builder()
            .employeeId(request.employeeId())
            .passwordHash(
                passwordEncoder.encode(
                    request.password()
                )
            )
            .firstName(request.firstName())
            .lastName(request.lastName())
            .departmentName(request.departmentName())
            .teamLeaderId(null)
            .roles(roles)
            .enabled(true)
            .accountLocked(false)
            .failedLoginAttempts(0)
            .createdAt(now)
            .updatedAt(now)
            .build();

        User savedUser =
            userRepository.save(user);

        return new RegisterResponse(
            savedUser.getId(),
            savedUser.getEmployeeId(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getDepartmentName(),
            savedUser.getRoles(),
            Boolean.TRUE.equals(
                savedUser.getEnabled()
            )
        );
    }


    // =========================================================
    // LOGIN
    // =========================================================

    public LoginResponse login(
        LoginRequest request
    ) {

        User user =
            userRepository
                .findByEmployeeId(
                    request.employeeId()
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Invalid employee ID or password"
                    )
                );

        if (
            !Boolean.TRUE.equals(
                user.getEnabled()
            )
        ) {

            throw new IllegalArgumentException(
                "Account is disabled"
            );
        }

        if (
            Boolean.TRUE.equals(
                user.getAccountLocked()
            )
        ) {

            throw new IllegalArgumentException(
                "Account is locked"
            );
        }

        if (
            !passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
            )
        ) {

            handleFailedLogin(user);

            throw new IllegalArgumentException(
                "Invalid employee ID or password"
            );
        }

        resetFailedLoginAttempts(user);

        String accessToken =
            jwtService.generateAccessToken(user);

        String refreshToken =
            refreshTokenService.generateToken();

        saveRefreshToken(
            user.getId(),
            refreshToken
        );

        return new LoginResponse(
            accessToken,
            refreshToken,
            "Bearer",
            jwtService
                .getAccessTokenExpirationSeconds()
        );
    }


    // =========================================================
    // REFRESH TOKEN
    // =========================================================

    public LoginResponse refresh(
        RefreshTokenRequest request
    ) {

        String tokenHash =
            refreshTokenService.hashToken(
                request.refreshToken()
            );

        Instant now = Instant.now();

        RefreshToken storedToken =
            refreshTokenRepository
                .findActiveToken(
                    tokenHash,
                    now
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Invalid, expired, or revoked refresh token"
                    )
                );

        User user =
            userRepository
                .findById(
                    storedToken.getUserId()
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "User not found"
                    )
                );

        if (
            !Boolean.TRUE.equals(
                user.getEnabled()
            )
        ) {

            throw new IllegalArgumentException(
                "Account is disabled"
            );
        }

        if (
            Boolean.TRUE.equals(
                user.getAccountLocked()
            )
        ) {

            throw new IllegalArgumentException(
                "Account is locked"
            );
        }

        /*
         * Rotate refresh token.
         *
         * Old token becomes unusable immediately.
         */
        storedToken.setRevokedAt(now);

        refreshTokenRepository.save(
            storedToken
        );

        String newAccessToken =
            jwtService.generateAccessToken(user);

        String newRefreshToken =
            refreshTokenService.generateToken();

        saveRefreshToken(
            user.getId(),
            newRefreshToken
        );

        return new LoginResponse(
            newAccessToken,
            newRefreshToken,
            "Bearer",
            jwtService
                .getAccessTokenExpirationSeconds()
        );
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    public void logout(
        RefreshTokenRequest request
    ) {

        String tokenHash =
            refreshTokenService.hashToken(
                request.refreshToken()
            );

        RefreshToken token =
            refreshTokenRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Invalid refresh token"
                    )
                );

        if (
            token.getRevokedAt() != null
        ) {

            throw new IllegalArgumentException(
                "Refresh token already revoked"
            );
        }

        token.setRevokedAt(
            Instant.now()
        );

        refreshTokenRepository.save(
            token
        );
    }


    // =========================================================
    // TEAM MANAGEMENT
    // TL + MD
    // =========================================================


    /**
     * Returns team members.
     *
     * TL:
     *     Returns only employees belonging
     *     to the authenticated TL.
     *
     * MD:
     *     Returns employees from all TL teams.
     *
     * The response contains teamLeaderId so the
     * frontend can identify which TL owns each team.
     */
    public List<TeamMemberResponse> getMyTeam(
        Authentication authentication
    ) {

        User manager =
            getAuthenticatedUser(
                authentication
            );

        requireTeamManagementRole(manager);

        /*
         * =====================================================
         * MD
         * =====================================================
         *
         * MD can see all employees that are currently
         * assigned to a team.
         *
         * We intentionally use teamLeaderId != null
         * so unassigned employees are not treated as
         * members of a team.
         */
        if (
            manager.getRoles()
                .contains(Role.MD)
        ) {

            return userRepository
                .findAll()
                .stream()
                .filter(user ->
                    user.getTeamLeaderId() != null
                )
                .filter(user ->
                    user.getRoles() != null &&
                    user.getRoles()
                        .contains(Role.EMPLOYEE)
                )
                .map(this::toTeamMemberResponse)
                .toList();
        }

        /*
         * =====================================================
         * TL
         * =====================================================
         *
         * TL can see only employees assigned to
         * their own MongoDB user ID.
         *
         * teamLeaderId stores the internal MongoDB ID.
         */
        return userRepository
            .findByTeamLeaderId(
                manager.getId()
            )
            .stream()
            .filter(user ->
                user.getRoles() != null &&
                user.getRoles()
                    .contains(Role.EMPLOYEE)
            )
            .map(this::toTeamMemberResponse)
            .toList();
    }


    /**
     * Adds an existing employee to a team.
     *
     * TL:
     *     teamLeaderEmployeeId must be the
     *     authenticated TL's employee ID.
     *
     * MD:
     *     Can select which TL should receive
     *     the employee.
     */
    public TeamMemberResponse addTeamMember(
        AddTeamMemberRequest request,
        Authentication authentication
    ) {

        User manager =
            getAuthenticatedUser(
                authentication
            );

        requireTeamManagementRole(manager);

        /*
         * Find the TL using the TL's EMPLOYEE ID.
         *
         * This is NOT the MongoDB _id.
         */
        User teamLeader =
            userRepository
                .findByEmployeeId(
                    request.teamLeaderEmployeeId()
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Team leader not found"
                    )
                );

        /*
         * The selected user must actually
         * have the TL role.
         */
        if (
            teamLeader.getRoles() == null ||
            !teamLeader.getRoles()
                .contains(Role.TL)
        ) {

            throw new IllegalArgumentException(
                "Specified user is not a team leader"
            );
        }

        /*
         * TL can only add employees to
         * their own team.
         */
        if (
            manager.getRoles()
                .contains(Role.TL) &&
            !manager.getId()
                .equals(teamLeader.getId())
        ) {

            throw new AccessDeniedException(
                "TL can only manage their own team"
            );
        }

        /*
         * Find employee using employee ID.
         */
        User employee =
            userRepository
                .findByEmployeeId(
                    request.employeeId()
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Employee not found"
                    )
                );

        /*
         * Only EMPLOYEE accounts can
         * be assigned to a team.
         */
        if (
            employee.getRoles() == null ||
            !employee.getRoles()
                .contains(Role.EMPLOYEE) ||
            employee.getRoles()
                .contains(Role.TL) ||
            employee.getRoles()
                .contains(Role.MD)
        ) {

            throw new IllegalArgumentException(
                "Only employees can be added to a team"
            );
        }

        /*
         * Prevent assigning an employee
         * to another team.
         */
        if (
            employee.getTeamLeaderId() != null &&
            !employee.getTeamLeaderId()
                .equals(teamLeader.getId())
        ) {

            throw new AccessDeniedException(
                "Employee already belongs to another team"
            );
        }

        /*
         * Assign employee to selected TL.
         *
         * Internally we store the TL MongoDB ID.
         */
        employee.setTeamLeaderId(
            teamLeader.getId()
        );

        employee.setUpdatedAt(
            Instant.now()
        );

        User saved =
            userRepository.save(employee);

        return toTeamMemberResponse(
            saved
        );
    }


    /**
     * Updates an employee in a specific team.
     *
     * Both TL and MD can use this operation.
     *
     * teamLeaderEmployeeId:
     *     Employee ID of the TL whose team
     *     contains the employee.
     *
     * employeeId:
     *     Employee ID of the employee being updated.
     */
    public TeamMemberResponse updateTeamMember(
        String teamLeaderEmployeeId,
        String employeeId,
        TeamMemberUpdateRequest request,
        Authentication authentication
    ) {

        User manager =
            getAuthenticatedUser(
                authentication
            );

        requireTeamManagementRole(manager);

        /*
         * Find the selected TL by employee ID.
         */
        User teamLeader =
            userRepository
                .findByEmployeeId(
                    teamLeaderEmployeeId
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Team leader not found"
                    )
                );

        /*
         * Selected user must be a TL.
         */
        if (
            teamLeader.getRoles() == null ||
            !teamLeader.getRoles()
                .contains(Role.TL)
        ) {

            throw new IllegalArgumentException(
                "Specified user is not a team leader"
            );
        }

        /*
         * TL can update only employees
         * in their own team.
         */
        if (
            manager.getRoles()
                .contains(Role.TL) &&
            !manager.getId()
                .equals(teamLeader.getId())
        ) {

            throw new AccessDeniedException(
                "TL can only manage their own team"
            );
        }

        /*
         * Find employee using BOTH:
         *
         * employeeId
         * +
         * teamLeaderId
         *
         * This prevents updating an employee
         * from another team.
         */
        User employee =
            userRepository
                .findByEmployeeIdAndTeamLeaderId(
                    employeeId,
                    teamLeader.getId()
                )
                .orElseThrow(() ->
                    new AccessDeniedException(
                        "Employee does not belong to the specified team"
                    )
                );

        /*
         * Update first name.
         */
        if (
            request.firstName() != null
        ) {

            employee.setFirstName(
                request.firstName()
            );
        }

        /*
         * Update last name.
         */
        if (
            request.lastName() != null
        ) {

            employee.setLastName(
                request.lastName()
            );
        }

        /*
         * Update department.
         *
         * Employee remains inside the same
         * team, so department must match
         * the TL department.
         */
        if (
            request.departmentName() != null
        ) {

            if (
                teamLeader.getDepartmentName() != null &&
                !teamLeader
                    .getDepartmentName()
                    .equalsIgnoreCase(
                        request.departmentName()
                    )
            ) {

                throw new IllegalArgumentException(
                    "Employee department must match the team leader department"
                );
            }

            employee.setDepartmentName(
                request.departmentName()
            );
        }

        employee.setUpdatedAt(
            Instant.now()
        );

        User saved =
            userRepository.save(employee);

        return toTeamMemberResponse(
            saved
        );
    }


    /**
     * Removes an employee from a specific team.
     *
     * This does NOT delete the employee account.
     *
     * It only clears teamLeaderId.
     */
    public void removeTeamMember(
        String teamLeaderEmployeeId,
        String employeeId,
        Authentication authentication
    ) {

        User manager =
            getAuthenticatedUser(
                authentication
            );

        requireTeamManagementRole(manager);

        /*
         * Find TL using employee ID.
         */
        User teamLeader =
            userRepository
                .findByEmployeeId(
                    teamLeaderEmployeeId
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Team leader not found"
                    )
                );

        /*
         * Selected user must be a TL.
         */
        if (
            teamLeader.getRoles() == null ||
            !teamLeader.getRoles()
                .contains(Role.TL)
        ) {

            throw new IllegalArgumentException(
                "Specified user is not a team leader"
            );
        }

        /*
         * TL can remove only from
         * their own team.
         */
        if (
            manager.getRoles()
                .contains(Role.TL) &&
            !manager.getId()
                .equals(teamLeader.getId())
        ) {

            throw new AccessDeniedException(
                "TL can only manage their own team"
            );
        }

        /*
         * Find employee using BOTH:
         *
         * employeeId
         * +
         * teamLeaderId
         */
        User employee =
            userRepository
                .findByEmployeeIdAndTeamLeaderId(
                    employeeId,
                    teamLeader.getId()
                )
                .orElseThrow(() ->
                    new AccessDeniedException(
                        "Employee does not belong to the specified team"
                    )
                );

        /*
         * Remove team relationship.
         *
         * Employee account is NOT deleted.
         */
        employee.setTeamLeaderId(null);

        employee.setUpdatedAt(
            Instant.now()
        );

        userRepository.save(employee);
    }


    // =========================================================
    // GET AUTHENTICATED USER
    // =========================================================

    private User getAuthenticatedUser(
        Authentication authentication
    ) {

        /*
         * JWT subject remains the internal
         * MongoDB user ID.
         *
         * Employee ID is the public business identifier.
         */
        return userRepository
            .findById(
                authentication.getName()
            )
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "Authenticated user not found"
                )
            );
    }


    // =========================================================
    // TEAM MANAGEMENT ROLE CHECK
    // =========================================================

    private void requireTeamManagementRole(
        User user
    ) {

        if (
            user.getRoles() == null ||
            (
                !user.getRoles()
                    .contains(Role.TL) &&
                !user.getRoles()
                    .contains(Role.MD)
            )
        ) {

            throw new AccessDeniedException(
                "Only TL and MD can manage teams"
            );
        }
    }


    // =========================================================
    // TEAM MEMBER RESPONSE
    // =========================================================

    private TeamMemberResponse toTeamMemberResponse(
        User user
    ) {

        return new TeamMemberResponse(
            user.getId(),
            user.getEmployeeId(),
            user.getFirstName(),
            user.getLastName(),
            user.getDepartmentName(),
            user.getRoles(),
            Boolean.TRUE.equals(
                user.getEnabled()
            ),
            Boolean.TRUE.equals(
                user.getAccountLocked()
            ),
            user.getTeamLeaderId()
        );
    }


    // =========================================================
    // GET ALL EMPLOYEES
    // =========================================================

    /**
     * Returns all employee accounts.
     *
     * MD accounts are excluded.
     *
     * Password hashes are never returned.
     */
    public List<RegisterResponse> getAllEmployees() {

        return userRepository
            .findAll()
            .stream()
            .filter(user ->
                user.getRoles() == null ||
                !user.getRoles()
                    .contains(Role.MD)
            )
            .map(user ->
                new RegisterResponse(
                    user.getId(),
                    user.getEmployeeId(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getDepartmentName(),
                    user.getRoles(),
                    Boolean.TRUE.equals(
                        user.getEnabled()
                    )
                )
            )
            .toList();
    }


    // =========================================================
    // UNLOCK ACCOUNT
    // =========================================================

    /**
     * Unlocks an employee account
     * using employeeId.
     */
    public void unlockAccount(
        String employeeId
    ) {

        User user =
            userRepository
                .findByEmployeeId(
                    employeeId
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Employee not found"
                    )
                );

        user.setAccountLocked(false);

        user.setFailedLoginAttempts(0);

        user.setUpdatedAt(
            Instant.now()
        );

        userRepository.save(user);
    }


    // =========================================================
    // DELETE EMPLOYEE
    // =========================================================

    @Transactional
    public void deleteEmployee(
        String employeeId
    ) {

        User user =
            userRepository
                .findByEmployeeId(
                    employeeId
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Employee not found"
                    )
                );

        /*
         * MD accounts must never be deleted
         * through employee-management API.
         */
        if (
            user.getRoles() != null &&
            user.getRoles()
                .contains(Role.MD)
        ) {

            throw new IllegalArgumentException(
                "MD account cannot be deleted"
            );
        }

        /*
         * Remove every refresh token belonging
         * to the employee.
         */
        refreshTokenRepository.deleteByUserId(
            user.getId()
        );

        /*
         * Delete employee account.
         */
        userRepository.delete(user);
    }


    // =========================================================
    // UPDATE EMPLOYEE ROLE
    // =========================================================

    /**
     * Updates an employee's role using
     * employeeId.
     */
    public void updateEmployeeRole(
        String employeeId,
        Role newRole
    ) {

        if (
            newRole == null
        ) {

            throw new IllegalArgumentException(
                "Role must not be null"
            );
        }

        /*
         * MD role cannot be assigned through
         * employee-management API.
         */
        if (
            newRole == Role.MD
        ) {

            throw new IllegalArgumentException(
                "MD role cannot be assigned through this API"
            );
        }

        User user =
            userRepository
                .findByEmployeeId(
                    employeeId
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Employee not found"
                    )
                );

        /*
         * Existing MD account cannot be
         * modified through role API.
         */
        if (
            user.getRoles() != null &&
            user.getRoles()
                .contains(Role.MD)
        ) {

            throw new IllegalArgumentException(
                "MD account role cannot be modified"
            );
        }

        /*
         * User receives exactly one role.
         */
        Set<Role> roles =
            new HashSet<>();

        roles.add(newRole);

        user.setRoles(roles);

        user.setUpdatedAt(
            Instant.now()
        );

        userRepository.save(user);
    }


    // =========================================================
    // UPDATE EMPLOYEE DETAILS
    // =========================================================

    /**
     * Updates employee details using
     * employeeId.
     *
     * currentEmployeeId:
     *     Existing employee ID.
     *
     * employeeId:
     *     New employee ID.
     */
    public void updateEmployeeDetails(
        String currentEmployeeId,
        String employeeId,
        String firstName,
        String lastName,
        String departmentName
    ) {

        User user =
            userRepository
                .findByEmployeeId(
                    currentEmployeeId
                )
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Employee not found"
                    )
                );

        /*
         * MD accounts cannot be modified
         * through employee-management API.
         */
        if (
            user.getRoles() != null &&
            user.getRoles()
                .contains(Role.MD)
        ) {

            throw new IllegalArgumentException(
                "MD account cannot be modified"
            );
        }

        /*
         * Employee ID cannot be empty.
         */
        if (
            employeeId == null ||
            employeeId.isBlank()
        ) {

            throw new IllegalArgumentException(
                "Employee ID must not be empty"
            );
        }

        /*
         * If employee ID changes, make sure
         * another account does not already
         * use it.
         */
        if (
            !employeeId.equals(
                user.getEmployeeId()
            ) &&
            userRepository.existsByEmployeeId(
                employeeId
            )
        ) {

            throw new IllegalArgumentException(
                "Employee ID already registered"
            );
        }

        user.setEmployeeId(
            employeeId
        );

        user.setFirstName(
            firstName
        );

        user.setLastName(
            lastName
        );

        user.setDepartmentName(
            departmentName
        );

        user.setUpdatedAt(
            Instant.now()
        );

        userRepository.save(user);
    }


    // =========================================================
    // SAVE REFRESH TOKEN
    // =========================================================

    private void saveRefreshToken(
        String userId,
        String rawRefreshToken
    ) {

        Instant now = Instant.now();

        RefreshToken refreshToken =
            RefreshToken.builder()
                .tokenHash(
                    refreshTokenService.hashToken(
                        rawRefreshToken
                    )
                )
                .userId(userId)
                .createdAt(now)
                .expiresAt(
                    now.plus(
                        REFRESH_TOKEN_DAYS,
                        ChronoUnit.DAYS
                    )
                )
                .revokedAt(null)
                .build();

        refreshTokenRepository.save(
            refreshToken
        );
    }


    // =========================================================
    // FAILED LOGIN
    // =========================================================

    private void handleFailedLogin(
        User user
    ) {

        int attempts =
            user.getFailedLoginAttempts() == null
                ? 0
                : user.getFailedLoginAttempts();

        attempts++;

        user.setFailedLoginAttempts(
            attempts
        );

        /*
         * Account-lock threshold.
         */
        if (
            attempts >= 5
        ) {

            user.setAccountLocked(
                true
            );
        }

        user.setUpdatedAt(
            Instant.now()
        );

        userRepository.save(user);
    }


    // =========================================================
    // RESET FAILED LOGIN ATTEMPTS
    // =========================================================

    private void resetFailedLoginAttempts(
        User user
    ) {

        if (
            user.getFailedLoginAttempts() != null &&
            user.getFailedLoginAttempts() > 0
        ) {

            user.setFailedLoginAttempts(
                0
            );

            user.setUpdatedAt(
                Instant.now()
            );

            userRepository.save(user);
        }
    }
}