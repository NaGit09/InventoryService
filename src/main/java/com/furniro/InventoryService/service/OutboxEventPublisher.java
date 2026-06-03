package com.furniro.InventoryService.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.furniro.InventoryService.database.entity.OutboxEvent;
import com.furniro.InventoryService.database.repository.OutboxRepository;
import com.furniro.InventoryService.service.kafka.KafkaProducer;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaProducer kafkaProducer;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findByStatus("PENDING");

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaProducer.send(event.getTopic(), event.getPayload());

                event.setStatus("PROCESSED");

                outboxRepository.save(event);

                log.info("Successfully published outbox event for order: {}", event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}. Will retry later.", event.getId(), e);
            }
        }
    }
}