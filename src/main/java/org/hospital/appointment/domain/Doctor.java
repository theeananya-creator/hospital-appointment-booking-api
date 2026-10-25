package org.hospital.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "doctors")
public class Doctor extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialization_id", nullable = false)
    private Specialization specialization;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "experience_years", nullable = false)
    private int experienceYears;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(length = 2000)
    private String bio;

    @Column(nullable = false)
    private boolean available = true;

    protected Doctor() {
    }

    public Doctor(AppUser user, Specialization specialization, String fullName, int experienceYears,
                  BigDecimal consultationFee, String bio) {
        this.user = user;
        this.specialization = specialization;
        this.fullName = fullName;
        this.experienceYears = experienceYears;
        this.consultationFee = consultationFee;
        this.bio = bio;
    }

    public AppUser getUser() {
        return user;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public String getFullName() {
        return fullName;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public String getBio() {
        return bio;
    }

    public boolean isAvailable() {
        return available;
    }

    public void update(String fullName, int experienceYears, BigDecimal consultationFee,
                       String bio, Specialization specialization, Boolean available) {
        this.fullName = fullName;
        this.experienceYears = experienceYears;
        this.consultationFee = consultationFee;
        this.bio = bio;
        this.specialization = specialization;
        if (available != null) {
            this.available = available;
        }
    }
}
