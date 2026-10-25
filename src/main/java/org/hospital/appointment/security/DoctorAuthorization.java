package org.hospital.appointment.security;

import java.util.UUID;
import org.hospital.appointment.repository.DoctorRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("doctorAuthorization")
public class DoctorAuthorization {

    private final DoctorRepository doctorRepository;

    public DoctorAuthorization(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    @Transactional(readOnly = true)
    public boolean isOwner(UUID doctorId, String email) {
        return doctorRepository.findById(doctorId)
                .map(doctor -> doctor.getUser().getEmail().equalsIgnoreCase(email))
                .orElse(false);
    }
}
