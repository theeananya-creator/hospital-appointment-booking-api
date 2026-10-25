package org.hospital.appointment.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hospital.appointment.domain.AppUser;
import org.hospital.appointment.domain.Doctor;
import org.hospital.appointment.domain.Specialization;
import org.hospital.appointment.domain.UserRole;
import org.hospital.appointment.dto.DoctorProfile;
import org.hospital.appointment.exception.NotFoundException;
import org.hospital.appointment.repository.AppUserRepository;
import org.hospital.appointment.repository.DoctorRepository;
import org.hospital.appointment.repository.SpecializationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppUserRepository userRepository;
    private final SpecializationRepository specializationRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final SlotService slotService;

    public DoctorService(DoctorRepository doctorRepository, AppUserRepository userRepository,
                         SpecializationRepository specializationRepository, PasswordEncoder passwordEncoder,
                         CurrentUserService currentUserService, SlotService slotService) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.specializationRepository = specializationRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.slotService = slotService;
    }

    @Transactional
    public DoctorProfile.Response create(DoctorProfile.Create request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new org.hospital.appointment.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes");
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new org.hospital.appointment.exception.ConflictException("An account with this email already exists");
        }
        Specialization specialization = specialization(request.specializationId());
        AppUser user = userRepository.save(new AppUser(email, passwordEncoder.encode(request.password()),
                UserRole.DOCTOR));
        Doctor doctor = doctorRepository.save(new Doctor(user, specialization, request.fullName().trim(),
                request.experienceYears(), request.consultationFee(), request.bio()));
        return toResponse(doctor);
    }

    @Transactional(readOnly = true)
    public Page<DoctorProfile.Response> search(String name, UUID specializationId, BigDecimal minFee,
                                                BigDecimal maxFee, Pageable pageable) {
        if (minFee != null && maxFee != null && minFee.compareTo(maxFee) > 0) {
            throw new org.hospital.appointment.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Minimum fee cannot exceed maximum fee");
        }
        if ((minFee != null && minFee.signum() < 0) || (maxFee != null && maxFee.signum() < 0)) {
            throw new org.hospital.appointment.exception.ApiException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Fee filters cannot be negative");
        }
        Specification<Doctor> spec = (root, query, cb) -> cb.isTrue(root.get("available"));
        if (StringUtils.hasText(name)) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("fullName")), "%" + name.trim().toLowerCase(Locale.ROOT) + "%"));
        }
        if (specializationId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("specialization").get("id"), specializationId));
        }
        if (minFee != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("consultationFee"), minFee));
        }
        if (maxFee != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("consultationFee"), maxFee));
        }
        return doctorRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DoctorProfile.Response get(UUID id) {
        return doctorRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Doctor was not found"));
    }

    @Transactional
    public DoctorProfile.Response updateMine(DoctorProfile.Update request) {
        Doctor doctor = doctorRepository.findByUser_Id(currentUserService.get().getId())
                .orElseThrow(() -> new NotFoundException("Doctor profile was not found"));
        boolean wasAvailable = doctor.isAvailable();
        doctor.update(request.fullName().trim(), request.experienceYears(), request.consultationFee(),
                request.bio(), specialization(request.specializationId()), request.available());
        if (request.available() != null && request.available() != wasAvailable) {
            slotService.synchronizeAvailability(doctor);
        }
        return toResponse(doctor);
    }

    public Doctor requireByUserId(UUID userId) {
        return doctorRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Doctor profile was not found"));
    }

    private Specialization specialization(UUID id) {
        return specializationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Specialization was not found"));
    }

    private DoctorProfile.Response toResponse(Doctor doctor) {
        return new DoctorProfile.Response(doctor.getId(), doctor.getFullName(),
                doctor.getSpecialization().getName(), doctor.getExperienceYears(), doctor.getConsultationFee(),
                doctor.getBio(), doctor.isAvailable());
    }
}
