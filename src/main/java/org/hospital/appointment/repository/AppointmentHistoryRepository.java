package org.hospital.appointment.repository;

import java.util.List;
import java.util.UUID;
import org.hospital.appointment.domain.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, UUID> {
    List<AppointmentHistory> findByAppointment_IdOrderByChangedAt(UUID appointmentId);
}
