/**
 * Asset Location Types - Shared interfaces for asset tracking
 * Feature: 005-asset-tracking-security
 */

export interface AssetLocation {
    assetId: string;
    assetIdentifier: string;
    name: string;
    category: string;
    latitude: number;
    longitude: number;
    status: string;
    currentZone?: string;
    currentZoneType?: string;
    zoneStatus: string;
    speed?: number;
    heading?: number;
    lastSeen: string;
    tenantCode: string;
    qrId?: string;
    value?: number;
    description?: string;
    isMoving: boolean;
    hasViolation: boolean;
    categoryColor: string;
    owner?: string;
}

export interface AssetLocationResponse {
    assets: AssetLocation[];
    totalCount: number;
    count: number;
    page: number;
    pageSize: number;
    totalPages: number;
}

export interface AssetFilters {
    category?: string;
    status?: string;
    zoneId?: string;
    groundHandler?: string;
}

/**
 * Live Asset Update Event (WebSocket payload)
 */
export interface AssetPositionUpdateEvent {
    assetId: string;
    latitude: number;
    longitude: number;
    speed?: number;
    heading?: number;
    status: string;
    timestamp: string;
    tenantCode: string;
    currentZone?: string;
    zoneStatus?: string;
}

/**
 * Restricted Zone Boundary Definition
 */
export interface ZoneBoundary {
    id: string;
    name: string;
    type: 'PROHIBITED' | 'RESTRICTED' | 'CONTROLLED' | 'MAINTENANCE';
    description?: string;
    geometry: GeoJSON.Polygon;
    active: boolean;
    tenantCode: string;
    createdAt?: string;
    updatedAt?: string;
}

/**
 * Zone Violation Event
 */
export interface ZoneViolation {
    id: string;
    assetId: string;
    assetIdentifier: string;
    zoneId: string;
    zoneName: string;
    zoneType: string;
    violationType: 'ENTRY' | 'UNAUTHORIZED_MOVEMENT' | 'LOITERING';
    timestamp: string;
    latitude: number;
    longitude: number;
    severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    resolved: boolean;
    tenantCode: string;
}

/**
 * Asset Tracking Hotspot Data
 */
export interface HotspotData {
    latitude: number;
    longitude: number;
    intensity: number;
    assetCount: number;
    timestamp: string;
}

/**
 * Asset Movement History Point
 */
export interface MovementHistoryPoint {
    latitude: number;
    longitude: number;
    timestamp: string;
    speed?: number;
    heading?: number;
    zone?: string;
}

/**
 * Category color mapping (from research.md)
 */
export const CATEGORY_COLORS: Record<string, string> = {
    'Emergency': '#EF4444',      // Red
    'Fueling': '#F97316',         // Orange
    'Cargo': '#3B82F6',           // Blue
    'Ground Support': '#10B981',  // Green
    'Transport': '#8B5CF6',       // Purple
    'Power': '#EAB308',           // Yellow
    'Services': '#14B8A6',        // Teal
    'Other': '#6B7280'            // Gray
};

/**
 * Status color mapping
 */
export const STATUS_COLORS: Record<string, string> = {
    'In Use': '#10B981',          // Green
    'Available': '#3B82F6',       // Blue
    'Maintenance': '#F59E0B',     // Amber
    'Out of Service': '#EF4444'   // Red
};
