package org.hospital.appointment.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.hospital.appointment.domain.Appointment;
import org.hospital.appointment.domain.Notification;
import org.hospital.appointment.domain.NotificationStatus;
import org.hospital.appointment.domain.NotificationType;
import org.hospital.appointment.repository.AppointmentRepository;
import org.hospital.appointment.repository.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailAppointmentNotificationService implements AppointmentNotificationService {

    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EmailAppointmentNotificationService(AppointmentRepository appointmentRepository,
                                               NotificationRepository notificationRepository,
                                               ApplicationEventPublisher eventPublisher) {
        this.appointmentRepository = appointmentRepository;
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void bookingConfirmation(UUID appointmentId) {
        enqueue(appointmentId, NotificationType.BOOKING_CONFIRMATION, null);
    }

    @Override
    @Transactional
    public void cancellation(UUID appointmentId) {
        enqueue(appointmentId, NotificationType.CANCELLATION, null);
    }

    @Override
    @Transactional
    public void reminder(UUID appointmentId, Instant scheduledAt) {
        enqueue(appointmentId, NotificationType.APPOINTMENT_REMINDER, scheduledAt);
    }

    private void enqueue(UUID appointmentId, NotificationType type, Instant scheduledAt) {
        Notification existing = notificationRepository
                .findByAppointment_IdAndNotificationType(appointmentId, type).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == NotificationStatus.SENT) {
                return;
            }
            eventPublisher.publishEvent(new NotificationRequested(existing.getId()));
            return;
        }

        Appointment appointment = appointmentRepository.findDetailedById(appointmentId)
                .orElseThrow(() -> new IllegalStateException("Appointment for notification was not found"));
        Notification notification = new Notification(appointment,
                appointment.getPatient().getUser().getEmail(), type);
        if (scheduledAt != null) {
            notification.scheduleFor(scheduledAt);
        }
        Notification saved = notificationRepository.save(notification);
        eventPublisher.publishEvent(new NotificationRequested(saved.getId()));
    }
}
