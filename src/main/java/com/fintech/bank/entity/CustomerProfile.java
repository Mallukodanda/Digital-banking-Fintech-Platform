package com.fintech.bank.entity;

import jakarta.persistence.*;

/**
 * Customer PII and KYC profile entity mapping to customer_profiles table.
 */
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile extends BaseAuditableEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAuth userAuth;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone", nullable = false, unique = true, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 30)
    private KycStatus kycStatus = KycStatus.UNVERIFIED;

    @Column(name = "kyc_tier", nullable = false)
    private int kycTier = 1;

    public CustomerProfile() {
    }

    public CustomerProfile(UserAuth userAuth, String firstName, String lastName, String phone) {
        this.userAuth = userAuth;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.kycStatus = KycStatus.UNVERIFIED;
        this.kycTier = 1;
    }

    public UserAuth getUserAuth() {
        return userAuth;
    }

    public void setUserAuth(UserAuth userAuth) {
        this.userAuth = userAuth;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public int getKycTier() {
        return kycTier;
    }

    public void setKycTier(int kycTier) {
        this.kycTier = kycTier;
    }
}
