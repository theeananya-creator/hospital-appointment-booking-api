package org.hospital.appointment.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SlotGenerationScheduler {

    private final SlotService slotService;

    public SlotGenerationScheduler(SlotService slotService) {
        this.slotService = slotService;
    }

    @Scheduled(cron = "${app.slots.generation-cron:0 15 2 * * *}")
    public void generateFutureSlots() {
        slotService.generateForAllDoctors();
    }
}
