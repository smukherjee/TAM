import React from 'react';
import { formatDistanceToNow, format } from 'date-fns';
import { ZoneViolation, SEVERITY_COLORS } from '../../types/tracking';
import { 
    CheckCircle, 
    AlertOctagon, 
    ChevronLeft, 
    ChevronRight,
    MapPin,
    Clock,
    User
} from 'lucide-react';

interface ViolationTableProps {
    violations: ZoneViolation[];
    totalPages: number;
    currentPage: number;
    onPageChange: (page: number) => void;
    onAcknowledge: (violation: ZoneViolation) => void;
}

/**
 * ViolationTable - Display zone violations in a sortable, paginated table
 * Feature: 005-asset-tracking-security
 * Phase 9: Zone Violations Report
 */
const ViolationTable: React.FC<ViolationTableProps> = ({
    violations,
    totalPages,
    currentPage,
    onPageChange,
    onAcknowledge
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

    // Zone type badge styling
    const getZoneTypeBadge = (zoneType: string) => {
        const colors: Record<string, { bg: string; text: string }> = {
            'PROHIBITED': { bg: 'bg-red-900/50', text: 'text-red-400' },
            'RESTRICTED': { bg: 'bg-orange-900/50', text: 'text-orange-400' },
            'CONTROLLED': { bg: 'bg-yellow-900/50', text: 'text-yellow-400' },
            'MAINTENANCE': { bg: 'bg-blue-900/50', text: 'text-blue-400' }
        };
        const style = colors[zoneType] || { bg: 'bg-gray-700', text: 'text-gray-300' };
        return (
            <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${style.bg} ${style.text}`}>
                {zoneType}
            </span>
        );
    };

    // Format duration
    const formatDuration = (seconds?: number) => {
        if (!seconds) return '-';
        if (seconds < 60) return `${seconds}s`;
        if (seconds < 3600) return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
        const hours = Math.floor(seconds / 3600);
        const mins = Math.floor((seconds % 3600) / 60);
        return `${hours}h ${mins}m`;
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
                                Zone
                            </th>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Entry Time
                            </th>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Duration
                            </th>
                            <th scope="col" className="px-4 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                                Severity
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
                        {violations.map((violation) => (
                            <tr 
                                key={violation.id} 
                                className={`hover:bg-gray-700 ${!violation.acknowledged ? 'bg-red-900/20' : ''}`}
                            >
                                {/* Asset */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    <div className="flex items-center">
                                        <div>
                                            <div className="text-sm font-medium text-white">
                                                {violation.assetIdentifier}
                                            </div>
                                            <div className="text-xs text-gray-400">
                                                {violation.assetName || violation.assetCategory}
                                            </div>
                                        </div>
                                    </div>
                                </td>

                                {/* Zone */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    <div className="flex flex-col">
                                        <div className="flex items-center text-sm text-gray-300">
                                            <MapPin className="h-4 w-4 text-gray-500 mr-1" />
                                            {violation.zoneName}
                                        </div>
                                        <div className="mt-1">
                                            {getZoneTypeBadge(violation.zoneType)}
                                        </div>
                                    </div>
                                </td>

                                {/* Entry Time */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    <div className="flex flex-col">
                                        <div className="text-sm text-gray-300">
                                            {format(new Date(violation.timestamp), 'MMM d, HH:mm')}
                                        </div>
                                        <div className="text-xs text-gray-500">
                                            {formatDistanceToNow(new Date(violation.timestamp), { addSuffix: true })}
                                        </div>
                                    </div>
                                </td>

                                {/* Duration */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    <div className="flex items-center text-sm text-gray-300">
                                        <Clock className="h-4 w-4 text-gray-500 mr-1" />
                                        {formatDuration(violation.durationSeconds)}
                                    </div>
                                </td>

                                {/* Severity */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    {getSeverityBadge(violation.severity)}
                                </td>

                                {/* Status */}
                                <td className="px-4 py-3 whitespace-nowrap">
                                    {violation.acknowledged ? (
                                        <div className="flex items-center">
                                            <CheckCircle className="h-4 w-4 text-green-500 mr-1" />
                                            <div className="flex flex-col">
                                                <span className="text-sm text-green-400">Acknowledged</span>
                                                {violation.acknowledgedBy && (
                                                    <span className="text-xs text-gray-500 flex items-center">
                                                        <User className="h-3 w-3 mr-1" />
                                                        {violation.acknowledgedBy}
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="flex items-center">
                                            <AlertOctagon className="h-4 w-4 text-red-500 mr-1" />
                                            <span className="text-sm text-red-400">Pending</span>
                                        </div>
                                    )}
                                </td>

                                {/* Actions */}
                                <td className="px-4 py-3 whitespace-nowrap text-right text-sm font-medium">
                                    {!violation.acknowledged && (
                                        <button
                                            onClick={() => onAcknowledge(violation)}
                                            className="inline-flex items-center px-3 py-1.5 border border-transparent 
                                                text-xs font-medium rounded-md text-white bg-blue-600 
                                                hover:bg-blue-700 focus:outline-none focus:ring-2 
                                                focus:ring-offset-2 focus:ring-blue-500"
                                        >
                                            <CheckCircle className="h-3.5 w-3.5 mr-1" />
                                            Acknowledge
                                        </button>
                                    )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            <div className="bg-gray-800 px-4 py-3 flex items-center justify-between border-t border-gray-700 sm:px-6">
                <div className="flex-1 flex justify-between sm:hidden">
                    <button
                        onClick={() => onPageChange(currentPage - 1)}
                        disabled={currentPage === 0}
                        className="relative inline-flex items-center px-4 py-2 border border-gray-600 
                            text-sm font-medium rounded-md text-gray-300 bg-gray-700 hover:bg-gray-600
                            disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        Previous
                    </button>
                    <button
                        onClick={() => onPageChange(currentPage + 1)}
                        disabled={currentPage >= totalPages - 1}
                        className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-600 
                            text-sm font-medium rounded-md text-gray-300 bg-gray-700 hover:bg-gray-600
                            disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        Next
                    </button>
                </div>
                <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                    <div>
                        <p className="text-sm text-gray-400">
                            Page <span className="font-medium text-white">{currentPage + 1}</span> of{' '}
                            <span className="font-medium text-white">{totalPages}</span>
                        </p>
                    </div>
                    <div>
                        <nav className="relative z-0 inline-flex rounded-md -space-x-px" aria-label="Pagination">
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

export default ViolationTable;
