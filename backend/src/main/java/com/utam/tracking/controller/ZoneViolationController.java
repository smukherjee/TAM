package com.utam.tracking.controller;

import com.utam.tracking.dto.AcknowledgeRequestDTO;
import com.utam.tracking.dto.ZoneViolationDTO;
import com.utam.tracking.service.ReportExportService;
import com.utam.tracking.service.ZoneViolationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for zone violation management.
 * Feature: 005-asset-tracking-security
 * Task: T059
 */
@RestController
@RequestMapping("/api/tracking/violations")
@Tag(name = "Zone Violations", description = "Zone violation management endpoints")
@PreAuthorize("hasAnyRole('ADMIN', 'GH')")
public class ZoneViolationController {

    private final ZoneViolationService violationService;
    private final ReportExportService reportExportService;

    public ZoneViolationController(ZoneViolationService violationService,
                                    ReportExportService reportExportService) {
        this.violationService = violationService;
        this.reportExportService = reportExportService;
    }

    @GetMapping
    @Operation(summary = "Get zone violations", description = "Get zone violations with filters and pagination")
    public ResponseEntity<Page<ZoneViolationDTO>> getViolations(
            @Parameter(description = "Tenant code") @RequestParam String tenantCode,
            @Parameter(description = "Start date") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @Parameter(description = "Severity filter (CRITICAL, HIGH, MEDIUM, LOW)") 
                @RequestParam(required = false) String severity,
            @Parameter(description = "Acknowledged filter") @RequestParam(required = false) Boolean acknowledged,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ZoneViolationDTO> violations = violationService.getViolations(
            tenantCode, startDate, endDate, severity, acknowledged, pageable);
        
        return ResponseEntity.ok(violations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get violation by ID", description = "Get a specific zone violation by its ID")
    public ResponseEntity<ZoneViolationDTO> getViolationById(
            @PathVariable UUID id) {
        
        ZoneViolationDTO violation = violationService.getViolationById(id);
        return ResponseEntity.ok(violation);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge violation", description = "Acknowledge a zone violation with resolution notes")
    public ResponseEntity<ZoneViolationDTO> acknowledgeViolation(
            @PathVariable UUID id,
            @Valid @RequestBody AcknowledgeRequestDTO request,
            @AuthenticationPrincipal UserDetails user) {

        String userId = user != null ? user.getUsername() : "system";
        ZoneViolationDTO acknowledged = violationService.acknowledgeViolation(
            id, userId, request.getResolutionNotes());
        
        return ResponseEntity.ok(acknowledged);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get violation statistics", description = "Get violation statistics for a tenant")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam String tenantCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {

        Map<String, Object> stats = violationService.getViolationStatistics(tenantCode, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent unacknowledged violations", description = "Get recent unacknowledged violations for alerts")
    public ResponseEntity<List<ZoneViolationDTO>> getRecentViolations(
            @RequestParam String tenantCode,
            @RequestParam(defaultValue = "10") int limit) {

        List<ZoneViolationDTO> violations = violationService.getRecentUnacknowledgedViolations(tenantCode, limit);
        return ResponseEntity.ok(violations);
    }

    // ============================================================
    // Export Endpoints (T055a, T055b)
    // ============================================================

    /**
     * Export zone violations to Excel format.
     * <p>
     * Task: T055a - Excel Export
     */
    @GetMapping("/export/excel")
    @Operation(summary = "Export violations to Excel",
            description = "Export zone violations report to Excel (.xlsx) format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Excel file generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<byte[]> exportViolationsExcel(
            @Parameter(description = "Tenant code", required = true)
            @RequestParam String tenantCode,

            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,

            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,

            @Parameter(description = "Severity filter")
            @RequestParam(required = false) String severity,

            @Parameter(description = "Acknowledged filter")
            @RequestParam(required = false) Boolean acknowledged) {

        // Get all violations (unpaged) for export
        List<ZoneViolationDTO> violations = violationService.getAllViolationsForExport(
            tenantCode, startDate, endDate, severity, acknowledged);

        String startDateStr = startDate != null ? startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";
        String endDateStr = endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";

        byte[] excelBytes = reportExportService.exportViolationsToExcel(
            violations, tenantCode, startDateStr, endDateStr);

        String filename = String.format("zone_violations_%s_%s_to_%s.xlsx",
            tenantCode, startDateStr, endDateStr);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * Export zone violations to PDF format.
     * <p>
     * Task: T055b - PDF Export
     */
    @GetMapping("/export/pdf")
    @Operation(summary = "Export violations to PDF",
            description = "Export zone violations report to PDF format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF file generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<byte[]> exportViolationsPdf(
            @Parameter(description = "Tenant code", required = true)
            @RequestParam String tenantCode,

            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,

            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,

            @Parameter(description = "Severity filter")
            @RequestParam(required = false) String severity,

            @Parameter(description = "Acknowledged filter")
            @RequestParam(required = false) Boolean acknowledged) {

        // Get all violations (unpaged) for export
        List<ZoneViolationDTO> violations = violationService.getAllViolationsForExport(
            tenantCode, startDate, endDate, severity, acknowledged);

        String startDateStr = startDate != null ? startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";
        String endDateStr = endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";

        byte[] pdfBytes = reportExportService.exportViolationsToPdf(
            violations, tenantCode, startDateStr, endDateStr);

        String filename = String.format("zone_violations_%s_%s_to_%s.pdf",
            tenantCode, startDateStr, endDateStr);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}
