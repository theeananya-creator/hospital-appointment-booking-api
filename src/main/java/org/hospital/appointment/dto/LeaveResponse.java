package org.hospital.appointment.dto;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveResponse(UUID id, LocalDate startDate, LocalDate endDate, String reason) {
}
