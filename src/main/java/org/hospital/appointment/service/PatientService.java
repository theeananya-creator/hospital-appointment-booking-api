package org.hospital.appointment.service;

import org.hospital.appointment.domain.Patient;
import org.hospital.appointment.dto.PatientProfile;
import org.hospital.appointment.exception.NotFoundException;
import org.hospital.appointment.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final CurrentUserService currentUserService;

    public PatientService(PatientRepository patientRepository, CurrentUserService currentUserService) {
        this.patientRepository = patientRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PatientProfile.Response getMine() {
        return toResponse(findMine());
    }

    @Transactional
    public PatientProfile.Response updateMine(PatientProfile.Update request) {
        Patient patient = findMine();
        patient.update(request.fullName().trim(), request.phone(), request.dateOfBirth());
        return toResponse(patient);
    }

    public Patient requireMine() {
        return findMine();
    }

    public Patient requireMineForUpdate() {
        return patientRepository.findByUserIdForUpdate(currentUserService.get().getId())
                .orElseThrow(() -> new NotFoundException("Patient profile was not found"));
    }

    public Patient lockForUpdate(UUID patientId) {
        return patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new NotFoundException("Patient profile was not found"));
    }

    private Patient findMine() {
        return patientRepository.findByUser_Id(currentUserService.get().getId())
                .orElseThrow(() -> new NotFoundException("Patient profile was not found"));
    }

    private PatientProfile.Response toResponse(Patient patient) {
        return new PatientProfile.Response(patient.getId(), patient.getFullName(),
                patient.getUser().getEmail(), patient.getPhone(), patient.getDateOfBirth());
    }
}
