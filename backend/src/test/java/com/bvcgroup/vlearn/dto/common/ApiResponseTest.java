package com.bvcgroup.vlearn.dto.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void builder_successResult_usesDefaultSuccessCode() {
        ApiResponse<String> response =
                ApiResponse.<String>builder().result("ready").build();

        assertThat(response.getCode()).isEqualTo(1000);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getResult()).isEqualTo("ready");
    }
}
