package org.hospital.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "appointment_history")
public class AppointmentHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private AppUser actor;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected AppointmentHistory() {
    }

    public AppointmentHistory(Appointment appointment, AppointmentStatus fromStatus,
                              AppointmentStatus toStatus, AppUser actor, String note) {
        this.appointment = appointment;
        this.fromStatus = fromStatus == null ? null : fromStatus.name();
        this.toStatus = toStatus.name();
        this.actor = actor;
        this.note = note;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public AppUser getActor() {
        return actor;
    }

    public String getNote() {
        return note;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
