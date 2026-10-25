package org.hospital.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpecializationRequest(@NotBlank @Size(max = 120) String name,
                                   @Size(max = 500) String description) {
}
