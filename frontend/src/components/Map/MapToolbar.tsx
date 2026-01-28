import React, { useState } from 'react';
import { 
    Plane, 
    Truck, 
    Package, 
    Flame, 
    Filter,
    ChevronDown,
    AlertTriangle,
    Eye
} from 'lucide-react';

interface LayerState {
    flights: boolean;
    vehicles: boolean;
    assets: boolean;
    heatmap: boolean;
    zones: boolean;
    alerts: boolean;
}

interface MapToolbarProps {
    layers: LayerState;
    onLayerToggle: (layer: keyof LayerState) => void;
    heatmapMode?: 'activity' | 'violations' | 'dwell' | null;
    onHeatmapModeChange?: (mode: 'activity' | 'violations' | 'dwell' | null) => void;
    onOpenFilters?: () => void;
    alertCount?: number;
}

/**
 * MapToolbar - Compact floating toolbar for map controls
 * Provides layer toggles and quick access to filters
 */
const MapToolbar: React.FC<MapToolbarProps> = ({
    layers,
    onLayerToggle,
    heatmapMode,
    onHeatmapModeChange,
    onOpenFilters,
    alertCount = 0
}) => {
    const [showHeatmapOptions, setShowHeatmapOptions] = useState(false);

    const layerButtons = [
        { key: 'flights' as const, icon: Plane, label: 'Flights', color: 'text-blue-400' },
        { key: 'vehicles' as const, icon: Truck, label: 'Vehicles', color: 'text-green-400' },
        { key: 'assets' as const, icon: Package, label: 'Assets', color: 'text-purple-400' },
        { key: 'zones' as const, icon: Eye, label: 'Zones', color: 'text-yellow-400' },
    ];

    const heatmapModes = [
        { key: 'activity' as const, label: 'Activity', description: 'Movement density' },
        { key: 'violations' as const, label: 'Violations', description: 'Zone breaches' },
        { key: 'dwell' as const, label: 'Dwell Time', description: 'Stationary hotspots' },
    ];

    return (
        <div className="absolute top-4 left-1/2 transform -translate-x-1/2 z-[1000]">
            <div className="bg-gray-900/95 backdrop-blur-sm rounded-xl shadow-2xl border border-gray-700/50 px-2 py-2 flex items-center gap-1">
                {/* Layer Toggle Buttons */}
                {layerButtons.map(({ key, icon: Icon, label, color }) => (
                    <button
                        key={key}
                        onClick={() => onLayerToggle(key)}
                        className={`relative group px-3 py-2 rounded-lg transition-all duration-200 flex items-center gap-2 ${
                            layers[key]
                                ? 'bg-gray-700 text-white'
                                : 'text-gray-500 hover:text-gray-300 hover:bg-gray-800'
                        }`}
                        title={label}
                    >
                        <Icon className={`w-4 h-4 ${layers[key] ? color : ''}`} />
                        <span className="text-xs font-medium hidden sm:inline">{label}</span>
                        
                        {/* Active indicator */}
                        {layers[key] && (
                            <span className={`absolute -top-1 -right-1 w-2 h-2 rounded-full ${color.replace('text-', 'bg-')}`} />
                        )}
                    </button>
                ))}

                {/* Divider */}
                <div className="w-px h-8 bg-gray-700 mx-1" />

                {/* Heatmap Toggle with Dropdown */}
                <div className="relative">
                    <button
                        onClick={() => {
                            if (heatmapMode) {
                                onHeatmapModeChange?.(null);
                                setShowHeatmapOptions(false);
                            } else {
                                setShowHeatmapOptions(!showHeatmapOptions);
                            }
                        }}
                        className={`px-3 py-2 rounded-lg transition-all duration-200 flex items-center gap-2 ${
                            heatmapMode
                                ? 'bg-orange-600/20 text-orange-400 border border-orange-500/30'
                                : 'text-gray-500 hover:text-gray-300 hover:bg-gray-800'
                        }`}
                    >
                        <Flame className={`w-4 h-4 ${heatmapMode ? 'text-orange-400' : ''}`} />
                        <span className="text-xs font-medium hidden sm:inline">
                            {heatmapMode ? heatmapMode.charAt(0).toUpperCase() + heatmapMode.slice(1) : 'Heatmap'}
                        </span>
                        <ChevronDown className={`w-3 h-3 transition-transform ${showHeatmapOptions ? 'rotate-180' : ''}`} />
                    </button>

                    {/* Heatmap Mode Dropdown */}
                    {showHeatmapOptions && !heatmapMode && (
                        <div className="absolute top-full mt-2 left-0 bg-gray-900 border border-gray-700 rounded-lg shadow-xl py-1 min-w-[160px]">
                            {heatmapModes.map(mode => (
                                <button
                                    key={mode.key}
                                    onClick={() => {
                                        onHeatmapModeChange?.(mode.key);
                                        setShowHeatmapOptions(false);
                                    }}
                                    className="w-full px-4 py-2 text-left hover:bg-gray-800 transition-colors"
                                >
                                    <div className="text-sm text-white">{mode.label}</div>
                                    <div className="text-xs text-gray-500">{mode.description}</div>
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                {/* Divider */}
                <div className="w-px h-8 bg-gray-700 mx-1" />

                {/* Alerts Toggle */}
                <button
                    onClick={() => onLayerToggle('alerts')}
                    className={`relative px-3 py-2 rounded-lg transition-all duration-200 flex items-center gap-2 ${
                        layers.alerts
                            ? 'bg-red-600/20 text-red-400 border border-red-500/30'
                            : 'text-gray-500 hover:text-gray-300 hover:bg-gray-800'
                    }`}
                    title="Alerts"
                >
                    <AlertTriangle className="w-4 h-4" />
                    {alertCount > 0 && (
                        <span className="absolute -top-1 -right-1 bg-red-500 text-white text-[10px] font-bold rounded-full min-w-[18px] h-[18px] flex items-center justify-center">
                            {alertCount > 99 ? '99+' : alertCount}
                        </span>
                    )}
                </button>

                {/* Filter Button */}
                {onOpenFilters && (
                    <button
                        onClick={onOpenFilters}
                        className="px-3 py-2 rounded-lg text-gray-500 hover:text-gray-300 hover:bg-gray-800 transition-all duration-200 flex items-center gap-2"
                        title="Advanced Filters"
                    >
                        <Filter className="w-4 h-4" />
                        <span className="text-xs font-medium hidden sm:inline">Filters</span>
                    </button>
                )}
            </div>
        </div>
    );
};

export default MapToolbar;
