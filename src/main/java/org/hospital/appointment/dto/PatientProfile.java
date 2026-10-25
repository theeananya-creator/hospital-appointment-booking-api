package org.hospital.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public final class PatientProfile {
    private PatientProfile() {
    }

    public record Update(@NotBlank @Size(max = 160) String fullName,
                         @Size(max = 30) String phone,
                         @Past LocalDate dateOfBirth) {
    }

    public record Response(UUID id, String fullName, String email, String phone, LocalDate dateOfBirth) {
    }
}
