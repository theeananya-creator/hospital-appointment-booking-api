package org.hospital.appointment.controller;

import java.util.List;
import java.util.Map;
import org.hospital.appointment.service.AdminReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final AdminReportService reportService;

    public AdminReportController(AdminReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/appointments-per-doctor-month")
    public List<Map<String, Object>> appointmentsPerDoctorPerMonth() {
        return reportService.appointmentsPerDoctorPerMonth();
    }

    @GetMapping("/cancellation-rate")
    public Map<String, Object> cancellationRate() {
        return reportService.cancellationRate();
    }

    @GetMapping("/peak-booking-hours")
    public List<Map<String, Object>> peakBookingHours() {
        return reportService.peakBookingHours();
    }

    @GetMapping("/top-specializations")
    public List<Map<String, Object>> topSpecializations() {
        return reportService.topSpecializations();
    }
}
