import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchMovementTrail, fetchTrailSummary, exportTrail } from '../services/trackingService';
import { MovementTrail, TrailSummary } from '../types/tracking';
import AssetSelector from '../components/Tracking/AssetSelector';
import TrailMap from '../components/Tracking/TrailMap';
import TrailTimeline from '../components/Tracking/TrailTimeline';
import TrailPlaybackControls from '../components/Tracking/TrailPlaybackControls';
import TrailInfoPanel from '../components/Tracking/TrailInfoPanel';
import TrailDateRangePicker from '../components/Tracking/TrailDateRangePicker';
import { format, subHours, subDays } from 'date-fns';
import { useSearchParams } from 'react-router-dom';
import toast from 'react-hot-toast';
import {
    Route,
    Download,
    Loader2,
    AlertCircle,
    MapPin,
    Clock
} from 'lucide-react';

/**
 * MovementTrailPage - Asset movement trail visualization
 * Feature: 005-asset-tracking-security
 * Phase 11: Movement Trail Visualization
 */
const MovementTrailPage: React.FC = () => {
    // State
    const [searchParams] = useSearchParams();
    const [selectedAssetId, setSelectedAssetId] = useState<string | null>(searchParams.get('assetId'));

    // Sync URL param if it changes
    useEffect(() => {
        const urlAssetId = searchParams.get('assetId');
        if (urlAssetId && urlAssetId !== selectedAssetId) {
            setSelectedAssetId(urlAssetId);
        }
    }, [searchParams]);

    const [startDate, setStartDate] = useState<Date>(subHours(new Date(), 24));
    const [endDate, setEndDate] = useState<Date>(new Date());
    const [currentPointIndex, setCurrentPointIndex] = useState<number>(0);
    const [isPlaying, setIsPlaying] = useState<boolean>(false);
    const [playbackSpeed, setPlaybackSpeed] = useState<number>(1);
    const [isExporting, setIsExporting] = useState<boolean>(false);

    // Fetch movement trail
    const {
        data: trailData,
        isLoading: isLoadingTrail,
        error: trailError,
        refetch: refetchTrail
    } = useQuery<MovementTrail>({
        queryKey: ['movementTrail', selectedAssetId, startDate.toISOString(), endDate.toISOString()],
        queryFn: () => fetchMovementTrail(
            selectedAssetId!,
            startDate.toISOString(),
            endDate.toISOString()
        ),
        enabled: !!selectedAssetId,
        staleTime: 60000, // 1 minute
    });

    // Fetch trail summary
    const { data: summaryData } = useQuery<TrailSummary>({
        queryKey: ['trailSummary', selectedAssetId, startDate.toISOString(), endDate.toISOString()],
        queryFn: () => fetchTrailSummary(
            selectedAssetId!,
            startDate.toISOString(),
            endDate.toISOString()
        ),
        enabled: !!selectedAssetId && !!trailData?.points?.length,
    });

    // Current point based on playback
    const currentPoint = useMemo(() => {
        if (!trailData?.points?.length) return null;
        return trailData.points[currentPointIndex] || trailData.points[0];
    }, [trailData?.points, currentPointIndex]);

    // Playback animation using requestAnimationFrame
    useEffect(() => {
        if (!isPlaying || !trailData?.points?.length) return;

        const intervalMs = 1000 / playbackSpeed;
        let lastTime = performance.now();
        let animationId: number;

        const animate = (currentTime: number) => {
            const delta = currentTime - lastTime;

            if (delta >= intervalMs) {
                lastTime = currentTime;
                setCurrentPointIndex(prev => {
                    const next = prev + 1;
                    if (next >= trailData.points.length) {
                        setIsPlaying(false);
                        return trailData.points.length - 1;
                    }
                    return next;
                });
            }

            animationId = requestAnimationFrame(animate);
        };

        animationId = requestAnimationFrame(animate);

        return () => {
            if (animationId) {
                cancelAnimationFrame(animationId);
            }
        };
    }, [isPlaying, playbackSpeed, trailData?.points?.length]);

    // Reset playback when trail changes
    useEffect(() => {
        setCurrentPointIndex(0);
        setIsPlaying(false);
    }, [trailData]);

    // Handle asset selection
    const handleAssetSelect = useCallback((assetId: string) => {
        setSelectedAssetId(assetId);
        setCurrentPointIndex(0);
        setIsPlaying(false);
    }, []);

    // Handle date range change
    const handleDateRangeChange = useCallback((start: Date, end: Date) => {
        setStartDate(start);
        setEndDate(end);
        setCurrentPointIndex(0);
        setIsPlaying(false);
    }, []);

    // Handle quick date presets
    const handleQuickDateSelect = useCallback((preset: string) => {
        const now = new Date();
        let start: Date;

        switch (preset) {
            case '1h':
                start = subHours(now, 1);
                break;
            case '6h':
                start = subHours(now, 6);
                break;
            case '24h':
                start = subHours(now, 24);
                break;
            case '7d':
                start = subDays(now, 7);
                break;
            case '30d':
                start = subDays(now, 30);
                break;
            default:
                start = subHours(now, 24);
        }

        setStartDate(start);
        setEndDate(now);
        setCurrentPointIndex(0);
        setIsPlaying(false);
    }, []);

    // Handle timeline scrub
    const handleTimelineScrub = useCallback((index: number) => {
        setCurrentPointIndex(index);
        setIsPlaying(false);
    }, []);

    // Handle playback toggle
    const handlePlayPause = useCallback(() => {
        if (!trailData?.points?.length) return;

        // If at end, restart from beginning
        if (currentPointIndex >= trailData.points.length - 1) {
            setCurrentPointIndex(0);
        }

        setIsPlaying(prev => !prev);
    }, [trailData?.points?.length, currentPointIndex]);

    // Handle speed change
    const handleSpeedChange = useCallback((speed: number) => {
        setPlaybackSpeed(speed);
    }, []);

    // Handle export
    const handleExport = async () => {
        if (!selectedAssetId) return;

        setIsExporting(true);
        try {
            const blob = await exportTrail(
                selectedAssetId,
                startDate.toISOString(),
                endDate.toISOString(),
                'csv'
            );

            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `trail_${selectedAssetId}_${format(startDate, 'yyyyMMdd')}_${format(endDate, 'yyyyMMdd')}.csv`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);

            toast.success('Trail data exported successfully');
        } catch (error) {
            toast.error('Failed to export trail data');
            console.error('Export error:', error);
        } finally {
            setIsExporting(false);
        }
    };

    return (
        <div className="h-full flex flex-col bg-gray-900">
            {/* Header */}
            <div className="bg-gray-900 border-b border-gray-700 px-6 py-4">
                <div className="flex items-center justify-between">
                    <div className="flex items-center">
                        <Route className="h-6 w-6 text-blue-500 mr-3" />
                        <h1 className="text-2xl font-semibold text-white">
                            Movement Trail
                        </h1>
                    </div>

                    <div className="flex items-center space-x-4">
                        {/* Export Button */}
                        <button
                            onClick={handleExport}
                            disabled={!selectedAssetId || !trailData?.points?.length || isExporting}
                            className="inline-flex items-center px-4 py-2 text-sm font-medium 
                                text-gray-300 bg-gray-800 border border-gray-600 rounded-md 
                                hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isExporting ? (
                                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                            ) : (
                                <Download className="h-4 w-4 mr-2" />
                            )}
                            Export CSV
                        </button>
                    </div>
                </div>
            </div>

            {/* Controls Bar */}
            <div className="bg-gray-800 border-b border-gray-700 px-6 py-4">
                <div className="flex items-center space-x-6">
                    {/* Asset Selector */}
                    <div className="flex-1 max-w-sm">
                        <AssetSelector
                            selectedAssetId={selectedAssetId}
                            onSelect={handleAssetSelect}
                        />
                    </div>

                    {/* Date Range Picker */}
                    <TrailDateRangePicker
                        startDate={startDate}
                        endDate={endDate}
                        onDateRangeChange={handleDateRangeChange}
                        onQuickSelect={handleQuickDateSelect}
                    />

                    {/* Refresh Button */}
                    <button
                        onClick={() => refetchTrail()}
                        disabled={!selectedAssetId || isLoadingTrail}
                        className="inline-flex items-center px-3 py-2 text-sm font-medium 
                            text-gray-300 bg-gray-700 border border-gray-600 rounded-md 
                            hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <Loader2 className={`h-4 w-4 ${isLoadingTrail ? 'animate-spin' : ''}`} />
                    </button>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 flex overflow-hidden">
                {/* Map Area */}
                <div className="flex-1 relative">
                    {!selectedAssetId ? (
                        // No Asset Selected State
                        <div className="h-full flex items-center justify-center bg-gray-800">
                            <div className="text-center">
                                <MapPin className="h-16 w-16 text-gray-600 mx-auto mb-4" />
                                <h3 className="text-lg font-medium text-gray-400 mb-2">
                                    Select an Asset
                                </h3>
                                <p className="text-sm text-gray-500">
                                    Choose an asset to view its movement trail
                                </p>
                            </div>
                        </div>
                    ) : isLoadingTrail ? (
                        // Loading State
                        <div className="h-full flex items-center justify-center bg-gray-800">
                            <div className="text-center">
                                <Loader2 className="h-12 w-12 text-blue-500 animate-spin mx-auto mb-4" />
                                <p className="text-sm text-gray-400">Loading movement trail...</p>
                            </div>
                        </div>
                    ) : trailError ? (
                        // Error State
                        <div className="h-full flex items-center justify-center bg-gray-800">
                            <div className="text-center">
                                <AlertCircle className="h-12 w-12 text-red-500 mx-auto mb-4" />
                                <h3 className="text-lg font-medium text-gray-300 mb-2">
                                    Failed to Load Trail
                                </h3>
                                <p className="text-sm text-gray-400 mb-4">
                                    There was an error loading the movement trail.
                                </p>
                                <button
                                    onClick={() => refetchTrail()}
                                    className="text-sm text-blue-400 hover:text-blue-300"
                                >
                                    Try Again
                                </button>
                            </div>
                        </div>
                    ) : !trailData?.points?.length ? (
                        // Empty State
                        <div className="h-full flex items-center justify-center bg-gray-800">
                            <div className="text-center">
                                <Clock className="h-12 w-12 text-gray-600 mx-auto mb-4" />
                                <h3 className="text-lg font-medium text-gray-400 mb-2">
                                    No Trail Data
                                </h3>
                                <p className="text-sm text-gray-500">
                                    No movement data found for the selected time range.
                                    <br />
                                    Try expanding the date range.
                                </p>
                            </div>
                        </div>
                    ) : (
                        // Map with Trail
                        <TrailMap
                            points={trailData.points}
                            zoneEntries={trailData.zoneEntries}
                            currentPointIndex={currentPointIndex}
                            tenantCode={trailData.tenantCode || 'VIDP'}
                        />
                    )}
                </div>

                {/* Info Panel */}
                {selectedAssetId && trailData?.points?.length && (
                    <div className="w-80 bg-gray-800 border-l border-gray-700 overflow-y-auto">
                        <TrailInfoPanel
                            currentPoint={currentPoint}
                            summary={summaryData}
                            assetInfo={trailData}
                        />
                    </div>
                )}
            </div>

            {/* Playback Controls */}
            {selectedAssetId && trailData?.points?.length && (
                <div className="bg-gray-800 border-t border-gray-700">
                    {/* Timeline */}
                    <TrailTimeline
                        points={trailData.points}
                        zoneEntries={trailData.zoneEntries}
                        currentIndex={currentPointIndex}
                        onScrub={handleTimelineScrub}
                    />

                    {/* Playback Controls */}
                    <TrailPlaybackControls
                        isPlaying={isPlaying}
                        playbackSpeed={playbackSpeed}
                        currentIndex={currentPointIndex}
                        totalPoints={trailData.points.length}
                        currentTimestamp={currentPoint?.timestamp}
                        onPlayPause={handlePlayPause}
                        onSpeedChange={handleSpeedChange}
                    />
                </div>
            )}
        </div>
    );
};

export default MovementTrailPage;
