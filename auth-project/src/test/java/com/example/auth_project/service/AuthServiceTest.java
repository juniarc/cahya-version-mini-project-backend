package com.example.auth_project.service;

import com.example.auth_project.model.entity.Employee;
import com.example.auth_project.model.entity.Role;
import com.example.auth_project.model.entity.User;
import com.example.auth_project.model.request.RegisterRequest;
import com.example.auth_project.repository.EmployeeRepository;
import com.example.auth_project.repository.RoleRepository;
import com.example.auth_project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setUsername("budi");
        request.setName("Budi Santoso");
        request.setEmail("budi@mail.com");
        request.setPassword("rahasia");
    }

    @Test
    void shouldRegisterUserWithEncodedPasswordAndUserRole() {
        Role userRole = new Role();
        when(userRepository.countByUsername("budi")).thenReturn(0);
        when(userRepository.countByEmail("budi@mail.com")).thenReturn(0);
        when(passwordEncoder.encode("rahasia")).thenReturn("hashed-rahasia");
        when(roleRepository.findByRoleName("user")).thenReturn(Optional.of(userRole));

        User result = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(result).isSameAs(savedUser);
        assertThat(savedUser.getUsername()).isEqualTo("budi");
        assertThat(savedUser.getName()).isEqualTo("Budi Santoso");
        assertThat(savedUser.getEmail()).isEqualTo("budi@mail.com");
        assertThat(savedUser.getPassword())
                .isEqualTo("hashed-rahasia")
                .isNotEqualTo("rahasia");
        assertThat(savedUser.getRoles()).containsExactly(userRole);
        assertThat(savedUser.getCreated_at()).isNotNull();
        assertThat(savedUser.getUpdated_at()).isNotNull();
    }

    @Test
    void shouldCreateEmployeeLinkedToNewUser() {
        when(userRepository.countByUsername("budi")).thenReturn(0);
        when(userRepository.countByEmail("budi@mail.com")).thenReturn(0);
        when(passwordEncoder.encode("rahasia")).thenReturn("hashed-rahasia");
        when(roleRepository.findByRoleName("user")).thenReturn(Optional.of(new Role()));

        User result = authService.register(request);

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());

        assertThat(employeeCaptor.getValue().getUser()).isSameAs(result);
        assertThat(employeeCaptor.getValue().getCreated_at()).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyTaken() {
        when(userRepository.countByUsername("budi")).thenReturn(1);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username is already taken.");

        verify(userRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyTaken() {
        when(userRepository.countByUsername("budi")).thenReturn(0);
        when(userRepository.countByEmail("budi@mail.com")).thenReturn(1);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email is already taken");

        verify(userRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenDefaultRoleNotFound() {
        when(userRepository.countByUsername("budi")).thenReturn(0);
        when(userRepository.countByEmail("budi@mail.com")).thenReturn(0);
        when(passwordEncoder.encode("rahasia")).thenReturn("hashed-rahasia");
        when(roleRepository.findByRoleName("user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Role not found.");

        verify(userRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }
}