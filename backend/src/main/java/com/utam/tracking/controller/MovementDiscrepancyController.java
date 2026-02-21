package com.utam.tracking.controller;

import com.utam.tracking.dto.AcknowledgeRequestDTO;
import com.utam.tracking.dto.MovementDiscrepancyDTO;
import com.utam.tracking.service.MovementDiscrepancyService;
import com.utam.tracking.service.ReportExportService;
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
 * REST controller for movement discrepancy management.
 * Feature: 005-asset-tracking-security
 * Task: T060
 */
@RestController
@RequestMapping("/api/tracking/discrepancies")
@Tag(name = "Movement Discrepancies", description = "Movement discrepancy management endpoints")
// @PreAuthorize("hasAnyRole('ADMIN', 'GH')") // Disabled for development
public class MovementDiscrepancyController {

    private final MovementDiscrepancyService discrepancyService;
    private final ReportExportService reportExportService;

    public MovementDiscrepancyController(MovementDiscrepancyService discrepancyService,
                                          ReportExportService reportExportService) {
        this.discrepancyService = discrepancyService;
        this.reportExportService = reportExportService;
    }

    @GetMapping
    @Operation(summary = "Get movement discrepancies", description = "Get movement discrepancies with filters and pagination")
    public ResponseEntity<Page<MovementDiscrepancyDTO>> getDiscrepancies(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @Parameter(description = "Tenant code") @RequestParam(required = false) String tenantCode,
            @Parameter(description = "Start date") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) 
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,
            @Parameter(description = "Discrepancy type filter") 
                @RequestParam(required = false) String discrepancyType,
            @Parameter(description = "Acknowledged filter") @RequestParam(required = false) Boolean acknowledged,
            @PageableDefault(size = 20) Pageable pageable) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);

        Page<MovementDiscrepancyDTO> discrepancies = discrepancyService.getDiscrepancies(
            resolvedTenantCode, startDate, endDate, discrepancyType, acknowledged, pageable);
        
        return ResponseEntity.ok(discrepancies);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get discrepancy by ID", description = "Get a specific movement discrepancy by its ID")
    public ResponseEntity<MovementDiscrepancyDTO> getDiscrepancyById(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @RequestParam(required = false) String tenantCode,
            @PathVariable UUID id) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);
        MovementDiscrepancyDTO discrepancy = discrepancyService.getDiscrepancyById(id, resolvedTenantCode);
        return ResponseEntity.ok(discrepancy);
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge discrepancy", description = "Acknowledge a movement discrepancy with resolution notes")
    public ResponseEntity<MovementDiscrepancyDTO> acknowledgeDiscrepancy(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @RequestParam(required = false) String tenantCode,
            @PathVariable UUID id,
            @Valid @RequestBody AcknowledgeRequestDTO request,
            @AuthenticationPrincipal UserDetails user) {

        String userId = user != null ? user.getUsername() : "system";
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);
        MovementDiscrepancyDTO acknowledged = discrepancyService.acknowledgeDiscrepancy(
            id, resolvedTenantCode, userId, request.getResolutionNotes());
        
        return ResponseEntity.ok(acknowledged);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get discrepancy statistics", description = "Get discrepancy statistics for a tenant")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @RequestParam(required = false) String tenantCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);

        Map<String, Object> stats = discrepancyService.getDiscrepancyStatistics(resolvedTenantCode, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    // ============================================================
    // Export Endpoints (T055a, T055b)
    // ============================================================

    /**
     * Export movement discrepancies to Excel format.
     * <p>
     * Task: T055a - Excel Export
     */
    @GetMapping("/export/excel")
    @Operation(summary = "Export discrepancies to Excel",
            description = "Export movement discrepancies report to Excel (.xlsx) format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Excel file generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<byte[]> exportDiscrepanciesExcel(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @Parameter(description = "Tenant code", required = true)
            @RequestParam(required = false) String tenantCode,

            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,

            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,

            @Parameter(description = "Discrepancy type filter")
            @RequestParam(required = false) String discrepancyType,

            @Parameter(description = "Acknowledged filter")
            @RequestParam(required = false) Boolean acknowledged) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);

        // Get all discrepancies (unpaged) for export
        List<MovementDiscrepancyDTO> discrepancies = discrepancyService.getAllDiscrepanciesForExport(
            resolvedTenantCode, startDate, endDate, discrepancyType, acknowledged);

        String startDateStr = startDate != null ? startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";
        String endDateStr = endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";

        byte[] excelBytes = reportExportService.exportDiscrepanciesToExcel(
            discrepancies, resolvedTenantCode, startDateStr, endDateStr);

        String filename = String.format("movement_discrepancies_%s_%s_to_%s.xlsx",
            resolvedTenantCode, startDateStr, endDateStr);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok().headers(headers).body(excelBytes);
    }

    /**
     * Export movement discrepancies to PDF format.
     * <p>
     * Task: T055b - PDF Export
     */
    @GetMapping("/export/pdf")
    @Operation(summary = "Export discrepancies to PDF",
            description = "Export movement discrepancies report to PDF format")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF file generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<byte[]> exportDiscrepanciesPdf(
            @RequestHeader(value = "X-User-ICAO", required = false) String icaoCodeHeader,
            @Parameter(description = "Tenant code", required = true)
            @RequestParam(required = false) String tenantCode,

            @Parameter(description = "Start date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime startDate,

            @Parameter(description = "End date")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime endDate,

            @Parameter(description = "Discrepancy type filter")
            @RequestParam(required = false) String discrepancyType,

            @Parameter(description = "Acknowledged filter")
            @RequestParam(required = false) Boolean acknowledged) {
        String resolvedTenantCode = resolveTenantCode(icaoCodeHeader, tenantCode);

        // Get all discrepancies (unpaged) for export
        List<MovementDiscrepancyDTO> discrepancies = discrepancyService.getAllDiscrepanciesForExport(
            resolvedTenantCode, startDate, endDate, discrepancyType, acknowledged);

        String startDateStr = startDate != null ? startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";
        String endDateStr = endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE) : "all";

        byte[] pdfBytes = reportExportService.exportDiscrepanciesToPdf(
            discrepancies, resolvedTenantCode, startDateStr, endDateStr);

        String filename = String.format("movement_discrepancies_%s_%s_to_%s.pdf",
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
