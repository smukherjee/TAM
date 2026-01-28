import React, { useEffect, useRef } from 'react';
import { Marker } from 'react-leaflet';
import L from 'leaflet';
import { AssetLocation, CATEGORY_COLORS } from '../../types/assetTracking';

interface AssetMarkerProps {
    asset: AssetLocation;
    onClick: (asset: AssetLocation) => void;
    isSelected: boolean;
}

/**
 * Asset Marker Component - Custom SVG markers with category color-coding
 * Feature: 005-asset-tracking-security
 * Task: T038
 */
const AssetMarker: React.FC<AssetMarkerProps> = ({ asset, onClick, isSelected }) => {
    const markerRef = useRef<L.Marker>(null);

    const getMarkerIcon = () => {
        const color = CATEGORY_COLORS[asset.category] || CATEGORY_COLORS['Other'];
        const size = 24;
        const strokeWidth = 2;
        
        // Different SVG based on status
        let svgContent = '';
        
        if (asset.status === 'In Use') {
            // Solid filled circle
            svgContent = `
                <svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="${size/2}" cy="${size/2}" r="${size/2 - strokeWidth}" 
                            fill="${color}" stroke="white" stroke-width="${strokeWidth}" />
                    ${asset.hasViolation ? `<circle cx="${size - 4}" cy="4" r="3" fill="#EF4444" stroke="white" stroke-width="1" />` : ''}
                </svg>
            `;
        } else if (asset.status === 'Available') {
            // Hollow circle
            svgContent = `
                <svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="${size/2}" cy="${size/2}" r="${size/2 - strokeWidth}" 
                            fill="white" stroke="${color}" stroke-width="${strokeWidth}" />
                    ${asset.hasViolation ? `<circle cx="${size - 4}" cy="4" r="3" fill="#EF4444" stroke="white" stroke-width="1" />` : ''}
                </svg>
            `;
        } else if (asset.status === 'Maintenance') {
            // Gray filled circle
            svgContent = `
                <svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="${size/2}" cy="${size/2}" r="${size/2 - strokeWidth}" 
                            fill="#9CA3AF" stroke="white" stroke-width="${strokeWidth}" />
                </svg>
            `;
        } else if (asset.status === 'Out of Service') {
            // Black circle with X
            svgContent = `
                <svg width="${size}" height="${size}" viewBox="0 0 ${size} ${size}" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="${size/2}" cy="${size/2}" r="${size/2 - strokeWidth}" 
                            fill="#1F2937" stroke="white" stroke-width="${strokeWidth}" />
                    <line x1="7" y1="7" x2="17" y2="17" stroke="white" stroke-width="2" />
                    <line x1="17" y1="7" x2="7" y2="17" stroke="white" stroke-width="2" />
                </svg>
            `;
        }

        // Add pulse animation for moving assets
        const className = asset.isMoving ? 'asset-marker-pulse' : 'asset-marker';

        const icon = L.divIcon({
            html: `
                <div class="${className} ${isSelected ? 'selected' : ''}" style="position: relative;">
                    ${svgContent}
                    ${asset.heading !== undefined ? `
                        <div style="position: absolute; top: -8px; left: ${size/2 - 2}px; width: 4px; height: 8px; background: ${color}; 
                                    transform: rotate(${asset.heading}deg); transform-origin: center bottom;"></div>
                    ` : ''}
                </div>
            `,
            className: '',
            iconSize: [size, size],
            iconAnchor: [size / 2, size / 2]
        });

        return icon;
    };

    // Update marker position with smooth animation
    useEffect(() => {
        if (markerRef.current) {
            const marker = markerRef.current;
            const newLatLng = L.latLng(asset.latitude, asset.longitude);
            
            // Smooth transition using Leaflet's built-in animation
            marker.setLatLng(newLatLng);
        }
    }, [asset.latitude, asset.longitude]);

    return (
        <Marker
            position={[asset.latitude, asset.longitude]}
            icon={getMarkerIcon()}
            ref={markerRef}
            eventHandlers={{
                click: () => onClick(asset)
            }}
        />
    );
};

export default AssetMarker;
