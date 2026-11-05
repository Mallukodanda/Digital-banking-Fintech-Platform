package com.fintech.bank.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.bank.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 403 Forbidden responses in RFC 7807 format when insufficient role or permissions are detected.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .type("https://api.fintech.bank/v1/errors/forbidden")
                .title("Forbidden")
                .status(HttpStatus.FORBIDDEN.value())
                .detail("You do not possess the required permissions to access this banking resource.")
                .instance(request.getRequestURI())
                .errorCode("AUTH_FORBIDDEN_ACCESS")
                .correlationId(correlationId != null ? correlationId : "N/A")
                .build();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
