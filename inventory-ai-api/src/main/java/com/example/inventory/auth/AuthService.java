package com.example.inventory.auth;

import com.example.inventory.auth.dto.LoginRequest;
import com.example.inventory.auth.dto.RefreshRequest;
import com.example.inventory.auth.dto.RegisterRequest;
import com.example.inventory.auth.dto.TokenResponse;
import com.example.inventory.auth.dto.UserResponse;
import com.example.inventory.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long refreshTtlDays;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       @Value("${app.jwt.refresh-ttl-days}") long refreshTtlDays) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTtlDays = refreshTtlDays;
    }

    // ---------- 1. REGISTER ----------
    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Role userRole = roleRepository.findByName("ROLE_USER") // roles are seeded with the ROLE_ prefix
                .orElseThrow(() -> new IllegalStateException("Role USER missing in roles table"));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.getRoles().add(userRole);
        userRepository.save(user);

        return new UserResponse(user.getId(), user.getEmail());
    }

    // ---------- 2. LOGIN ----------
    @Transactional
    public TokenResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, req.password()));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = userRepository.findByEmail(email).orElseThrow();
        return issueTokens(user);
    }

    // ---------- 3. REFRESH ----------
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TokenResponse refresh(RefreshRequest req) {
        String hash = jwtService.sha256Hex(req.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(this::invalidRefresh);

        // Reuse detection: this token was already used or revoked
        if (stored.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(stored.getUserId());
            throw invalidRefresh();
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw invalidRefresh();
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(this::invalidRefresh);
        if (!user.isEnabled()) {
            throw invalidRefresh();
        }

        stored.setRevoked(true);      // rotation: old token dies here
        return issueTokens(user);     // new access token + new refresh token
    }

    // ---------- 4. LOGOUT ----------
    @Transactional
    public void logout(RefreshRequest req) {
        String hash = jwtService.sha256Hex(req.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> t.setRevoked(true));
    }

    // ---------- helpers (private) ----------
    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user);

        JwtService.NewRefreshToken refresh = jwtService.mintRefreshToken();
        refreshTokenRepository.save(new RefreshToken(
                refresh.hash(),
                user.getId(),
                Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS)));

        return new TokenResponse(accessToken, refresh.raw(), "Bearer");
    }

    private ResponseStatusException invalidRefresh() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
    }
}