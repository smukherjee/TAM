import React from 'react';
import { MovementTrailPoint, TrailSummary, MovementTrail } from '../../types/tracking';
import { format } from 'date-fns';
import { 
    MapPin, 
    Clock, 
    Gauge, 
    Navigation,
    Route,
    AlertTriangle,
    Timer,
    Activity
} from 'lucide-react';

interface TrailInfoPanelProps {
    currentPoint: MovementTrailPoint | null;
    summary?: TrailSummary;
    assetInfo?: MovementTrail;
}

// Zone type colors
const ZONE_TYPE_COLORS: Record<string, string> = {
    'PROHIBITED': 'text-red-400 bg-red-900/50',
    'RESTRICTED': 'text-orange-400 bg-orange-900/50',
    'CONTROLLED': 'text-yellow-400 bg-yellow-900/50',
    'MAINTENANCE': 'text-blue-400 bg-blue-900/50',
};

/**
 * TrailInfoPanel - Side panel showing current position and summary stats
 * Feature: 005-asset-tracking-security
 * Phase 11: Movement Trail Visualization
 */
const TrailInfoPanel: React.FC<TrailInfoPanelProps> = ({
    currentPoint,
    summary,
    assetInfo
}) => {
    // Format distance
    const formatDistance = (meters?: number) => {
        if (!meters) return '0 m';
        if (meters < 1000) return `${meters.toFixed(0)} m`;
        return `${(meters / 1000).toFixed(2)} km`;
    };

    // Format duration
    const formatDuration = (minutes?: number) => {
        if (!minutes) return '0 min';
        if (minutes < 60) return `${minutes.toFixed(0)} min`;
        const hours = Math.floor(minutes / 60);
        const mins = Math.round(minutes % 60);
        return `${hours}h ${mins}m`;
    };

    return (
        <div className="h-full flex flex-col">
            {/* Asset Header */}
            {assetInfo && (
                <div className="p-4 border-b border-gray-700 bg-gray-700">
                    <div className="flex items-start justify-between">
                        <div>
                            <h3 className="font-semibold text-white">
                                {assetInfo.assetIdentifier}
                            </h3>
                            {assetInfo.assetName && (
                                <p className="text-sm text-gray-400">{assetInfo.assetName}</p>
                            )}
                        </div>
                        {assetInfo.assetCategory && (
                            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs 
                                font-medium bg-gray-600 text-gray-300">
                                {assetInfo.assetCategory}
                            </span>
                        )}
                    </div>
                </div>
            )}

            {/* Current Position Section */}
            <div className="p-4 border-b border-gray-700">
                <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                    Current Position
                </h4>
                
                {currentPoint ? (
                    <div className="space-y-3">
                        {/* Timestamp */}
                        <div className="flex items-start">
                            <Clock className="h-4 w-4 text-gray-500 mr-2 mt-0.5" />
                            <div>
                                <div className="text-sm font-medium text-white">
                                    {format(new Date(currentPoint.timestamp), 'HH:mm:ss')}
                                </div>
                                <div className="text-xs text-gray-500">
                                    {format(new Date(currentPoint.timestamp), 'MMM d, yyyy')}
                                </div>
                            </div>
                        </div>

                        {/* Coordinates */}
                        <div className="flex items-start">
                            <MapPin className="h-4 w-4 text-gray-500 mr-2 mt-0.5" />
                            <div>
                                <div className="font-mono text-sm text-gray-300">
                                    {currentPoint.latitude.toFixed(6)}
                                </div>
                                <div className="font-mono text-sm text-gray-300">
                                    {currentPoint.longitude.toFixed(6)}
                                </div>
                            </div>
                        </div>

                        {/* Speed */}
                        <div className="flex items-center">
                            <Gauge className="h-4 w-4 text-gray-500 mr-2" />
                            <span className="text-sm text-gray-300">
                                {currentPoint.speed?.toFixed(1) || 0} km/h
                            </span>
                        </div>

                        {/* Heading */}
                        {currentPoint.heading !== undefined && (
                            <div className="flex items-center">
                                <Navigation className="h-4 w-4 text-gray-500 mr-2" 
                                    style={{ transform: `rotate(${currentPoint.heading}deg)` }}
                                />
                                <span className="text-sm text-gray-300">
                                    {currentPoint.heading.toFixed(0)}° bearing
                                </span>
                            </div>
                        )}

                        {/* Current Zone */}
                        {currentPoint.currentZone && (
                            <div className="flex items-start">
                                <AlertTriangle className="h-4 w-4 text-orange-500 mr-2 mt-0.5" />
                                <div>
                                    <div className="text-sm font-medium text-orange-400">
                                        {currentPoint.currentZone}
                                    </div>
                                    <div className="text-xs text-gray-500">
                                        In restricted zone
                                    </div>
                                </div>
                            </div>
                        )}

                        {/* Status */}
                        {currentPoint.status && (
                            <div className="flex items-center">
                                <Activity className="h-4 w-4 text-gray-500 mr-2" />
                                <span className="inline-flex items-center px-2 py-0.5 rounded text-xs 
                                    font-medium bg-green-900/50 text-green-400">
                                    {currentPoint.status}
                                </span>
                            </div>
                        )}
                    </div>
                ) : (
                    <p className="text-sm text-gray-500">No position data</p>
                )}
            </div>

            {/* Trail Summary Section */}
            {summary && (
                <div className="p-4 border-b border-gray-700">
                    <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                        Trail Summary
                    </h4>
                    
                    <div className="grid grid-cols-2 gap-3">
                        {/* Total Distance */}
                        <div className="bg-gray-700 rounded-lg p-3">
                            <div className="flex items-center text-gray-500 mb-1">
                                <Route className="h-4 w-4 mr-1" />
                                <span className="text-xs">Distance</span>
                            </div>
                            <div className="text-lg font-semibold text-white">
                                {formatDistance(summary.totalDistanceMeters)}
                            </div>
                        </div>

                        {/* Duration */}
                        <div className="bg-gray-700 rounded-lg p-3">
                            <div className="flex items-center text-gray-500 mb-1">
                                <Timer className="h-4 w-4 mr-1" />
                                <span className="text-xs">Duration</span>
                            </div>
                            <div className="text-lg font-semibold text-white">
                                {formatDuration(summary.totalDurationMinutes)}
                            </div>
                        </div>

                        {/* Avg Speed */}
                        <div className="bg-gray-700 rounded-lg p-3">
                            <div className="flex items-center text-gray-500 mb-1">
                                <Gauge className="h-4 w-4 mr-1" />
                                <span className="text-xs">Avg Speed</span>
                            </div>
                            <div className="text-lg font-semibold text-white">
                                {summary.averageSpeedKmh?.toFixed(1) || 0} km/h
                            </div>
                        </div>

                        {/* Max Speed */}
                        <div className="bg-gray-700 rounded-lg p-3">
                            <div className="flex items-center text-gray-500 mb-1">
                                <Gauge className="h-4 w-4 mr-1" />
                                <span className="text-xs">Max Speed</span>
                            </div>
                            <div className="text-lg font-semibold text-white">
                                {summary.maxSpeedKmh?.toFixed(1) || 0} km/h
                            </div>
                        </div>

                        {/* Points */}
                        <div className="bg-gray-700 rounded-lg p-3">
                            <div className="flex items-center text-gray-500 mb-1">
                                <MapPin className="h-4 w-4 mr-1" />
                                <span className="text-xs">Data Points</span>
                            </div>
                            <div className="text-lg font-semibold text-white">
                                {summary.pointCount || 0}
                            </div>
                        </div>

                        {/* Zone Entries */}
                        <div className="bg-gray-700 rounded-lg p-3">
                            <div className="flex items-center text-gray-500 mb-1">
                                <AlertTriangle className="h-4 w-4 mr-1" />
                                <span className="text-xs">Zone Entries</span>
                            </div>
                            <div className="text-lg font-semibold text-white">
                                {summary.zoneEntryCount || 0}
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Zone Dwell Times Section */}
            {summary?.zoneDwellTimes && summary.zoneDwellTimes.length > 0 && (
                <div className="p-4 flex-1 overflow-y-auto">
                    <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                        Zone Dwell Times
                    </h4>
                    
                    <div className="space-y-2">
                        {summary.zoneDwellTimes.map((zone, index) => (
                            <div 
                                key={`dwell-${index}`}
                                className={`rounded-lg p-3 ${
                                    ZONE_TYPE_COLORS[zone.zoneType] || 'bg-gray-700 text-gray-300'
                                }`}
                            >
                                <div className="flex justify-between items-start">
                                    <div>
                                        <div className="font-medium text-sm">
                                            {zone.zoneName}
                                        </div>
                                        <div className="text-xs opacity-75">
                                            {zone.zoneType}
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <div className="font-semibold">
                                            {formatDuration(zone.dwellTimeMinutes)}
                                        </div>
                                        <div className="text-xs opacity-75">
                                            {zone.entryCount} {zone.entryCount === 1 ? 'entry' : 'entries'}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {/* Empty state for zone dwell times */}
            {(!summary?.zoneDwellTimes || summary.zoneDwellTimes.length === 0) && (
                <div className="p-4 flex-1">
                    <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                        Zone Dwell Times
                    </h4>
                    <p className="text-sm text-gray-500 text-center py-4">
                        No zone entries recorded
                    </p>
                </div>
            )}
        </div>
    );
};

export default TrailInfoPanel;
