package org.hospital.appointment.repository;

import java.util.Optional;
import java.util.UUID;
import org.hospital.appointment.domain.Patient;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByUser_Id(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.user.id = :userId")
    Optional<Patient> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.id = :patientId")
    Optional<Patient> findByIdForUpdate(@Param("patientId") UUID patientId);
}
