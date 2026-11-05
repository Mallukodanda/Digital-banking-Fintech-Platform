package com.fintech.bank.config;

import com.fintech.bank.entity.*;
import com.fintech.bank.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Ensures required demonstration seed accounts (Customer, Admin, Beneficiaries)
 * exist in the database with properly hashed passwords on system startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserAuthRepository userAuthRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountRepository accountRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserAuthRepository userAuthRepository,
                           CustomerProfileRepository customerProfileRepository,
                           AccountRepository accountRepository,
                           BeneficiaryRepository beneficiaryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userAuthRepository = userAuthRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.accountRepository = accountRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedCustomerUser();
        seedSarahUser();
        seedAdminUser();
    }

    private void seedCustomerUser() {
        if (userAuthRepository.existsByEmail("customer@bank.com")) {
            return;
        }

        log.info("Seeding default demo customer account: customer@bank.com");
        UserAuth customerAuth = new UserAuth(
                "customer@bank.com",
                passwordEncoder.encode("Customer@123"),
                UserRole.ROLE_CUSTOMER
        );
        UserAuth savedAuth = userAuthRepository.save(customerAuth);

        CustomerProfile profile = new CustomerProfile(
                savedAuth,
                "Alex",
                "Mercer",
                "+919876543210"
        );
        profile.setKycStatus(KycStatus.VERIFIED);
        profile.setKycTier(2);
        CustomerProfile savedProfile = customerProfileRepository.save(profile);

        if (!accountRepository.existsByAccountNumber("100000000001")) {
            Account account = new Account(
                    savedProfile,
                    "100000000001",
                    AccountType.CHECKING,
                    "INR"
            );
            account.setCachedBalance(new BigDecimal("50000.0000"));
            accountRepository.save(account);
            log.info("Seeded primary checking account 100000000001 with balance ₹50,000.00");
        }
    }

    private void seedSarahUser() {
        if (userAuthRepository.existsByEmail("sarah@bank.com")) {
            return;
        }

        log.info("Seeding transfer counterparty: sarah@bank.com");
        UserAuth sarahAuth = new UserAuth(
                "sarah@bank.com",
                passwordEncoder.encode("Sarah@123"),
                UserRole.ROLE_CUSTOMER
        );
        UserAuth savedAuth = userAuthRepository.save(sarahAuth);

        CustomerProfile profile = new CustomerProfile(
                savedAuth,
                "Sarah",
                "Connor",
                "+919876543211"
        );
        profile.setKycStatus(KycStatus.VERIFIED);
        profile.setKycTier(2);
        CustomerProfile savedProfile = customerProfileRepository.save(profile);

        if (!accountRepository.existsByAccountNumber("100000000002")) {
            Account account = new Account(
                    savedProfile,
                    "100000000002",
                    AccountType.CHECKING,
                    "INR"
            );
            account.setCachedBalance(new BigDecimal("10000.0000"));
            accountRepository.save(account);
        }

        // Add Sarah as a beneficiary for customer@bank.com
        userAuthRepository.findByEmail("customer@bank.com")
                .flatMap(u -> customerProfileRepository.findByUserAuthId(u.getId()))
                .ifPresent(alexProfile -> {
                    if (!beneficiaryRepository.existsActiveByCustomerIdAndAccountNumber(alexProfile.getId(), "100000000002")) {
                        Beneficiary beneficiary = new Beneficiary(
                                alexProfile,
                                "Sarah Connor",
                                "100000000002",
                                "Sarah Friend"
                        );
                        beneficiaryRepository.save(beneficiary);
                        log.info("Seeded Sarah Connor as beneficiary for customer@bank.com");
                    }
                });
    }

    private void seedAdminUser() {
        if (userAuthRepository.existsByEmail("admin@bank.com")) {
            return;
        }

        log.info("Seeding default demo admin account: admin@bank.com");
        UserAuth adminAuth = new UserAuth(
                "admin@bank.com",
                passwordEncoder.encode("Admin@123"),
                UserRole.ROLE_ADMIN
        );
        UserAuth savedAuth = userAuthRepository.save(adminAuth);

        CustomerProfile profile = new CustomerProfile(
                savedAuth,
                "System",
                "Administrator",
                "+919999999999"
        );
        profile.setKycStatus(KycStatus.VERIFIED);
        profile.setKycTier(3);
        customerProfileRepository.save(profile);
    }
}

