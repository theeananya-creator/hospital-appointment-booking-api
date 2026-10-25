package org.hospital.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.hospital.appointment.domain.AppUser;
import org.hospital.appointment.domain.Appointment;
import org.hospital.appointment.domain.Doctor;
import org.hospital.appointment.domain.Notification;
import org.hospital.appointment.domain.NotificationStatus;
import org.hospital.appointment.domain.NotificationType;
import org.hospital.appointment.domain.Patient;
import org.hospital.appointment.domain.Specialization;
import org.hospital.appointment.domain.TimeSlot;
import org.hospital.appointment.domain.UserRole;
import org.hospital.appointment.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationGateway gateway;

    @Test
    void retriesMailFailureAndPersistsSuccessfulDeliveryState() {
        Specialization specialization = new Specialization("Test", null);
        Doctor doctor = new Doctor(new AppUser("doctor@example.com", "hash", UserRole.DOCTOR),
                specialization, "Doctor", 4, new BigDecimal("50.00"), null);
        Patient patient = new Patient(new AppUser("patient@example.com", "hash", UserRole.PATIENT),
                "Patient", null, null);
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        Appointment appointment = new Appointment(patient, doctor,
                new TimeSlot(doctor, start, start.plusMinutes(30)));
        Notification notification = new Notification(appointment, "patient@example.com",
                NotificationType.BOOKING_CONFIRMATION);
        when(notificationRepository.findById(any(UUID.class))).thenReturn(Optional.of(notification));
        doThrow(new MailSendException("temporary smtp failure"))
                .doNothing().when(gateway).send(anyString(), anyString(), anyString());

        NotificationDispatcher dispatcher = new NotificationDispatcher(notificationRepository, gateway, 3, 0);
        dispatcher.dispatch(new NotificationRequested(UUID.randomUUID()));

        assertEquals(NotificationStatus.SENT, notification.getStatus());
        assertEquals(2, notification.getAttempts());
        verify(gateway, org.mockito.Mockito.times(2)).send(anyString(), anyString(), anyString());
    }
}
