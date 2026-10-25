package org.hospital.appointment.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hospital.appointment.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminReportService {

    private final AppointmentRepository appointmentRepository;

    public AdminReportService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> appointmentsPerDoctorPerMonth() {
        return rows(appointmentRepository.appointmentsPerDoctorPerMonth(),
                new String[]{"doctorId", "doctorName", "year", "month", "appointmentCount"});
    }

    @Transactional(readOnly = true)
    public Map<String, Object> cancellationRate() {
        Object[] counts = appointmentRepository.cancellationCounts();
        long cancelled = ((Number) counts[0]).longValue();
        long total = ((Number) counts[1]).longValue();
        double rate = total == 0 ? 0.0 : (double) cancelled / total;
        return Map.of("cancelledAppointments", cancelled, "totalAppointments", total,
                "cancellationRate", rate);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> peakBookingHours() {
        return rows(appointmentRepository.peakBookingHours(),
                new String[]{"hourOfDay", "appointmentCount"});
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> topSpecializations() {
        return rows(appointmentRepository.topSpecializations(),
                new String[]{"specialization", "appointmentCount"});
    }

    private List<Map<String, Object>> rows(List<Object[]> results, String[] keys) {
        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            for (int i = 0; i < keys.length; i++) {
                Object value = row[i];
                if (value instanceof java.math.BigInteger integer) {
                    value = integer.longValue();
                }
                mapped.put(keys[i], value);
            }
            response.add(mapped);
        }
        return response;
    }
}
