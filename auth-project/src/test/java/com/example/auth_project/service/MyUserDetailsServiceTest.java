package com.example.auth_project.service;

import com.example.auth_project.model.UserPrincipal;
import com.example.auth_project.model.entity.User;
import com.example.auth_project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MyUserDetailsService userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("budi");
        user.setPassword("$2a$hashed");
    }

    @Test
    void shouldLoadUserByUsernameWhenUserExists() {
        when(userRepository.findByUsername("budi")).thenReturn(user);

        UserDetails result = userDetailsService.loadUserByUsername("budi");

        assertThat(result).isInstanceOf(UserPrincipal.class);
        assertThat(result.getUsername()).isEqualTo("budi");
        assertThat(result.getPassword()).isEqualTo("$2a$hashed");
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findByUsername("tidakada")).thenReturn(null);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("tidakada"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Username is not found.");
    }
}