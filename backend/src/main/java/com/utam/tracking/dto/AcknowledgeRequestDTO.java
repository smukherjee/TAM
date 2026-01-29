package com.utam.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for acknowledge requests (violations and discrepancies).
 * Feature: 005-asset-tracking-security
 * Task: T055
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcknowledgeRequestDTO {
    
    @NotBlank(message = "Resolution notes are required")
    private String resolutionNotes;
}
