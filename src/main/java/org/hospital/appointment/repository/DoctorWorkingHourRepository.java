package org.hospital.appointment.repository;

import java.util.List;
import java.util.UUID;
import org.hospital.appointment.domain.DoctorWorkingHour;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorWorkingHourRepository extends JpaRepository<DoctorWorkingHour, UUID> {
    List<DoctorWorkingHour> findByDoctor_Id(UUID doctorId);
    void deleteByDoctor_Id(UUID doctorId);
}
