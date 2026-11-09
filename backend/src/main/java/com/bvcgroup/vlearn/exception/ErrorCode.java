package com.bvcgroup.vlearn.exception;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(1001, "Invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1002, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1003, "Unauthorized", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1004, "Resource not found", HttpStatus.NOT_FOUND),
    RESOURCE_CONFLICT(1005, "Resource conflict", HttpStatus.CONFLICT),
    INTERNAL_ERROR(9999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus status;

    public static ErrorCode fromKey(String key) {
        if (key == null) {
            return INVALID_REQUEST;
        }

        return Arrays.stream(values())
                .filter(errorCode -> errorCode.name().equals(key))
                .findFirst()
                .orElse(INVALID_REQUEST);
    }
}
