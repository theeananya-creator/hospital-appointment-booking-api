package org.hospital.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "recipient_email", nullable = false, length = 254)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    protected Notification() {
    }

    public Notification(Appointment appointment, String recipientEmail, NotificationType type) {
        this.appointment = appointment;
        this.recipientEmail = recipientEmail;
        this.notificationType = type;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void recordAttempt(String error) {
        attempts++;
        lastError = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
        if (error == null) {
            status = NotificationStatus.SENT;
            sentAt = Instant.now();
        }
    }

    public void markFailed(String error) {
        status = NotificationStatus.FAILED;
        lastError = error == null ? "Delivery failed" : error.substring(0, Math.min(error.length(), 1000));
    }

    public void scheduleFor(Instant time) {
        scheduledAt = time.truncatedTo(ChronoUnit.MICROS);
    }

    public int getAttempts() {
        return attempts;
    }

    public String getLastError() {
        return lastError;
    }
}
