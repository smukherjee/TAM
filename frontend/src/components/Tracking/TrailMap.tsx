import React, { useMemo, useEffect, useRef } from 'react';
import { MapContainer, TileLayer, Polyline, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import { MovementTrailPoint, ZoneEntry } from '../../types/tracking';
import { format } from 'date-fns';
import 'leaflet/dist/leaflet.css';

// Tenant center coordinates
const TENANT_CENTERS: Record<string, { lat: number; lng: number; zoom: number }> = {
    'VIDP': { lat: 28.5562, lng: 77.1000, zoom: 15 },
    'LIRN': { lat: 40.8860, lng: 14.2908, zoom: 15 },
    'YBBN': { lat: -27.3942, lng: 153.1218, zoom: 15 },
};

// Zone type colors
const ZONE_COLORS: Record<string, { fill: string; stroke: string }> = {
    'PROHIBITED': { fill: 'rgba(239, 68, 68, 0.2)', stroke: '#dc2626' },
    'RESTRICTED': { fill: 'rgba(249, 115, 22, 0.2)', stroke: '#ea580c' },
    'CONTROLLED': { fill: 'rgba(234, 179, 8, 0.2)', stroke: '#ca8a04' },
    'MAINTENANCE': { fill: 'rgba(59, 130, 246, 0.2)', stroke: '#2563eb' },
};

interface TrailMapProps {
    points: MovementTrailPoint[];
    zoneEntries?: ZoneEntry[];
    currentPointIndex: number;
    tenantCode: string;
}

// Component to update map view on current point change
const MapUpdater: React.FC<{ 
    point: MovementTrailPoint | null; 
    followAsset: boolean;
}> = ({ point, followAsset }) => {
    const map = useMap();
    
    useEffect(() => {
        if (point && followAsset) {
            map.panTo([point.latitude, point.longitude], { animate: true });
        }
    }, [map, point, followAsset]);

    return null;
};

// Component to fit bounds on initial load
const BoundsFitter: React.FC<{ points: MovementTrailPoint[] }> = ({ points }) => {
    const map = useMap();
    const hasSetBounds = useRef(false);

    useEffect(() => {
        if (points.length > 0 && !hasSetBounds.current) {
            const bounds = L.latLngBounds(
                points.map(p => [p.latitude, p.longitude] as [number, number])
            );
            map.fitBounds(bounds, { padding: [50, 50] });
            hasSetBounds.current = true;
        }
    }, [map, points]);

    // Reset when points change significantly
    useEffect(() => {
        hasSetBounds.current = false;
    }, [points.length]);

    return null;
};

/**
 * TrailMap - Leaflet map showing asset movement trail
 * Feature: 005-asset-tracking-security
 * Phase 11: Movement Trail Visualization
 */
const TrailMap: React.FC<TrailMapProps> = ({
    points,
    zoneEntries = [],
    currentPointIndex,
    tenantCode
}) => {
    const center = TENANT_CENTERS[tenantCode] || TENANT_CENTERS['VIDP'];
    const currentPoint = points[currentPointIndex];
    const followAsset = currentPointIndex < points.length - 1;

    // Traveled path (up to current index)
    const traveledPath = useMemo(() => {
        return points.slice(0, currentPointIndex + 1).map(p => [p.latitude, p.longitude] as [number, number]);
    }, [points, currentPointIndex]);

    // Future path (after current index)
    const futurePath = useMemo(() => {
        return points.slice(currentPointIndex).map(p => [p.latitude, p.longitude] as [number, number]);
    }, [points, currentPointIndex]);

    // Zone entry markers
    const zoneMarkers = useMemo(() => {
        return zoneEntries.map((entry, index) => ({
            ...entry,
            index,
            position: [entry.entryLatitude, entry.entryLongitude] as [number, number]
        }));
    }, [zoneEntries]);

    // Create custom marker icons
    const createCurrentPositionIcon = () => {
        return L.divIcon({
            className: 'custom-marker',
            html: `
                <div class="relative">
                    <div class="absolute -top-3 -left-3 w-6 h-6 bg-blue-500 rounded-full border-3 border-white shadow-lg flex items-center justify-center">
                        <svg class="w-3 h-3 text-white" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd"/>
                        </svg>
                    </div>
                    <div class="absolute -top-4 -left-4 w-8 h-8 bg-blue-500/30 rounded-full animate-ping"></div>
                </div>
            `,
            iconSize: [24, 24],
            iconAnchor: [12, 12]
        });
    };

    const createZoneEntryIcon = (zoneType: string) => {
        const color = ZONE_COLORS[zoneType]?.stroke || '#6b7280';
        return L.divIcon({
            className: 'zone-entry-marker',
            html: `
                <div class="w-5 h-5 rounded-full border-2 flex items-center justify-center" 
                     style="background-color: ${color}20; border-color: ${color};">
                    <svg class="w-3 h-3" style="color: ${color};" fill="currentColor" viewBox="0 0 20 20">
                        <path fill-rule="evenodd" d="M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 10a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM3 15a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd"/>
                    </svg>
                </div>
            `,
            iconSize: [20, 20],
            iconAnchor: [10, 10]
        });
    };

    const createStartIcon = () => {
        return L.divIcon({
            className: 'start-marker',
            html: `
                <div class="w-6 h-6 bg-green-500 rounded-full border-2 border-white shadow-lg flex items-center justify-center">
                    <span class="text-white text-xs font-bold">S</span>
                </div>
            `,
            iconSize: [24, 24],
            iconAnchor: [12, 12]
        });
    };

    const createEndIcon = () => {
        return L.divIcon({
            className: 'end-marker',
            html: `
                <div class="w-6 h-6 bg-red-500 rounded-full border-2 border-white shadow-lg flex items-center justify-center">
                    <span class="text-white text-xs font-bold">E</span>
                </div>
            `,
            iconSize: [24, 24],
            iconAnchor: [12, 12]
        });
    };

    if (points.length === 0) {
        return (
            <div className="h-full flex items-center justify-center bg-gray-800">
                <p className="text-gray-400">No trail data available</p>
            </div>
        );
    }

    return (
        <MapContainer
            center={[center.lat, center.lng]}
            zoom={center.zoom}
            className="h-full w-full"
            scrollWheelZoom={true}
        >
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
            />

            <BoundsFitter points={points} />
            <MapUpdater point={currentPoint} followAsset={followAsset} />

            {/* Traveled path - solid line */}
            {traveledPath.length > 1 && (
                <Polyline
                    positions={traveledPath}
                    pathOptions={{
                        color: '#3b82f6',
                        weight: 4,
                        opacity: 1
                    }}
                />
            )}

            {/* Future path - dashed line */}
            {futurePath.length > 1 && (
                <Polyline
                    positions={futurePath}
                    pathOptions={{
                        color: '#94a3b8',
                        weight: 2,
                        opacity: 0.5,
                        dashArray: '5, 10'
                    }}
                />
            )}

            {/* Start marker */}
            {points.length > 0 && (
                <Marker
                    position={[points[0].latitude, points[0].longitude]}
                    icon={createStartIcon()}
                >
                    <Popup>
                        <div className="p-2 bg-gray-900 text-gray-100">
                            <div className="font-semibold text-green-600">Start Point</div>
                            <div className="text-xs text-gray-500 mt-1">
                                {format(new Date(points[0].timestamp), 'MMM d, yyyy HH:mm:ss')}
                            </div>
                        </div>
                    </Popup>
                </Marker>
            )}

            {/* End marker (if not same as current) */}
            {points.length > 1 && currentPointIndex < points.length - 1 && (
                <Marker
                    position={[points[points.length - 1].latitude, points[points.length - 1].longitude]}
                    icon={createEndIcon()}
                >
                    <Popup>
                        <div className="p-2 bg-gray-900 text-gray-100">
                            <div className="font-semibold text-red-600">End Point</div>
                            <div className="text-xs text-gray-500 mt-1">
                                {format(new Date(points[points.length - 1].timestamp), 'MMM d, yyyy HH:mm:ss')}
                            </div>
                        </div>
                    </Popup>
                </Marker>
            )}

            {/* Current position marker */}
            {currentPoint && (
                <Marker
                    position={[currentPoint.latitude, currentPoint.longitude]}
                    icon={createCurrentPositionIcon()}
                >
                    <Popup>
                        <div className="p-2 min-w-[200px] bg-gray-900 text-gray-100">
                            <div className="font-semibold text-blue-600">Current Position</div>
                            <div className="text-xs text-gray-600 mt-2 space-y-1">
                                <div>
                                    <span className="text-gray-400">Time:</span>{' '}
                                    {format(new Date(currentPoint.timestamp), 'HH:mm:ss')}
                                </div>
                                <div>
                                    <span className="text-gray-400">Speed:</span>{' '}
                                    {currentPoint.speed?.toFixed(1) || 0} km/h
                                </div>
                                {currentPoint.currentZone && (
                                    <div>
                                        <span className="text-gray-400">Zone:</span>{' '}
                                        <span className="text-orange-600">{currentPoint.currentZone}</span>
                                    </div>
                                )}
                                <div className="font-mono text-[10px] text-gray-400 mt-1">
                                    {currentPoint.latitude.toFixed(6)}, {currentPoint.longitude.toFixed(6)}
                                </div>
                            </div>
                        </div>
                    </Popup>
                </Marker>
            )}

            {/* Zone entry markers */}
            {zoneMarkers.map((entry) => (
                <Marker
                    key={`zone-${entry.index}`}
                    position={entry.position}
                    icon={createZoneEntryIcon(entry.zoneType)}
                >
                    <Popup>
                        <div className="p-2 bg-gray-900 text-gray-100">
                            <div className="font-semibold" style={{ color: ZONE_COLORS[entry.zoneType]?.stroke }}>
                                {entry.zoneName}
                            </div>
                            <div className="text-xs text-gray-500 mt-1">
                                <span className="inline-block px-1.5 py-0.5 rounded text-xs" 
                                    style={{ 
                                        backgroundColor: ZONE_COLORS[entry.zoneType]?.fill,
                                        color: ZONE_COLORS[entry.zoneType]?.stroke
                                    }}>
                                    {entry.zoneType}
                                </span>
                            </div>
                            <div className="text-xs text-gray-600 mt-2">
                                <div>Entry: {format(new Date(entry.entryTime), 'HH:mm:ss')}</div>
                                {entry.exitTime && (
                                    <div>Exit: {format(new Date(entry.exitTime), 'HH:mm:ss')}</div>
                                )}
                                <div>Duration: {entry.dwellTimeMinutes?.toFixed(1) || '—'} min</div>
                            </div>
                        </div>
                    </Popup>
                </Marker>
            ))}

            {/* Map Legend */}
            <div className="absolute bottom-4 left-4 z-[1000] bg-gray-800 rounded-lg shadow-md p-3 border border-gray-700">
                <div className="text-xs font-semibold text-gray-300 mb-2">Legend</div>
                <div className="space-y-1 text-xs">
                    <div className="flex items-center">
                        <div className="w-4 h-1 bg-blue-500 rounded mr-2"></div>
                        <span className="text-gray-400">Traveled Path</span>
                    </div>
                    <div className="flex items-center">
                        <div className="w-4 h-1 bg-gray-500 rounded mr-2" style={{ borderTop: '1px dashed #64748b' }}></div>
                        <span className="text-gray-400">Remaining Path</span>
                    </div>
                    <div className="flex items-center">
                        <div className="w-3 h-3 bg-green-500 rounded-full mr-2"></div>
                        <span className="text-gray-400">Start</span>
                    </div>
                    <div className="flex items-center">
                        <div className="w-3 h-3 bg-red-500 rounded-full mr-2"></div>
                        <span className="text-gray-400">End</span>
                    </div>
                    <div className="flex items-center">
                        <div className="w-3 h-3 bg-blue-500 rounded-full mr-2 animate-pulse"></div>
                        <span className="text-gray-400">Current</span>
                    </div>
                </div>
            </div>
        </MapContainer>
    );
};

export default TrailMap;
