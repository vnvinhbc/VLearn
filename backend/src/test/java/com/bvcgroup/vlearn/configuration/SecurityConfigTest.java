package com.bvcgroup.vlearn.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoder_applicationContext_exposesStrongEncoder() {
        String encoded = passwordEncoder.encode("correct horse battery staple");

        assertThat(passwordEncoder.matches("correct horse battery staple", encoded))
                .isTrue();
        assertThat(encoded).doesNotContain("correct horse battery staple");
    }

    @Test
    void health_unauthenticated_isPublic() throws Exception {
        mockMvc.perform(get("/vlearn/actuator/health").contextPath("/vlearn")).andExpect(status().isOk());
    }

    @Test
    void unspecifiedEndpoint_unauthenticated_isUnauthorizedByDefault() throws Exception {
        mockMvc.perform(get("/vlearn/not-configured").contextPath("/vlearn")).andExpect(status().isUnauthorized());
    }
}
