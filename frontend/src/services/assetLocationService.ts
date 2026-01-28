import api from './api';
import { AssetLocation, AssetLocationResponse } from '../types/assetTracking';
import { webSocketService } from './WebSocketService';

/**
 * Asset Location Service
 * Handles API calls for live asset tracking
 * Feature: 005-asset-tracking-security (Task T045)
 */

export interface AssetFilter {
    tenantCode: string;
    category?: string;
    status?: string;
    zoneId?: string;
    page?: number;
    pageSize?: number;
}

/**
 * Fetch all live assets with optional filters
 */
export const fetchLiveAssets = async (filters: AssetFilter): Promise<AssetLocationResponse> => {
    const params = new URLSearchParams();
    
    params.append('tenantCode', filters.tenantCode);
    
    if (filters.category) {
        params.append('category', filters.category);
    }
    if (filters.status) {
        params.append('status', filters.status);
    }
    if (filters.zoneId) {
        params.append('zoneId', filters.zoneId);
    }
    if (filters.page) {
        params.append('page', String(filters.page));
    }
    if (filters.pageSize) {
        params.append('pageSize', String(filters.pageSize));
    }

    try {
        const response = await api.get<AssetLocationResponse>(
            `/tracking/assets/live?${params.toString()}`
        );
        return response.data;
    } catch (error) {
        console.error('Failed to fetch live assets:', error);
        throw error;
    }
};

/**
 * Fetch a single asset by ID
 */
export const fetchAssetById = async (
    assetId: string,
    tenantCode: string
): Promise<AssetLocation> => {
    try {
        const response = await api.get<AssetLocation>(
            `/tracking/assets/live/${assetId}?tenantCode=${tenantCode}`
        );
        return response.data;
    } catch (error) {
        console.error(`Failed to fetch asset ${assetId}:`, error);
        throw error;
    }
};

/**
 * Fetch assets in a specific zone
 */
export const fetchAssetsInZone = async (
    zoneId: string,
    tenantCode: string
): Promise<AssetLocation[]> => {
    try {
        const response = await api.get<AssetLocation[]>(
            `/tracking/assets/live/zone/${zoneId}?tenantCode=${tenantCode}`
        );
        return response.data;
    } catch (error) {
        console.error(`Failed to fetch assets in zone ${zoneId}:`, error);
        throw error;
    }
};

/**
 * Fetch assets by category
 */
export const fetchAssetsByCategory = async (
    category: string,
    tenantCode: string
): Promise<AssetLocation[]> => {
    try {
        const response = await api.get<AssetLocation[]>(
            `/tracking/assets/live/category/${category}?tenantCode=${tenantCode}`
        );
        return response.data;
    } catch (error) {
        console.error(`Failed to fetch assets by category ${category}:`, error);
        throw error;
    }
};

/**
 * Subscribe to live asset updates via WebSocket
 * @param tenantCode Tenant code to subscribe to
 * @param callback Function to call when updates arrive
 * @returns Unsubscribe function
 */
export const subscribeToLiveUpdates = (
    tenantCode: string,
    callback: (update: any) => void
): (() => void) => {
    const topic = `/topic/asset-tracking/${tenantCode}`;
    
    let unsubscribeFn: (() => void) | null = null;
    
    // Connect if not already connected
    webSocketService.connect(() => {
        const subscription = webSocketService.subscribe(topic, callback);
        unsubscribeFn = subscription.unsubscribe;
    });

    // Return unsubscribe function
    return () => {
        if (unsubscribeFn) {
            unsubscribeFn();
        }
    };
};

/**
 * Custom hook for live asset updates (React)
 */
export const useAssetLiveUpdates = (
    tenantCode: string,
    onUpdate: (update: any) => void
) => {
    return {
        subscribe: () => subscribeToLiveUpdates(tenantCode, onUpdate),
        disconnect: () => webSocketService.disconnect()
    };
};

export default {
    fetchLiveAssets,
    fetchAssetById,
    fetchAssetsInZone,
    fetchAssetsByCategory,
    subscribeToLiveUpdates,
    useAssetLiveUpdates
};
