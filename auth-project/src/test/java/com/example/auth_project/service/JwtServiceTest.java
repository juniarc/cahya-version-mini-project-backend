package com.example.auth_project.service;

import com.example.auth_project.model.entity.Role;
import com.example.auth_project.model.entity.User;
import com.example.auth_project.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Stub getUsername() tidak diletakkan di sini karena tidak semua test memakainya
        userDetails = mock(UserDetails.class);
    }

    // Helper: membuat User dengan role, seperti data normal di database
    private User createUser(String username, String... roleNames) {
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            Role role = new Role();
            role.setRoleName(name);
            roles.add(role);
        }

        User user = new User();
        user.setUsername(username);
        user.setRoles(roles);
        return user;
    }

    // Helper: mengambil secret key dari JwtService untuk membuat token manual di test
    private Key secretKey() {
        String secret = (String) ReflectionTestUtils.getField(jwtService, "SECRET_KEY");
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // ==================== generateToken ====================

    @Test
    void shouldGenerateTokenWithUsernameAsSubject() {
        when(userRepository.findByUsername("budi")).thenReturn(createUser("budi", "user"));

        String token = jwtService.generateToken("budi");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("budi");
    }

    @Test
    void shouldIncludeAllUserRolesInToken() {
        when(userRepository.findByUsername("budi")).thenReturn(createUser("budi", "user", "admin"));

        String token = jwtService.generateToken("budi");

        Claims claims = Jwts.parser()
                .setSigningKey(secretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.get("roles", List.class)).containsExactlyInAnyOrder("user", "admin");
    }

    // ==================== extractUsername ====================

    @Test
    void shouldExtractUsernameFromValidToken() {
        when(userRepository.findByUsername("budi")).thenReturn(createUser("budi", "user"));
        String token = jwtService.generateToken("budi");

        assertThat(jwtService.extractUsername(token)).isEqualTo("budi");
    }

    // ==================== validateToken ====================

    @Test
    void shouldReturnTrueWhenTokenValidAndUsernameMatches() {
        when(userRepository.findByUsername("budi")).thenReturn(createUser("budi", "user"));
        String token = jwtService.generateToken("budi");

        when(userDetails.getUsername()).thenReturn("budi");

        assertThat(jwtService.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenUsernameDoesNotMatch() {
        when(userRepository.findByUsername("budi")).thenReturn(createUser("budi", "user"));
        String token = jwtService.generateToken("budi");

        UserDetails otherUser = mock(UserDetails.class);
        when(otherUser.getUsername()).thenReturn("siti");

        assertThat(jwtService.validateToken(token, otherUser)).isFalse();
    }

    @Test
    void shouldThrowExpiredJwtExceptionWhenTokenExpired() {
        Date past = new Date(System.currentTimeMillis() - 60_000);
        String expiredToken = Jwts.builder()
                .setSubject("budi")
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(past)
                .signWith(secretKey(), SignatureAlgorithm.HS256)
                .compact();

        // Parser langsung melempar exception sebelum getUsername() dipanggil
        assertThatThrownBy(() -> jwtService.validateToken(expiredToken, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldThrowExceptionWhenTokenSignedWithDifferentKey() {
        Key otherKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        String forgedToken = Jwts.builder()
                .setSubject("budi")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey, SignatureAlgorithm.HS256)
                .compact();

        // Parser menolak token karena signature tidak cocok
        assertThatThrownBy(() -> jwtService.validateToken(forgedToken, userDetails))
                .isInstanceOf(JwtException.class);
    }
}