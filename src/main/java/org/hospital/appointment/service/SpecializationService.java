package org.hospital.appointment.service;

import java.util.List;
import org.hospital.appointment.domain.Specialization;
import org.hospital.appointment.dto.SpecializationResponse;
import org.hospital.appointment.exception.ConflictException;
import org.hospital.appointment.repository.SpecializationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpecializationService {

    private final SpecializationRepository repository;

    public SpecializationService(SpecializationRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SpecializationResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public SpecializationResponse create(String name, String description) {
        if (repository.existsByNameIgnoreCase(name.trim())) {
            throw new ConflictException("Specialization already exists");
        }
        Specialization specialization = new Specialization(name.trim(), description);
        return toResponse(repository.save(specialization));
    }

    private SpecializationResponse toResponse(Specialization specialization) {
        return new SpecializationResponse(specialization.getId(), specialization.getName(),
                specialization.getDescription());
    }
}
