package org.hospital.appointment.repository;

import java.util.Optional;
import java.util.UUID;
import org.hospital.appointment.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DoctorRepository extends JpaRepository<Doctor, UUID>, JpaSpecificationExecutor<Doctor> {
    Optional<Doctor> findByUser_Id(UUID userId);
}
