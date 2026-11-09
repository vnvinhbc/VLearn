package com.bvcgroup.vlearn.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.bvcgroup.vlearn.dto.common.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAppException_knownBusinessError_usesErrorCodeContract() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAppException(new AppException(ErrorCode.RESOURCE_NOT_FOUND));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    void handleUnexpectedException_anyException_hidesInternalMessage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleUnexpectedException(new IllegalStateException("database password leaked"));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INTERNAL_ERROR.getMessage());
        assertThat(response.getBody().getMessage()).doesNotContain("password");
    }

    @Test
    void handleAuthenticationException_badCredentials_returnsUnauthenticated() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAuthenticationException(new BadCredentialsException("hidden detail"));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.UNAUTHENTICATED.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNAUTHENTICATED.getCode());
    }

    @Test
    void handleAccessDeniedException_denied_returnsUnauthorized() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAccessDeniedException(new AccessDeniedException("hidden detail"));

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    void handleNoResourceFoundException_unknownRoute_returnsNotFound() {
        NoResourceFoundException exception =
                new NoResourceFoundException(HttpMethod.GET, "/missing", "No static resource");

        ResponseEntity<ApiResponse<Void>> response = handler.handleNoResourceFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getCode());
    }
}
