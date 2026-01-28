import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import MapComponent from '../components/Map/MapComponent';
import ZoneBoundariesLayer from '../components/Tracking/ZoneBoundariesLayer';
import HotspotDetailModal from '../components/Tracking/HotspotDetailModal';
import HeatLayer from '../components/Tracking/HeatLayer';
import {
    fetchActivityHeatmap,
    fetchViolationsHeatmap,
    fetchDwellHeatmap,
    transformToLeafletHeatFormat,
    exportHeatmapCSV,
    HeatmapFilters
} from '../services/heatmapService';
import { useAuth } from '../context/AuthContext';
import { 
    Flame, 
    Activity, 
    AlertTriangle, 
    Clock, 
    Download, 
    RefreshCw, 
    X, 
    ChevronDown,
    MapPin,
    Loader2,
    BarChart2
} from 'lucide-react';

type HeatmapMode = 'activity' | 'violations' | 'dwell';
type TimeRangePreset = '1h' | '24h' | '7d' | '30d';
type GridSize = 10 | 25 | 50 | 100;

/**
 * HotspotAnalysisPage - Dedicated heatmap analysis view
 * Uses same map component as Live Map for consistency
 */
const HotspotAnalysisPage: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const tenantCode = user?.icaoCode || 'YBBN';
    
    // State
    const [mode, setMode] = useState<HeatmapMode>('activity');
    const [gridSize, setGridSize] = useState<GridSize>(25);
    const [intensity] = useState<number>(75);
    const [timeRange, setTimeRange] = useState<TimeRangePreset>('24h');
    const [autoRefresh, setAutoRefresh] = useState<boolean>(false);
    const [modalOpen, setModalOpen] = useState<boolean>(false);
    const [selectedLocation] = useState<{ lat: number; lng: number } | null>(null);
    const [showZones, setShowZones] = useState<boolean>(true);

    // Map center based on tenant
    const getCenter = (): [number, number] => {
        if (tenantCode === 'VABB') return [19.0896, 72.8656];
        if (tenantCode === 'LIRN') return [40.8844, 14.2908];
        if (tenantCode === 'YBBN') return [-27.3842, 153.1175];
        return [28.5562, 77.1000]; // Default VIDP
    };

    // Calculate time range from preset
    const getTimeRange = (preset: TimeRangePreset): { start: Date; end: Date } => {
        const end = new Date();
        const start = new Date();
        
        switch (preset) {
            case '1h': start.setHours(start.getHours() - 1); break;
            case '24h': start.setHours(start.getHours() - 24); break;
            case '7d': start.setDate(start.getDate() - 7); break;
            case '30d': start.setDate(start.getDate() - 30); break;
        }
        
        return { start, end };
    };

    // Build filters
    const buildFilters = (): HeatmapFilters => {
        const { start, end } = getTimeRange(timeRange);
        return {
            tenantCode,
            gridSize,
            startTime: start.toISOString(),
            endTime: end.toISOString()
        };
    };

    // Fetch heatmap data
    const { data: heatmapData, isLoading, error, refetch } = useQuery({
        queryKey: ['heatmap', mode, gridSize, timeRange, tenantCode],
        queryFn: async () => {
            const filters = buildFilters();
            
            switch (mode) {
                case 'activity':
                    return await fetchActivityHeatmap(filters);
                case 'violations':
                    return await fetchViolationsHeatmap(filters);
                case 'dwell':
                    return await fetchDwellHeatmap(filters);
                default:
                    return await fetchActivityHeatmap(filters);
            }
        },
        refetchInterval: autoRefresh ? 60000 : false
    });

    // Transform data for Leaflet.heat
    const leafletHeatData = heatmapData?.data
        ? transformToLeafletHeatFormat(heatmapData.data, heatmapData.maxIntensity || 1)
        : [];

    // Export handlers
    const handleExport = (format: 'csv' | 'png') => {
        if (!heatmapData) return;

        if (format === 'csv') {
            const csv = exportHeatmapCSV(heatmapData.data);
            const blob = new Blob([csv], { type: 'text/csv' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `heatmap_${mode}_${new Date().toISOString().split('T')[0]}.csv`;
            a.click();
            URL.revokeObjectURL(url);
        }
    };

    const modeConfig = {
        activity: { icon: Activity, label: 'Activity', color: 'text-blue-400', description: 'Asset movement density' },
        violations: { icon: AlertTriangle, label: 'Violations', color: 'text-red-400', description: 'Zone violation hotspots' },
        dwell: { icon: Clock, label: 'Dwell Time', color: 'text-amber-400', description: 'Extended dwell areas' }
    };

    return (
        <div className="h-full w-full relative">
            {/* Loading Overlay */}
            {isLoading && (
                <div className="absolute top-20 left-1/2 transform -translate-x-1/2 z-[1100] bg-gray-900/90 rounded-lg shadow-lg px-4 py-2 flex items-center gap-2 border border-gray-700">
                    <Loader2 className="w-4 h-4 animate-spin text-orange-400" />
                    <span className="text-sm text-gray-300">Loading heatmap data...</span>
                </div>
            )}

            {/* Error Overlay */}
            {error && (
                <div className="absolute top-20 left-1/2 transform -translate-x-1/2 z-[1100] bg-red-900/90 rounded-lg shadow-lg px-4 py-3 border border-red-700">
                    <p className="text-red-200 text-sm mb-2">Error loading heatmap</p>
                    <button
                        onClick={() => refetch()}
                        className="px-3 py-1 bg-red-600 text-white text-sm rounded hover:bg-red-700"
                    >
                        Retry
                    </button>
                </div>
            )}

            {/* Top Toolbar - Mode Selection */}
            <div className="absolute top-4 left-1/2 transform -translate-x-1/2 z-[1000]">
                <div className="bg-gray-900/95 backdrop-blur-sm rounded-xl shadow-lg border border-gray-700 p-2 flex items-center gap-2">
                    {/* Mode Buttons */}
                    {(Object.keys(modeConfig) as HeatmapMode[]).map((m) => {
                        const config = modeConfig[m];
                        const Icon = config.icon;
                        const isActive = mode === m;
                        
                        return (
                            <button
                                key={m}
                                onClick={() => setMode(m)}
                                className={`
                                    flex items-center gap-2 px-3 py-2 rounded-lg transition-all text-sm font-medium
                                    ${isActive 
                                        ? 'bg-orange-600 text-white' 
                                        : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                                    }
                                `}
                                title={config.description}
                            >
                                <Icon size={16} />
                                <span>{config.label}</span>
                            </button>
                        );
                    })}

                    {/* Divider */}
                    <div className="w-px h-6 bg-gray-700 mx-1" />

                    {/* Time Range Dropdown */}
                    <div className="relative group">
                        <button className="flex items-center gap-2 px-3 py-2 rounded-lg text-gray-400 hover:bg-gray-800 hover:text-white transition-all text-sm">
                            <Clock size={16} />
                            <span>{timeRange}</span>
                            <ChevronDown size={14} />
                        </button>
                        <div className="absolute top-full left-0 mt-1 bg-gray-900 border border-gray-700 rounded-lg shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all">
                            {(['1h', '24h', '7d', '30d'] as TimeRangePreset[]).map((tr) => (
                                <button
                                    key={tr}
                                    onClick={() => setTimeRange(tr)}
                                    className={`block w-full text-left px-4 py-2 text-sm hover:bg-gray-800 ${
                                        timeRange === tr ? 'text-orange-400' : 'text-gray-300'
                                    }`}
                                >
                                    Last {tr}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Grid Size */}
                    <div className="relative group">
                        <button className="flex items-center gap-2 px-3 py-2 rounded-lg text-gray-400 hover:bg-gray-800 hover:text-white transition-all text-sm">
                            <MapPin size={16} />
                            <span>{gridSize}m</span>
                            <ChevronDown size={14} />
                        </button>
                        <div className="absolute top-full left-0 mt-1 bg-gray-900 border border-gray-700 rounded-lg shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all">
                            {([10, 25, 50, 100] as GridSize[]).map((gs) => (
                                <button
                                    key={gs}
                                    onClick={() => setGridSize(gs)}
                                    className={`block w-full text-left px-4 py-2 text-sm hover:bg-gray-800 ${
                                        gridSize === gs ? 'text-orange-400' : 'text-gray-300'
                                    }`}
                                >
                                    {gs}m grid
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Divider */}
                    <div className="w-px h-6 bg-gray-700 mx-1" />

                    {/* Auto Refresh */}
                    <button
                        onClick={() => setAutoRefresh(!autoRefresh)}
                        className={`p-2 rounded-lg transition-all ${
                            autoRefresh 
                                ? 'bg-green-600 text-white' 
                                : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                        }`}
                        title={autoRefresh ? 'Auto-refresh ON' : 'Auto-refresh OFF'}
                    >
                        <RefreshCw size={16} className={autoRefresh ? 'animate-spin' : ''} />
                    </button>

                    {/* Export */}
                    <button
                        onClick={() => handleExport('csv')}
                        className="p-2 rounded-lg text-gray-400 hover:bg-gray-800 hover:text-white transition-all"
                        title="Export CSV"
                    >
                        <Download size={16} />
                    </button>

                    {/* Toggle Zones */}
                    <button
                        onClick={() => setShowZones(!showZones)}
                        className={`p-2 rounded-lg transition-all ${
                            showZones 
                                ? 'bg-purple-600 text-white' 
                                : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                        }`}
                        title="Toggle Zones"
                    >
                        <MapPin size={16} />
                    </button>

                    {/* Back to Live Map */}
                    <button
                        onClick={() => navigate('/')}
                        className="p-2 rounded-lg text-gray-400 hover:bg-gray-800 hover:text-white transition-all"
                        title="Back to Live Map"
                    >
                        <X size={16} />
                    </button>
                </div>
            </div>

            {/* Map */}
            <MapComponent center={getCenter()} zoom={14} theme="dark">
                {showZones && <ZoneBoundariesLayer />}
                
                {leafletHeatData.length > 0 && (
                    <HeatLayer
                        data={leafletHeatData}
                        gridSize={gridSize}
                        intensity={intensity}
                    />
                )}
            </MapComponent>

            {/* Data Summary - Bottom Left */}
            {heatmapData && heatmapData.data && heatmapData.data.length > 0 && (
                <div className="absolute bottom-4 left-4 z-[1000] bg-gray-900/95 backdrop-blur-sm rounded-lg border border-gray-700 p-3">
                    <div className="flex items-center gap-4 text-sm">
                        <div className="flex items-center gap-2">
                            <BarChart2 size={14} className="text-orange-400" />
                            <span className="text-gray-400">Hotspots:</span>
                            <span className="text-white font-medium">{heatmapData.totalPoints || 0}</span>
                        </div>
                        <div className="w-px h-4 bg-gray-700" />
                        <div className="flex items-center gap-2">
                            <Flame size={14} className="text-red-400" />
                            <span className="text-gray-400">Max:</span>
                            <span className="text-white font-medium">{(heatmapData.maxIntensity || 0).toFixed(1)}</span>
                        </div>
                    </div>
                </div>
            )}

            {/* Heatmap Legend - Bottom Right */}
            <div className="absolute bottom-4 right-4 z-[1000] bg-gray-900/95 backdrop-blur-sm rounded-lg border border-gray-700 p-3">
                <div className="flex items-center gap-2 mb-2">
                    <Flame size={14} className="text-orange-400" />
                    <span className="text-xs text-gray-400 uppercase tracking-wide">Intensity</span>
                </div>
                <div className="h-3 w-40 rounded-full" style={{
                    background: 'linear-gradient(to right, #000033, #0000ff, #00ff00, #ffff00, #ff0000)'
                }} />
                <div className="flex justify-between text-xs text-gray-500 mt-1">
                    <span>Low</span>
                    <span>High</span>
                </div>
            </div>

            {/* Hotspot Detail Modal */}
            {selectedLocation && (
                <HotspotDetailModal
                    isOpen={modalOpen}
                    onClose={() => setModalOpen(false)}
                    latitude={selectedLocation.lat}
                    longitude={selectedLocation.lng}
                    gridSize={gridSize}
                    mode={mode}
                    tenantCode={tenantCode}
                    startTime={buildFilters().startTime}
                    endTime={buildFilters().endTime}
                />
            )}
        </div>
    );
};

export default HotspotAnalysisPage;
