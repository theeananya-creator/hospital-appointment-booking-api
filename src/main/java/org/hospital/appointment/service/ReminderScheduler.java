package org.hospital.appointment.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.hospital.appointment.domain.AppointmentStatus;
import org.hospital.appointment.repository.AppointmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentNotificationService notificationService;

    public ReminderScheduler(AppointmentRepository appointmentRepository,
                             AppointmentNotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${app.notifications.reminder-interval-ms:900000}")
    public void enqueueUpcomingReminders() {
        LocalDateTime from = LocalDateTime.now().plusHours(24);
        LocalDateTime until = from.plusMinutes(15);
        Instant scheduledAt = from.atZone(ZoneId.systemDefault()).toInstant();
        appointmentRepository.findAppointmentsForReminder(AppointmentStatus.BOOKED, from, until)
                .forEach(appointment -> notificationService.reminder(appointment.getId(), scheduledAt));
    }
}
