package com.bvcgroup.vlearn.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCodeTest {

    @Test
    void fromKey_knownKey_returnsMatchingErrorCode() {
        assertThat(ErrorCode.fromKey("RESOURCE_NOT_FOUND")).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void fromKey_unknownKey_returnsInvalidRequest() {
        assertThat(ErrorCode.fromKey("NOT_A_REAL_CODE")).isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void resourceConflict_hasConflictHttpStatus() {
        assertThat(ErrorCode.RESOURCE_CONFLICT.getStatus().value()).isEqualTo(409);
    }
}
