package org.hospital.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hospital.appointment.domain.AppUser;
import org.hospital.appointment.domain.Patient;
import org.hospital.appointment.domain.RefreshToken;
import org.hospital.appointment.domain.UserRole;
import org.hospital.appointment.dto.AuthRequests;
import org.hospital.appointment.repository.AppUserRepository;
import org.hospital.appointment.repository.PatientRepository;
import org.hospital.appointment.repository.RefreshTokenRepository;
import org.hospital.appointment.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("unit-test-secret-with-at-least-32-bytes", 15) {
            @Override
            public String createAccessToken(AppUser user) {
                return "signed-access-token";
            }

            @Override
            public long getAccessTokenSeconds() {
                return 900L;
            }
        };
        authService = new AuthService(userRepository, patientRepository, refreshTokenRepository,
                passwordEncoder, jwtService, 30);
        when(userRepository.existsByEmailIgnoreCase("patient@example.com")).thenReturn(false);
        when(passwordEncoder.encode("very-secure-password")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void registrationCreatesPatientAccountAndIssuesRefreshToken() {
        AuthRequests.Register request = new AuthRequests.Register("Patient@Example.com",
                "very-secure-password", "Patient One", null, null);

        AuthRequests.TokenResponse response = authService.register(request);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("patient@example.com", userCaptor.getValue().getEmail());
        assertEquals(UserRole.PATIENT, userCaptor.getValue().getRole());
        verify(patientRepository).save(any(Patient.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        assertEquals("signed-access-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertNotNull(response.refreshToken());
        assertEquals(900L, response.expiresInSeconds());
    }
}
