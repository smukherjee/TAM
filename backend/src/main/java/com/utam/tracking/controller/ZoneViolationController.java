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
// @PreAuthorize("hasAnyRole('ADMIN', 'GH')") // Disabled for development
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
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @Parameter(description = "Tenant code") @RequestParam(required = false) String tenantCode,
            @Parameter(description = "Start date") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @Parameter(description = "Severity filter (CRITICAL, HIGH, MEDIUM, LOW)") 
                @RequestParam(required = false) String severity,
            @Parameter(description = "Acknowledged filter") @RequestParam(required = false) Boolean acknowledged,
            @PageableDefault(size = 20) Pageable pageable) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);

        Page<ZoneViolationDTO> violations = violationService.getViolations(
            resolvedTenantCode, startDate, endDate, severity, acknowledged, pageable);
        
        return ResponseEntity.ok(violations);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get violation by ID", description = "Get a specific zone violation by its ID")
    public ResponseEntity<ZoneViolationDTO> getViolationById(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @RequestParam(required = false) String tenantCode,
            @PathVariable UUID id) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);
        ZoneViolationDTO violation = violationService.getViolationById(id, resolvedTenantCode);
        return ResponseEntity.ok(violation);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge violation", description = "Acknowledge a zone violation with resolution notes")
    public ResponseEntity<ZoneViolationDTO> acknowledgeViolation(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @RequestParam(required = false) String tenantCode,
            @PathVariable UUID id,
            @Valid @RequestBody AcknowledgeRequestDTO request,
            @AuthenticationPrincipal UserDetails user) {

        String userId = user != null ? user.getUsername() : "system";
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);
        ZoneViolationDTO acknowledged = violationService.acknowledgeViolation(
            id, resolvedTenantCode, userId, request.getResolutionNotes());
        
        return ResponseEntity.ok(acknowledged);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get violation statistics", description = "Get violation statistics for a tenant")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @RequestParam(required = false) String tenantCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);

        Map<String, Object> stats = violationService.getViolationStatistics(resolvedTenantCode, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent unacknowledged violations", description = "Get recent unacknowledged violations for alerts")
    public ResponseEntity<List<ZoneViolationDTO>> getRecentViolations(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(defaultValue = "10") int limit) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);

        List<ZoneViolationDTO> violations = violationService.getRecentUnacknowledgedViolations(resolvedTenantCode, limit);
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
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @Parameter(description = "Tenant code", required = true)
            @RequestParam(required = false) String tenantCode,

            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,

            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,

            @Parameter(description = "Severity filter")
            @RequestParam(required = false) String severity,

            @Parameter(description = "Acknowledged filter")
            @RequestParam(required = false) Boolean acknowledged) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);

        // Get all violations (unpaged) for export
        List<ZoneViolationDTO> violations = violationService.getAllViolationsForExport(
            resolvedTenantCode, startDate, endDate, severity, acknowledged);

        String startDateStr = startDate != null ? startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";
        String endDateStr = endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";

        byte[] excelBytes = reportExportService.exportViolationsToExcel(
            violations, resolvedTenantCode, startDateStr, endDateStr);

        String filename = String.format("zone_violations_%s_%s_to_%s.xlsx",
            resolvedTenantCode, startDateStr, endDateStr);

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
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @Parameter(description = "Tenant code", required = true)
            @RequestParam(required = false) String tenantCode,

            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,

            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,

            @Parameter(description = "Severity filter")
            @RequestParam(required = false) String severity,

            @Parameter(description = "Acknowledged filter")
            @RequestParam(required = false) Boolean acknowledged) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);

        // Get all violations (unpaged) for export
        List<ZoneViolationDTO> violations = violationService.getAllViolationsForExport(
            resolvedTenantCode, startDate, endDate, severity, acknowledged);

        String startDateStr = startDate != null ? startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";
        String endDateStr = endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";

        byte[] pdfBytes = reportExportService.exportViolationsToPdf(
            violations, resolvedTenantCode, startDateStr, endDateStr);

        String filename = String.format("zone_violations_%s_%s_to_%s.pdf",
            resolvedTenantCode, startDateStr, endDateStr);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    private String resolveTenantCode(String icaoCodeHeader, String tenantCodeParam) {
        if (icaoCodeHeader != null && !icaoCodeHeader.isBlank()) {
            return icaoCodeHeader;
        }
        if (tenantCodeParam != null && !tenantCodeParam.isBlank()) {
            return tenantCodeParam;
        }
        return "VIDP";
    }
}
