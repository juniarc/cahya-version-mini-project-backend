package com.example.auth_project.controller;

import com.example.auth_project.model.UserPrincipal;
import com.example.auth_project.model.entity.User;
import com.example.auth_project.model.request.LoginRequest;
import com.example.auth_project.model.request.RegisterRequest;
import com.example.auth_project.repository.RoleRepository;
import com.example.auth_project.service.AuthService;
import com.example.auth_project.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    private AuthenticationManager authManager;

    @MockBean
    private JwtService jwtService;

    // ==================== REGISTER ====================

    @Test
    void shouldRegisterAndReturnCreatedWithoutPassword() throws Exception {
        User savedUser = new User();
        savedUser.setUsername("budi");
        savedUser.setName("Budi Santoso");
        savedUser.setEmail("budi@mail.com");
        savedUser.setPassword("hashed-rahasia");

        when(authService.register(any(RegisterRequest.class))).thenReturn(savedUser);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "budi", "name": "Budi Santoso",
                             "email": "budi@mail.com", "password": "rahasia"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Register successfully"))
                .andExpect(jsonPath("$.user.username").value("budi"))
                .andExpect(jsonPath("$.user.email").value("budi@mail.com"))
                // Controller mengosongkan password sebelum dikirim (null)
                .andExpect(jsonPath("$.user.password").isEmpty());

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void shouldReturnBadRequestWhenUsernameAlreadyTaken() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username is already taken."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "budi", "name": "Budi Santoso",
                             "email": "budi@mail.com", "password": "rahasia"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Username is already taken."));
    }

    @Test
    void shouldReturnBadRequestWhenEmailAlreadyTaken() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email is already taken"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "budi", "name": "Budi Santoso",
                             "email": "budi@mail.com", "password": "rahasia"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is already taken"));
    }

    @Test
    void shouldReturnBadRequestWhenRequiredFieldEmpty() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "", "name": "Budi Santoso",
                             "email": "budi@mail.com", "password": "rahasia"}
                            """))
                .andExpect(status().isBadRequest());

        verify(authService, never()).register(any());
    }

    // ==================== LOGIN ====================

    @Test
    void shouldReturnTokenWhenLoginSuccess() throws Exception {
        UserPrincipal principal = mock(UserPrincipal.class);
        when(principal.getUsername()).thenReturn("budi");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken("budi")).thenReturn("token-abc");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "budi", "password": "rahasia"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successfully"))
                .andExpect(jsonPath("$.accessToken").value("token-abc"));

        verify(jwtService).generateToken("budi");
    }

    @Test
    void shouldReturnBadRequestWhenCredentialsInvalid() throws Exception {
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "budi", "password": "salah"}
                            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad credentials"));

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void shouldReturnBadRequestWhenUsernameEmpty() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username": "", "password": "rahasia"}
                            """))
                .andExpect(status().isBadRequest());

        verify(authManager, never()).authenticate(any());
    }
}