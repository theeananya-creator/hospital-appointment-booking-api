CREATE TABLE users (
    id CHAR(36) NOT NULL PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'DOCTOR', 'PATIENT'))
);

CREATE TABLE specializations (
    id CHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_specializations_name UNIQUE (name)
);

CREATE TABLE doctors (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    specialization_id CHAR(36) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    experience_years INT NOT NULL,
    consultation_fee DECIMAL(10, 2) NOT NULL,
    bio VARCHAR(2000),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_doctors_user UNIQUE (user_id),
    CONSTRAINT fk_doctors_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_doctors_specialization FOREIGN KEY (specialization_id) REFERENCES specializations(id),
    CONSTRAINT chk_doctors_experience CHECK (experience_years >= 0),
    CONSTRAINT chk_doctors_fee CHECK (consultation_fee >= 0),
    INDEX idx_doctors_specialization_fee (specialization_id, consultation_fee),
    INDEX idx_doctors_name (full_name)
);

CREATE TABLE patients (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(30),
    date_of_birth DATE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_patients_user UNIQUE (user_id),
    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_patients_name (full_name)
);

CREATE TABLE doctor_working_hours (
    id CHAR(36) NOT NULL PRIMARY KEY,
    doctor_id CHAR(36) NOT NULL,
    day_of_week INT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    slot_duration_minutes INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_working_hours_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT chk_working_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_working_hours_duration CHECK (slot_duration_minutes > 0),
    INDEX idx_working_hours_doctor_day (doctor_id, day_of_week)
);

CREATE TABLE doctor_leaves (
    id CHAR(36) NOT NULL PRIMARY KEY,
    doctor_id CHAR(36) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_doctor_leaves_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id) ON DELETE CASCADE,
    CONSTRAINT chk_doctor_leaves_dates CHECK (end_date >= start_date),
    INDEX idx_doctor_leaves_dates (doctor_id, start_date, end_date)
);

CREATE TABLE time_slots (
    id CHAR(36) NOT NULL PRIMARY KEY,
    doctor_id CHAR(36) NOT NULL,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_time_slots_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT uk_time_slots_doctor_start UNIQUE (doctor_id, start_time),
    CONSTRAINT chk_time_slots_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'CLOSED')),
    INDEX idx_time_slots_available (doctor_id, status, start_time)
);

CREATE TABLE appointments (
    id CHAR(36) NOT NULL PRIMARY KEY,
    patient_id CHAR(36) NOT NULL,
    doctor_id CHAR(36) NOT NULL,
    time_slot_id CHAR(36) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reschedule_count INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id),
    CONSTRAINT fk_appointments_slot FOREIGN KEY (time_slot_id) REFERENCES time_slots(id),
    CONSTRAINT chk_appointments_status CHECK (status IN ('BOOKED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
    CONSTRAINT chk_appointments_reschedules CHECK (reschedule_count >= 0),
    INDEX idx_appointments_doctor_status_created (doctor_id, status, created_at),
    INDEX idx_appointments_time_slot (time_slot_id),
    INDEX idx_appointments_patient_status (patient_id, status),
    INDEX idx_appointments_status (status)
);

CREATE TABLE appointment_history (
    id CHAR(36) NOT NULL PRIMARY KEY,
    appointment_id CHAR(36) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    actor_user_id CHAR(36) NOT NULL,
    note VARCHAR(500),
    changed_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_appointment_history_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_appointment_history_actor FOREIGN KEY (actor_user_id) REFERENCES users(id),
    INDEX idx_appointment_history_appointment_time (appointment_id, changed_at)
);

CREATE TABLE notifications (
    id CHAR(36) NOT NULL PRIMARY KEY,
    appointment_id CHAR(36) NOT NULL,
    recipient_email VARCHAR(254) NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    scheduled_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    sent_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notifications_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT uk_notifications_appointment_type UNIQUE (appointment_id, notification_type),
    INDEX idx_notifications_status_scheduled (status, scheduled_at)
);

CREATE TABLE refresh_tokens (
    id CHAR(36) NOT NULL PRIMARY KEY,
    user_id CHAR(36) NOT NULL,

    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    replaced_by CHAR(36),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    INDEX idx_refresh_tokens_user (user_id, revoked_at)
);
