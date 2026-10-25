package org.hospital.appointment.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.hospital.appointment.dto.AppointmentRequests;
import org.hospital.appointment.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentRequests.Response book(@Valid @RequestBody AppointmentRequests.Booking request) {
        return appointmentService.book(request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentRequests.Response cancel(@PathVariable UUID id) {
        return appointmentService.cancel(id);
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('PATIENT')")
    public AppointmentRequests.Response reschedule(@PathVariable UUID id,
                                                    @Valid @RequestBody AppointmentRequests.Reschedule request) {
        return appointmentService.reschedule(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public AppointmentRequests.Response status(@PathVariable UUID id,
                                               @Valid @RequestBody AppointmentRequests.StatusChange request) {
        return appointmentService.changeStatus(id, request);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    public List<AppointmentRequests.Response> mine() {
        return appointmentService.mine();
    }

    @GetMapping("/{id}/history")
    public List<AppointmentRequests.History> history(@PathVariable UUID id) {
        return appointmentService.history(id);
    }
}
