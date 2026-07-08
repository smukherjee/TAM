import React, { useEffect, useState, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import MapComponent from '../components/Map/MapComponent';
import FlightLayer from '../components/Map/FlightLayer';
import VehicleLayer from '../components/Map/VehicleLayer';
import AssetMarkersLayer from '../components/Tracking/AssetMarkersLayer';
import ZoneBoundariesLayer from '../components/Tracking/ZoneBoundariesLayer';
import MapToolbar from '../components/Map/MapToolbar';
import FilterDrawer from '../components/Map/FilterDrawer';
import CompactLegend from '../components/Map/CompactLegend';
import EnhancedAlertList from '../components/EnhancedAlertList';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import { webSocketService } from '../services/WebSocketService';
import { Vehicle } from '../services/vehicleService';
import { AssetLocation, AssetLocationResponse, AssetFilters } from '../types/assetTracking';
import { Loader2 } from 'lucide-react';

interface Alert {
    alertId: string;
    type: string;
    entityId: string;
    value: number;
    timestamp: string;
    latitude: number;
    longitude: number;
}

interface LayerState {
    flights: boolean;
    vehicles: boolean;
    assets: boolean;
    heatmap?: boolean;
    zones: boolean;
    alerts: boolean;
}

/**
 * UnifiedMapPage - Single operations map with all layers
 * Clean UX with floating toolbar and slide-out filters
 */
const UnifiedMapPage: React.FC = () => {
    const { user } = useAuth();
    const tenantCode = user?.icaoCode || 'VIDP';
    
    // Layer visibility state
    const [layers, setLayers] = useState<LayerState>({
        flights: true,
        vehicles: true,
        assets: true,
        zones: true,
        alerts: true
    });

    // Data state
    const [vehicles, setVehicles] = useState<Vehicle[]>([]);
    const [alerts, setAlerts] = useState<Alert[]>([]);
    const [assets, setAssets] = useState<AssetLocation[]>([]);
    
    // Filters and UI state
    const [assetFilters, setAssetFilters] = useState<AssetFilters>({});
    const [isFilterDrawerOpen, setIsFilterDrawerOpen] = useState(false);
    const { theme: mapTheme, toggleTheme: toggleMapTheme } = useTheme();

    const getCenter = (): [number, number] => {
        if (tenantCode === 'VABB') return [19.0896, 72.8656];
        if (tenantCode === 'LIRN') return [40.8844, 14.2908];
        if (tenantCode === 'YBBN') return [-27.3842, 153.1175];
        return [28.5562, 77.1000]; // Default VIDP
    };

    // Fetch vehicles
    const fetchVehicles = async () => {
        try {
            const response = await api.get<Vehicle[]>('/vehicles');
            if (response.data && Array.isArray(response.data)) {
                setVehicles(response.data);
            }
        } catch (error) {
            console.error('Error fetching vehicles:', error);
        }
    };

    // Fetch alerts
    const fetchAlerts = async () => {
        try {
            const response = await api.get<Alert[]>('/vehicle-alerts');
            if (response.data && Array.isArray(response.data)) {
                setAlerts(response.data);
            }
        } catch (error) {
            console.error('Error fetching alerts:', error);
        }
    };

    // Fetch assets using React Query
    const { data: assetData, isLoading: assetsLoading } = useQuery({
        queryKey: ['liveAssets', tenantCode, assetFilters],
        queryFn: async () => {
            // GH users are always scoped to their own company, regardless of drawer state (UI-only scoping).
            const effectiveGroundHandler = user?.role === 'GH' ? user.company : assetFilters.groundHandler;
            const params = new URLSearchParams({
                tenantCode,
                ...(assetFilters.category && { category: assetFilters.category }),
                ...(assetFilters.status && { status: assetFilters.status }),
                ...(assetFilters.zoneId && { zoneId: assetFilters.zoneId }),
                ...(effectiveGroundHandler && { groundHandler: effectiveGroundHandler }),
                pageSize: '500'
            });
            
            const response = await api.get<AssetLocationResponse>(
                `/tracking/assets/live?${params}`
            );
            return response.data;
        },
        refetchInterval: 5000,
        staleTime: 5000
    });

    // Update assets when data changes
    useEffect(() => {
        if (assetData?.assets && Array.isArray(assetData.assets)) {
            setAssets(assetData.assets);
        }
    }, [assetData]);

    // Vehicles are the same physical entities as filtered assets (assets.asset_id ===
    // vehicle.vehicle_no, see vehicle_asset_map). Gate the vehicle layer by the already
    // filtered+scoped asset list so category/status/ground-handler filters actually
    // restrict the map instead of only affecting the asset markers. Query above always
    // runs (not gated on layers.assets) so this stays correct even with the Assets layer off.
    const visibleVehicles = useMemo(() => {
        const visibleIds = new Set(assets.map(a => a.assetIdentifier));
        return vehicles.filter(v => visibleIds.has(v.vehicle_no));
    }, [vehicles, assets]);

    // WebSocket subscriptions
    useEffect(() => {
        fetchVehicles();
        fetchAlerts();

        const handleVehicleUpdate = (vehicle: Vehicle) => {
            setVehicles(prev => {
                if (!prev || !Array.isArray(prev)) return [vehicle];
                const index = prev.findIndex(v => v.vehicle_no === vehicle.vehicle_no);
                if (index !== -1) {
                    const newVehicles = [...prev];
                    newVehicles[index] = vehicle;
                    return newVehicles;
                }
                return [...prev, vehicle];
            });
        };

        const handleAlertUpdate = (alert: Alert) => {
            setAlerts(prev => {
                if (!prev || !Array.isArray(prev)) return [alert];
                return [alert, ...prev].slice(0, 50);
            });
        };

        const handleAssetUpdate = (update: any) => {
            setAssets(prev => {
                if (!prev || !Array.isArray(prev)) return prev;
                const index = prev.findIndex(a => a.assetId === update.assetId);
                if (index !== -1) {
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
            webSocketService.subscribe('/topic/vehicles/' + tenantCode, handleVehicleUpdate);
            webSocketService.subscribe('/topic/alerts/' + tenantCode, handleAlertUpdate);
            webSocketService.subscribe(`/topic/asset-tracking/${tenantCode}`, handleAssetUpdate);
        });

        return () => {
            webSocketService.disconnect();
        };
    }, [tenantCode]);

    // Handle layer toggle
    const handleLayerToggle = (layer: keyof LayerState) => {
        setLayers(prev => ({ ...prev, [layer]: !prev[layer] }));
    };

    // Handle alert dismiss
    const handleAlertDismiss = (alertId: string) => {
        setAlerts(prev => prev.filter(a => a.alertId !== alertId));
    };

    // Transform alerts for display
    const enhancedAlerts = useMemo(() => {
        if (!alerts || !Array.isArray(alerts)) return [];
        return alerts.map(a => ({
            id: a.alertId,
            vehicleId: a.entityId,
            message: `${a.type}: ${a.value}`,
            severity: a.type?.toLowerCase().includes('critical') ? 'critical' as const :
                      a.type?.toLowerCase().includes('warning') ? 'warning' as const : 'info' as const,
            timestamp: new Date(a.timestamp)
        }));
    }, [alerts]);

    const isLoading = assetsLoading;

    return (
        <div className="h-full w-full relative">
            {/* Loading indicator */}
            {isLoading && (
                <div className="absolute top-20 left-1/2 transform -translate-x-1/2 z-[1100] bg-gray-900/90 rounded-lg shadow-lg px-4 py-2 flex items-center gap-2 border border-gray-700">
                    <Loader2 className="w-4 h-4 animate-spin text-blue-400" />
                    <span className="text-sm text-gray-300">Loading...</span>
                </div>
            )}

            {/* Map Toolbar - Centered at top */}
            <MapToolbar
                layers={layers}
                onLayerToggle={handleLayerToggle}
                onOpenFilters={() => setIsFilterDrawerOpen(true)}
                alertCount={enhancedAlerts.length}
                showHeatmapToggle={false}
                mapTheme={mapTheme}
                onMapThemeToggle={toggleMapTheme}
            />

            {/* Filter Drawer */}
            <FilterDrawer
                isOpen={isFilterDrawerOpen}
                onClose={() => setIsFilterDrawerOpen(false)}
                filters={assetFilters}
                onFilterChange={setAssetFilters}
                assetCount={assets.length}
                totalCount={assetData?.totalCount || 0}
                tenantCode={tenantCode}
                userRole={user?.role}
                userCompany={user?.company}
            />

            {/* Main Map */}
            <MapComponent center={getCenter()} zoom={14} theme={mapTheme}>
                {/* Zone Boundaries */}
                {layers.zones && <ZoneBoundariesLayer />}
                
                {/* Entity Layers */}
                {layers.flights && <FlightLayer />}
                {layers.vehicles && visibleVehicles.length > 0 && (
                    <VehicleLayer vehicles={visibleVehicles} />
                )}
                {layers.assets && assets && Array.isArray(assets) && assets.length > 0 && (
                    <AssetMarkersLayer assets={assets} />
                )}
            </MapComponent>

            {/* Compact Legend */}
            <CompactLegend
                showFlights={layers.flights}
                showVehicles={layers.vehicles}
                showAssets={layers.assets}
            />

            {/* Alert List */}
            {layers.alerts && enhancedAlerts.length > 0 && (
                <EnhancedAlertList
                    alerts={enhancedAlerts}
                    onDismiss={handleAlertDismiss}
                />
            )}
        </div>
    );
};

export default UnifiedMapPage;
