import React from 'react';
import { formatDistanceToNow, format } from 'date-fns';
import { MovementDiscrepancy, SEVERITY_COLORS, DISCREPANCY_TYPE_LABELS } from '../../types/tracking';
import { 
    CheckCircle, 
    AlertTriangle, 
    ChevronLeft, 
    ChevronRight,
    MapPin,
    ArrowRight
} from 'lucide-react';

interface DiscrepancyTableProps {
    discrepancies: MovementDiscrepancy[];
    totalPages: number;
    currentPage: number;
    onPageChange: (page: number) => void;
    onAcknowledge: (discrepancy: MovementDiscrepancy) => void;
    onViewOnMap: (discrepancy: MovementDiscrepancy) => void;
}

/**
 * DiscrepancyTable - Display movement discrepancies in a sortable, paginated table
 * Feature: 005-asset-tracking-security
 * Phase 10: Movement Discrepancy Report
 */
const DiscrepancyTable: React.FC<DiscrepancyTableProps> = ({
    discrepancies,
    totalPages,
    currentPage,
    onPageChange,
    onAcknowledge,
    onViewOnMap
}) => {
    // Severity badge styling
    const getSeverityBadge = (severity: string) => {
        const color = SEVERITY_COLORS[severity] || '#6B7280';
        return (
            <span
                className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                style={{ 
                    backgroundColor: `${color}20`, 
                    color: color,
                    border: `1px solid ${color}40`
                }}
            >
                {severity}
            </span>
        );
    };

    // Discrepancy type badge
    const getTypeBadge = (type: string) => {
        const colors: Record<string, { bg: string; text: string }> = {
            'UNEXPECTED_MOVEMENT': { bg: 'bg-red-900/50', text: 'text-red-400' },
            'STATUS_MISMATCH': { bg: 'bg-yellow-900/50', text: 'text-yellow-400' },
            'SPEED_ANOMALY': { bg: 'bg-purple-900/50', text: 'text-purple-400' },
            'GHOST_ASSET': { bg: 'bg-gray-700', text: 'text-gray-300' },
            'LOCATION_JUMP': { bg: 'bg-blue-900/50', text: 'text-blue-400' }
        };
        const style = colors[type] || { bg: 'bg-gray-700', text: 'text-gray-300' };
        const label = DISCREPANCY_TYPE_LABELS[type as keyof typeof DISCREPANCY_TYPE_LABELS] || type;
        return (
            <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${style.bg} ${style.text}`}>
                {label}
            </span>
        );
    };

    // Format deviation
    const formatDeviation = (meters?: number) => {
        if (!meters) return '-';
        if (meters < 1000) return `${meters.toFixed(0)}m`;
        return `${(meters / 1000).toFixed(2)}km`;
    };

    return (
        <div className="bg-gray-800 rounded-lg overflow-hidden">
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-700">
                    <thead className="bg-gray-700">
                        <tr>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Asset
                            </th>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Type
                            </th>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Location
                            </th>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Deviation
                            </th>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Severity
                            </th>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Time
                            </th>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Status
                            </th>
                            <th scope="col" className="px-4 py-3 text-right text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Actions
                            </th>
                        </tr>
                    </thead>
                    <tbody className="bg-gray-800 divide-y divide-gray-700">
                        {discrepancies.map((discrepancy) => (
                            <tr 
                                key={discrepancy.id} 
                                className={`hover:bg-gray-700 ${!discrepancy.acknowledged ? 'bg-orange-900/20' : ''}`}
                            >
                                {/* Asset */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    <div className="flex items-center">
                                        <div>
                                            <div className="text-sm font-medium text-white">
                                                {discrepancy.assetIdentifier}
                                            </div>
                                            <div className="text-xs text-gray-400">
                                                {discrepancy.assetName || discrepancy.assetCategory}
                                            </div>
                                        </div>
                                    </div>
                                </td>

                                {/* Type */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    {getTypeBadge(discrepancy.discrepancyType)}
                                </td>

                                {/* Location */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    <div className="flex items-center text-sm">
                                        {discrepancy.expectedLatitude && discrepancy.expectedLongitude ? (
                                            <div className="flex items-center">
                                                <div className="flex items-center text-green-400">
                                                    <MapPin className="h-3.5 w-3.5 mr-1" />
                                                    <span className="font-mono text-xs">
                                                        {discrepancy.expectedLatitude.toFixed(4)}
                                                    </span>
                                                </div>
                                                <ArrowRight className="h-3 w-3 mx-1 text-gray-500" />
                                                <div className="flex items-center text-red-400">
                                                    <MapPin className="h-3.5 w-3.5 mr-1" />
                                                    <span className="font-mono text-xs">
                                                        {discrepancy.actualLatitude?.toFixed(4)}
                                                    </span>
                                                </div>
                                            </div>
                                        ) : (
                                            <div className="flex items-center text-gray-300">
                                                <MapPin className="h-3.5 w-3.5 mr-1" />
                                                <span className="font-mono text-xs">
                                                    {discrepancy.actualLatitude?.toFixed(4)}, {discrepancy.actualLongitude?.toFixed(4)}
                                                </span>
                                            </div>
                                        )}
                                    </div>
                                </td>

                                {/* Deviation */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    <span className={`text-sm font-medium ${
                                        (discrepancy.deviationMeters || 0) > 100 ? 'text-red-400' : 'text-gray-300'
                                    }`}>
                                        {formatDeviation(discrepancy.deviationMeters)}
                                    </span>
                                </td>

                                {/* Severity */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    {getSeverityBadge(discrepancy.severity)}
                                </td>

                                {/* Time */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    <div className="flex flex-col">
                                        <div className="text-sm text-gray-300">
                                            {format(new Date(discrepancy.timestamp), 'MMM d, HH:mm')}
                                        </div>
                                        <div className="text-xs text-gray-500">
                                            {formatDistanceToNow(new Date(discrepancy.timestamp), { addSuffix: true })}
                                        </div>
                                    </div>
                                </td>

                                {/* Status */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    {discrepancy.acknowledged ? (
                                        <div className="flex items-center">
                                            <CheckCircle className="h-4 w-4 text-green-500 mr-1" />
                                            <span className="text-sm text-green-400">Acknowledged</span>
                                        </div>
                                    ) : (
                                        <div className="flex items-center">
                                            <AlertTriangle className="h-4 w-4 text-orange-500 mr-1" />
                                            <span className="text-sm text-orange-400">Pending</span>
                                        </div>
                                    )}
                                </td>

                                {/* Actions */}
                                <td className="px-4 py-3 whitespace-nowrap text-right text-sm font-medium">
                                    <div className="flex justify-end space-x-2">
                                        <button
                                            onClick={() => onViewOnMap(discrepancy)}
                                            className="inline-flex items-center px-2 py-1 border border-gray-600 
                                                text-xs font-medium rounded text-gray-300 bg-gray-700 
                                                hover:bg-gray-600"
                                        >
                                            <MapPin className="h-3 w-3 mr-1" />
                                            Map
                                        </button>
                                        {!discrepancy.acknowledged && (
                                            <button
                                                onClick={() => onAcknowledge(discrepancy)}
                                                className="inline-flex items-center px-2 py-1 border border-transparent 
                                                    text-xs font-medium rounded text-white bg-blue-600 
                                                    hover:bg-blue-700"
                                            >
                                                <CheckCircle className="h-3 w-3 mr-1" />
                                                Ack
                                            </button>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            <div className="bg-gray-800 px-4 py-3 flex items-center justify-between border-t border-gray-700 sm:px-6">
                <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                    <div>
                        <p className="text-sm text-gray-400">
                            Page <span className="font-medium text-white">{currentPage + 1}</span> of{' '}
                            <span className="font-medium text-white">{totalPages}</span>
                        </p>
                    </div>
                    <div>
                        <nav className="relative z-0 inline-flex rounded-md -space-x-px">
                            <button
                                onClick={() => onPageChange(currentPage - 1)}
                                disabled={currentPage === 0}
                                className="relative inline-flex items-center px-2 py-2 rounded-l-md border 
                                    border-gray-600 bg-gray-700 text-sm font-medium text-gray-400 
                                    hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                <ChevronLeft className="h-5 w-5" />
                            </button>
                            <button
                                onClick={() => onPageChange(currentPage + 1)}
                                disabled={currentPage >= totalPages - 1}
                                className="relative inline-flex items-center px-2 py-2 rounded-r-md border 
                                    border-gray-600 bg-gray-700 text-sm font-medium text-gray-400 
                                    hover:bg-gray-600 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                <ChevronRight className="h-5 w-5" />
                            </button>
                        </nav>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default DiscrepancyTable;
