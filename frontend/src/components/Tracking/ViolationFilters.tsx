import React, { useState } from 'react';
import { ViolationFilters } from '../../types/tracking';
import { 
    Filter, 
    X, 
    Calendar,
    AlertTriangle,
    MapPin,
    Package,
    CheckCircle
} from 'lucide-react';

interface ViolationFiltersPanelProps {
    filters: ViolationFilters;
    onFilterChange: (filters: Partial<ViolationFilters>) => void;
    tenantCode: string;
}

/**
 * ViolationFiltersPanel - Filter controls for zone violations
 * Feature: 005-asset-tracking-security
 * Phase 9: Zone Violations Report
 */
const ViolationFiltersPanel: React.FC<ViolationFiltersPanelProps> = ({
    filters,
    onFilterChange,
    tenantCode
}) => {
    // Calculate default dates (last 7 days)
    const today = new Date();
    const lastWeek = new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000);
    
    const [startDate, setStartDate] = useState(
        filters.startDate || lastWeek.toISOString().split('T')[0]
    );
    const [endDate, setEndDate] = useState(
        filters.endDate || today.toISOString().split('T')[0]
    );

    // Apply date filter
    const handleDateChange = () => {
        onFilterChange({
            startDate: startDate ? new Date(startDate).toISOString() : undefined,
            endDate: endDate ? new Date(endDate + 'T23:59:59').toISOString() : undefined
        });
    };

    // Clear all filters
    const handleClearFilters = () => {
        setStartDate('');
        setEndDate('');
        onFilterChange({
            tenantCode,
            startDate: undefined,
            endDate: undefined,
            severity: undefined,
            zoneType: undefined,
            assetCategory: undefined,
            acknowledged: undefined
        });
    };

    // Check if any filter is active
    const hasActiveFilters = 
        filters.startDate || 
        filters.endDate || 
        filters.severity || 
        filters.zoneType || 
        filters.assetCategory || 
        filters.acknowledged !== undefined;

    return (
        <div className="space-y-6">
            {/* Header */}
            <div className="flex items-center justify-between">
                <div className="flex items-center">
                    <Filter className="h-5 w-5 text-gray-400 mr-2" />
                    <h3 className="text-sm font-medium text-white">Filters</h3>
                </div>
                {hasActiveFilters && (
                    <button
                        onClick={handleClearFilters}
                        className="text-xs text-blue-400 hover:text-blue-300 flex items-center"
                    >
                        <X className="h-3 w-3 mr-1" />
                        Clear All
                    </button>
                )}
            </div>

            {/* Date Range */}
            <div className="space-y-2">
                <label className="flex items-center text-xs font-medium text-gray-300">
                    <Calendar className="h-4 w-4 mr-1" />
                    Date Range
                </label>
                <div className="space-y-2">
                    <input
                        type="date"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        className="block w-full px-3 py-2 border border-gray-600 rounded-md 
                            text-sm bg-gray-700 text-white focus:outline-none focus:ring-1 
                            focus:ring-blue-500 focus:border-blue-500"
                        placeholder="Start Date"
                    />
                    <input
                        type="date"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        className="block w-full px-3 py-2 border border-gray-600 rounded-md 
                            text-sm bg-gray-700 text-white focus:outline-none focus:ring-1 
                            focus:ring-blue-500 focus:border-blue-500"
                        placeholder="End Date"
                    />
                    <button
                        onClick={handleDateChange}
                        className="w-full px-3 py-2 bg-blue-600 text-white text-sm font-medium 
                            rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 
                            focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-blue-500"
                    >
                        Apply Dates
                    </button>
                </div>
            </div>

            {/* Severity Filter */}
            <div className="space-y-2">
                <label className="flex items-center text-xs font-medium text-gray-300">
                    <AlertTriangle className="h-4 w-4 mr-1" />
                    Severity
                </label>
                <select
                    value={filters.severity || ''}
                    onChange={(e) => onFilterChange({ severity: e.target.value as any || undefined })}
                    className="block w-full px-3 py-2 border border-gray-600 rounded-md 
                        text-sm bg-gray-700 text-white focus:outline-none focus:ring-1 
                        focus:ring-blue-500 focus:border-blue-500"
                >
                    <option value="">All Severities</option>
                    <option value="CRITICAL">Critical</option>
                    <option value="HIGH">High</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="LOW">Low</option>
                </select>
            </div>

            {/* Zone Type Filter */}
            <div className="space-y-2">
                <label className="flex items-center text-xs font-medium text-gray-300">
                    <MapPin className="h-4 w-4 mr-1" />
                    Zone Type
                </label>
                <select
                    value={filters.zoneType || ''}
                    onChange={(e) => onFilterChange({ zoneType: e.target.value || undefined })}
                    className="block w-full px-3 py-2 border border-gray-600 rounded-md 
                        text-sm bg-gray-700 text-white focus:outline-none focus:ring-1 
                        focus:ring-blue-500 focus:border-blue-500"
                >
                    <option value="">All Zone Types</option>
                    <option value="PROHIBITED">Prohibited</option>
                    <option value="RESTRICTED">Restricted</option>
                    <option value="CONTROLLED">Controlled</option>
                    <option value="MAINTENANCE">Maintenance</option>
                </select>
            </div>

            {/* Asset Category Filter */}
            <div className="space-y-2">
                <label className="flex items-center text-xs font-medium text-gray-300">
                    <Package className="h-4 w-4 mr-1" />
                    Asset Category
                </label>
                <select
                    value={filters.assetCategory || ''}
                    onChange={(e) => onFilterChange({ assetCategory: e.target.value || undefined })}
                    className="block w-full px-3 py-2 border border-gray-600 rounded-md 
                        text-sm bg-gray-700 text-white focus:outline-none focus:ring-1 
                        focus:ring-blue-500 focus:border-blue-500"
                >
                    <option value="">All Categories</option>
                    <option value="Emergency">Emergency</option>
                    <option value="Fueling">Fueling</option>
                    <option value="Cargo">Cargo</option>
                    <option value="Ground Support">Ground Support</option>
                    <option value="Transport">Transport</option>
                    <option value="Power">Power</option>
                    <option value="Services">Services</option>
                </select>
            </div>

            {/* Status Filter */}
            <div className="space-y-2">
                <label className="flex items-center text-xs font-medium text-gray-300">
                    <CheckCircle className="h-4 w-4 mr-1" />
                    Status
                </label>
                <select
                    value={filters.acknowledged === undefined ? '' : String(filters.acknowledged)}
                    onChange={(e) => {
                        const value = e.target.value;
                        onFilterChange({ 
                            acknowledged: value === '' ? undefined : value === 'true' 
                        });
                    }}
                    className="block w-full px-3 py-2 border border-gray-600 rounded-md 
                        text-sm bg-gray-700 text-white focus:outline-none focus:ring-1 
                        focus:ring-blue-500 focus:border-blue-500"
                >
                    <option value="">All Statuses</option>
                    <option value="false">Pending</option>
                    <option value="true">Acknowledged</option>
                </select>
            </div>

            {/* Quick Filters */}
            <div className="space-y-2">
                <label className="text-xs font-medium text-gray-300">Quick Filters</label>
                <div className="space-y-2">
                    <button
                        onClick={() => onFilterChange({ 
                            acknowledged: false, 
                            severity: 'CRITICAL' 
                        })}
                        className="w-full px-3 py-2 text-left text-sm text-red-400 
                            bg-red-900/50 rounded-md hover:bg-red-900/70 border border-red-700"
                    >
                        🚨 Critical Unacknowledged
                    </button>
                    <button
                        onClick={() => onFilterChange({ 
                            acknowledged: false 
                        })}
                        className="w-full px-3 py-2 text-left text-sm text-orange-400 
                            bg-orange-900/50 rounded-md hover:bg-orange-900/70 border border-orange-700"
                    >
                        ⚠️ All Unacknowledged
                    </button>
                    <button
                        onClick={() => {
                            const lastHour = new Date();
                            lastHour.setHours(lastHour.getHours() - 1);
                            onFilterChange({
                                startDate: lastHour.toISOString()
                            });
                        }}
                        className="w-full px-3 py-2 text-left text-sm text-blue-400 
                            bg-blue-900/50 rounded-md hover:bg-blue-900/70 border border-blue-700"
                    >
                        🕐 Last Hour
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ViolationFiltersPanel;
