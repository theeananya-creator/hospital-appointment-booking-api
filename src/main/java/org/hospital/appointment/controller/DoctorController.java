package org.hospital.appointment.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;
import org.hospital.appointment.dto.DoctorProfile;
import org.hospital.appointment.service.DoctorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public Page<DoctorProfile.Response> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) UUID specializationId,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal minFee,
            @RequestParam(required = false) @DecimalMin("0.00") BigDecimal maxFee,
            @PageableDefault(size = 20, sort = "fullName") Pageable pageable) {
        return doctorService.search(name, specializationId, minFee, maxFee, pageable);
    }

    @GetMapping("/{id}")
    public DoctorProfile.Response get(@PathVariable UUID id) {
        return doctorService.get(id);
    }

    @PatchMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public DoctorProfile.Response updateMine(@Valid @RequestBody DoctorProfile.Update request) {
        return doctorService.updateMine(request);
    }
}
