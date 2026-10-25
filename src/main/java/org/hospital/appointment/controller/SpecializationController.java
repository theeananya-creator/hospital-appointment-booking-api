package org.hospital.appointment.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.hospital.appointment.dto.SpecializationRequest;
import org.hospital.appointment.dto.SpecializationResponse;
import org.hospital.appointment.service.SpecializationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/specializations")
public class SpecializationController {

    private final SpecializationService service;

    public SpecializationController(SpecializationService service) {
        this.service = service;
    }

    @GetMapping
    public List<SpecializationResponse> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public SpecializationResponse create(@Valid @RequestBody SpecializationRequest request) {
        return service.create(request.name(), request.description());
    }
}
