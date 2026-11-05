package com.fintech.bank.service;

import com.fintech.bank.config.CacheNames;
import com.fintech.bank.dto.BeneficiaryResponse;
import com.fintech.bank.dto.UserProfileResponse;
import com.fintech.bank.entity.*;
import com.fintech.bank.repository.AccountRepository;
import com.fintech.bank.repository.BeneficiaryRepository;
import com.fintech.bank.repository.CustomerProfileRepository;
import com.fintech.bank.repository.UserAuthRepository;
import com.fintech.bank.security.JwtTokenProvider;
import com.fintech.bank.service.impl.AuthServiceImpl;
import com.fintech.bank.service.impl.BeneficiaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({CacheBehaviorTest.TestCacheConfig.class, BeneficiaryServiceImpl.class, AuthServiceImpl.class})
@DisplayName("Cache-Aside & Invalidation Integration Tests")
@SuppressWarnings("null")
class CacheBehaviorTest {

    @TestConfiguration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheNames.BENEFICIARIES, CacheNames.CUSTOMER_PROFILES, CacheNames.SYSTEM_CONFIGS);
        }
    }

    @Autowired
    private BeneficiaryService beneficiaryService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private BeneficiaryRepository beneficiaryRepository;

    @MockBean
    private CustomerProfileRepository customerProfileRepository;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private UserAuthRepository userAuthRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtTokenProvider tokenProvider;

    private UUID sampleUserId;
    private UUID sampleBeneficiaryId;

    @BeforeEach
    void setUp() {
        sampleUserId = UUID.randomUUID();
        sampleBeneficiaryId = UUID.randomUUID();
        org.springframework.cache.Cache benCache = cacheManager.getCache(CacheNames.BENEFICIARIES);
        if (benCache != null) {
            benCache.clear();
        }
        org.springframework.cache.Cache profCache = cacheManager.getCache(CacheNames.CUSTOMER_PROFILES);
        if (profCache != null) {
            profCache.clear();
        }
    }

    @Test
    @DisplayName("Should serve subsequent beneficiary queries from Cache-Aside without hitting repository")
    void shouldServeBeneficiaryFromCache() {
        UserAuth auth = new UserAuth("user@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        CustomerProfile profile = new CustomerProfile(auth, "Alice Johnson", "1234567890", null);
        Beneficiary b1 = new Beneficiary(profile, "Bob", "1000000002", "Bob Smith");

        when(beneficiaryRepository.findAllByUserIdAndStatus(eq(sampleUserId), eq(BeneficiaryStatus.ACTIVE)))
                .thenReturn(List.of(b1));

        // First call: Cache miss -> queries repository
        List<BeneficiaryResponse> firstResult = beneficiaryService.getBeneficiariesByUserId(sampleUserId);
        assertThat(firstResult).hasSize(1);
        verify(beneficiaryRepository, times(1)).findAllByUserIdAndStatus(eq(sampleUserId), eq(BeneficiaryStatus.ACTIVE));

        // Second call: Cache hit -> returns directly from cache, repository NOT called again
        List<BeneficiaryResponse> secondResult = beneficiaryService.getBeneficiariesByUserId(sampleUserId);
        assertThat(secondResult).hasSize(1);
        verify(beneficiaryRepository, times(1)).findAllByUserIdAndStatus(eq(sampleUserId), eq(BeneficiaryStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should evict beneficiary cache on delete so next query fetches fresh data from DB")
    void shouldEvictCacheOnBeneficiaryRemoval() {
        UserAuth auth = new UserAuth("user@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        CustomerProfile profile = new CustomerProfile(auth, "Alice Johnson", "1234567890", null);
        Beneficiary b1 = new Beneficiary(profile, "Bob", "1000000002", "Bob Smith");

        when(beneficiaryRepository.findAllByUserIdAndStatus(eq(sampleUserId), eq(BeneficiaryStatus.ACTIVE)))
                .thenReturn(List.of(b1));
        when(beneficiaryRepository.findById(sampleBeneficiaryId)).thenReturn(Optional.of(b1));

        // Populate cache
        beneficiaryService.getBeneficiariesByUserId(sampleUserId);
        verify(beneficiaryRepository, times(1)).findAllByUserIdAndStatus(eq(sampleUserId), eq(BeneficiaryStatus.ACTIVE));

        // Evict via removeBeneficiary
        beneficiaryService.removeBeneficiary(auth.getId(), sampleBeneficiaryId);

        // Next read: Cache is evicted -> repository queried again
        beneficiaryService.getBeneficiariesByUserId(sampleUserId);
        verify(beneficiaryRepository, times(2)).findAllByUserIdAndStatus(eq(sampleUserId), eq(BeneficiaryStatus.ACTIVE));
    }

    @Test
    @DisplayName("Should cache Customer Profile reads on repeated invocations")
    void shouldCacheCustomerProfile() {
        UserAuth auth = new UserAuth("user@bank.com", "hash", UserRole.ROLE_CUSTOMER);
        CustomerProfile profile = new CustomerProfile(auth, "Charlie Brown", "9988776655", null);

        when(customerProfileRepository.findByUserAuthId(eq(sampleUserId))).thenReturn(Optional.of(profile));

        // First call: DB query
        UserProfileResponse res1 = authService.getCurrentUserProfile(sampleUserId);
        assertThat(res1.fullName()).isEqualTo("Charlie Brown");
        verify(customerProfileRepository, times(1)).findByUserAuthId(eq(sampleUserId));

        // Second call: Cache hit
        UserProfileResponse res2 = authService.getCurrentUserProfile(sampleUserId);
        assertThat(res2.fullName()).isEqualTo("Charlie Brown");
        verify(customerProfileRepository, times(1)).findByUserAuthId(eq(sampleUserId));
    }
}
