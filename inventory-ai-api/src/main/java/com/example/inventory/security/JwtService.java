package com.example.inventory.security;

import com.example.inventory.auth.Role;
import com.example.inventory.auth.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class JwtService {

    private static final SecureRandom random = new SecureRandom();

    private final JwtEncoder encoder;
    private final long accessTtlMinutes;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.access-ttl-minutes}") long accessTtlMinutes){
        this.encoder = encoder;
        this.accessTtlMinutes = accessTtlMinutes;
    }

    /** Creates the signed access token (the JWT). */
    public String issueAccessToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(accessTtlMinutes, ChronoUnit.MINUTES))
                .id(UUID.randomUUID().toString())
                .claim("uid", user.getId())
                .claim("roles", user.getRoles().stream().map(Role::getName).toList())
                .build();

        // Must say HS256 explicitly, or the encoder defaults to RS256 and fails
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header,claims)).getTokenValue();
    }

    /** Creates a random refresh token. Send `raw` to the client, save `hash` in the DB. */
    public NewRefreshToken mintRefreshToken(){
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new NewRefreshToken(raw,sha256Hex(raw));
    }

    /** Same input always gives the same hash, so you can look up the DB row by it. */
    public String sha256Hex(String value){
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e){
            throw new IllegalStateException(e);
        }
    }

    public record NewRefreshToken(String raw, String hash){}


}
