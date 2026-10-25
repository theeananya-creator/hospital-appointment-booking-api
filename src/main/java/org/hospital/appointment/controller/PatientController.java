package org.hospital.appointment.controller;

import jakarta.validation.Valid;
import org.hospital.appointment.dto.PatientProfile;
import org.hospital.appointment.service.PatientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients/me")
@PreAuthorize("hasRole('PATIENT')")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public PatientProfile.Response getMine() {
        return patientService.getMine();
    }

    @PatchMapping
    public PatientProfile.Response updateMine(@Valid @RequestBody PatientProfile.Update request) {
        return patientService.updateMine(request);
    }
}
