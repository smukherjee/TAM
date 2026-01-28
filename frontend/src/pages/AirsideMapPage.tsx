import React, { useState, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import UniversalAssetMap from '../components/Tracking/UniversalAssetMap';
import AssetFilterPanel from '../components/Tracking/AssetFilterPanel';
import { useAuth } from '../context/AuthContext';
import { webSocketService } from '../services/WebSocketService';
import api from '../services/api';
import { Loader2 } from 'lucide-react';
import { AssetLocation, AssetLocationResponse, AssetFilters } from '../types/assetTracking';

/**
 * Airside Map Page - Universal Asset Map (US5)
 * Feature: 005-asset-tracking-security
 * Task: T036
 */
const AirsideMapPage: React.FC = () => {
    const { user } = useAuth();
    const tenantCode = user?.icaoCode || 'VIDP';
    
    const [filters, setFilters] = useState<AssetFilters>({});
    const [assets, setAssets] = useState<AssetLocation[]>([]);
    const [isFilterPanelOpen, setIsFilterPanelOpen] = useState(true);

    // Fetch live assets using React Query
    const { data, isLoading, error, refetch } = useQuery({
        queryKey: ['liveAssets', tenantCode, filters],
        queryFn: async () => {
            const params = new URLSearchParams({
                tenantCode,
                ...(filters.category && { category: filters.category }),
                ...(filters.status && { status: filters.status }),
                ...(filters.zoneId && { zoneId: filters.zoneId }),
                pageSize: '500'
            });
            
            const response = await api.get<AssetLocationResponse>(
                `/api/tracking/assets/live?${params}`
            );
            return response.data;
        },
        refetchInterval: 5000, // Refetch every 5 seconds
        staleTime: 5000
    });

    // Update assets when data changes
    useEffect(() => {
        if (data?.assets) {
            setAssets(data.assets);
        }
    }, [data]);

    // WebSocket subscription for real-time updates
    useEffect(() => {
        const handleAssetUpdate = (update: any) => {
            setAssets(prev => {
                const index = prev.findIndex(a => a.assetId === update.assetId);
                if (index !== -1) {
                    // Update existing asset
                    const updated = [...prev];
                    updated[index] = {
                        ...updated[index],
                        latitude: update.latitude,
                        longitude: update.longitude,
                        speed: update.speed,
                        heading: update.heading,
                        status: update.status,
                        lastSeen: update.timestamp
                    };
                    return updated;
                }
                return prev;
            });
        };

        webSocketService.connect(() => {
            webSocketService.subscribe(
                `/topic/asset-tracking/${tenantCode}`,
                handleAssetUpdate
            );
        });

        return () => {
            webSocketService.disconnect();
        };
    }, [tenantCode]);

    const handleFilterChange = (newFilters: AssetFilters) => {
        setFilters(newFilters);
    };

    if (error) {
        return (
            <div className="flex items-center justify-center h-screen">
                <div className="text-center">
                    <h2 className="text-2xl font-bold text-red-600 mb-2">Error Loading Assets</h2>
                    <p className="text-gray-600 mb-4">
                        {error instanceof Error ? error.message : 'Unknown error occurred'}
                    </p>
                    <button
                        onClick={() => refetch()}
                        className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
                    >
                        Retry
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="relative h-screen w-full overflow-hidden">
            {/* Loading Spinner */}
            {isLoading && (
                <div className="absolute top-4 left-1/2 transform -translate-x-1/2 z-[1000] bg-white rounded-lg shadow-lg px-4 py-2 flex items-center gap-2">
                    <Loader2 className="w-5 h-5 animate-spin text-blue-600" />
                    <span className="text-sm font-medium">Loading assets...</span>
                </div>
            )}

            {/* Filter Panel */}
            <AssetFilterPanel
                isOpen={isFilterPanelOpen}
                onToggle={() => setIsFilterPanelOpen(!isFilterPanelOpen)}
                onFilterChange={handleFilterChange}
                assetCount={assets.length}
                totalCount={data?.totalCount || 0}
            />

            {/* Map Component */}
            <div className={`h-full transition-all duration-300 ${isFilterPanelOpen ? 'ml-80' : 'ml-0'}`}>
                <UniversalAssetMap
                    assets={assets}
                    tenantCode={tenantCode}
                    center={getMapCenter(tenantCode)}
                    zoom={14}
                />
            </div>
        </div>
    );
};

/**
 * Get map center coordinates based on tenant
 */
function getMapCenter(tenantCode: string): [number, number] {
    const centers: Record<string, [number, number]> = {
        'VIDP': [28.5562, 77.1000], // Delhi
        'LIRN': [40.8844, 14.2908], // Naples
        'YBBN': [-27.3842, 153.1175] // Brisbane
    };
    return centers[tenantCode] || centers['VIDP'];
}

export default AirsideMapPage;
