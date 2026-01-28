import { useState } from 'react';
import { ChevronUp, ChevronDown } from 'lucide-react';

/**
 * HeatmapLegend Component
 * Displays color gradient legend for heatmap intensity
 * Feature: 005-asset-tracking-security (Task T052)
 */

interface HeatmapLegendProps {
    mode: 'activity' | 'violations' | 'dwell';
    gridSize: number;
    timeRange: string;
    defaultExpanded?: boolean;
}

const HeatmapLegend: React.FC<HeatmapLegendProps> = ({
    mode,
    gridSize,
    timeRange,
    defaultExpanded = true
}) => {
    const [isExpanded, setIsExpanded] = useState(defaultExpanded);

    const getModeLabel = () => {
        switch (mode) {
            case 'activity':
                return 'Activity Density';
            case 'violations':
                return 'Violation Intensity';
            case 'dwell':
                return 'Dwell Time';
            default:
                return 'Intensity';
        }
    };

    return (
        <div className="absolute bottom-4 left-4 bg-white rounded-lg shadow-lg z-[1000] overflow-hidden">
            {/* Header */}
            <button
                onClick={() => setIsExpanded(!isExpanded)}
                className="w-full px-4 py-3 flex items-center justify-between hover:bg-gray-50 transition"
            >
                <span className="text-sm font-semibold text-gray-800">Heatmap Legend</span>
                {isExpanded ? (
                    <ChevronDown className="w-4 h-4 text-gray-500" />
                ) : (
                    <ChevronUp className="w-4 h-4 text-gray-500" />
                )}
            </button>

            {/* Content */}
            {isExpanded && (
                <div className="px-4 pb-4 border-t border-gray-200">
                    {/* Color Gradient */}
                    <div className="mt-3">
                        <div className="flex items-center gap-3">
                            {/* Gradient Bar */}
                            <div 
                                className="w-8 h-48 rounded"
                                style={{
                                    background: 'linear-gradient(to top, rgb(0, 0, 255) 0%, rgb(0, 255, 255) 25%, rgb(0, 255, 0) 50%, rgb(255, 255, 0) 75%, rgb(255, 0, 0) 100%)'
                                }}
                            />

                            {/* Labels */}
                            <div className="flex flex-col justify-between h-48 text-xs text-gray-600">
                                <div className="flex items-center gap-2">
                                    <div className="w-2 h-px bg-gray-300"></div>
                                    <span>High (100)</span>
                                </div>
                                <div className="flex items-center gap-2">
                                    <div className="w-2 h-px bg-gray-300"></div>
                                    <span>75</span>
                                </div>
                                <div className="flex items-center gap-2">
                                    <div className="w-2 h-px bg-gray-300"></div>
                                    <span>50</span>
                                </div>
                                <div className="flex items-center gap-2">
                                    <div className="w-2 h-px bg-gray-300"></div>
                                    <span>25</span>
                                </div>
                                <div className="flex items-center gap-2">
                                    <div className="w-2 h-px bg-gray-300"></div>
                                    <span>Low (0)</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Info */}
                    <div className="mt-4 pt-3 border-t border-gray-200 space-y-1.5 text-xs">
                        <div className="flex items-center justify-between">
                            <span className="text-gray-500">Showing:</span>
                            <span className="font-medium text-gray-700">{getModeLabel()}</span>
                        </div>
                        <div className="flex items-center justify-between">
                            <span className="text-gray-500">Grid:</span>
                            <span className="font-medium text-gray-700">{gridSize}m</span>
                        </div>
                        <div className="flex items-center justify-between">
                            <span className="text-gray-500">Period:</span>
                            <span className="font-medium text-gray-700">
                                {timeRange === '1h' && 'Last 1 hour'}
                                {timeRange === '24h' && 'Last 24 hours'}
                                {timeRange === '7d' && 'Last 7 days'}
                                {timeRange === '30d' && 'Last 30 days'}
                                {timeRange === 'custom' && 'Custom range'}
                            </span>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default HeatmapLegend;
