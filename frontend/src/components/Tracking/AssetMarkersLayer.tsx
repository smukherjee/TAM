import React, { useState } from 'react';
import { Marker } from 'react-leaflet';
import { AssetLocation } from '../../types/assetTracking';
import { createVehicleIcon, VehicleIconOptions } from '../MapIcons';
import { VehicleInfoCard } from '../InfoCards';
import 'leaflet/dist/leaflet.css';
import '../MapIcons.css';

interface AssetMarkersLayerProps {
    assets: AssetLocation[];
}

/**
 * Asset Markers Layer - Displays asset tracking markers on map
 * Uses unified SVG icon system for consistent look with vehicles
 */
const AssetMarkersLayer: React.FC<AssetMarkersLayerProps> = ({ assets }) => {
    const [selectedAsset, setSelectedAsset] = useState<AssetLocation | null>(null);

    // Map asset categories to vehicle icon types
    const getAssetIconType = (category: string): VehicleIconOptions['type'] => {
        const categoryMap: Record<string, VehicleIconOptions['type']> = {
            'Emergency': 'emergency',
            'Fueling': 'fuel_truck',
            'Ground Support': 'gpu',
            'Cargo': 'cargo_loader',
            'Power': 'power_unit',
            'Catering': 'catering',
            'Cleaning': 'cleaning',
            'Maintenance': 'maintenance',
            'Belt Loader': 'belt_loader',
            'Tug': 'tug',
            'Baggage': 'tug',
            'Bus': 'bus',
            'Passenger Bus': 'bus',
            'De-icing': 'deicing',
            'Deicing': 'deicing',
            'De-Icing': 'deicing',
            'Stairs': 'stairs',
            'Passenger Stairs': 'stairs',
        };
        return categoryMap[category] || 'other';
    };

    const getAssetIconOptions = (asset: AssetLocation): VehicleIconOptions => {
        const type = getAssetIconType(asset.category);
        
        // Determine status based on asset state
        let status: VehicleIconOptions['status'] = 'idle';
        if (asset.hasViolation) {
            status = 'alert';
        } else if (asset.status === 'Active' || (asset.speed && asset.speed > 0)) {
            status = 'active';
        } else if (asset.status === 'Maintenance') {
            status = 'warning';
        }
        
        return { type, status };
    };

    // Guard against undefined or non-array assets
    if (!assets || !Array.isArray(assets)) {
        console.warn('AssetMarkersLayer: assets is not an array', assets);
        return null;
    }

    return (
        <>
            {assets.map(asset => {
                const isSelected = selectedAsset?.assetId === asset.assetId;
                const iconOptions = getAssetIconOptions(asset);
                
                // Highlight selected asset
                if (isSelected) {
                    iconOptions.status = 'warning';
                }
                
                return (
                    <Marker
                        key={asset.assetId}
                        position={[asset.latitude, asset.longitude]}
                        icon={createVehicleIcon(iconOptions)}
                        eventHandlers={{
                            click: () => setSelectedAsset(asset)
                        }}
                        zIndexOffset={isSelected ? 1000 : asset.hasViolation ? 500 : 0}
                    />
                );
            })}
            
            {selectedAsset && (
                <VehicleInfoCard
                    vehicle={{
                        id: selectedAsset.assetIdentifier,
                        type: selectedAsset.category,
                        status: selectedAsset.hasViolation ? 'alert' : (selectedAsset.status === 'Active' ? 'active' : 'idle'),
                        location: { lat: selectedAsset.latitude, lng: selectedAsset.longitude },
                        speed: selectedAsset.speed,
                        lastUpdate: new Date(selectedAsset.lastSeen)
                    }}
                    onClose={() => setSelectedAsset(null)}
                />
            )}
        </>
    );
};

export default AssetMarkersLayer;
