package com.example.auth_project.config;

import com.example.auth_project.controller.AdminController;
import com.example.auth_project.service.JwtService;
import com.example.auth_project.service.TimesheetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import({SecurityConfig.class, SecurityConfigTest.TestEndpoints.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Dependency yang diminta SecurityConfig dan JwtFilter
    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtService jwtService;

    // Dependency AdminController
    @MockBean
    private TimesheetService timesheetService;

    // Endpoint dummy untuk menguji aturan path /api/auth/** dan endpoint lain
    @RestController
    static class TestEndpoints {

        @GetMapping("/api/auth/ping")
        String authPing() {
            return "ok";
        }

        @GetMapping("/api/secure/ping")
        String securePing() {
            return "ok";
        }
    }

    // ==================== Aturan akses path ====================

    @Test
    void shouldAllowAnonymousAccessToAuthPath() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAnonymousAccessToProtectedPath() throws Exception {
        // Tanpa formLogin dan httpBasic, Spring Security mengembalikan 403 untuk anonymous
        mockMvc.perform(get("/api/secure/ping"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "user")
    void shouldAllowAuthenticatedUserToAccessProtectedPath() throws Exception {
        mockMvc.perform(get("/api/secure/ping"))
                .andExpect(status().isOk());
    }

    // ==================== Role-based access (@EnableMethodSecurity) ====================

    @Test
    @WithMockUser(roles = "admin")
    void shouldAllowAdminToAccessAdminEndpoint() throws Exception {
        when(timesheetService.getAllTimesheetByAdmin()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/timesheet/admin"))
                .andExpect(status().isOk());

        verify(timesheetService).getAllTimesheetByAdmin();
    }

    @Test
    @WithMockUser(roles = "user")
    void shouldForbidNonAdminFromAdminEndpoint() throws Exception {
        mockMvc.perform(get("/timesheet/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectAnonymousAccessToAdminEndpoint() throws Exception {
        mockMvc.perform(get("/timesheet/admin"))
                .andExpect(status().isForbidden());
    }

    // ==================== CORS ====================

    @Test
    void shouldAllowPreflightFromConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/secure/ping")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void shouldNotAllowPreflightFromUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/secure/ping")
                        .header("Origin", "http://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // ==================== PasswordEncoder ====================

    @Test
    void shouldUseBCryptPasswordEncoder() {
        String encoded = passwordEncoder.encode("rahasia");

        assertThat(encoded).startsWith("$2");
        assertThat(passwordEncoder.matches("rahasia", encoded)).isTrue();
        assertThat(passwordEncoder.matches("salah", encoded)).isFalse();
    }
}