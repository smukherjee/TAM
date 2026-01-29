/**
 * Tracking Service - API methods for zone violations, discrepancies, and movement trails
 * Feature: 005-asset-tracking-security
 * Phase 8: Frontend Services
 */

import api from './api';
import {
    ZoneViolation,
    MovementDiscrepancy,
    MovementTrail,
    TrailSummary,
    RestrictedZone,
    ViolationFilters,
    DiscrepancyFilters,
    PaginatedResponse,
    ViolationStatistics,
    DiscrepancyStatistics,
    AcknowledgeRequest
} from '../types/tracking';

const TRACKING_API = '/tracking';

// ============================================================================
// Zone Violations
// ============================================================================

/**
 * Fetch zone violations with filters and pagination
 */
export const fetchZoneViolations = async (
    filters: ViolationFilters
): Promise<PaginatedResponse<ZoneViolation>> => {
    const params = new URLSearchParams();
    
    if (filters.tenantCode) params.append('tenantCode', filters.tenantCode);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.severity) params.append('severity', filters.severity);
    if (filters.zoneType) params.append('zoneType', filters.zoneType);
    if (filters.assetCategory) params.append('assetCategory', filters.assetCategory);
    if (filters.acknowledged !== undefined) params.append('acknowledged', String(filters.acknowledged));
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));
    if (filters.sort) params.append('sort', filters.sort);

    const response = await api.get(`${TRACKING_API}/violations`, { params });
    return response.data;
};

/**
 * Fetch a single zone violation by ID
 */
export const fetchZoneViolationById = async (violationId: string): Promise<ZoneViolation> => {
    const response = await api.get(`${TRACKING_API}/violations/${violationId}`);
    return response.data;
};

/**
 * Acknowledge a zone violation
 */
export const acknowledgeViolation = async (
    violationId: string,
    request: AcknowledgeRequest
): Promise<ZoneViolation> => {
    const response = await api.post(`${TRACKING_API}/violations/${violationId}/acknowledge`, request);
    return response.data;
};

/**
 * Get violation statistics for a tenant
 */
export const fetchViolationStatistics = async (
    tenantCode: string,
    startDate: string,
    endDate: string
): Promise<ViolationStatistics> => {
    const response = await api.get(`${TRACKING_API}/violations/statistics`, {
        params: { tenantCode, startDate, endDate }
    });
    return response.data;
};

/**
 * Fetch recent unacknowledged violations
 */
export const fetchRecentViolations = async (
    tenantCode: string,
    limit: number = 10
): Promise<ZoneViolation[]> => {
    const response = await api.get(`${TRACKING_API}/violations/recent`, {
        params: { tenantCode, limit }
    });
    return response.data;
};

// ============================================================================
// Movement Discrepancies
// ============================================================================

/**
 * Fetch movement discrepancies with filters and pagination
 */
export const fetchMovementDiscrepancies = async (
    filters: DiscrepancyFilters
): Promise<PaginatedResponse<MovementDiscrepancy>> => {
    const params = new URLSearchParams();
    
    if (filters.tenantCode) params.append('tenantCode', filters.tenantCode);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.discrepancyType) params.append('discrepancyType', filters.discrepancyType);
    if (filters.severity) params.append('severity', filters.severity);
    if (filters.acknowledged !== undefined) params.append('acknowledged', String(filters.acknowledged));
    if (filters.page !== undefined) params.append('page', String(filters.page));
    if (filters.size !== undefined) params.append('size', String(filters.size));
    if (filters.sort) params.append('sort', filters.sort);

    const response = await api.get(`${TRACKING_API}/discrepancies`, { params });
    return response.data;
};

/**
 * Fetch a single discrepancy by ID
 */
export const fetchDiscrepancyById = async (discrepancyId: string): Promise<MovementDiscrepancy> => {
    const response = await api.get(`${TRACKING_API}/discrepancies/${discrepancyId}`);
    return response.data;
};

/**
 * Acknowledge a movement discrepancy
 */
export const acknowledgeDiscrepancy = async (
    discrepancyId: string,
    request: AcknowledgeRequest
): Promise<MovementDiscrepancy> => {
    const response = await api.post(`${TRACKING_API}/discrepancies/${discrepancyId}/acknowledge`, request);
    return response.data;
};

/**
 * Get discrepancy statistics for a tenant
 */
export const fetchDiscrepancyStatistics = async (
    tenantCode: string,
    startDate: string,
    endDate: string
): Promise<DiscrepancyStatistics> => {
    const response = await api.get(`${TRACKING_API}/discrepancies/statistics`, {
        params: { tenantCode, startDate, endDate }
    });
    return response.data;
};

// ============================================================================
// Movement Trail
// ============================================================================

/**
 * Fetch movement trail for an asset within a date range
 */
export const fetchMovementTrail = async (
    assetId: string,
    startDate: string,
    endDate: string
): Promise<MovementTrail> => {
    const response = await api.get(`${TRACKING_API}/trail/${assetId}`, {
        params: { startDate, endDate }
    });
    return response.data;
};

/**
 * Fetch only the trail summary (without points) for performance
 */
export const fetchTrailSummary = async (
    assetId: string,
    startDate: string,
    endDate: string
): Promise<TrailSummary> => {
    const response = await api.get(`${TRACKING_API}/trail/${assetId}/summary`, {
        params: { startDate, endDate }
    });
    return response.data;
};

/**
 * Export movement trail data in specified format
 */
export const exportTrail = async (
    assetId: string,
    startDate: string,
    endDate: string,
    format: 'csv' | 'json' = 'csv'
): Promise<Blob> => {
    const response = await api.get(`${TRACKING_API}/trail/${assetId}/export`, {
        params: { startDate, endDate, format },
        responseType: 'blob'
    });
    return response.data;
};

/**
 * Download trail export file
 */
export const downloadTrailExport = async (
    assetId: string,
    startDate: string,
    endDate: string,
    format: 'csv' | 'json' = 'csv'
): Promise<void> => {
    const blob = await exportTrail(assetId, startDate, endDate, format);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `movement_trail_${assetId}_${new Date().toISOString().split('T')[0]}.${format}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
};

// ============================================================================
// Restricted Zones
// ============================================================================

/**
 * Fetch all restricted zones for a tenant
 */
export const fetchRestrictedZones = async (tenantCode: string): Promise<RestrictedZone[]> => {
    const response = await api.get(`${TRACKING_API}/zones`, {
        params: { tenantCode }
    });
    return response.data;
};

/**
 * Fetch active restricted zones for a tenant
 */
export const fetchActiveRestrictedZones = async (tenantCode: string): Promise<RestrictedZone[]> => {
    const response = await api.get(`${TRACKING_API}/zones`, {
        params: { tenantCode, activeOnly: true }
    });
    return response.data;
};

/**
 * Fetch a single restricted zone by ID
 */
export const fetchRestrictedZoneById = async (zoneId: string): Promise<RestrictedZone> => {
    const response = await api.get(`${TRACKING_API}/zones/${zoneId}`);
    return response.data;
};

/**
 * Create a new restricted zone (ADMIN only)
 */
export const createRestrictedZone = async (zone: Omit<RestrictedZone, 'id' | 'createdAt' | 'updatedAt'>): Promise<RestrictedZone> => {
    const response = await api.post(`${TRACKING_API}/zones`, zone);
    return response.data;
};

/**
 * Update an existing restricted zone (ADMIN only)
 */
export const updateRestrictedZone = async (
    zoneId: string,
    zone: Partial<RestrictedZone>
): Promise<RestrictedZone> => {
    const response = await api.put(`${TRACKING_API}/zones/${zoneId}`, zone);
    return response.data;
};

/**
 * Delete a restricted zone (ADMIN only)
 */
export const deleteRestrictedZone = async (zoneId: string): Promise<void> => {
    await api.delete(`${TRACKING_API}/zones/${zoneId}`);
};

// ============================================================================
// Export Functions
// ============================================================================

/**
 * Export violations to specified format
 */
export const exportViolations = async (
    filters: ViolationFilters,
    format: 'csv' | 'pdf' = 'csv'
): Promise<Blob> => {
    const params = new URLSearchParams();
    
    if (filters.tenantCode) params.append('tenantCode', filters.tenantCode);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.severity) params.append('severity', filters.severity);
    if (filters.acknowledged !== undefined) params.append('acknowledged', String(filters.acknowledged));
    params.append('format', format);

    const response = await api.get(`${TRACKING_API}/violations/export`, {
        params,
        responseType: 'blob'
    });
    return response.data;
};

/**
 * Export discrepancies to specified format
 */
export const exportDiscrepancies = async (
    filters: DiscrepancyFilters,
    format: 'csv' | 'pdf' = 'csv'
): Promise<Blob> => {
    const params = new URLSearchParams();
    
    if (filters.tenantCode) params.append('tenantCode', filters.tenantCode);
    if (filters.startDate) params.append('startDate', filters.startDate);
    if (filters.endDate) params.append('endDate', filters.endDate);
    if (filters.discrepancyType) params.append('discrepancyType', filters.discrepancyType);
    if (filters.acknowledged !== undefined) params.append('acknowledged', String(filters.acknowledged));
    params.append('format', format);

    const response = await api.get(`${TRACKING_API}/discrepancies/export`, {
        params,
        responseType: 'blob'
    });
    return response.data;
};

/**
 * Download violations export file
 */
export const downloadViolationsExport = async (
    filters: ViolationFilters,
    format: 'csv' | 'pdf' = 'csv'
): Promise<void> => {
    const blob = await exportViolations(filters, format);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `zone_violations_${new Date().toISOString().split('T')[0]}.${format}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
};

/**
 * Download discrepancies export file
 */
export const downloadDiscrepanciesExport = async (
    filters: DiscrepancyFilters,
    format: 'csv' | 'pdf' = 'csv'
): Promise<void> => {
    const blob = await exportDiscrepancies(filters, format);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `movement_discrepancies_${new Date().toISOString().split('T')[0]}.${format}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
};
