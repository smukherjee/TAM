import { useState } from 'react';
import { 
    Activity, 
    AlertTriangle, 
    Clock, 
    Download, 
    Map as MapIcon 
} from 'lucide-react';

/**
 * HeatmapControls Component
 * Control panel for heatmap analysis with filters and export
 * Feature: 005-asset-tracking-security (Task T050)
 */

export type HeatmapMode = 'activity' | 'violations' | 'dwell';
export type GridSize = 10 | 25 | 50 | 100;
export type TimeRangePreset = '1h' | '24h' | '7d' | '30d' | 'custom';

interface HeatmapControlsProps {
    mode: HeatmapMode;
    gridSize: GridSize;
    intensity: number;
    timeRange: TimeRangePreset;
    autoRefresh: boolean;
    onModeChange: (mode: HeatmapMode) => void;
    onGridSizeChange: (size: GridSize) => void;
    onIntensityChange: (intensity: number) => void;
    onTimeRangeChange: (range: TimeRangePreset) => void;
    onCustomTimeRange?: (start: Date, end: Date) => void;
    onAutoRefreshToggle: (enabled: boolean) => void;
    onSwitchToAssetView: () => void;
    onExport: (format: 'png' | 'csv' | 'pdf') => void;
}

const HeatmapControls: React.FC<HeatmapControlsProps> = ({
    mode,
    gridSize,
    intensity,
    timeRange,
    autoRefresh,
    onModeChange,
    onGridSizeChange,
    onIntensityChange,
    onTimeRangeChange,
    onAutoRefreshToggle,
    onSwitchToAssetView,
    onExport
}) => {
    const [showExportMenu, setShowExportMenu] = useState(false);

    const handleExport = (format: 'png' | 'csv' | 'pdf') => {
        onExport(format);
        setShowExportMenu(false);
    };

    return (
        <div className="bg-white rounded-lg shadow-lg p-6 space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between border-b pb-4">
                <h2 className="text-lg font-semibold text-gray-800">Heatmap Controls</h2>
                <button
                    onClick={onSwitchToAssetView}
                    className="flex items-center gap-2 px-3 py-1.5 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 transition"
                >
                    <MapIcon className="w-4 h-4" />
                    Asset View
                </button>
            </div>

            {/* Mode Selector */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                    Analysis Mode
                </label>
                <div className="grid grid-cols-3 gap-2">
                    <button
                        onClick={() => onModeChange('activity')}
                        className={`flex flex-col items-center gap-1 p-3 rounded-lg border-2 transition ${
                            mode === 'activity'
                                ? 'border-blue-500 bg-blue-50 text-blue-700'
                                : 'border-gray-200 hover:border-gray-300 text-gray-600'
                        }`}
                    >
                        <Activity className="w-5 h-5" />
                        <span className="text-xs font-medium">Activity</span>
                    </button>
                    <button
                        onClick={() => onModeChange('violations')}
                        className={`flex flex-col items-center gap-1 p-3 rounded-lg border-2 transition ${
                            mode === 'violations'
                                ? 'border-red-500 bg-red-50 text-red-700'
                                : 'border-gray-200 hover:border-gray-300 text-gray-600'
                        }`}
                    >
                        <AlertTriangle className="w-5 h-5" />
                        <span className="text-xs font-medium">Violations</span>
                    </button>
                    <button
                        onClick={() => onModeChange('dwell')}
                        className={`flex flex-col items-center gap-1 p-3 rounded-lg border-2 transition ${
                            mode === 'dwell'
                                ? 'border-amber-500 bg-amber-50 text-amber-700'
                                : 'border-gray-200 hover:border-gray-300 text-gray-600'
                        }`}
                    >
                        <Clock className="w-5 h-5" />
                        <span className="text-xs font-medium">Dwell</span>
                    </button>
                </div>
            </div>

            {/* Grid Resolution */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                    Grid Resolution
                </label>
                <select
                    value={gridSize}
                    onChange={(e) => onGridSizeChange(Number(e.target.value) as GridSize)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                    <option value={10}>10m (Fine)</option>
                    <option value={25}>25m (Medium)</option>
                    <option value={50}>50m (Coarse)</option>
                    <option value={100}>100m (Very Coarse)</option>
                </select>
            </div>

            {/* Time Range */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                    Time Range
                </label>
                <div className="grid grid-cols-4 gap-2 mb-2">
                    {(['1h', '24h', '7d', '30d'] as TimeRangePreset[]).map((preset) => (
                        <button
                            key={preset}
                            onClick={() => onTimeRangeChange(preset)}
                            className={`px-3 py-2 text-sm rounded-md transition ${
                                timeRange === preset
                                    ? 'bg-blue-600 text-white'
                                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                            }`}
                        >
                            {preset}
                        </button>
                    ))}
                </div>
                <button
                    onClick={() => onTimeRangeChange('custom')}
                    className={`w-full px-3 py-2 text-sm rounded-md transition ${
                        timeRange === 'custom'
                            ? 'bg-blue-600 text-white'
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                >
                    Custom Range
                </button>
                {/* TODO: Add date picker for custom range */}
            </div>

            {/* Intensity Slider */}
            <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                    Intensity: {intensity}%
                </label>
                <input
                    type="range"
                    min="0"
                    max="100"
                    value={intensity}
                    onChange={(e) => onIntensityChange(Number(e.target.value))}
                    className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-blue-600"
                />
                <div className="flex justify-between text-xs text-gray-500 mt-1">
                    <span>Low</span>
                    <span>High</span>
                </div>
            </div>

            {/* Auto-refresh Toggle */}
            <div className="flex items-center justify-between">
                <label className="text-sm font-medium text-gray-700">
                    Auto-refresh (60s)
                </label>
                <button
                    onClick={() => onAutoRefreshToggle(!autoRefresh)}
                    className={`relative inline-flex h-6 w-11 items-center rounded-full transition ${
                        autoRefresh ? 'bg-blue-600' : 'bg-gray-300'
                    }`}
                >
                    <span
                        className={`inline-block h-4 w-4 transform rounded-full bg-white transition ${
                            autoRefresh ? 'translate-x-6' : 'translate-x-1'
                        }`}
                    />
                </button>
            </div>

            {/* Export Menu */}
            <div className="relative">
                <button
                    onClick={() => setShowExportMenu(!showExportMenu)}
                    className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition"
                >
                    <Download className="w-4 h-4" />
                    Export
                </button>
                {showExportMenu && (
                    <div className="absolute bottom-full mb-2 w-full bg-white rounded-lg shadow-lg border border-gray-200 py-1 z-10">
                        <button
                            onClick={() => handleExport('png')}
                            className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 transition"
                        >
                            Export as PNG
                        </button>
                        <button
                            onClick={() => handleExport('csv')}
                            className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 transition"
                        >
                            Export as CSV
                        </button>
                        <button
                            onClick={() => handleExport('pdf')}
                            className="w-full px-4 py-2 text-left text-sm hover:bg-gray-100 transition"
                        >
                            Export as PDF
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default HeatmapControls;
