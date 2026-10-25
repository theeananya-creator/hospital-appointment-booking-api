package org.hospital.appointment.service;

import java.time.Instant;
import java.util.UUID;

public interface AppointmentNotificationService {
    void bookingConfirmation(UUID appointmentId);
    void cancellation(UUID appointmentId);
    void reminder(UUID appointmentId, Instant scheduledAt);
}
