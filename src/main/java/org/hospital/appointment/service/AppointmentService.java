package org.hospital.appointment.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.hospital.appointment.domain.AppUser;
import org.hospital.appointment.domain.Appointment;
import org.hospital.appointment.domain.AppointmentHistory;
import org.hospital.appointment.domain.AppointmentStatus;
import org.hospital.appointment.domain.Patient;
import org.hospital.appointment.domain.SlotStatus;
import org.hospital.appointment.domain.TimeSlot;
import org.hospital.appointment.domain.UserRole;
import org.hospital.appointment.dto.AppointmentRequests;
import org.hospital.appointment.exception.ConflictException;
import org.hospital.appointment.exception.ForbiddenException;
import org.hospital.appointment.exception.NotFoundException;
import org.hospital.appointment.repository.AppointmentHistoryRepository;
import org.hospital.appointment.repository.AppointmentRepository;
import org.hospital.appointment.repository.TimeSlotRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private static final int MAX_RESCHEDULES = 3;
    private static final long CANCELLATION_NOTICE_HOURS = 2;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentHistoryRepository historyRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PatientService patientService;
    private final CurrentUserService currentUserService;
    private final AppointmentNotificationService notificationService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              AppointmentHistoryRepository historyRepository,
                              TimeSlotRepository timeSlotRepository, PatientService patientService,
                              CurrentUserService currentUserService,
                              AppointmentNotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.historyRepository = historyRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.patientService = patientService;
        this.currentUserService = currentUserService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AppointmentRequests.Response book(AppointmentRequests.Booking request) {
        AppUser actor = currentUserService.get();
        Patient patient = patientService.requireMineForUpdate();
        TimeSlot slot = timeSlotRepository.findByIdAndStatus(request.slotId(), SlotStatus.AVAILABLE)
                .orElseThrow(() -> new ConflictException("Time slot is no longer available"));
        ensureFuture(slot);
        if (appointmentRepository.hasOverlappingAppointment(patient.getId(), AppointmentStatus.BOOKED,
                slot.getStartTime(), slot.getEndTime(), null)) {
            throw new ConflictException("You already have an overlapping appointment");
        }

        slot.book();
        try {
            timeSlotRepository.saveAndFlush(slot);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ConflictException("Time slot was booked by another patient");
        }
        Appointment appointment = appointmentRepository.saveAndFlush(
                new Appointment(patient, slot.getDoctor(), slot));
        addHistory(appointment, null, AppointmentStatus.BOOKED, actor, "Appointment booked");
        notificationService.bookingConfirmation(appointment.getId());
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentRequests.Response cancel(UUID appointmentId) {
        AppUser actor = currentUserService.get();
        Appointment appointment = lockedAppointment(appointmentId);
        requirePatientOwner(appointment, actor);
        patientService.lockForUpdate(appointment.getPatient().getId());
        ensureBooked(appointment);
        if (ChronoUnit.MINUTES.between(LocalDateTime.now(), appointment.getTimeSlot().getStartTime())
                < CANCELLATION_NOTICE_HOURS * 60) {
            throw new ConflictException("Appointments can only be cancelled at least 2 hours in advance");
        }
        appointment.cancel();
        appointment.getTimeSlot().release();
        timeSlotRepository.saveAndFlush(appointment.getTimeSlot());
        appointmentRepository.saveAndFlush(appointment);
        addHistory(appointment, AppointmentStatus.BOOKED, AppointmentStatus.CANCELLED, actor, "Appointment cancelled");
        notificationService.cancellation(appointment.getId());
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentRequests.Response reschedule(UUID appointmentId, AppointmentRequests.Reschedule request) {
        AppUser actor = currentUserService.get();
        Appointment appointment = lockedAppointment(appointmentId);
        requirePatientOwner(appointment, actor);
        patientService.lockForUpdate(appointment.getPatient().getId());
        ensureBooked(appointment);
        if (appointment.getRescheduleCount() >= MAX_RESCHEDULES) {
            throw new ConflictException("Appointment has reached the limit of 3 reschedules");
        }
        if (ChronoUnit.MINUTES.between(LocalDateTime.now(), appointment.getTimeSlot().getStartTime())
                < CANCELLATION_NOTICE_HOURS * 60) {
            throw new ConflictException("Appointments can only be rescheduled at least 2 hours in advance");
        }

        TimeSlot newSlot = timeSlotRepository.findByIdAndStatus(request.newSlotId(), SlotStatus.AVAILABLE)
                .orElseThrow(() -> new ConflictException("New time slot is no longer available"));
        ensureFuture(newSlot);
        if (appointmentRepository.hasOverlappingAppointment(appointment.getPatient().getId(),
                AppointmentStatus.BOOKED, newSlot.getStartTime(), newSlot.getEndTime(), appointment.getId())) {
            throw new ConflictException("You already have an overlapping appointment");
        }
        TimeSlot previousSlot = appointment.getTimeSlot();
        newSlot.book();
        try {
            timeSlotRepository.saveAndFlush(newSlot);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ConflictException("New time slot was booked by another patient");
        }
        previousSlot.release();
        timeSlotRepository.saveAndFlush(previousSlot);
        appointment.reschedule(newSlot);
        appointmentRepository.saveAndFlush(appointment);
        addHistory(appointment, AppointmentStatus.BOOKED, AppointmentStatus.BOOKED, actor,
                "Appointment rescheduled from " + previousSlot.getStartTime() + " to " + newSlot.getStartTime());
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentRequests.Response changeStatus(UUID appointmentId, AppointmentRequests.StatusChange request) {
        AppUser actor = currentUserService.get();
        if (request.status() != AppointmentStatus.COMPLETED && request.status() != AppointmentStatus.NO_SHOW) {
            throw new ConflictException("Doctors and admins may set appointments to COMPLETED or NO_SHOW only");
        }
        Appointment appointment = lockedAppointment(appointmentId);
        if ((actor.getRole() == UserRole.DOCTOR
                && !appointment.getDoctor().getUser().getId().equals(actor.getId()))
                || (actor.getRole() != UserRole.DOCTOR && actor.getRole() != UserRole.ADMIN)) {
            throw new ForbiddenException("You may only update your own appointment schedule");
        }
        ensureBooked(appointment);
        AppointmentStatus previous = appointment.getStatus();
        appointment.changeStatus(request.status());
        appointmentRepository.saveAndFlush(appointment);
        addHistory(appointment, previous, request.status(), actor, request.note());
        return toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public List<AppointmentRequests.Response> mine() {
        AppUser actor = currentUserService.get();
        if (actor.getRole() == UserRole.PATIENT) {
            return appointmentRepository.findByPatient_User_IdOrderByCreatedAtDesc(actor.getId())
                    .stream().map(this::toResponse).toList();
        }
        if (actor.getRole() == UserRole.DOCTOR) {
            return appointmentRepository.findByDoctor_User_IdOrderByCreatedAtDesc(actor.getId())
                    .stream().map(this::toResponse).toList();
        }
        throw new ForbiddenException("Use the admin reports API for administrative access");
    }

    @Transactional(readOnly = true)
    public List<AppointmentRequests.History> history(UUID appointmentId) {
        AppUser actor = currentUserService.get();
        Appointment appointment = appointmentRepository.findDetailedById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment was not found"));
        boolean patientOwner = appointment.getPatient().getUser().getId().equals(actor.getId());
        boolean doctorOwner = appointment.getDoctor().getUser().getId().equals(actor.getId());
        if (!patientOwner && !doctorOwner && actor.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("You may not view this appointment history");
        }
        return historyRepository.findByAppointment_IdOrderByChangedAt(appointmentId).stream()
                .map(entry -> new AppointmentRequests.History(entry.getId(), entry.getFromStatus(),
                        entry.getToStatus(), entry.getActor().getEmail(), entry.getNote(), entry.getChangedAt()))
                .toList();
    }

    private Appointment lockedAppointment(UUID id) {
        return appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Appointment was not found"));
    }

    private void requirePatientOwner(Appointment appointment, AppUser actor) {
        if (actor.getRole() != UserRole.PATIENT
                || !appointment.getPatient().getUser().getId().equals(actor.getId())) {
            throw new ForbiddenException("You may only manage your own appointments");
        }
    }

    private void ensureBooked(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new ConflictException("Only booked appointments can be changed");
        }
    }

    private void ensureFuture(TimeSlot slot) {
        if (!slot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Appointments must be scheduled in the future");
        }
    }

    private void addHistory(Appointment appointment, AppointmentStatus from, AppointmentStatus to,
                            AppUser actor, String note) {
        historyRepository.save(new AppointmentHistory(appointment, from, to, actor, note));
    }

    private AppointmentRequests.Response toResponse(Appointment appointment) {
        return new AppointmentRequests.Response(appointment.getId(), appointment.getPatient().getId(),
                appointment.getPatient().getFullName(), appointment.getDoctor().getId(),
                appointment.getDoctor().getFullName(), appointment.getTimeSlot().getId(),
                appointment.getTimeSlot().getStartTime(), appointment.getTimeSlot().getEndTime(),
                appointment.getStatus(), appointment.getRescheduleCount());
    }
}
