package com.example.inventory.security;

import com.example.inventory.auth.Role;
import com.example.inventory.auth.User;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final SecretKey key = new SecretKeySpec(
            "test-secret-test-secret-test-secret-32".getBytes(StandardCharsets.UTF_8),
            "HmacSHA256");

    private final JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256).build();

    private final JwtService jwtService = new JwtService(encoder, 15);

    private User testUser() {
        Role admin = new Role();
        admin.setName("ADMIN");

        User u = new User();
        ReflectionTestUtils.setField(u, "id", 1L);   // id has no setter
        u.setEmail("test@example.com");
        u.getRoles().add(admin);
        return u;
    }

    @Test
    void accessTokenHasExpectedClaims() {
        String token = jwtService.issueAccessToken(testUser());
        System.out.println("TOKEN: " + token);

        Jwt jwt = decoder.decode(token);   // throws if signature or expiry is bad

        assertEquals("test@example.com", jwt.getSubject());
        assertEquals(List.of("ADMIN"), jwt.getClaimAsStringList("roles"));
        assertEquals(1L, ((Number) jwt.getClaim("uid")).longValue());
        assertNotNull(jwt.getId());          // jti
        assertNotNull(jwt.getExpiresAt());   // exp
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.issueAccessToken(testUser());
        String tampered = token.substring(0, token.length() - 2) + "xx";  // damage the signature

        assertThrows(JwtException.class, () -> decoder.decode(tampered));
    }

    @Test
    void refreshTokenHashMatchesRawToken() {
        JwtService.NewRefreshToken t = jwtService.mintRefreshToken();

        assertEquals(43, t.raw().length());   // 32 bytes in Base64URL, no padding
        assertEquals(64, t.hash().length());  // SHA-256 as hex
        assertEquals(t.hash(), jwtService.sha256Hex(t.raw()));
        assertNotEquals(t.raw(), jwtService.mintRefreshToken().raw());  // random each time
    }

    @Test
    void rolesClaimMapsToSinglePrefixedAuthority() {
        // DB stores ROLE_* already; converter must not add a second ROLE_
        var converter = new com.example.inventory.config.SecurityConfig().jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256")
                .subject("admin@local")
                .claim("roles", List.of("ROLE_ADMIN"))
                .build();

        var auth = converter.convert(jwt);
        var authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        assertTrue(authorities.contains("ROLE_ADMIN"));
        assertFalse(authorities.contains("ROLE_ROLE_ADMIN"));  // no double prefix
    }
}