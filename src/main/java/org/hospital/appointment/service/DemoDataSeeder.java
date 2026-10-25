package org.hospital.appointment.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hospital.appointment.domain.AppUser;
import org.hospital.appointment.domain.Doctor;
import org.hospital.appointment.domain.DoctorWorkingHour;
import org.hospital.appointment.domain.Specialization;
import org.hospital.appointment.domain.UserRole;
import org.hospital.appointment.repository.AppUserRepository;
import org.hospital.appointment.repository.DoctorRepository;
import org.hospital.appointment.repository.DoctorWorkingHourRepository;
import org.hospital.appointment.repository.SpecializationRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private final AppUserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final SpecializationRepository specializationRepository;
    private final DoctorWorkingHourRepository workingHourRepository;
    private final PasswordEncoder passwordEncoder;
    private final SlotService slotService;

    public DemoDataSeeder(AppUserRepository userRepository, DoctorRepository doctorRepository,
                          SpecializationRepository specializationRepository,
                          DoctorWorkingHourRepository workingHourRepository,
                          PasswordEncoder passwordEncoder, SlotService slotService) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.specializationRepository = specializationRepository;
        this.workingHourRepository = workingHourRepository;
        this.passwordEncoder = passwordEncoder;
        this.slotService = slotService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Specialization> specializations = specializationRepository.findAll().stream()
                .collect(Collectors.toMap(Specialization::getName, item -> item));
        for (String name : List.of("Cardiology", "Dermatology", "Pediatrics", "Orthopedics")) {
            if (!specializations.containsKey(name)) {
                specializations.put(name, specializationRepository.save(
                        new Specialization(name, "Demo listing for local development.")));
            }
        }

        seedDoctor("Dr. Asha Rao", "cardiology.demo@example.test", "Cardiology", 14,
                "950.00", "Heart health, preventive cardiology, and patient-first care.", specializations);
        seedDoctor("Dr. Vikram Shah", "dermatology.demo@example.test", "Dermatology", 11,
                "700.00", "Medical and routine skin care for all ages.", specializations);
        seedDoctor("Dr. Meera Nair", "pediatrics.demo@example.test", "Pediatrics", 9,
                "600.00", "Thoughtful pediatric care and family wellness.", specializations);
        seedDoctor("Dr. Arjun Reddy", "orthopedics.demo@example.test", "Orthopedics", 16,
                "850.00", "Joint, sports injury, and mobility care.", specializations);
    }

    private void seedDoctor(String name, String email, String specializationName, int experience,
                           String fee, String bio, Map<String, Specialization> specializations) {
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                userRepository.save(new AppUser(email, passwordEncoder.encode(randomSecret()), UserRole.DOCTOR)));
        Doctor doctor = doctorRepository.findByUser_Id(user.getId()).orElseGet(() ->
                doctorRepository.save(new Doctor(user, specializations.get(specializationName), name,
                        experience, new BigDecimal(fee), bio)));

        if (workingHourRepository.findByDoctor_Id(doctor.getId()).isEmpty()) {
            List<DoctorWorkingHour> hours = List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
                    DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY).stream()
                    .map(day -> new DoctorWorkingHour(doctor, day, LocalTime.of(9, 0),
                            LocalTime.of(17, 0), 30))
                    .toList();
            workingHourRepository.saveAll(hours);
        }
        slotService.generateForDoctor(doctor.getId());
    }

    private String randomSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return java.util.Base64.getEncoder().encodeToString(secret);
    }
}
