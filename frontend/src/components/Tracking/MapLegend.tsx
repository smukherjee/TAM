import React, { useState } from 'react';
import { ChevronDown, ChevronUp } from 'lucide-react';

interface MapLegendProps {
    defaultExpanded?: boolean;
}

/**
 * Map Legend Component - Displays marker colors and states
 * Feature: 005-asset-tracking-security (Task T043)
 */
const MapLegend: React.FC<MapLegendProps> = ({ defaultExpanded = true }) => {
    const [expanded, setExpanded] = useState(defaultExpanded);

    const categoryColors = [
        { name: 'Emergency', color: '#ef4444', description: 'Ambulance, Fire Truck' },
        { name: 'Fueling', color: '#f97316', description: 'Fuel Trucks' },
        { name: 'Ground Support', color: '#22c55e', description: 'GPU, Equipment' },
        { name: 'Cargo', color: '#3b82f6', description: 'Belt Loaders, Cargo Equipment' },
        { name: 'Power', color: '#eab308', description: 'Power Units' },
        { name: 'Catering', color: '#a855f7', description: 'Catering Trucks' },
        { name: 'Cleaning', color: '#06b6d4', description: 'Cleaning Vehicles' },
        { name: 'Maintenance', color: '#f59e0b', description: 'Maintenance Equipment' },
    ];

    const markerStates = [
        { name: 'Active', color: '#3b82f6', description: 'Moving, operational' },
        { name: 'Alert', color: '#ef4444', description: 'Violation, emergency (pulsing)' },
        { name: 'Warning', color: '#f59e0b', description: 'Needs attention' },
        { name: 'Idle', color: '#6b7280', description: 'Stationary, inactive' },
    ];

    const zoneColors = [
        { name: 'Prohibited', color: '#ff0000', description: 'No unauthorized access' },
        { name: 'Restricted', color: '#ffa500', description: 'Limited access' },
        { name: 'Controlled', color: '#ffff00', description: 'Monitored access' },
        { name: 'Maintenance', color: '#0000ff', description: 'Maintenance zones' },
    ];

    return (
        <div className="absolute bottom-6 right-6 bg-gray-900 bg-opacity-95 rounded-lg shadow-lg border border-gray-700 z-[1000]">
            {/* Header */}
            <button
                onClick={() => setExpanded(!expanded)}
                className="w-full flex items-center justify-between px-4 py-3 hover:bg-gray-800 transition-colors rounded-t-lg"
            >
                <h3 className="text-sm font-semibold text-white">Map Legend</h3>
                {expanded ? (
                    <ChevronDown className="w-4 h-4 text-gray-400" />
                ) : (
                    <ChevronUp className="w-4 h-4 text-gray-400" />
                )}
            </button>

            {/* Content */}
            {expanded && (
                <div className="px-4 pb-4 space-y-4 max-h-96 overflow-y-auto">
                    {/* Asset Categories */}
                    <div>
                        <h4 className="text-xs font-semibold text-gray-400 mb-2 uppercase tracking-wide">
                            Asset Categories
                        </h4>
                        <div className="space-y-1.5">
                            {categoryColors.map((category) => (
                                <div key={category.name} className="flex items-start gap-2">
                                    <div className="mt-0.5">
                                        <svg width="16" height="16" viewBox="0 0 16 16">
                                            <rect
                                                x="2"
                                                y="2"
                                                width="12"
                                                height="12"
                                                rx="2"
                                                fill={category.color}
                                                stroke="white"
                                                strokeWidth="1.5"
                                            />
                                        </svg>
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="text-xs font-medium text-gray-200">
                                            {category.name}
                                        </div>
                                        <div className="text-[10px] text-gray-500">
                                            {category.description}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Marker States */}
                    <div className="border-t border-gray-700 pt-3">
                        <h4 className="text-xs font-semibold text-gray-400 mb-2 uppercase tracking-wide">
                            Marker States
                        </h4>
                        <div className="space-y-1.5">
                            {markerStates.map((state) => (
                                <div key={state.name} className="flex items-start gap-2">
                                    <div className="mt-0.5">
                                        <svg width="16" height="16" viewBox="0 0 16 16">
                                            <circle
                                                cx="8"
                                                cy="8"
                                                r="6"
                                                fill={state.color}
                                                stroke="white"
                                                strokeWidth="1.5"
                                            />
                                        </svg>
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="text-xs font-medium text-gray-200">
                                            {state.name}
                                        </div>
                                        <div className="text-[10px] text-gray-500">
                                            {state.description}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Zone Colors */}
                    <div className="border-t border-gray-700 pt-3">
                        <h4 className="text-xs font-semibold text-gray-400 mb-2 uppercase tracking-wide">
                            Restricted Zones
                        </h4>
                        <div className="space-y-1.5">
                            {zoneColors.map((zone) => (
                                <div key={zone.name} className="flex items-start gap-2">
                                    <div className="mt-0.5">
                                        <svg width="16" height="16" viewBox="0 0 16 16">
                                            <rect
                                                x="2"
                                                y="2"
                                                width="12"
                                                height="12"
                                                fill={`${zone.color}33`}  // 20% opacity
                                                stroke={zone.color}
                                                strokeWidth="2"
                                            />
                                        </svg>
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <div className="text-xs font-medium text-gray-200">
                                            {zone.name}
                                        </div>
                                        <div className="text-[10px] text-gray-500">
                                            {zone.description}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Additional Info */}
                    <div className="border-t border-gray-700 pt-3">
                        <h4 className="text-xs font-semibold text-gray-400 mb-2 uppercase tracking-wide">
                            Interactions
                        </h4>
                        <div className="space-y-1 text-[10px] text-gray-500">
                            <div>• Click marker to view asset details</div>
                            <div>• Pulsing markers indicate violations</div>
                            <div>• Use search bar to find specific assets</div>
                            <div>• Toggle layers in control panel</div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default MapLegend;
