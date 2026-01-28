import React from 'react';
import { X, Package, MapPin, RotateCcw } from 'lucide-react';
import { AssetFilters } from '../../types/assetTracking';

interface FilterDrawerProps {
    isOpen: boolean;
    onClose: () => void;
    filters: AssetFilters;
    onFilterChange: (filters: AssetFilters) => void;
    assetCount: number;
    totalCount: number;
}

const CATEGORIES = [
    'Emergency',
    'Fueling',
    'Cargo',
    'Ground Support',
    'Transport',
    'Power',
    'Services',
    'Other'
];

const STATUSES = [
    'In Use',
    'Available',
    'Maintenance',
    'Out of Service'
];

/**
 * FilterDrawer - Slide-out drawer for advanced filtering
 * Appears from the right side when triggered
 */
const FilterDrawer: React.FC<FilterDrawerProps> = ({
    isOpen,
    onClose,
    filters,
    onFilterChange,
    assetCount,
    totalCount
}) => {
    const selectedCategories = filters.category?.split(',').filter(Boolean) || [];
    const selectedStatuses = filters.status?.split(',').filter(Boolean) || [];

    const handleCategoryToggle = (category: string) => {
        const newCategories = selectedCategories.includes(category)
            ? selectedCategories.filter(c => c !== category)
            : [...selectedCategories, category];
        
        onFilterChange({
            ...filters,
            category: newCategories.length > 0 ? newCategories.join(',') : undefined
        });
    };

    const handleStatusToggle = (status: string) => {
        const newStatuses = selectedStatuses.includes(status)
            ? selectedStatuses.filter(s => s !== status)
            : [...selectedStatuses, status];
        
        onFilterChange({
            ...filters,
            status: newStatuses.length > 0 ? newStatuses.join(',') : undefined
        });
    };

    const handleZoneChange = (zoneId: string) => {
        onFilterChange({
            ...filters,
            zoneId: zoneId || undefined
        });
    };

    const handleClearAll = () => {
        onFilterChange({});
    };

    const hasActiveFilters = selectedCategories.length > 0 || selectedStatuses.length > 0 || filters.zoneId;

    return (
        <>
            {/* Backdrop */}
            {isOpen && (
                <div 
                    className="fixed inset-0 bg-black/50 z-[1500] transition-opacity"
                    onClick={onClose}
                />
            )}

            {/* Drawer */}
            <div className={`fixed top-0 right-0 h-full w-80 bg-gray-900 border-l border-gray-700 z-[1600] transform transition-transform duration-300 ${
                isOpen ? 'translate-x-0' : 'translate-x-full'
            }`}>
                {/* Header */}
                <div className="flex items-center justify-between p-4 border-b border-gray-700">
                    <h2 className="text-lg font-semibold text-white">Filters</h2>
                    <button
                        onClick={onClose}
                        className="p-2 text-gray-400 hover:text-white hover:bg-gray-800 rounded-lg transition-colors"
                    >
                        <X className="w-5 h-5" />
                    </button>
                </div>

                {/* Content */}
                <div className="p-4 overflow-y-auto h-[calc(100%-140px)]">
                    {/* Asset Count */}
                    <div className="mb-6 p-3 bg-blue-900/30 rounded-lg border border-blue-700/50">
                        <p className="text-sm text-gray-300">
                            Showing <span className="font-bold text-blue-400">{assetCount}</span> of{' '}
                            <span className="font-bold text-white">{totalCount}</span> assets
                        </p>
                    </div>

                    {/* Category Filter */}
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-3">
                            <Package className="w-4 h-4 text-gray-400" />
                            <h3 className="text-sm font-semibold text-white">Category</h3>
                        </div>
                        <div className="grid grid-cols-2 gap-2">
                            {CATEGORIES.map(category => (
                                <button
                                    key={category}
                                    onClick={() => handleCategoryToggle(category)}
                                    className={`px-3 py-2 text-xs rounded-lg transition-colors text-left ${
                                        selectedCategories.includes(category)
                                            ? 'bg-blue-600 text-white'
                                            : 'bg-gray-800 text-gray-400 hover:bg-gray-700 hover:text-gray-300'
                                    }`}
                                >
                                    {category}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Status Filter */}
                    <div className="mb-6">
                        <h3 className="text-sm font-semibold text-white mb-3">Status</h3>
                        <div className="grid grid-cols-2 gap-2">
                            {STATUSES.map(status => (
                                <button
                                    key={status}
                                    onClick={() => handleStatusToggle(status)}
                                    className={`px-3 py-2 text-xs rounded-lg transition-colors text-left ${
                                        selectedStatuses.includes(status)
                                            ? 'bg-green-600 text-white'
                                            : 'bg-gray-800 text-gray-400 hover:bg-gray-700 hover:text-gray-300'
                                    }`}
                                >
                                    {status}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Zone Filter */}
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-3">
                            <MapPin className="w-4 h-4 text-gray-400" />
                            <h3 className="text-sm font-semibold text-white">Zone</h3>
                        </div>
                        <select
                            value={filters.zoneId || ''}
                            onChange={(e) => handleZoneChange(e.target.value)}
                            className="w-full px-3 py-2 bg-gray-800 border border-gray-600 text-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        >
                            <option value="">All Zones</option>
                            <option value="apron-a">Apron A</option>
                            <option value="apron-b">Apron B</option>
                            <option value="taxiway-1">Taxiway 1</option>
                            <option value="cargo-area">Cargo Area</option>
                        </select>
                    </div>

                    {/* Active Filters */}
                    {hasActiveFilters && (
                        <div className="p-3 bg-gray-800 rounded-lg border border-gray-700">
                            <div className="flex items-center justify-between mb-2">
                                <span className="text-xs text-gray-400">Active Filters</span>
                                <span className="text-xs text-blue-400">
                                    {selectedCategories.length + selectedStatuses.length + (filters.zoneId ? 1 : 0)} applied
                                </span>
                            </div>
                            <div className="flex flex-wrap gap-1">
                                {selectedCategories.map(cat => (
                                    <span
                                        key={cat}
                                        className="px-2 py-0.5 bg-blue-900/50 text-blue-300 text-[10px] rounded-full"
                                    >
                                        {cat}
                                    </span>
                                ))}
                                {selectedStatuses.map(stat => (
                                    <span
                                        key={stat}
                                        className="px-2 py-0.5 bg-green-900/50 text-green-300 text-[10px] rounded-full"
                                    >
                                        {stat}
                                    </span>
                                ))}
                                {filters.zoneId && (
                                    <span className="px-2 py-0.5 bg-purple-900/50 text-purple-300 text-[10px] rounded-full">
                                        {filters.zoneId}
                                    </span>
                                )}
                            </div>
                        </div>
                    )}
                </div>

                {/* Footer */}
                <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-gray-700 bg-gray-900">
                    <div className="flex gap-2">
                        <button
                            onClick={handleClearAll}
                            disabled={!hasActiveFilters}
                            className="flex-1 px-4 py-2 text-sm bg-gray-800 text-gray-300 rounded-lg hover:bg-gray-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                        >
                            <RotateCcw className="w-4 h-4" />
                            Reset
                        </button>
                        <button
                            onClick={onClose}
                            className="flex-1 px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                        >
                            Apply
                        </button>
                    </div>
                </div>
            </div>
        </>
    );
};

export default FilterDrawer;
