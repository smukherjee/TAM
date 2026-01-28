import React, { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, X, Filter, MapPin, Package } from 'lucide-react';
import { AssetFilters } from '../../types/assetTracking';

interface AssetFilterPanelProps {
    isOpen: boolean;
    onToggle: () => void;
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
 * Asset Filter Panel - Collapsible sidebar for filtering assets
 * Feature: 005-asset-tracking-security
 * Task: T040
 */
const AssetFilterPanel: React.FC<AssetFilterPanelProps> = ({
    isOpen,
    onToggle,
    onFilterChange,
    assetCount,
    totalCount
}) => {
    const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
    const [selectedStatuses, setSelectedStatuses] = useState<string[]>([]);
    const [selectedZone, setSelectedZone] = useState<string>('');

    // Load filters from localStorage on mount
    useEffect(() => {
        const saved = localStorage.getItem('assetFilters');
        if (saved) {
            try {
                const parsed = JSON.parse(saved);
                setSelectedCategories(parsed.categories || []);
                setSelectedStatuses(parsed.statuses || []);
                setSelectedZone(parsed.zone || '');
            } catch (e) {
                console.error('Failed to parse saved filters:', e);
            }
        }
    }, []);

    // Save filters to localStorage whenever they change
    useEffect(() => {
        localStorage.setItem('assetFilters', JSON.stringify({
            categories: selectedCategories,
            statuses: selectedStatuses,
            zone: selectedZone
        }));
    }, [selectedCategories, selectedStatuses, selectedZone]);

    // Apply filters with debounce
    useEffect(() => {
        const timer = setTimeout(() => {
            const newFilters: AssetFilters = {};
            
            if (selectedCategories.length > 0) {
                newFilters.category = selectedCategories.join(',');
            }
            if (selectedStatuses.length > 0) {
                newFilters.status = selectedStatuses.join(',');
            }
            if (selectedZone) {
                newFilters.zoneId = selectedZone;
            }

            onFilterChange(newFilters);
        }, 300);

        return () => clearTimeout(timer);
    }, [selectedCategories, selectedStatuses, selectedZone, onFilterChange]);

    const handleCategoryToggle = (category: string) => {
        setSelectedCategories(prev =>
            prev.includes(category)
                ? prev.filter(c => c !== category)
                : [...prev, category]
        );
    };

    const handleStatusToggle = (status: string) => {
        setSelectedStatuses(prev =>
            prev.includes(status)
                ? prev.filter(s => s !== status)
                : [...prev, status]
        );
    };

    const handleClearAll = () => {
        setSelectedCategories([]);
        setSelectedStatuses([]);
        setSelectedZone('');
        onFilterChange({});
    };

    const hasActiveFilters = selectedCategories.length > 0 || selectedStatuses.length > 0 || selectedZone !== '';

    return (
        <>
            {/* Toggle Button */}
            <button
                onClick={onToggle}
                className="absolute top-4 left-4 z-[1000] bg-white rounded-lg shadow-lg p-2 hover:bg-gray-50 transition-colors"
                aria-label={isOpen ? 'Close filter panel' : 'Open filter panel'}
            >
                {isOpen ? <ChevronLeft className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
            </button>

            {/* Filter Panel */}
            <div
                className={`absolute top-0 left-0 h-full bg-white shadow-xl z-[999] transition-transform duration-300 overflow-y-auto ${
                    isOpen ? 'translate-x-0' : '-translate-x-full'
                }`}
                style={{ width: '320px' }}
            >
                <div className="p-4">
                    {/* Header */}
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-2">
                            <Filter className="w-5 h-5 text-gray-700" />
                            <h2 className="text-lg font-bold text-gray-900">Filters</h2>
                        </div>
                        {hasActiveFilters && (
                            <button
                                onClick={handleClearAll}
                                className="text-sm text-blue-600 hover:text-blue-700 font-medium flex items-center gap-1"
                            >
                                <X className="w-4 h-4" />
                                Clear All
                            </button>
                        )}
                    </div>

                    {/* Asset Count Badge */}
                    <div className="mb-4 p-3 bg-blue-50 rounded-lg">
                        <p className="text-sm text-gray-700">
                            Showing <span className="font-bold text-blue-700">{assetCount}</span> of{' '}
                            <span className="font-bold">{totalCount}</span> assets
                        </p>
                    </div>

                    {/* Category Filter */}
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-3">
                            <Package className="w-4 h-4 text-gray-600" />
                            <h3 className="text-sm font-semibold text-gray-900">Category</h3>
                        </div>
                        <div className="space-y-2">
                            {CATEGORIES.map(category => (
                                <label
                                    key={category}
                                    className="flex items-center gap-2 cursor-pointer hover:bg-gray-50 p-2 rounded"
                                >
                                    <input
                                        type="checkbox"
                                        checked={selectedCategories.includes(category)}
                                        onChange={() => handleCategoryToggle(category)}
                                        className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                                    />
                                    <span className="text-sm text-gray-700">{category}</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Status Filter */}
                    <div className="mb-6">
                        <h3 className="text-sm font-semibold text-gray-900 mb-3">Status</h3>
                        <div className="space-y-2">
                            {STATUSES.map(status => (
                                <label
                                    key={status}
                                    className="flex items-center gap-2 cursor-pointer hover:bg-gray-50 p-2 rounded"
                                >
                                    <input
                                        type="checkbox"
                                        checked={selectedStatuses.includes(status)}
                                        onChange={() => handleStatusToggle(status)}
                                        className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                                    />
                                    <span className="text-sm text-gray-700">{status}</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Zone Filter (placeholder - will be populated with actual zones) */}
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-3">
                            <MapPin className="w-4 h-4 text-gray-600" />
                            <h3 className="text-sm font-semibold text-gray-900">Zone</h3>
                        </div>
                        <select
                            value={selectedZone}
                            onChange={(e) => setSelectedZone(e.target.value)}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        >
                            <option value="">All Zones</option>
                            {/* TODO: Fetch zones from API and populate */}
                            <option value="apron-a">Apron A</option>
                            <option value="apron-b">Apron B</option>
                            <option value="taxiway-1">Taxiway 1</option>
                            <option value="cargo-area">Cargo Area</option>
                        </select>
                    </div>

                    {/* Active Filters Summary */}
                    {hasActiveFilters && (
                        <div className="mt-6 p-3 bg-gray-50 rounded-lg">
                            <p className="text-xs font-semibold text-gray-700 mb-2">Active Filters:</p>
                            <div className="flex flex-wrap gap-2">
                                {selectedCategories.map(cat => (
                                    <span
                                        key={cat}
                                        className="px-2 py-1 bg-blue-100 text-blue-800 text-xs rounded-full flex items-center gap-1"
                                    >
                                        {cat}
                                        <button
                                            onClick={() => handleCategoryToggle(cat)}
                                            className="hover:bg-blue-200 rounded-full"
                                        >
                                            <X className="w-3 h-3" />
                                        </button>
                                    </span>
                                ))}
                                {selectedStatuses.map(stat => (
                                    <span
                                        key={stat}
                                        className="px-2 py-1 bg-green-100 text-green-800 text-xs rounded-full flex items-center gap-1"
                                    >
                                        {stat}
                                        <button
                                            onClick={() => handleStatusToggle(stat)}
                                            className="hover:bg-green-200 rounded-full"
                                        >
                                            <X className="w-3 h-3" />
                                        </button>
                                    </span>
                                ))}
                                {selectedZone && (
                                    <span className="px-2 py-1 bg-purple-100 text-purple-800 text-xs rounded-full flex items-center gap-1">
                                        Zone: {selectedZone}
                                        <button
                                            onClick={() => setSelectedZone('')}
                                            className="hover:bg-purple-200 rounded-full"
                                        >
                                            <X className="w-3 h-3" />
                                        </button>
                                    </span>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </>
    );
};

export default AssetFilterPanel;
