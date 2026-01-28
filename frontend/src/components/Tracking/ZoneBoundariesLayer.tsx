import React, { useState, useEffect } from 'react';
import { Polygon, Tooltip } from 'react-leaflet';
import { LatLngExpression } from 'leaflet';

interface RestrictedZone {
    id: string;
    name: string;
    zone_type: 'PROHIBITED' | 'RESTRICTED' | 'CONTROLLED' | 'MAINTENANCE';
    geometry: {
        type: 'Polygon';
        coordinates: number[][][];  // GeoJSON format
    };
    is_active: boolean;
    description?: string;
}

/**
 * Zone Boundaries Layer - Displays restricted zones on map
 * Feature: 005-asset-tracking-security (Task T041)
 */
const ZoneBoundariesLayer: React.FC = () => {
    const [zones, setZones] = useState<RestrictedZone[]>([]);
    const [visible] = useState<boolean>(() => {
        // Load visibility state from localStorage
        const saved = localStorage.getItem('zoneBoundariesVisible');
        return saved !== null ? saved === 'true' : true;
    });

    // Fetch zones from backend (will be implemented when backend endpoint is ready)
    useEffect(() => {
        const fetchZones = async () => {
            try {
                // TODO: Replace with actual API call when backend endpoint is ready
                // const response = await fetch('/api/tracking/zones?tenantCode=YBBN');
                // const data = await response.json();
                // setZones(data.zones || data);
                
                // For now, use empty array (will be populated when backend is ready)
                setZones([]);
            } catch (error) {
                console.error('Failed to fetch zones:', error);
                setZones([]);
            }
        };

        if (visible) {
            fetchZones();
        }
    }, [visible]);

    // Zone type to color mapping
    const getZoneColor = (zoneType: RestrictedZone['zone_type']): { fillColor: string; color: string } => {
        const colorMap: Record<RestrictedZone['zone_type'], { fillColor: string; color: string }> = {
            'PROHIBITED': { fillColor: 'rgba(255, 0, 0, 0.2)', color: '#ff0000' },       // Red
            'RESTRICTED': { fillColor: 'rgba(255, 165, 0, 0.2)', color: '#ffa500' },     // Orange
            'CONTROLLED': { fillColor: 'rgba(255, 255, 0, 0.2)', color: '#ffff00' },     // Yellow
            'MAINTENANCE': { fillColor: 'rgba(0, 0, 255, 0.2)', color: '#0000ff' }       // Blue
        };
        return colorMap[zoneType];
    };

    // Convert GeoJSON coordinates to Leaflet LatLngExpression format
    const geoJSONToLeaflet = (coordinates: number[][][]): LatLngExpression[][] => {
        return coordinates.map(ring => 
            ring.map(coord => [coord[1], coord[0]] as LatLngExpression)  // GeoJSON is [lng, lat], Leaflet is [lat, lng]
        );
    };

    if (!visible || zones.length === 0) {
        return null;
    }

    return (
        <>
            {zones.filter(zone => zone.is_active).map(zone => {
                const { fillColor, color } = getZoneColor(zone.zone_type);
                const positions = geoJSONToLeaflet(zone.geometry.coordinates);

                return (
                    <Polygon
                        key={zone.id}
                        positions={positions}
                        pathOptions={{
                            fillColor: fillColor,
                            fillOpacity: 0.3,
                            color: color,
                            weight: 2,
                            opacity: 0.8
                        }}
                    >
                        <Tooltip sticky>
                            <div className="text-sm">
                                <div className="font-bold">{zone.name}</div>
                                <div className="text-xs text-gray-600 capitalize">
                                    {zone.zone_type.toLowerCase().replace('_', ' ')}
                                </div>
                                {zone.description && (
                                    <div className="text-xs text-gray-500 mt-1">
                                        {zone.description}
                                    </div>
                                )}
                            </div>
                        </Tooltip>
                    </Polygon>
                );
            })}
        </>
    );
};

export default ZoneBoundariesLayer;

/**
 * Zone Boundaries Toggle Button Component
 * To be used in map controls
 */
export const ZoneBoundariesToggle: React.FC<{
    visible: boolean;
    onToggle: () => void;
}> = ({ visible, onToggle }) => {
    return (
        <button
            onClick={onToggle}
            className={`px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                visible 
                    ? 'bg-blue-600 text-white hover:bg-blue-700' 
                    : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
            title={visible ? 'Hide Zone Boundaries' : 'Show Zone Boundaries'}
        >
            <svg 
                className="w-5 h-5" 
                fill="none" 
                stroke="currentColor" 
                viewBox="0 0 24 24"
            >
                {visible ? (
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                ) : (
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                )}
            </svg>
        </button>
    );
};
