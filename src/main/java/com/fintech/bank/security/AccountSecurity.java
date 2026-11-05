package com.fintech.bank.security;

import com.fintech.bank.repository.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Security SpEL evaluator for verifying account ownership and preventing IDOR attacks.
 */
@Component("accountSecurity")
public class AccountSecurity {

    private final AccountRepository accountRepository;

    public AccountSecurity(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Checks if the authenticated principal owns the specified account or has administrative privileges.
     */
    public boolean isAccountOwner(UUID accountId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Admins and Auditors have global visibility
        boolean isStaff = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AUDITOR"));

        if (isStaff) {
            return true;
        }

        if (accountId == null) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            return false;
        }

        Optional<UUID> ownerUserId = accountRepository.findOwnerUserIdByAccountId(accountId);
        return ownerUserId.isPresent() && ownerUserId.get().equals(userPrincipal.getId());
    }
}
