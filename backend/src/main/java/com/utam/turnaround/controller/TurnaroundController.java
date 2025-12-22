package com.utam.turnaround.controller;

import com.utam.turnaround.dto.TurnaroundSessionDetailDTO;
import com.utam.turnaround.dto.TurnaroundSessionSummaryDTO;
import com.utam.turnaround.service.TurnaroundService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/turnaround")
@CrossOrigin(origins = "*")
public class TurnaroundController {

    private final TurnaroundService turnaroundService;

    public TurnaroundController(TurnaroundService turnaroundService) {
        this.turnaroundService = turnaroundService;
    }

    @GetMapping("/sessions")
    public List<TurnaroundSessionSummaryDTO> getAllSessions(
            @RequestParam(defaultValue = "VIDP") String icaoCode,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return turnaroundService.getAllSessions(icaoCode);
    }

    @GetMapping("/sessions/{id}")
    public TurnaroundSessionDetailDTO getSessionDetails(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "VIDP") String icaoCode) {
        return turnaroundService.getSessionDetails(id, icaoCode);
    }
}
