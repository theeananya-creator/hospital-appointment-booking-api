package org.hospital.appointment.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotResponse(UUID id, UUID doctorId, LocalDateTime startTime, LocalDateTime endTime) {
}
