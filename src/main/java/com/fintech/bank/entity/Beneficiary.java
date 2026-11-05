package com.fintech.bank.entity;

import jakarta.persistence.*;

/**
 * Beneficiary entity mapping to the beneficiaries table.
 */
@Entity
@Table(name = "beneficiaries", uniqueConstraints = {
        @UniqueConstraint(name = "uq_beneficiaries_cust_acc", columnNames = {"customer_id", "destination_account_number"})
})
public class Beneficiary extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customerProfile;

    @Column(name = "beneficiary_name", nullable = false, length = 100)
    private String beneficiaryName;

    @Column(name = "destination_account_number", nullable = false, length = 20)
    private String destinationAccountNumber;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BeneficiaryStatus status = BeneficiaryStatus.ACTIVE;

    public Beneficiary() {
    }

    public Beneficiary(CustomerProfile customerProfile, String beneficiaryName, String destinationAccountNumber, String nickname) {
        this.customerProfile = customerProfile;
        this.beneficiaryName = beneficiaryName;
        this.destinationAccountNumber = destinationAccountNumber;
        this.nickname = nickname;
        this.status = BeneficiaryStatus.ACTIVE;
    }

    public CustomerProfile getCustomerProfile() {
        return customerProfile;
    }

    public void setCustomerProfile(CustomerProfile customerProfile) {
        this.customerProfile = customerProfile;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public BeneficiaryStatus getStatus() {
        return status;
    }

    public void setStatus(BeneficiaryStatus status) {
        this.status = status;
    }
}
