package org.hospital.appointment.repository;

import java.util.UUID;
import org.hospital.appointment.domain.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecializationRepository extends JpaRepository<Specialization, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
