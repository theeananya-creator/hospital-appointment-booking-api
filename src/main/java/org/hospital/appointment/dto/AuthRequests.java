package org.hospital.appointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Past;

public final class AuthRequests {
    private AuthRequests() {
    }

    public record Register(@NotBlank @Email @Size(max = 254) String email,
                           @NotBlank @Size(min = 12, max = 72) String password,
                           @NotBlank @Size(max = 160) String fullName,
                           @Size(max = 30) String phone,
                           @Past java.time.LocalDate dateOfBirth) {
    }

    public record Login(@NotBlank @Email @Size(max = 254) String email, @NotBlank String password) {
    }

    public record Refresh(@NotBlank String refreshToken) {
    }

    public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                                long expiresInSeconds) {
    }
}
