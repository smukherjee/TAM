import React, { useState } from 'react';
import { ChevronUp, ChevronDown, Info } from 'lucide-react';

interface CompactLegendProps {
    showFlights?: boolean;
    showVehicles?: boolean;
    showAssets?: boolean;
    showHeatmap?: boolean;
    heatmapMode?: 'activity' | 'violations' | 'dwell' | null;
}

/**
 * CompactLegend - Minimal map legend that expands on hover/click
 * Shows only relevant legends based on active layers
 */
const CompactLegend: React.FC<CompactLegendProps> = ({
    showFlights = false,
    showVehicles = false,
    showAssets = false,
    showHeatmap = false,
    heatmapMode = null
}) => {
    const [expanded, setExpanded] = useState(false);

    // Don't render if no layers are active
    if (!showFlights && !showVehicles && !showAssets && !showHeatmap) {
        return null;
    }

    const assetCategories = [
        { name: 'Emergency', color: '#ef4444' },
        { name: 'Fueling', color: '#f97316' },
        { name: 'Ground Support', color: '#22c55e' },
        { name: 'Cargo', color: '#3b82f6' },
        { name: 'Catering', color: '#a855f7' },
    ];

    const markerStates = [
        { name: 'Active', color: '#3b82f6' },
        { name: 'Alert', color: '#ef4444' },
        { name: 'Idle', color: '#6b7280' },
    ];

    const heatmapGradient = [
        { label: 'Low', color: '#3b82f6' },
        { label: 'Medium', color: '#22c55e' },
        { label: 'High', color: '#eab308' },
        { label: 'Critical', color: '#ef4444' },
    ];

    return (
        <div className="absolute bottom-4 right-4 z-[1000]">
            <div className={`bg-gray-900/95 backdrop-blur-sm rounded-lg border border-gray-700/50 shadow-xl overflow-hidden transition-all duration-300 ${
                expanded ? 'w-48' : 'w-auto'
            }`}>
                {/* Header - Always visible */}
                <button
                    onClick={() => setExpanded(!expanded)}
                    className="w-full flex items-center justify-between px-3 py-2 hover:bg-gray-800/50 transition-colors"
                >
                    <div className="flex items-center gap-2">
                        <Info className="w-4 h-4 text-gray-400" />
                        <span className="text-xs font-medium text-gray-300">Legend</span>
                    </div>
                    {expanded ? (
                        <ChevronDown className="w-3 h-3 text-gray-500" />
                    ) : (
                        <ChevronUp className="w-3 h-3 text-gray-500" />
                    )}
                </button>

                {/* Expanded Content */}
                {expanded && (
                    <div className="px-3 pb-3 space-y-3 max-h-64 overflow-y-auto">
                        {/* Heatmap Legend */}
                        {showHeatmap && heatmapMode && (
                            <div>
                                <div className="text-[10px] text-gray-500 uppercase tracking-wide mb-1.5">
                                    {heatmapMode} Intensity
                                </div>
                                <div className="flex items-center gap-1">
                                    {heatmapGradient.map((item) => (
                                        <div key={item.label} className="flex-1 text-center">
                                            <div 
                                                className="h-2 rounded-sm" 
                                                style={{ backgroundColor: item.color }}
                                            />
                                            <div className="text-[8px] text-gray-500 mt-0.5">{item.label}</div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Asset Categories */}
                        {showAssets && (
                            <div>
                                <div className="text-[10px] text-gray-500 uppercase tracking-wide mb-1.5">
                                    Asset Types
                                </div>
                                <div className="grid grid-cols-2 gap-1">
                                    {assetCategories.map(cat => (
                                        <div key={cat.name} className="flex items-center gap-1.5">
                                            <div 
                                                className="w-2.5 h-2.5 rounded-sm" 
                                                style={{ backgroundColor: cat.color }}
                                            />
                                            <span className="text-[10px] text-gray-400">{cat.name}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Marker States */}
                        {(showAssets || showVehicles) && (
                            <div>
                                <div className="text-[10px] text-gray-500 uppercase tracking-wide mb-1.5">
                                    Status
                                </div>
                                <div className="flex gap-3">
                                    {markerStates.map(state => (
                                        <div key={state.name} className="flex items-center gap-1">
                                            <div 
                                                className="w-2 h-2 rounded-full" 
                                                style={{ backgroundColor: state.color }}
                                            />
                                            <span className="text-[10px] text-gray-400">{state.name}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Flight States */}
                        {showFlights && (
                            <div>
                                <div className="text-[10px] text-gray-500 uppercase tracking-wide mb-1.5">
                                    Flights
                                </div>
                                <div className="flex gap-3">
                                    <div className="flex items-center gap-1">
                                        <div className="w-0 h-0 border-l-[4px] border-l-transparent border-r-[4px] border-r-transparent border-b-[6px] border-b-blue-400" />
                                        <span className="text-[10px] text-gray-400">Airborne</span>
                                    </div>
                                    <div className="flex items-center gap-1">
                                        <div className="w-0 h-0 border-l-[4px] border-l-transparent border-r-[4px] border-r-transparent border-b-[6px] border-b-green-400" />
                                        <span className="text-[10px] text-gray-400">Landed</span>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default CompactLegend;
