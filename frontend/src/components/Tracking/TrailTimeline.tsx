import React, { useMemo, useRef, useCallback } from 'react';
import { MovementTrailPoint, ZoneEntry } from '../../types/tracking';
import { format } from 'date-fns';

// Zone type colors for timeline markers
const ZONE_COLORS: Record<string, string> = {
    'PROHIBITED': '#dc2626',
    'RESTRICTED': '#ea580c',
    'CONTROLLED': '#ca8a04',
    'MAINTENANCE': '#2563eb',
};

interface TrailTimelineProps {
    points: MovementTrailPoint[];
    zoneEntries?: ZoneEntry[];
    currentIndex: number;
    onScrub: (index: number) => void;
}

/**
 * TrailTimeline - Timeline slider for scrubbing through trail
 * Feature: 005-asset-tracking-security
 * Phase 11: Movement Trail Visualization
 */
const TrailTimeline: React.FC<TrailTimelineProps> = ({
    points,
    zoneEntries = [],
    currentIndex,
    onScrub
}) => {
    const timelineRef = useRef<HTMLDivElement>(null);
    const isDragging = useRef(false);

    // Calculate time range
    const timeRange = useMemo(() => {
        if (points.length === 0) return { start: 0, end: 0, duration: 0 };
        const start = new Date(points[0].timestamp).getTime();
        const end = new Date(points[points.length - 1].timestamp).getTime();
        return { start, end, duration: end - start };
    }, [points]);

    // Calculate zone entry positions on timeline
    const zoneMarkers = useMemo(() => {
        if (zoneEntries.length === 0 || timeRange.duration === 0) return [];

        return zoneEntries.map((entry) => {
            const entryTime = new Date(entry.entryTime).getTime();
            const position = ((entryTime - timeRange.start) / timeRange.duration) * 100;
            
            let width = 0;
            if (entry.exitTime) {
                const exitTime = new Date(entry.exitTime).getTime();
                width = ((exitTime - entryTime) / timeRange.duration) * 100;
            }

            return {
                ...entry,
                position: Math.max(0, Math.min(100, position)),
                width: Math.max(1, Math.min(100 - position, width))
            };
        });
    }, [zoneEntries, timeRange]);

    // Calculate progress percentage
    const progress = useMemo(() => {
        if (points.length === 0) return 0;
        return (currentIndex / (points.length - 1)) * 100;
    }, [currentIndex, points.length]);

    // Handle timeline click/drag
    const handleTimelineInteraction = useCallback((clientX: number) => {
        if (!timelineRef.current || points.length === 0) return;

        const rect = timelineRef.current.getBoundingClientRect();
        const x = clientX - rect.left;
        const percentage = Math.max(0, Math.min(1, x / rect.width));
        const newIndex = Math.round(percentage * (points.length - 1));
        onScrub(newIndex);
    }, [points.length, onScrub]);

    const handleMouseDown = (e: React.MouseEvent) => {
        isDragging.current = true;
        handleTimelineInteraction(e.clientX);
    };

    const handleMouseMove = (e: React.MouseEvent) => {
        if (isDragging.current) {
            handleTimelineInteraction(e.clientX);
        }
    };

    const handleMouseUp = () => {
        isDragging.current = false;
    };

    const handleMouseLeave = () => {
        isDragging.current = false;
    };

    // Format time labels
    const formatTimeLabel = (timestamp: string) => {
        return format(new Date(timestamp), 'HH:mm');
    };

    if (points.length === 0) {
        return null;
    }

    return (
        <div className="px-6 py-3 border-b border-gray-700">
            {/* Time Labels */}
            <div className="flex justify-between text-xs text-gray-500 mb-2">
                <span>{formatTimeLabel(points[0].timestamp)}</span>
                <span className="font-medium text-gray-300">
                    {format(new Date(points[currentIndex]?.timestamp), 'HH:mm:ss')}
                </span>
                <span>{formatTimeLabel(points[points.length - 1].timestamp)}</span>
            </div>

            {/* Timeline Track */}
            <div 
                ref={timelineRef}
                className="relative h-8 cursor-pointer select-none"
                onMouseDown={handleMouseDown}
                onMouseMove={handleMouseMove}
                onMouseUp={handleMouseUp}
                onMouseLeave={handleMouseLeave}
            >
                {/* Background track */}
                <div className="absolute top-1/2 left-0 right-0 h-2 -translate-y-1/2 bg-gray-700 rounded-full" />

                {/* Zone entry markers (background) */}
                {zoneMarkers.map((marker, index) => (
                    <div
                        key={`zone-bg-${index}`}
                        className="absolute top-1/2 h-4 -translate-y-1/2 rounded opacity-40"
                        style={{
                            left: `${marker.position}%`,
                            width: `${marker.width}%`,
                            backgroundColor: ZONE_COLORS[marker.zoneType] || '#6b7280',
                            minWidth: '4px'
                        }}
                        title={`${marker.zoneName} (${marker.zoneType})`}
                    />
                ))}

                {/* Progress track */}
                <div 
                    className="absolute top-1/2 left-0 h-2 -translate-y-1/2 bg-blue-500 rounded-full transition-all duration-75"
                    style={{ width: `${progress}%` }}
                />

                {/* Zone entry markers (foreground pins) */}
                {zoneMarkers.map((marker, index) => (
                    <div
                        key={`zone-pin-${index}`}
                        className="absolute top-0 -translate-x-1/2 group"
                        style={{ left: `${marker.position}%` }}
                    >
                        <div 
                            className="w-3 h-3 rounded-full border-2 border-white shadow-sm"
                            style={{ backgroundColor: ZONE_COLORS[marker.zoneType] || '#6b7280' }}
                        />
                        {/* Tooltip */}
                        <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 px-2 py-1 
                            bg-gray-800 text-white text-xs rounded whitespace-nowrap opacity-0 
                            group-hover:opacity-100 transition-opacity pointer-events-none z-10">
                            {marker.zoneName}
                            <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 
                                border-transparent border-t-gray-800" />
                        </div>
                    </div>
                ))}

                {/* Scrubber handle */}
                <div 
                    className="absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-5 h-5 
                        bg-white border-2 border-blue-500 rounded-full shadow-lg 
                        cursor-grab active:cursor-grabbing transition-all duration-75
                        hover:scale-110"
                    style={{ left: `${progress}%` }}
                />

                {/* Point markers (small dots for data points) */}
                {points.length <= 100 && points.map((_point, index) => {
                    const position = (index / (points.length - 1)) * 100;
                    return (
                        <div
                            key={`point-${index}`}
                            className={`absolute top-1/2 -translate-y-1/2 -translate-x-1/2 w-1.5 h-1.5 
                                rounded-full transition-all duration-75 ${
                                    index <= currentIndex ? 'bg-blue-400' : 'bg-gray-600'
                                }`}
                            style={{ left: `${position}%` }}
                        />
                    );
                })}
            </div>

            {/* Point count indicator */}
            <div className="flex justify-between items-center mt-2 text-xs text-gray-500">
                <span>Point {currentIndex + 1} of {points.length}</span>
                <div className="flex items-center space-x-3">
                    {/* Zone legend */}
                    {zoneMarkers.length > 0 && (
                        <div className="flex items-center space-x-2">
                            <span>Zones:</span>
                            {Object.entries(ZONE_COLORS).slice(0, 3).map(([type, color]) => (
                                <div key={type} className="flex items-center">
                                    <div 
                                        className="w-2 h-2 rounded-full mr-1"
                                        style={{ backgroundColor: color }}
                                    />
                                    <span className="text-[10px]">{type.slice(0, 3)}</span>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default TrailTimeline;
