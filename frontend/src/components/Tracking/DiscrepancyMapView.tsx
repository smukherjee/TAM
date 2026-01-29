import React, { useMemo } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import { MovementDiscrepancy, SEVERITY_COLORS, DISCREPANCY_TYPE_LABELS } from '../../types/tracking';
import { format } from 'date-fns';
import { CheckCircle, AlertTriangle } from 'lucide-react';

interface DiscrepancyMapViewProps {
    discrepancies: MovementDiscrepancy[];
    selectedDiscrepancy: MovementDiscrepancy | null;
    onSelectDiscrepancy: (discrepancy: MovementDiscrepancy | null) => void;
    onAcknowledge: (discrepancy: MovementDiscrepancy) => void;
    tenantCode: string;
}

// Map center helper
const MapCenterUpdater: React.FC<{ center: [number, number]; zoom: number }> = ({ center, zoom }) => {
    const map = useMap();
    React.useEffect(() => {
        map.setView(center, zoom);
    }, [center, zoom, map]);
    return null;
};

// Create custom icons
const createMarkerIcon = (color: string, label: string) => {
    return L.divIcon({
        className: 'custom-div-icon',
        html: `
            <div style="
                background-color: ${color};
                width: 24px;
                height: 24px;
                border-radius: 50%;
                border: 2px solid white;
                box-shadow: 0 2px 4px rgba(0,0,0,0.3);
                display: flex;
                align-items: center;
                justify-content: center;
                color: white;
                font-size: 10px;
                font-weight: bold;
            ">${label}</div>
        `,
        iconSize: [24, 24],
        iconAnchor: [12, 12]
    });
};

const expectedIcon = createMarkerIcon('#10B981', 'E'); // Green
const actualIcon = createMarkerIcon('#EF4444', 'A');   // Red

/**
 * DiscrepancyMapView - Map visualization for movement discrepancies
 * Feature: 005-asset-tracking-security
 * Phase 10: Movement Discrepancy Report
 */
const DiscrepancyMapView: React.FC<DiscrepancyMapViewProps> = ({
    discrepancies,
    selectedDiscrepancy,
    onSelectDiscrepancy,
    onAcknowledge,
    tenantCode
}) => {
    // Determine map center based on tenant or selected discrepancy
    const mapCenter = useMemo((): [number, number] => {
        if (selectedDiscrepancy?.actualLatitude && selectedDiscrepancy?.actualLongitude) {
            return [selectedDiscrepancy.actualLatitude, selectedDiscrepancy.actualLongitude];
        }
        
        // Default centers by tenant
        switch (tenantCode) {
            case 'VABB': return [19.0896, 72.8656];
            case 'LIRN': return [40.8844, 14.2908];
            case 'YBBN': return [-27.3842, 153.1175];
            default: return [28.5562, 77.1000]; // VIDP
        }
    }, [selectedDiscrepancy, tenantCode]);

    const mapZoom = selectedDiscrepancy ? 17 : 15;

    // Format deviation
    const formatDeviation = (meters?: number) => {
        if (!meters) return '-';
        if (meters < 1000) return `${meters.toFixed(0)}m`;
        return `${(meters / 1000).toFixed(2)}km`;
    };

    return (
        <div className="h-full w-full rounded-lg overflow-hidden shadow-lg relative">
            <MapContainer
                center={mapCenter}
                zoom={mapZoom}
                className="h-full w-full"
                style={{ minHeight: '400px' }}
            >
                <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                />
                
                <MapCenterUpdater center={mapCenter} zoom={mapZoom} />

                {/* Render discrepancies */}
                {discrepancies.map((discrepancy) => {
                    const hasExpected = discrepancy.expectedLatitude && discrepancy.expectedLongitude;
                    const hasActual = discrepancy.actualLatitude && discrepancy.actualLongitude;
                    const isSelected = selectedDiscrepancy?.id === discrepancy.id;

                    return (
                        <React.Fragment key={discrepancy.id}>
                            {/* Expected Location (green) */}
                            {hasExpected && (
                                <Marker
                                    position={[discrepancy.expectedLatitude!, discrepancy.expectedLongitude!]}
                                    icon={expectedIcon}
                                    eventHandlers={{
                                        click: () => onSelectDiscrepancy(discrepancy)
                                    }}
                                >
                                    <Popup>
                                        <div className="text-sm">
                                            <div className="font-medium text-green-600">Expected Location</div>
                                            <div className="text-gray-600">{discrepancy.assetIdentifier}</div>
                                        </div>
                                    </Popup>
                                </Marker>
                            )}

                            {/* Actual Location (red) */}
                            {hasActual && (
                                <Marker
                                    position={[discrepancy.actualLatitude!, discrepancy.actualLongitude!]}
                                    icon={actualIcon}
                                    eventHandlers={{
                                        click: () => onSelectDiscrepancy(discrepancy)
                                    }}
                                >
                                    <Popup>
                                        <div className="text-sm">
                                            <div className="font-medium text-red-600">Actual Location</div>
                                            <div className="text-gray-600">{discrepancy.assetIdentifier}</div>
                                        </div>
                                    </Popup>
                                </Marker>
                            )}

                            {/* Line between expected and actual */}
                            {hasExpected && hasActual && (
                                <Polyline
                                    positions={[
                                        [discrepancy.expectedLatitude!, discrepancy.expectedLongitude!],
                                        [discrepancy.actualLatitude!, discrepancy.actualLongitude!]
                                    ]}
                                    pathOptions={{
                                        color: isSelected ? '#3B82F6' : '#EF4444',
                                        weight: isSelected ? 3 : 2,
                                        dashArray: '5, 10',
                                        opacity: isSelected ? 1 : 0.7
                                    }}
                                />
                            )}
                        </React.Fragment>
                    );
                })}
            </MapContainer>

            {/* Selected Discrepancy Details Panel */}
            {selectedDiscrepancy && (
                <div className="absolute bottom-4 left-4 right-4 bg-gray-800 rounded-lg shadow-lg p-4 max-w-md border border-gray-700">
                    <div className="flex items-start justify-between mb-3">
                        <div className="flex items-center">
                            <AlertTriangle 
                                className="h-5 w-5 mr-2" 
                                style={{ color: SEVERITY_COLORS[selectedDiscrepancy.severity] }} 
                            />
                            <div>
                                <div className="font-medium text-white">
                                    {selectedDiscrepancy.assetIdentifier}
                                </div>
                                <div className="text-xs text-gray-400">
                                    {DISCREPANCY_TYPE_LABELS[selectedDiscrepancy.discrepancyType as keyof typeof DISCREPANCY_TYPE_LABELS]}
                                </div>
                            </div>
                        </div>
                        <button
                            onClick={() => onSelectDiscrepancy(null)}
                            className="text-gray-500 hover:text-gray-300"
                        >
                            ×
                        </button>
                    </div>

                    <div className="grid grid-cols-2 gap-3 text-sm mb-3 text-gray-300">
                        <div>
                            <span className="text-gray-400">Deviation:</span>
                            <span className="ml-1 font-medium text-white">
                                {formatDeviation(selectedDiscrepancy.deviationMeters)}
                            </span>
                        </div>
                        <div>
                            <span className="text-gray-400">Severity:</span>
                            <span 
                                className="ml-1 inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium"
                                style={{ 
                                    backgroundColor: `${SEVERITY_COLORS[selectedDiscrepancy.severity]}20`,
                                    color: SEVERITY_COLORS[selectedDiscrepancy.severity]
                                }}
                            >
                                {selectedDiscrepancy.severity}
                            </span>
                        </div>
                        <div>
                            <span className="text-gray-400">Time:</span>
                            <span className="ml-1 font-medium text-white">
                                {format(new Date(selectedDiscrepancy.timestamp), 'HH:mm')}
                            </span>
                        </div>
                        <div>
                            <span className="text-gray-400">Status:</span>
                            {selectedDiscrepancy.acknowledged ? (
                                <span className="ml-1 text-green-400 font-medium">Acknowledged</span>
                            ) : (
                                <span className="ml-1 text-orange-400 font-medium">Pending</span>
                            )}
                        </div>
                    </div>

                    {selectedDiscrepancy.description && (
                        <p className="text-xs text-gray-400 mb-3">{selectedDiscrepancy.description}</p>
                    )}

                    <div className="flex items-center space-x-2">
                        <div className="flex items-center text-xs text-gray-400">
                            <div className="w-3 h-3 rounded-full bg-green-500 mr-1"></div>
                            Expected
                        </div>
                        <div className="flex items-center text-xs text-gray-400">
                            <div className="w-3 h-3 rounded-full bg-red-500 mr-1"></div>
                            Actual
                        </div>
                        <div className="flex-1"></div>
                        {!selectedDiscrepancy.acknowledged && (
                            <button
                                onClick={() => onAcknowledge(selectedDiscrepancy)}
                                className="inline-flex items-center px-3 py-1.5 text-xs font-medium 
                                    text-white bg-blue-600 rounded hover:bg-blue-700"
                            >
                                <CheckCircle className="h-3.5 w-3.5 mr-1" />
                                Acknowledge
                            </button>
                        )}
                    </div>
                </div>
            )}

            {/* Legend */}
            <div className="absolute top-4 right-4 bg-gray-800 rounded-lg shadow-lg p-3 border border-gray-700">
                <h4 className="text-xs font-medium text-gray-300 mb-2">Legend</h4>
                <div className="space-y-1.5">
                    <div className="flex items-center text-xs">
                        <div className="w-4 h-4 rounded-full bg-green-500 mr-2 flex items-center justify-center text-white text-[8px] font-bold">E</div>
                        <span className="text-gray-400">Expected Location</span>
                    </div>
                    <div className="flex items-center text-xs">
                        <div className="w-4 h-4 rounded-full bg-red-500 mr-2 flex items-center justify-center text-white text-[8px] font-bold">A</div>
                        <span className="text-gray-400">Actual Location</span>
                    </div>
                    <div className="flex items-center text-xs">
                        <div className="w-4 h-0.5 bg-red-500 mr-2" style={{ borderStyle: 'dashed' }}></div>
                        <span className="text-gray-400">Deviation</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default DiscrepancyMapView;
