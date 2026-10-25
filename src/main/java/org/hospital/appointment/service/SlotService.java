package org.hospital.appointment.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.hospital.appointment.domain.Doctor;
import org.hospital.appointment.domain.DoctorLeave;
import org.hospital.appointment.domain.DoctorWorkingHour;
import org.hospital.appointment.domain.SlotStatus;
import org.hospital.appointment.domain.TimeSlot;
import org.hospital.appointment.dto.DoctorProfile;
import org.hospital.appointment.dto.LeaveResponse;
import org.hospital.appointment.dto.SlotResponse;
import org.hospital.appointment.exception.ConflictException;
import org.hospital.appointment.exception.NotFoundException;
import org.hospital.appointment.repository.DoctorLeaveRepository;
import org.hospital.appointment.repository.DoctorRepository;
import org.hospital.appointment.repository.DoctorWorkingHourRepository;
import org.hospital.appointment.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlotService {

    private final DoctorRepository doctorRepository;
    private final DoctorWorkingHourRepository workingHourRepository;
    private final DoctorLeaveRepository leaveRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final CurrentUserService currentUserService;
    private final int generationDays;

    public SlotService(DoctorRepository doctorRepository, DoctorWorkingHourRepository workingHourRepository,
                       DoctorLeaveRepository leaveRepository, TimeSlotRepository timeSlotRepository,
                       CurrentUserService currentUserService,
                       @Value("${app.slots.generation-days:30}") int generationDays) {
        this.doctorRepository = doctorRepository;
        this.workingHourRepository = workingHourRepository;
        this.leaveRepository = leaveRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.currentUserService = currentUserService;
        this.generationDays = generationDays;
    }

    @Transactional
    public void setWorkingHours(List<DoctorProfile.WorkingHourRequest> requests) {
        Doctor doctor = doctorRepository.findByUser_Id(currentUserService.get().getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile was not found"));
        validateWorkingHours(requests);
        LocalDate today = LocalDate.now();
        timeSlotRepository.closeAvailableSlots(doctor.getId(), SlotStatus.AVAILABLE, SlotStatus.CLOSED,
                LocalDateTime.now(), today.plusDays(generationDays).atStartOfDay());
        workingHourRepository.deleteByDoctor_Id(doctor.getId());
        List<DoctorWorkingHour> hours = requests.stream()
                .map(request -> new DoctorWorkingHour(doctor, request.dayOfWeek(), request.startTime(),
                        request.endTime(), request.slotDurationMinutes()))
                .toList();
        workingHourRepository.saveAll(hours);
        generateForDoctor(doctor, LocalDate.now(), generationDays);
    }

    @Transactional
    public LeaveResponse addLeave(DoctorProfile.LeaveRequest request) {
        Doctor doctor = doctorRepository.findByUser_Id(currentUserService.get().getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile was not found"));
        if (request.endDate().isBefore(request.startDate()) || request.startDate().isBefore(LocalDate.now())) {
            throw new ConflictException("Leave dates must be today or later and end on or after the start date");
        }
        LocalDateTime start = request.startDate().atStartOfDay();
        LocalDateTime end = request.endDate().plusDays(1).atStartOfDay();
        if (timeSlotRepository.existsByDoctor_IdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                doctor.getId(), SlotStatus.BOOKED, end, start)) {
            throw new ConflictException("Leave dates overlap an existing appointment");
        }
        DoctorLeave leave = leaveRepository.save(new DoctorLeave(doctor, request.startDate(),
                request.endDate(), request.reason()));
        timeSlotRepository.closeAvailableSlots(doctor.getId(), SlotStatus.AVAILABLE, SlotStatus.CLOSED, start, end);
        return new LeaveResponse(leave.getId(), leave.getStartDate(), leave.getEndDate(), request.reason());
    }

    @Transactional
    public List<SlotResponse> generateForDoctor(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new NotFoundException("Doctor was not found"));
        if (!doctor.isAvailable()) {
            throw new ConflictException("Doctor is not currently accepting appointments");
        }
        generateForDoctor(doctor, LocalDate.now(), generationDays);
        return availableSlots(doctorId, LocalDate.now(), LocalDate.now().plusDays(generationDays));
    }

    @Transactional
    public void synchronizeAvailability(Doctor doctor) {
        LocalDate today = LocalDate.now();
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime until = today.plusDays(generationDays).atStartOfDay();
        if (!doctor.isAvailable()) {
            timeSlotRepository.closeAvailableSlots(doctor.getId(), SlotStatus.AVAILABLE,
                    SlotStatus.CLOSED, from, until);
        } else {
            generateForDoctor(doctor, today, generationDays);
        }
    }

    @Transactional(readOnly = true)
    public List<SlotResponse> availableSlots(UUID doctorId, LocalDate date) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new NotFoundException("Doctor was not found");
        }
        return availableSlots(doctorId, date, date.plusDays(1));
    }

    @Transactional
    public void generateForAllDoctors() {
        LocalDate start = LocalDate.now();
        for (Doctor doctor : doctorRepository.findAll()) {
            if (doctor.isAvailable()) {
                generateForDoctor(doctor, start, generationDays);
            }
        }
    }

    private void generateForDoctor(Doctor doctor, LocalDate from, int days) {
        if (!doctor.isAvailable()) {
            return;
        }
        List<DoctorWorkingHour> hours = workingHourRepository.findByDoctor_Id(doctor.getId());
        LocalDate today = LocalDate.now();
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = from.plusDays(offset);
            if (leaveRepository.existsByDoctor_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    doctor.getId(), date, date)) {
                continue;
            }
            for (DoctorWorkingHour hour : hours) {
                if (hour.getDayOfWeek() != date.getDayOfWeek().getValue()) {
                    continue;
                }
                LocalTime slotStart = hour.getStartTime();
                while (!slotStart.plusMinutes(hour.getSlotDurationMinutes()).isAfter(hour.getEndTime())) {
                    LocalDateTime startTime = LocalDateTime.of(date, slotStart);
                    LocalDateTime endTime = startTime.plusMinutes(hour.getSlotDurationMinutes());
                    if (date.isAfter(today) || startTime.isAfter(LocalDateTime.now())) {
                        timeSlotRepository.insertIfAbsent(UUID.randomUUID().toString(),
                                doctor.getId().toString(), startTime, endTime);
                    }
                    slotStart = slotStart.plusMinutes(hour.getSlotDurationMinutes());
                }
            }
        }
    }

    private List<SlotResponse> availableSlots(UUID doctorId, LocalDate from, LocalDate until) {
        return timeSlotRepository.findByDoctor_IdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
                        doctorId, SlotStatus.AVAILABLE, from.atStartOfDay(), until.atStartOfDay())
                .stream().map(slot -> new SlotResponse(slot.getId(), doctorId,
                        slot.getStartTime(), slot.getEndTime())).toList();
    }

    private void validateWorkingHours(List<DoctorProfile.WorkingHourRequest> requests) {
        List<DoctorProfile.WorkingHourRequest> sorted = new ArrayList<>(requests);
        sorted.sort(Comparator.comparing(DoctorProfile.WorkingHourRequest::dayOfWeek)
                .thenComparing(DoctorProfile.WorkingHourRequest::startTime));
        for (int i = 0; i < sorted.size(); i++) {
            var current = sorted.get(i);
            if (!current.endTime().isAfter(current.startTime())) {
                throw new ConflictException("Working-hour end time must be after start time");
            }
            if (i > 0) {
                var previous = sorted.get(i - 1);
                if (previous.dayOfWeek() == current.dayOfWeek()
                        && previous.endTime().isAfter(current.startTime())) {
                    throw new ConflictException("Working-hour intervals cannot overlap");
                }
            }
        }
    }
}
