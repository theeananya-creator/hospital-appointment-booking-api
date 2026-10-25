package org.hospital.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.hospital.appointment.domain.AppointmentStatus;

public final class AppointmentRequests {
    private AppointmentRequests() {
    }

    public record Booking(@NotNull UUID slotId) {
    }

    public record Reschedule(@NotNull UUID newSlotId) {
    }

    public record StatusChange(@NotNull AppointmentStatus status,
                               @Size(max = 500) String note) {
    }

    public record Response(UUID id, UUID patientId, String patientName, UUID doctorId, String doctorName,
                           UUID slotId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime,
                           AppointmentStatus status, int rescheduleCount) {
    }

    public record History(UUID id, String fromStatus, String toStatus, String actorEmail,
                          String note, java.time.Instant changedAt) {
    }
}
