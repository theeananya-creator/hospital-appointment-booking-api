package org.hospital.appointment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hospital.appointment.domain.Appointment;
import org.hospital.appointment.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("""
            select (count(a) > 0) from Appointment a
            where a.patient.id = :patientId and a.status = :status
              and a.timeSlot.startTime < :endTime and a.timeSlot.endTime > :startTime
              and (:excludedAppointmentId is null or a.id <> :excludedAppointmentId)
            """)
    boolean hasOverlappingAppointment(@Param("patientId") UUID patientId,
                                      @Param("status") AppointmentStatus status,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      @Param("excludedAppointmentId") UUID excludedAppointmentId);

    @Query("""
            select a from Appointment a
            join fetch a.patient p
            join fetch p.user
            join fetch a.doctor d
            join fetch a.timeSlot s
            where a.id = :id
            """)
    Optional<Appointment> findDetailedById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a join fetch a.patient p join fetch a.doctor d join fetch a.timeSlot s where a.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);

    List<Appointment> findByPatient_User_IdOrderByCreatedAtDesc(UUID userId);
    List<Appointment> findByDoctor_User_IdOrderByCreatedAtDesc(UUID userId);

    @Query("""
            select a from Appointment a
            join fetch a.patient p
            join fetch p.user
            join fetch a.doctor d
            join fetch a.timeSlot s
            where a.status = :status and s.startTime >= :from and s.startTime < :until
            """)
    List<Appointment> findAppointmentsForReminder(@Param("status") AppointmentStatus status,
                                                  @Param("from") LocalDateTime from,
                                                  @Param("until") LocalDateTime until);

    @Query(value = """
            SELECT d.id AS doctorId, d.full_name AS doctorName, YEAR(s.start_time) AS year,
                   MONTH(s.start_time) AS month, COUNT(a.id) AS appointmentCount
            FROM appointments a
            JOIN doctors d ON d.id = a.doctor_id
            JOIN time_slots s ON s.id = a.time_slot_id
            GROUP BY d.id, d.full_name, YEAR(s.start_time), MONTH(s.start_time)
            ORDER BY year DESC, month DESC, doctorName
            """, nativeQuery = true)
    List<Object[]> appointmentsPerDoctorPerMonth();

    @Query(value = """
            SELECT COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled,
                   COUNT(*) AS total
            FROM appointments
            """, nativeQuery = true)
    Object[] cancellationCounts();

    @Query(value = """
            SELECT HOUR(start_time) AS bookingHour, COUNT(*) AS appointmentCount
            FROM appointments a JOIN time_slots s ON s.id = a.time_slot_id
            WHERE a.status <> 'CANCELLED'
            GROUP BY HOUR(start_time)
            ORDER BY appointmentCount DESC
            """, nativeQuery = true)
    List<Object[]> peakBookingHours();

    @Query(value = """
            SELECT sp.name AS specialization, COUNT(a.id) AS appointmentCount
            FROM appointments a
            JOIN doctors d ON d.id = a.doctor_id
            JOIN specializations sp ON sp.id = d.specialization_id
            WHERE a.status <> 'CANCELLED'
            GROUP BY sp.id, sp.name
            ORDER BY appointmentCount DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Object[]> topSpecializations();
}
