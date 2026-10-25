package org.hospital.appointment.service;

public interface NotificationGateway {
    void send(String recipient, String subject, String body);
}
