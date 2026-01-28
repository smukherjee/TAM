import React, { useState, useRef } from 'react';
import { MapContainer, TileLayer, useMap } from 'react-leaflet';
import MarkerClusterGroup from 'react-leaflet-cluster';
import AssetMarker from './AssetMarker';
import AssetPopup from './AssetPopup';
import 'leaflet/dist/leaflet.css';
import './AssetTracking.css';
import L from 'leaflet';
import { AssetLocation } from '../../types/assetTracking';

interface UniversalAssetMapProps {
    assets: AssetLocation[];
    tenantCode: string;
    center: [number, number];
    zoom: number;
}

/**
 * Universal Asset Map Component - Core map visualization for asset tracking
 * Feature: 005-asset-tracking-security
 * Task: T037
 */
const UniversalAssetMap: React.FC<UniversalAssetMapProps> = ({
    assets,
    center,
    zoom
}) => {
    const [selectedAsset, setSelectedAsset] = useState<AssetLocation | null>(null);
    const mapRef = useRef<L.Map | null>(null);

    const handleAssetClick = (asset: AssetLocation) => {
        setSelectedAsset(asset);
    };

    const handlePopupClose = () => {
        setSelectedAsset(null);
    };

    return (
        <MapContainer
            center={center}
            zoom={zoom}
            style={{ height: '100%', width: '100%' }}
            ref={mapRef}
            className="z-0"
        >
            {/* OpenStreetMap Tile Layer */}
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />

            {/* Clustered Asset Markers */}
            <MarkerClusterGroup
                chunkedLoading
                maxClusterRadius={50}
                spiderfyOnMaxZoom={true}
                showCoverageOnHover={false}
                zoomToBoundsOnClick={true}
                iconCreateFunction={(cluster) => {
                    const count = cluster.getChildCount();
                    const size = count < 10 ? 'small' : count < 50 ? 'medium' : 'large';
                    
                    return L.divIcon({
                        html: `<div class="cluster-icon cluster-${size}"><span>${count}</span></div>`,
                        className: 'custom-marker-cluster',
                        iconSize: L.point(40, 40, true)
                    });
                }}
            >
                {assets.map(asset => (
                    <AssetMarker
                        key={asset.assetId}
                        asset={asset}
                        onClick={handleAssetClick}
                        isSelected={selectedAsset?.assetId === asset.assetId}
                    />
                ))}
            </MarkerClusterGroup>

            {/* Asset Popup */}
            {selectedAsset && (
                <AssetPopup
                    asset={selectedAsset}
                    onClose={handlePopupClose}
                />
            )}

            {/* Map Control Components */}
            <MapControls />
        </MapContainer>
    );
};

/**
 * Map Controls - Scale bar and layer controls
 */
const MapControls: React.FC = () => {
    const map = useMap();

    React.useEffect(() => {
        // Add scale control
        L.control.scale({ position: 'bottomleft' }).addTo(map);
    }, [map]);

    return null;
};

export default UniversalAssetMap;
