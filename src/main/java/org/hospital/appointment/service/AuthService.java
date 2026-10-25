package org.hospital.appointment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import org.hospital.appointment.domain.AppUser;
import org.hospital.appointment.domain.Patient;
import org.hospital.appointment.domain.RefreshToken;
import org.hospital.appointment.domain.UserRole;
import org.hospital.appointment.dto.AuthRequests;
import org.hospital.appointment.exception.ConflictException;
import org.hospital.appointment.exception.UnauthorizedException;
import org.hospital.appointment.repository.AppUserRepository;
import org.hospital.appointment.repository.PatientRepository;
import org.hospital.appointment.repository.RefreshTokenRepository;
import org.hospital.appointment.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PatientRepository patientRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppUserRepository userRepository, PatientRepository patientRepository,
                       RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.jwt.refresh-token-days:30}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public AuthRequests.TokenResponse register(AuthRequests.Register request) {
        String email = normalizeEmail(request.email());
        ensureBcryptLength(request.password());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        AppUser user = userRepository.save(new AppUser(email,
                passwordEncoder.encode(request.password()), UserRole.PATIENT));
        patientRepository.save(new Patient(user, request.fullName().trim(), request.phone(), request.dateOfBirth()));
        return issueTokens(user);
    }

    @Transactional
    public AuthRequests.TokenResponse login(AuthRequests.Login request) {
        ensureBcryptLength(request.password());
        AppUser user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthRequests.TokenResponse refresh(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired"));
        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(Instant.now())
                || !current.getUser().isEnabled()) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
        AppUser user = current.getUser();
        String nextRawToken = newRefreshToken();
        RefreshToken next = refreshTokenRepository.save(new RefreshToken(user, hash(nextRawToken),
                Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS)));
        current.revoke(next.getId());
        return new AuthRequests.TokenResponse(jwtService.createAccessToken(user), nextRawToken, "Bearer",
                jwtService.getAccessTokenSeconds());
    }

    private AuthRequests.TokenResponse issueTokens(AppUser user) {
        String refreshToken = newRefreshToken();
        refreshTokenRepository.save(new RefreshToken(user, hash(refreshToken),
                Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS)));
        return new AuthRequests.TokenResponse(jwtService.createAccessToken(user), refreshToken, "Bearer",
                jwtService.getAccessTokenSeconds());
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private void ensureBcryptLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new org.hospital.appointment.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes");
        }
    }
}
