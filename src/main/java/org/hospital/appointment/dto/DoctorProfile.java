package org.hospital.appointment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public final class DoctorProfile {
    private DoctorProfile() {
    }

    public record Create(@Email @NotBlank @Size(max = 254) String email,
                         @NotBlank @Size(min = 12, max = 72) String password,
                         @NotBlank @Size(max = 160) String fullName,
                         @NotNull UUID specializationId,
                         @Min(0) int experienceYears,
                         @NotNull @DecimalMin("0.00") BigDecimal consultationFee,
                         @Size(max = 2000) String bio) {
    }

    public record Update(@NotBlank @Size(max = 160) String fullName,
                        @NotNull UUID specializationId,
                        @Min(0) int experienceYears,
                        @NotNull @DecimalMin("0.00") BigDecimal consultationFee,
                        @Size(max = 2000) String bio,
                        Boolean available) {
    }

    public record Response(UUID id, String fullName, String specialization,
                           int experienceYears, BigDecimal consultationFee, String bio, boolean available) {
    }

    public record WorkingHourRequest(@NotNull java.time.DayOfWeek dayOfWeek,
                                     @NotNull LocalTime startTime,
                                     @NotNull LocalTime endTime,
                                     @Min(5) int slotDurationMinutes) {
    }

    public record LeaveRequest(@NotNull LocalDate startDate, @NotNull LocalDate endDate,
                               @Size(max = 500) String reason) {
    }
}
