package com.fintech.bank.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 401 Unauthorized responses in RFC 7807 format when unauthenticated access occurs.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type("https://api.fintech.bank/v1/errors/unauthorized")
                .title("Unauthorized")
                .status(HttpStatus.UNAUTHORIZED.value())
                .detail("Authentication required. Please provide a valid Bearer token.")
                .instance(request.getRequestURI())
                .errorCode("AUTH_UNAUTHORIZED")
                .correlationId(correlationId != null ? correlationId : "N/A")
                .build();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
