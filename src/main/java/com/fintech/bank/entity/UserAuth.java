package com.fintech.bank.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Authentication and security credentials entity mapping to users_auth table.
 */
@Entity
@Table(name = "users_auth")
public class UserAuth extends BaseAuditableEntity {

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role = UserRole.ROLE_CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AuthStatus status = AuthStatus.ACTIVE;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @OneToOne(mappedBy = "userAuth", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CustomerProfile customerProfile;

    public UserAuth() {
    }

    public UserAuth(String email, String passwordHash, UserRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = AuthStatus.ACTIVE;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public AuthStatus getStatus() {
        return status;
    }

    public void setStatus(AuthStatus status) {
        this.status = status;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
        if (customerProfile != null) {
            customerProfile.setUserAuth(this);
        }
    }

    public boolean isAccountNonLocked() {
        if (status == AuthStatus.LOCKED) {
            if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
                return true; // Lock has expired
            }
            return false;
        }
        return status == AuthStatus.ACTIVE;
    }
}
