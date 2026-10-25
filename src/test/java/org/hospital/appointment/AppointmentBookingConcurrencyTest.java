package org.hospital.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.hospital.appointment.domain.AppUser;
import org.hospital.appointment.domain.Doctor;
import org.hospital.appointment.domain.Patient;
import org.hospital.appointment.domain.SlotStatus;
import org.hospital.appointment.domain.Specialization;
import org.hospital.appointment.domain.TimeSlot;
import org.hospital.appointment.domain.UserRole;
import org.hospital.appointment.dto.AppointmentRequests;
import org.hospital.appointment.exception.ConflictException;
import org.hospital.appointment.repository.AppUserRepository;
import org.hospital.appointment.repository.AppointmentRepository;
import org.hospital.appointment.repository.DoctorRepository;
import org.hospital.appointment.repository.PatientRepository;
import org.hospital.appointment.repository.SpecializationRepository;
import org.hospital.appointment.repository.TimeSlotRepository;
import org.hospital.appointment.service.AppointmentService;
import org.hospital.appointment.service.NotificationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "app.jwt.secret=testcontainers-secret-with-at-least-32-bytes",
        "app.notifications.retry-delay-ms=0",
        "app.notifications.reminder-interval-ms=3600000"
})
@Testcontainers(disabledWithoutDocker = true)
class AppointmentBookingConcurrencyTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("hospital_booking")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private SpecializationRepository specializationRepository;
    @Autowired
    private TimeSlotRepository slotRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private AppointmentService appointmentService;

    @MockitoBean
    private NotificationGateway notificationGateway;

    @Test
    void onlyOneOfFiftyConcurrentRequestsBooksTheSlot() throws Exception {
        Specialization specialization = specializationRepository.saveAndFlush(
                new Specialization("Concurrency test", "Test specialization"));
        AppUser doctorUser = userRepository.saveAndFlush(
                new AppUser("doctor@test.example", "hash", UserRole.DOCTOR));
        Doctor doctor = doctorRepository.saveAndFlush(new Doctor(doctorUser, specialization, "Test Doctor",
                10, new java.math.BigDecimal("50.00"), null));
        AppUser patientUser = userRepository.saveAndFlush(
                new AppUser("patient@test.example", "hash", UserRole.PATIENT));
        patientRepository.saveAndFlush(new Patient(patientUser, "Test Patient", null, null));
        LocalDateTime slotStart = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        TimeSlot slot = slotRepository.saveAndFlush(new TimeSlot(doctor, slotStart, slotStart.plusMinutes(30)));

        int requestCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Future<?>> requests = new ArrayList<>();
        try {
            for (int i = 0; i < requestCount; i++) {
                requests.add(executor.submit(() -> {
                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                            "patient@test.example", null, List.of()));
                    SecurityContextHolder.setContext(context);
                    ready.countDown();
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Concurrent booking test did not start");
                        }
                        appointmentService.book(new AppointmentRequests.Booking(slot.getId()));
                        successes.incrementAndGet();
                    } catch (ConflictException expected) {
                        // A conflict is the expected result for every request after the winner.
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Booking workers did not become ready");
            }
            start.countDown();
            for (Future<?> request : requests) {
                request.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, successes.get());
        assertEquals(1, appointmentRepository.count());
        assertEquals(SlotStatus.BOOKED, slotRepository.findById(slot.getId()).orElseThrow().getStatus());
    }
}
