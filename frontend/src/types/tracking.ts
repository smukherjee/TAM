/**
 * Tracking Types - Interfaces for zone violations, discrepancies, and movement trails
 * Feature: 005-asset-tracking-security
 * Phase 8: Frontend Services
 */

/**
 * Zone Violation - Security violation when unauthorized asset enters restricted zone
 */
export interface ZoneViolation {
    id: string;
    violationId: string;
    assetId: string;
    assetIdentifier: string;
    assetName?: string;
    assetCategory?: string;
    restrictedZoneId: string;
    zoneName: string;
    zoneType: 'PROHIBITED' | 'RESTRICTED' | 'CONTROLLED' | 'MAINTENANCE';
    entryLatitude?: number;
    entryLongitude?: number;
    severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    durationSeconds?: number;
    timestamp: string;
    acknowledged: boolean;
    acknowledgedBy?: string;
    acknowledgedAt?: string;
    resolutionNotes?: string;
    tenantCode: string;
}

/**
 * Movement Discrepancy - Detected anomaly in asset movement
 */
export interface MovementDiscrepancy {
    id: string;
    discrepancyId: string;
    assetId: string;
    assetIdentifier: string;
    assetName?: string;
    assetCategory?: string;
    discrepancyType: DiscrepancyType;
    expectedLatitude?: number;
    expectedLongitude?: number;
    actualLatitude?: number;
    actualLongitude?: number;
    deviationMeters?: number;
    expectedSpeed?: number;
    actualSpeed?: number;
    expectedStatus?: string;
    actualStatus?: string;
    description?: string;
    severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    timestamp: string;
    acknowledged: boolean;
    acknowledgedBy?: string;
    acknowledgedAt?: string;
    resolutionNotes?: string;
    tenantCode: string;
}

export type DiscrepancyType = 
    | 'UNEXPECTED_MOVEMENT'
    | 'STATUS_MISMATCH'
    | 'SPEED_ANOMALY'
    | 'GHOST_ASSET'
    | 'LOCATION_JUMP';

/**
 * Movement Trail - Complete movement history for an asset
 */
export interface MovementTrail {
    assetId: string;
    assetIdentifier: string;
    assetName?: string;
    assetCategory?: string;
    startDate: string;
    endDate: string;
    points: MovementTrailPoint[];
    zoneEntries: ZoneEntry[];
    summary: TrailSummary;
    tenantCode: string;
}

/**
 * Individual point in a movement trail
 */
export interface MovementTrailPoint {
    id: number;
    latitude: number;
    longitude: number;
    speed?: number;
    heading?: number;
    status?: string;
    timestamp: string;
    currentZone?: string;
    currentZoneName?: string;
    zoneType?: string;
    inRestrictedZone: boolean;
    metadata?: Record<string, unknown>;
}

/**
 * Zone Entry - When asset entered/exited a zone
 */
export interface ZoneEntry {
    zoneName: string;
    zoneType: string;
    entryTime: string;
    entryLatitude?: number;
    entryLongitude?: number;
    exitTime?: string;
    dwellTimeSeconds?: number;
    dwellTimeMinutes?: number;
    wasAuthorized: boolean;
}

/**
 * Trail Summary - Statistics for a movement trail (matches backend TrailSummaryDTO)
 */
export interface TrailSummary {
    totalPoints?: number;
    totalDistanceMeters?: number;
    totalDurationSeconds?: number;
    averageSpeedKmh?: number;
    maxSpeedKmh?: number;
    zonesEntered?: number;
    restrictedZonesEntered?: number;
    violationsCount?: number;
    totalDwellTimeSeconds?: number;
    dwellTimeByZone?: Record<string, number>;
    statusBreakdown?: Record<string, number>;
}

/**
 * Restricted Zone - Boundary definition for security zones
 */
export interface RestrictedZone {
    id: string;
    zoneId: string;
    zoneName: string;
    zoneType: 'PROHIBITED' | 'RESTRICTED' | 'CONTROLLED' | 'MAINTENANCE';
    description?: string;
    boundaryCoordinates: number[][];  // [[lng, lat], ...]
    authorizedAssetCategories?: string[];
    isActive: boolean;
    effectiveFrom?: string;
    effectiveTo?: string;
    tenantCode: string;
    createdAt?: string;
    updatedAt?: string;
}

/**
 * Filter options for zone violations query
 */
export interface ViolationFilters {
    tenantCode: string;
    startDate?: string;
    endDate?: string;
    severity?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    zoneType?: string;
    assetCategory?: string;
    acknowledged?: boolean;
    page?: number;
    size?: number;
    sort?: string;
}

/**
 * Filter options for movement discrepancies query
 */
export interface DiscrepancyFilters {
    tenantCode: string;
    startDate?: string;
    endDate?: string;
    discrepancyType?: DiscrepancyType;
    severity?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    acknowledged?: boolean;
    page?: number;
    size?: number;
    sort?: string;
}

/**
 * Paginated response wrapper
 */
export interface PaginatedResponse<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    size: number;
    number: number;
    first: boolean;
    last: boolean;
}

/**
 * Violation Statistics
 */
export interface ViolationStatistics {
    total: number;
    unacknowledged: number;
    bySeverity: Record<string, number>;
    tenantCode: string;
    startDate: string;
    endDate: string;
}

/**
 * Discrepancy Statistics
 */
export interface DiscrepancyStatistics {
    total: number;
    unacknowledged: number;
    byType: Record<string, number>;
    tenantCode: string;
    startDate: string;
    endDate: string;
}

/**
 * Acknowledge Request payload
 */
export interface AcknowledgeRequest {
    notes?: string;
}

/**
 * Severity color mapping for UI
 */
export const SEVERITY_COLORS: Record<string, string> = {
    'CRITICAL': '#DC2626',  // Red 600
    'HIGH': '#EA580C',      // Orange 600
    'MEDIUM': '#CA8A04',    // Yellow 600
    'LOW': '#16A34A'        // Green 600
};

/**
 * Zone type color mapping for UI
 */
export const ZONE_TYPE_COLORS: Record<string, string> = {
    'PROHIBITED': 'rgba(255, 0, 0, 0.3)',
    'RESTRICTED': 'rgba(255, 165, 0, 0.3)',
    'CONTROLLED': 'rgba(255, 255, 0, 0.3)',
    'MAINTENANCE': 'rgba(0, 0, 255, 0.3)'
};

/**
 * Discrepancy type display names
 */
export const DISCREPANCY_TYPE_LABELS: Record<DiscrepancyType, string> = {
    'UNEXPECTED_MOVEMENT': 'Unexpected Movement',
    'STATUS_MISMATCH': 'Status Mismatch',
    'SPEED_ANOMALY': 'Speed Anomaly',
    'GHOST_ASSET': 'Ghost Asset',
    'LOCATION_JUMP': 'Location Jump'
};
