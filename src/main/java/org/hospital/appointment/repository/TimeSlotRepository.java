package org.hospital.appointment.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hospital.appointment.domain.SlotStatus;
import org.hospital.appointment.domain.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, UUID> {

    List<TimeSlot> findByDoctor_IdAndStatusAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
            UUID doctorId, SlotStatus status, LocalDateTime from, LocalDateTime until);

    Optional<TimeSlot> findByIdAndStatus(UUID id, SlotStatus status);

    boolean existsByDoctor_IdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            UUID doctorId, SlotStatus status, LocalDateTime endTime, LocalDateTime startTime);

    @Modifying
    @Query("""
            update TimeSlot slot set slot.status = :closed, slot.version = slot.version + 1
            where slot.doctor.id = :doctorId and slot.status = :available
              and slot.startTime >= :from and slot.startTime < :until
            """)
    int closeAvailableSlots(@Param("doctorId") UUID doctorId, @Param("available") SlotStatus available,
                            @Param("closed") SlotStatus closed, @Param("from") LocalDateTime from,
                            @Param("until") LocalDateTime until);

    @Modifying
    @Query(value = """
            INSERT INTO time_slots
              (id, doctor_id, start_time, end_time, status, version, created_at, updated_at)
            VALUES
              (:id, :doctorId, :startTime, :endTime, 'AVAILABLE', 0, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE
              version = IF(time_slots.status = 'CLOSED', time_slots.version + 1, time_slots.version),
              end_time = IF(time_slots.status = 'BOOKED', time_slots.end_time, VALUES(end_time)),
              status = IF(time_slots.status = 'CLOSED', 'AVAILABLE', time_slots.status),
              updated_at = CURRENT_TIMESTAMP(6)
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") String id, @Param("doctorId") String doctorId,
                       @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
