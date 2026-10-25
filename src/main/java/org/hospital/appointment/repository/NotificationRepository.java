package org.hospital.appointment.repository;

import java.util.Optional;
import java.util.UUID;
import org.hospital.appointment.domain.Notification;
import org.hospital.appointment.domain.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Optional<Notification> findByAppointment_IdAndNotificationType(UUID appointmentId, NotificationType type);
}
