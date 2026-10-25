package org.hospital.appointment.repository;

import java.time.LocalDate;
import java.util.UUID;
import org.hospital.appointment.domain.DoctorLeave;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorLeaveRepository extends JpaRepository<DoctorLeave, UUID> {
    boolean existsByDoctor_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID doctorId, LocalDate endDate, LocalDate startDate);
}
