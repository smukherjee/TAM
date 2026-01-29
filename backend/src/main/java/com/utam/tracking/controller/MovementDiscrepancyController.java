package com.utam.tracking.controller;

import com.utam.tracking.dto.AcknowledgeRequestDTO;
import com.utam.tracking.dto.MovementDiscrepancyDTO;
import com.utam.tracking.service.MovementDiscrepancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for movement discrepancy management.
 * Feature: 005-asset-tracking-security
 * Task: T060
 */
@RestController
@RequestMapping("/api/tracking/discrepancies")
@Tag(name = "Movement Discrepancies", description = "Movement discrepancy management endpoints")
@PreAuthorize("hasAnyRole('ADMIN', 'GH')")
public class MovementDiscrepancyController {

    private final MovementDiscrepancyService discrepancyService;

    public MovementDiscrepancyController(MovementDiscrepancyService discrepancyService) {
        this.discrepancyService = discrepancyService;
    }

    @GetMapping
    @Operation(summary = "Get movement discrepancies", description = "Get movement discrepancies with filters and pagination")
    public ResponseEntity<Page<MovementDiscrepancyDTO>> getDiscrepancies(
            @Parameter(description = "Tenant code") @RequestParam String tenantCode,
            @Parameter(description = "Start date") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @Parameter(description = "Discrepancy type filter") 
                @RequestParam(required = false) String discrepancyType,
            @Parameter(description = "Acknowledged filter") @RequestParam(required = false) Boolean acknowledged,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<MovementDiscrepancyDTO> discrepancies = discrepancyService.getDiscrepancies(
            tenantCode, startDate, endDate, discrepancyType, acknowledged, pageable);
        
        return ResponseEntity.ok(discrepancies);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get discrepancy by ID", description = "Get a specific movement discrepancy by its ID")
    public ResponseEntity<MovementDiscrepancyDTO> getDiscrepancyById(
            @PathVariable UUID id) {
        
        MovementDiscrepancyDTO discrepancy = discrepancyService.getDiscrepancyById(id);
        return ResponseEntity.ok(discrepancy);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge discrepancy", description = "Acknowledge a movement discrepancy with resolution notes")
    public ResponseEntity<MovementDiscrepancyDTO> acknowledgeDiscrepancy(
            @PathVariable UUID id,
            @Valid @RequestBody AcknowledgeRequestDTO request,
            @AuthenticationPrincipal UserDetails user) {

        String userId = user != null ? user.getUsername() : "system";
        MovementDiscrepancyDTO acknowledged = discrepancyService.acknowledgeDiscrepancy(
            id, userId, request.getResolutionNotes());
        
        return ResponseEntity.ok(acknowledged);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get discrepancy statistics", description = "Get discrepancy statistics for a tenant")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam String tenantCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {

        Map<String, Object> stats = discrepancyService.getDiscrepancyStatistics(tenantCode, startDate, endDate);
        return ResponseEntity.ok(stats);
    }
}
