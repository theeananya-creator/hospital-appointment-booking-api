package org.hospital.appointment.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.hospital.appointment.dto.DoctorProfile;
import org.hospital.appointment.dto.LeaveResponse;
import org.hospital.appointment.dto.SlotResponse;
import org.hospital.appointment.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @PutMapping("/doctors/me/working-hours")
    @PreAuthorize("hasRole('DOCTOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setWorkingHours(@Valid @RequestBody List<DoctorProfile.WorkingHourRequest> requests) {
        slotService.setWorkingHours(requests);
    }

    @PostMapping("/doctors/me/leaves")
    @PreAuthorize("hasRole('DOCTOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveResponse addLeave(@Valid @RequestBody DoctorProfile.LeaveRequest request) {
        return slotService.addLeave(request);
    }

    @PostMapping("/doctors/{doctorId}/slots/generate")
    @PreAuthorize("hasRole('ADMIN') or @doctorAuthorization.isOwner(#doctorId, authentication.name)")
    public List<SlotResponse> generate(@PathVariable UUID doctorId) {
        return slotService.generateForDoctor(doctorId);
    }

    @GetMapping("/doctors/{doctorId}/slots")
    public List<SlotResponse> available(@PathVariable UUID doctorId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.availableSlots(doctorId, date);
    }
}
