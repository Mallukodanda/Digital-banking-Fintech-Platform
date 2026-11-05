package com.fintech.bank.security;

import com.fintech.bank.config.SecurityConfig;
import com.fintech.bank.controller.AdminController;
import com.fintech.bank.controller.CustomerController;
import com.fintech.bank.dto.UserProfileResponse;
import com.fintech.bank.entity.KycStatus;
import com.fintech.bank.entity.UserRole;
import com.fintech.bank.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CustomerController.class, AdminController.class})
@Import({SecurityConfig.class, CorrelationIdFilter.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("Security & RBAC Authorization Tests")
class SecurityAuthorizationWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Unauthenticated request to protected endpoint should return 401 Unauthorized")
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/customers/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("ROLE_CUSTOMER accessing /api/v1/customers/me should return 200 OK")
    void testCustomerAuthorizedAccess() throws Exception {
        UserProfileResponse profile = new UserProfileResponse(
                UUID.randomUUID(),
                "customer@bank.com",
                "Jane",
                "Doe",
                "Jane Doe",
                "+14155551234",
                UserRole.ROLE_CUSTOMER,
                KycStatus.VERIFIED,
                1,
                Instant.now()
        );

        when(authService.getCurrentUserProfile(any(UUID.class))).thenReturn(profile);

        mockMvc.perform(get("/api/v1/customers/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "customer@bank.com", roles = {"CUSTOMER"})
    @DisplayName("ROLE_CUSTOMER accessing admin endpoint should return 403 Forbidden")
    void testCustomerForbiddenFromAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_FORBIDDEN_ACCESS"));
    }

    @Test
    @WithMockUser(username = "admin@bank.com", roles = {"ADMIN"})
    @DisplayName("ROLE_ADMIN accessing admin endpoint should return 200 OK")
    void testAdminAuthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.adminAccessVerified").value(true));
    }
}
