package com.example.auth_project.config;

import com.example.auth_project.model.UserPrincipal;
import com.example.auth_project.model.entity.Role;
import com.example.auth_project.model.entity.User;
import com.example.auth_project.service.JwtService;
import com.example.auth_project.service.MyUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import jakarta.servlet.FilterChain;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private ApplicationContext context;

    @Mock
    private MyUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // SecurityContext bersifat thread-local, bersihkan agar tidak bocor ke test lain
        SecurityContextHolder.clearContext();
    }

    // Helper: membuat UserPrincipal dengan role, seperti data normal di database
    private UserPrincipal createPrincipal(String username, String... roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            Role role = new Role();
            role.setRoleName(name);
            roles.add(role);
        }

        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setRoles(roles);
        return new UserPrincipal(user);
    }

    // ==================== Tanpa token ====================

    @Test
    void shouldContinueChainWithoutAuthenticationWhenHeaderMissing() throws Exception {
        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    void shouldIgnoreHeaderThatIsNotBearer() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).extractUsername(anyString());
    }

    // ==================== Token valid ====================

    @Test
    void shouldSetAuthenticationWhenTokenValid() throws Exception {
        request.addHeader("Authorization", "Bearer token-abc");
        UserPrincipal principal = createPrincipal("budi", "user");

        when(jwtService.extractUsername("token-abc")).thenReturn("budi");
        when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("budi")).thenReturn(principal);
        when(jwtService.validateToken(eq("token-abc"), any(UserDetails.class))).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.isAuthenticated()).isTrue();
        assertThat(auth.getPrincipal()).isSameAs(principal);
        assertThat(auth.getName()).isEqualTo("budi");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldMapEachRoleToRolePrefixedAuthority() throws Exception {
        request.addHeader("Authorization", "Bearer token-abc");
        UserPrincipal principal = createPrincipal("budi", "user", "admin");

        when(jwtService.extractUsername("token-abc")).thenReturn("budi");
        when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("budi")).thenReturn(principal);
        when(jwtService.validateToken(eq("token-abc"), any(UserDetails.class))).thenReturn(true);

        jwtFilter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_user", "ROLE_admin");
    }

    // ==================== Token tidak valid ====================

    @Test
    void shouldNotSetAuthenticationWhenTokenInvalid() throws Exception {
        request.addHeader("Authorization", "Bearer token-salah");
        UserPrincipal principal = createPrincipal("budi", "user");

        when(jwtService.extractUsername("token-salah")).thenReturn("budi");
        when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("budi")).thenReturn(principal);
        when(jwtService.validateToken(eq("token-salah"), any(UserDetails.class))).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldPropagateExceptionWhenTokenExpired() {
        request.addHeader("Authorization", "Bearer token-expired");

        when(jwtService.extractUsername("token-expired"))
                .thenThrow(mock(ExpiredJwtException.class));

        // Filter tidak menangkap exception dari parser JWT
        assertThatThrownBy(() -> jwtFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(ExpiredJwtException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldPropagateExceptionWhenUserNotFound() {
        request.addHeader("Authorization", "Bearer token-abc");

        when(jwtService.extractUsername("token-abc")).thenReturn("hantu");
        when(context.getBean(MyUserDetailsService.class)).thenReturn(userDetailsService);
        when(userDetailsService.loadUserByUsername("hantu"))
                .thenThrow(new UsernameNotFoundException("Username is not found."));

        // Filter tidak menangkap UsernameNotFoundException dari UserDetailsService
        assertThatThrownBy(() -> jwtFilter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(UsernameNotFoundException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ==================== Sudah ada authentication ====================

    @Test
    void shouldNotReloadUserWhenAuthenticationAlreadySet() throws Exception {
        request.addHeader("Authorization", "Bearer token-abc");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing", null, Collections.emptyList()));

        when(jwtService.extractUsername("token-abc")).thenReturn("budi");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(context, never()).getBean(MyUserDetailsService.class);
        verify(jwtService, never()).validateToken(anyString(), any(UserDetails.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("existing");
        verify(filterChain).doFilter(request, response);
    }
}