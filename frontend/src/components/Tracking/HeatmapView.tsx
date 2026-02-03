import { useEffect, useRef, useState } from 'react';
import { MapContainer, TileLayer, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet.heat';
import 'leaflet/dist/leaflet.css';
import HeatmapLegend from './HeatmapLegend';
import ZoneBoundariesLayer, { ZoneBoundariesToggle } from './ZoneBoundariesLayer';

/**
 * HeatmapView Component
 * Renders a Leaflet map with heatmap overlay using Leaflet.heat
 * Feature: 005-asset-tracking-security (Task T049)
 */

interface HeatmapViewProps {
    heatmapData: [number, number, number][]; // [lat, lng, intensity]
    gridSize: number; // 10, 25, 50, or 100 meters
    intensity: number; // 0-100 from slider
    center: [number, number];
    zoom: number;
    mode?: 'activity' | 'violations' | 'dwell';
    timeRange?: string;
    onCellClick?: (lat: number, lng: number) => void;
    showZoneBoundaries?: boolean;
}

/**
 * Heat Layer Component - Must be inside MapContainer
 */
const HeatLayer: React.FC<{
    data: [number, number, number][];
    gridSize: number;
    intensity: number;
    onCellClick?: (lat: number, lng: number) => void;
}> = ({ data, gridSize, intensity, onCellClick }) => {
    const map = useMap();
    const heatLayerRef = useRef<L.HeatLayer | null>(null);

    useEffect(() => {
        if (!map) return;

        // Remove existing heat layer if it exists
        if (heatLayerRef.current) {
            map.removeLayer(heatLayerRef.current);
        }

        // Convert grid size (meters) to pixel radius
        // Approximate: 10m=5px, 25m=10px, 50m=15px, 100m=20px
        const radiusMap: Record<number, number> = {
            10: 5,
            25: 10,
            50: 15,
            100: 20
        };
        const radius = radiusMap[gridSize] || 15;

        // Create heat layer with gradient and intensity
        heatLayerRef.current = L.heatLayer(data, {
            radius: radius,
            blur: radius * 1.5,
            maxZoom: 17,
            max: 1.0, // Max intensity (normalized data)
            minOpacity: 0.3,
            gradient: {
                0.0: 'blue',
                0.25: 'cyan',
                0.5: 'lime',
                0.75: 'yellow',
                1.0: 'red'
            }
        });

        // Adjust intensity (0-100 → 0-1)
        const normalizedIntensity = intensity / 100;
        if (heatLayerRef.current.setOptions) {
            heatLayerRef.current.setOptions({
                max: normalizedIntensity > 0 ? 1 / normalizedIntensity : 1
            });
        }

        heatLayerRef.current.addTo(map);

        // Add click handler for cell clicks
        if (onCellClick) {
            map.on('click', (e: L.LeafletMouseEvent) => {
                onCellClick(e.latlng.lat, e.latlng.lng);
            });
        }

        // Cleanup
        return () => {
            if (heatLayerRef.current && map) {
                map.removeLayer(heatLayerRef.current);
            }
            if (onCellClick) {
                map.off('click');
            }
        };
    }, [map, data, gridSize, intensity, onCellClick]);

    return null;
};

/**
 * Main HeatmapView Component
 */
const HeatmapView: React.FC<HeatmapViewProps> = ({
    heatmapData,
    gridSize,
    intensity,
    center,
    zoom,
    mode = 'activity',
    timeRange = '24h',
    onCellClick,
    showZoneBoundaries: initialShowZones = false
}) => {
    // Zone boundaries visibility state - persisted to localStorage
    const [showZoneBoundaries, setShowZoneBoundaries] = useState<boolean>(() => {
        const saved = localStorage.getItem('heatmapZoneBoundariesVisible');
        return saved !== null ? saved === 'true' : initialShowZones;
    });

    // Persist zone visibility to localStorage
    const handleZoneToggle = () => {
        const newValue = !showZoneBoundaries;
        setShowZoneBoundaries(newValue);
        localStorage.setItem('heatmapZoneBoundariesVisible', String(newValue));
    };

    return (
        <div className="relative w-full h-full">
            <MapContainer
                center={center}
                zoom={zoom}
                className="w-full h-full"
                zoomControl={true}
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />

                <HeatLayer
                    data={heatmapData}
                    gridSize={gridSize}
                    intensity={intensity}
                    onCellClick={onCellClick}
                />

                {/* Zone boundaries layer - conditionally rendered */}
                {showZoneBoundaries && <ZoneBoundariesLayer />}
            </MapContainer>

            {/* Zone Boundaries Toggle */}
            <div className="absolute top-4 left-4 z-[1000]">
                <ZoneBoundariesToggle 
                    visible={showZoneBoundaries} 
                    onToggle={handleZoneToggle} 
                />
            </div>

            {/* Heatmap Legend */}
            <HeatmapLegend
                mode={mode}
                gridSize={gridSize}
                timeRange={timeRange}
                defaultExpanded={true}
            />

            {/* Overlay controls */}
            <div className="absolute top-4 right-4 bg-white rounded-lg shadow-lg p-3 z-[1000]">
                <div className="text-sm text-gray-700">
                    <div>Grid: {gridSize}m</div>
                    <div>Intensity: {intensity}%</div>
                    <div className="text-xs text-gray-500 mt-1">
                        {heatmapData.length} hotspots
                    </div>
                </div>
            </div>
        </div>
    );
};

export default HeatmapView;
