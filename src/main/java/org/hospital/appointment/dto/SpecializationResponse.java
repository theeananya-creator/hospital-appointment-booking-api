package org.hospital.appointment.dto;

import java.util.UUID;

public record SpecializationResponse(UUID id, String name, String description) {
}
