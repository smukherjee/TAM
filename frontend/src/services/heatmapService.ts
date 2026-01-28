import api from './api';

/**
 * Heatmap Service - API calls for hotspot analysis
 * Feature: 005-asset-tracking-security (Task T055)
 */

export interface HeatmapFilters {
    tenantCode: string;
    gridSize: number; // Grid cell size in meters (10, 25, 50, 100)
    startTime: string; // ISO 8601 datetime
    endTime: string;   // ISO 8601 datetime
    zoneId?: string;   // Optional: filter by specific zone
}

export interface HeatmapMetadata {
    activityCount?: number;
    uniqueAssets?: number;
    avgSpeed?: number;
    maxSpeed?: number;
    criticalCount?: number;
    highCount?: number;
    mediumCount?: number;
    lowCount?: number;
    firstActivity?: string;
    lastActivity?: string;
}

export interface HeatmapDataPoint {
    latitude: number;
    longitude: number;
    intensity: number;
    metadata?: HeatmapMetadata;
}

export interface HeatmapResponse {
    gridSize: number;
    startTime: string;
    endTime: string;
    totalPoints: number;
    maxIntensity: number;
    data: HeatmapDataPoint[];
}

export interface HotspotDetail {
    latitude: number;
    longitude: number;
    gridSize: number;
    mode: 'activity' | 'violations' | 'dwell';
    totalCount: number;
    intensity: number;
    assets?: HotspotAsset[];
    violations?: HotspotViolation[];
    dwellStats?: DwellStats;
    timeDistribution?: TimeDistributionPoint[];
}

export interface HotspotAsset {
    assetId: string;
    assetIdentifier: string;
    name: string;
    category: string;
    count: number; // Number of times in this cell
    avgSpeed?: number;
}

export interface HotspotViolation {
    severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
    count: number;
    assetIds: string[];
}

export interface DwellStats {
    totalDwellTime: number; // seconds
    avgDwellTime: number;   // seconds
    maxDwellTime: number;
    assetCount: number;
}

export interface TimeDistributionPoint {
    hour: number; // 0-23
    count: number;
}

/**
 * Fetch activity heatmap data
 */
export const fetchActivityHeatmap = async (
    filters: HeatmapFilters
): Promise<HeatmapResponse> => {
    try {
        const params = new URLSearchParams({
            tenantCode: filters.tenantCode,
            gridSize: `${filters.gridSize}m`,
            startTime: filters.startTime,
            endTime: filters.endTime
        });
        
        if (filters.zoneId) {
            params.append('zoneId', filters.zoneId);
        }

        const response = await api.get<HeatmapDataPoint[]>(
            `/tracking/heatmap/activity?${params.toString()}`
        );
        
        // Backend returns array directly, transform to HeatmapResponse
        const data = Array.isArray(response.data) ? response.data : [];
        const maxIntensity = data.length > 0 
            ? Math.max(...data.map(d => d.metadata?.activityCount || 0))
            : 0;
        
        return {
            gridSize: filters.gridSize,
            startTime: filters.startTime,
            endTime: filters.endTime,
            totalPoints: data.length,
            maxIntensity,
            data: data.map(point => ({
                latitude: point.latitude,
                longitude: point.longitude,
                intensity: point.metadata?.activityCount || 0,
                metadata: point.metadata
            }))
        };
    } catch (error) {
        console.error('Failed to fetch activity heatmap:', error);
        throw error;
    }
};

/**
 * Fetch violations heatmap data
 */
export const fetchViolationsHeatmap = async (
    filters: HeatmapFilters
): Promise<HeatmapResponse> => {
    try {
        const params = new URLSearchParams({
            tenantCode: filters.tenantCode,
            gridSize: `${filters.gridSize}m`,
            startTime: filters.startTime,
            endTime: filters.endTime
        });
        
        if (filters.zoneId) {
            params.append('zoneId', filters.zoneId);
        }

        const response = await api.get<HeatmapDataPoint[]>(
            `/tracking/heatmap/violations?${params.toString()}`
        );
        
        // Backend returns array directly, transform to HeatmapResponse
        const data = Array.isArray(response.data) ? response.data : [];
        const maxIntensity = data.length > 0 
            ? Math.max(...data.map(d => d.metadata?.activityCount || 0))
            : 0;
        
        return {
            gridSize: filters.gridSize,
            startTime: filters.startTime,
            endTime: filters.endTime,
            totalPoints: data.length,
            maxIntensity,
            data: data.map(point => ({
                latitude: point.latitude,
                longitude: point.longitude,
                intensity: point.metadata?.activityCount || 0,
                metadata: point.metadata
            }))
        };
    } catch (error) {
        console.error('Failed to fetch violations heatmap:', error);
        throw error;
    }
};

/**
 * Fetch dwell time heatmap data
 */
export const fetchDwellHeatmap = async (
    filters: HeatmapFilters
): Promise<HeatmapResponse> => {
    try {
        const params = new URLSearchParams({
            tenantCode: filters.tenantCode,
            gridSize: `${filters.gridSize}m`,
            startTime: filters.startTime,
            endTime: filters.endTime
        });
        
        if (filters.zoneId) {
            params.append('zoneId', filters.zoneId);
        }

        const response = await api.get<HeatmapDataPoint[]>(
            `/tracking/heatmap/dwell?${params.toString()}`
        );
        
        // Backend returns array directly, transform to HeatmapResponse
        const data = Array.isArray(response.data) ? response.data : [];
        const maxIntensity = data.length > 0 
            ? Math.max(...data.map(d => d.metadata?.activityCount || 0))
            : 0;
        
        return {
            gridSize: filters.gridSize,
            startTime: filters.startTime,
            endTime: filters.endTime,
            totalPoints: data.length,
            maxIntensity,
            data: data.map(point => ({
                latitude: point.latitude,
                longitude: point.longitude,
                intensity: point.metadata?.activityCount || 0,
                metadata: point.metadata
            }))
        };
    } catch (error) {
        console.error('Failed to fetch dwell heatmap:', error);
        throw error;
    }
};

/**
 * Fetch hotspot detail for a specific location
 */
export const fetchHotspotDetail = async (
    latitude: number,
    longitude: number,
    gridSize: number,
    mode: 'activity' | 'violations' | 'dwell',
    tenantCode: string,
    startTime: string,
    endTime: string
): Promise<HotspotDetail> => {
    try {
        const params = new URLSearchParams({
            latitude: String(latitude),
            longitude: String(longitude),
            gridSize: String(gridSize),
            mode,
            tenantCode,
            startTime,
            endTime
        });

        const response = await api.get<HotspotDetail>(
            `/tracking/heatmap/hotspot?${params.toString()}`
        );
        return response.data;
    } catch (error) {
        console.error('Failed to fetch hotspot detail:', error);
        throw error;
    }
};

/**
 * Transform HeatmapResponse to Leaflet.heat format
 * Leaflet.heat expects: [[lat, lng, intensity], ...]
 */
export const transformToLeafletHeatFormat = (
    heatmapData: HeatmapDataPoint[] | undefined,
    maxIntensity: number
): [number, number, number][] => {
    if (!heatmapData || !Array.isArray(heatmapData) || heatmapData.length === 0) {
        return [];
    }
    
    // Prevent division by zero
    const normalizer = maxIntensity > 0 ? maxIntensity : 1;
    
    return heatmapData.map(point => [
        point.latitude,
        point.longitude,
        point.intensity / normalizer // Normalize to 0-1
    ]);
};

/**
 * Export heatmap data as CSV
 */
export const exportHeatmapCSV = (
    heatmapData: HeatmapDataPoint[]
): string => {
    if (!heatmapData || !Array.isArray(heatmapData)) {
        return 'Latitude,Longitude,Intensity,Count\n';
    }
    
    const headers = ['Latitude', 'Longitude', 'Intensity', 'Count'];
    const rows = heatmapData.map(point => [
        point.latitude.toFixed(6),
        point.longitude.toFixed(6),
        point.intensity.toFixed(2),
        (point.metadata?.activityCount || 0).toString()
    ]);

    const csvLines = [
        headers.join(','),
        ...rows.map(row => row.join(','))
    ];

    return csvLines.join('\n');
};

export default {
    fetchActivityHeatmap,
    fetchViolationsHeatmap,
    fetchDwellHeatmap,
    fetchHotspotDetail,
    transformToLeafletHeatFormat,
    exportHeatmapCSV
};
