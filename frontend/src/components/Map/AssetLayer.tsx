import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { fetchLiveAssets } from '../../services/assetLocationService';
import { AssetLocation } from '../../types/assetTracking';
import AssetMarker from '../Tracking/AssetMarker';
import { webSocketService } from '../../services/WebSocketService';

/**
 * Leaflet layer for live asset markers on the Live Map.
 * Fetches assets periodically and applies incremental WebSocket updates.
 */
const AssetLayer: React.FC = () => {
    const { user } = useAuth();
    const tenantCode = user?.icaoCode || 'VIDP';
    const [assets, setAssets] = useState<AssetLocation[]>([]);

    // Initial + periodic fetch
    useEffect(() => {
        let isMounted = true;

        const loadAssets = async () => {
            try {
                const data = await fetchLiveAssets({ tenantCode, pageSize: 500 });
                if (isMounted) {
                    setAssets(data.assets || []);
                }
            } catch (err) {
                console.error('AssetLayer: Failed to fetch assets', err);
            }
        };

        loadAssets();
        const interval = setInterval(loadAssets, 5000);

        return () => {
            isMounted = false;
            clearInterval(interval);
        };
    }, [tenantCode]);

    // WebSocket live updates
    useEffect(() => {
        const topic = `/topic/asset-tracking/${tenantCode}`;

        const subscription = webSocketService.subscribe(topic, (msg: any) => {
            const update: Partial<AssetLocation> & { assetId?: string; assetIdentifier?: string } = msg;
            setAssets(prev => {
                const idx = prev.findIndex(a => a.assetId === update.assetId || a.assetIdentifier === update.assetIdentifier);
                if (idx >= 0) {
                    const next = [...prev];
                    next[idx] = {
                        ...next[idx],
                        latitude: update.latitude ?? next[idx].latitude,
                        longitude: update.longitude ?? next[idx].longitude,
                        speed: update.speed ?? next[idx].speed,
                        heading: update.heading ?? next[idx].heading,
                        status: update.status ?? next[idx].status,
                        lastSeen: (update as any).timestamp ?? next[idx].lastSeen,
                        currentZone: (update as any).currentZone ?? next[idx].currentZone,
                        zoneStatus: (update as any).zoneStatus ?? next[idx].zoneStatus,
                        isMoving: (update as any).speed ? (update as any).speed > 0 : next[idx].isMoving,
                    } as AssetLocation;
                    return next;
                }
                // If we get an update for an unknown asset, ignore for now
                return prev;
            });
        });

        return () => {
            subscription.unsubscribe();
        };
    }, [tenantCode]);

    const handleClick = (_asset: AssetLocation) => {
        // Placeholder for future selection logic
    };

    return (
        <>
            {assets.map(asset => (
                <AssetMarker
                    key={asset.assetId}
                    asset={asset}
                    onClick={handleClick}
                    isSelected={false}
                />
            ))}
        </>
    );
};

export default AssetLayer;
