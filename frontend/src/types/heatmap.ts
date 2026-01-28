/**
 * Heatmap TypeScript Interfaces
 * Feature: 005-asset-tracking-security (Task T056)
 */

/**
 * Heatmap data point for visualization
 */
export interface HeatmapPoint {
    latitude: number;
    longitude: number;
    intensity: number;
    count: number;
    metadata?: {
        assetIds?: string[];
        violationIds?: string[];
        zoneId?: string;
    };
}

/**
 * Hotspot detail information
 */
export interface HotspotDetail {
    location: {
        latitude: number;
        longitude: number;
    };
    mode: HeatmapMode;
    gridSize: number;
    activityCount?: number;
    assets?: HotspotAsset[];
    violations?: HotspotViolation[];
    dwellStats?: DwellStatistics;
    timeDistribution?: TimeDistributionPoint[];
}

/**
 * Asset information in hotspot
 */
export interface HotspotAsset {
    assetId: string;
    assetIdentifier: string;
    name: string;
    category: string;
    count: number;
    avgSpeed?: number;
}

/**
 * Violation information in hotspot
 */
export interface HotspotViolation {
    severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    count: number;
    assetIds: string[];
}

/**
 * Dwell time statistics
 */
export interface DwellStatistics {
    totalDwellTime: number; // seconds
    avgDwellTime: number;   // seconds
    maxDwellTime: number;   // seconds
    assetCount: number;
}

/**
 * Time distribution point (hourly)
 */
export interface TimeDistributionPoint {
    hour: number; // 0-23
    count: number;
}

/**
 * Heatmap filter parameters
 */
export interface HeatmapFilters {
    tenantCode: string;
    startDate: Date;
    endDate: Date;
    gridSize: number;
    zoneId?: string;
    category?: string;
}

/**
 * Heatmap mode enum
 */
export enum HeatmapMode {
    ACTIVITY = 'activity',
    VIOLATION = 'violations',
    DWELL = 'dwell'
}

/**
 * Heatmap statistics summary
 */
export interface HeatmapStatistics {
    totalCells: number;
    hotspotCells: number;
    maxIntensity: number;
    avgIntensity: number;
    medianIntensity?: number;
    stdDevIntensity?: number;
}

/**
 * Heatmap API response
 */
export interface HeatmapResponse {
    gridSize: number;
    startTime: string;
    endTime: string;
    totalPoints: number;
    maxIntensity: number;
    data: HeatmapPoint[];
    statistics?: HeatmapStatistics;
}

/**
 * Export format options
 */
export type ExportFormat = 'png' | 'csv' | 'pdf';

/**
 * Export options
 */
export interface ExportOptions {
    format: ExportFormat;
    filename?: string;
    includeMetadata?: boolean;
    includeMap?: boolean;
    includeStatistics?: boolean;
}
