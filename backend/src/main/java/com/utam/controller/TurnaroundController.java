package com.utam.controller;

import com.utam.model.TurnaroundEvent;
import com.utam.service.TurnaroundService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/turnaround")
public class TurnaroundController {

    private final TurnaroundService service;

    public TurnaroundController(TurnaroundService service) {
        this.service = service;
    }

    @GetMapping("/events")
    public List<TurnaroundEvent> getEvents(@RequestHeader(value = "X-User-ICAO", required = false) String icaoCode) {
        return service.getAllEvents(icaoCode);
    }
}
