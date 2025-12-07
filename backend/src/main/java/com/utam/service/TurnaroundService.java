package com.utam.service;

import com.utam.model.TurnaroundEvent;
import com.utam.repository.TurnaroundEventRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TurnaroundService {

    private final TurnaroundEventRepository repository;

    public TurnaroundService(TurnaroundEventRepository repository) {
        this.repository = repository;
    }

    @KafkaListener(topics = "turnaround-raw-json", groupId = "utam-group")
    public void consumeTurnaroundEvent(TurnaroundEvent event) {
        repository.save(event);
    }

    public List<TurnaroundEvent> getAllEvents() {
        return repository.findAll();
    }
}
