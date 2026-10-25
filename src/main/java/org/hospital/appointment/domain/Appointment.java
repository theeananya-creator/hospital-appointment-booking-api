package org.hospital.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.BOOKED;

    @Column(name = "reschedule_count", nullable = false)
    private int rescheduleCount;

    @Version
    @Column(nullable = false)
    private long version;

    protected Appointment() {
    }

    public Appointment(Patient patient, Doctor doctor, TimeSlot timeSlot) {
        this.patient = patient;
        this.doctor = doctor;
        this.timeSlot = timeSlot;
    }

    public Patient getPatient() {
        return patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public int getRescheduleCount() {
        return rescheduleCount;
    }

    public void cancel() {
        if (status != AppointmentStatus.BOOKED) {
            throw new IllegalStateException("Only booked appointments can be cancelled");
        }
        status = AppointmentStatus.CANCELLED;
    }

    public void reschedule(TimeSlot newSlot) {
        if (status != AppointmentStatus.BOOKED) {
            throw new IllegalStateException("Only booked appointments can be rescheduled");
        }
        timeSlot = newSlot;
        doctor = newSlot.getDoctor();
        rescheduleCount++;
    }

    public void changeStatus(AppointmentStatus newStatus) {
        status = newStatus;
    }
}
