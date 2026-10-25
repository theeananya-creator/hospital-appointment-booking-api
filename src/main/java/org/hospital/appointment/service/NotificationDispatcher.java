package org.hospital.appointment.service;

import java.time.format.DateTimeFormatter;
import org.hospital.appointment.domain.Appointment;
import org.hospital.appointment.domain.Notification;
import org.hospital.appointment.domain.NotificationStatus;
import org.hospital.appointment.domain.NotificationType;
import org.hospital.appointment.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationDispatcher {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final NotificationRepository notificationRepository;
    private final NotificationGateway gateway;
    private final int retryAttempts;
    private final long retryDelayMs;

    public NotificationDispatcher(NotificationRepository notificationRepository, NotificationGateway gateway,
                                  @Value("${app.notifications.retry-attempts:3}") int retryAttempts,
                                  @Value("${app.notifications.retry-delay-ms:1000}") long retryDelayMs) {
        this.notificationRepository = notificationRepository;
        this.gateway = gateway;
        this.retryAttempts = Math.max(1, retryAttempts);
        this.retryDelayMs = Math.max(0, retryDelayMs);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(NotificationRequested event) {
        Notification notification = notificationRepository.findById(event.notificationId()).orElse(null);
        if (notification == null || notification.getStatus() == NotificationStatus.SENT) {
            return;
        }
        for (int attempt = 0; attempt < retryAttempts; attempt++) {
            try {
                gateway.send(notification.getRecipientEmail(), subject(notification.getNotificationType()),
                        body(notification.getAppointment(), notification.getNotificationType()));
                notification.recordAttempt(null);
                return;
            } catch (MailException exception) {
                notification.recordAttempt(exception.getMessage());
                if (attempt + 1 < retryAttempts && retryDelayMs > 0) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        notification.markFailed("Notification retry was interrupted");
                        return;
                    }
                }
            }
        }
        notification.markFailed(notification.getLastError());
    }

    private String subject(NotificationType type) {
        return switch (type) {
            case BOOKING_CONFIRMATION -> "Appointment booking confirmation";
            case CANCELLATION -> "Appointment cancellation";
            case APPOINTMENT_REMINDER -> "Appointment reminder";
        };
    }

    private String body(Appointment appointment, NotificationType type) {
        String date = DATE_FORMAT.format(appointment.getTimeSlot().getStartTime());
        String message = switch (type) {
            case BOOKING_CONFIRMATION -> "Your appointment is booked.";
            case CANCELLATION -> "Your appointment has been cancelled.";
            case APPOINTMENT_REMINDER -> "This is a reminder for your appointment.";
        };
        return message + "\nDoctor: " + appointment.getDoctor().getFullName() + "\nWhen: " + date;
    }
}
