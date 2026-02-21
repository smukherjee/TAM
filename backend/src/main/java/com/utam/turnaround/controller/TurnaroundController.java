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
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCode,
            @RequestParam(value = "icaoCode", required = false) String icaoCodeParam,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        String tenantCode = icaoCode != null ? icaoCode : (icaoCodeParam != null ? icaoCodeParam : "VIDP");
        return turnaroundService.getAllSessions(tenantCode);
    }

    @GetMapping("/sessions/{id}")
    public TurnaroundSessionDetailDTO getSessionDetails(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCode,
            @RequestParam(value = "icaoCode", required = false) String icaoCodeParam) {
        String tenantCode = icaoCode != null ? icaoCode : (icaoCodeParam != null ? icaoCodeParam : "VIDP");
        return turnaroundService.getSessionDetails(id, tenantCode);
    }
}
