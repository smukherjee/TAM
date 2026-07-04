import React, { useState, useEffect } from 'react';
import { ChevronLeft, ChevronRight, X, Filter, MapPin, Package, Truck } from 'lucide-react';
import { AssetFilters } from '../../types/assetTracking';
import api from '../../services/api';

interface AssetFilterPanelProps {
    isOpen: boolean;
    onToggle: () => void;
    onFilterChange: (filters: AssetFilters) => void;
    assetCount: number;
    totalCount: number;
    tenantCode?: string;
    userRole?: string;
    userCompany?: string;
}

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
    totalCount,
    tenantCode,
    userRole,
    userCompany
}) => {
    const isGhUser = userRole === 'GH';

    const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
    const [selectedStatuses, setSelectedStatuses] = useState<string[]>([]);
    const [selectedZone, setSelectedZone] = useState<string>('');
    const [selectedGroundHandler, setSelectedGroundHandler] = useState<string>('');
    const [groundHandlers, setGroundHandlers] = useState<string[]>([]);
    const [categories, setCategories] = useState<string[]>([]);

    // GH users are locked to their own company; everyone else picks from the tenant's list.
    useEffect(() => {
        if (isGhUser || !tenantCode) {
            return;
        }
        api.get<{ success: boolean; data: string[] }>('/ground-handlers', { params: { tenantCode } })
            .then(res => setGroundHandlers(res.data.data || []))
            .catch(e => console.error('Failed to load ground handlers:', e));
    }, [isGhUser, tenantCode]);

    // Categories are asset tags, not a fixed enum — fetch the tenant's actual set from the DB.
    useEffect(() => {
        if (!tenantCode) {
            return;
        }
        api.get<{ success: boolean; data: string[] }>('/asset-categories', { params: { tenantCode } })
            .then(res => setCategories(res.data.data || []))
            .catch(e => console.error('Failed to load asset categories:', e));
    }, [tenantCode]);

    // Load filters from localStorage on mount
    useEffect(() => {
        const saved = localStorage.getItem('assetFilters');
        if (saved) {
            try {
                const parsed = JSON.parse(saved);
                setSelectedCategories(parsed.categories || []);
                setSelectedStatuses(parsed.statuses || []);
                setSelectedZone(parsed.zone || '');
                if (!isGhUser) {
                    setSelectedGroundHandler(parsed.groundHandler || '');
                }
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
            zone: selectedZone,
            groundHandler: selectedGroundHandler
        }));
    }, [selectedCategories, selectedStatuses, selectedZone, selectedGroundHandler]);

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
            const effectiveGroundHandler = isGhUser ? userCompany : selectedGroundHandler;
            if (effectiveGroundHandler) {
                newFilters.groundHandler = effectiveGroundHandler;
            }

            onFilterChange(newFilters);
        }, 300);

        return () => clearTimeout(timer);
    }, [selectedCategories, selectedStatuses, selectedZone, selectedGroundHandler, isGhUser, userCompany, onFilterChange]);

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
        setSelectedGroundHandler('');
        onFilterChange(isGhUser && userCompany ? { groundHandler: userCompany } : {});
    };

    const hasActiveFilters = selectedCategories.length > 0 || selectedStatuses.length > 0 || selectedZone !== ''
        || (!isGhUser && selectedGroundHandler !== '');

    return (
        <>
            {/* Toggle Button */}
            <button
                onClick={onToggle}
                className="absolute top-4 left-4 z-[1000] bg-gray-800 rounded-lg shadow-lg p-2 hover:bg-gray-700 transition-colors border border-gray-700"
                aria-label={isOpen ? 'Close filter panel' : 'Open filter panel'}
            >
                {isOpen ? <ChevronLeft className="w-5 h-5 text-gray-300" /> : <ChevronRight className="w-5 h-5 text-gray-300" />}
            </button>

            {/* Filter Panel */}
            <div
                className={`absolute top-0 left-0 h-full bg-gray-900 shadow-xl z-[999] transition-transform duration-300 overflow-y-auto border-r border-gray-700 ${
                    isOpen ? 'translate-x-0' : '-translate-x-full'
                }`}
                style={{ width: '320px' }}
            >
                <div className="p-4">
                    {/* Header */}
                    <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center gap-2">
                            <Filter className="w-5 h-5 text-gray-400" />
                            <h2 className="text-lg font-bold text-white">Filters</h2>
                        </div>
                        {hasActiveFilters && (
                            <button
                                onClick={handleClearAll}
                                className="text-sm text-blue-400 hover:text-blue-300 font-medium flex items-center gap-1"
                            >
                                <X className="w-4 h-4" />
                                Clear All
                            </button>
                        )}
                    </div>

                    {/* Asset Count Badge */}
                    <div className="mb-4 p-3 bg-blue-900/30 rounded-lg border border-blue-700/50">
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
                        <div className="space-y-2">
                            {categories.map(category => (
                                <label
                                    key={category}
                                    className="flex items-center gap-2 cursor-pointer hover:bg-gray-800 p-2 rounded"
                                >
                                    <input
                                        type="checkbox"
                                        checked={selectedCategories.includes(category)}
                                        onChange={() => handleCategoryToggle(category)}
                                        className="w-4 h-4 text-blue-500 bg-gray-700 border-gray-600 rounded focus:ring-blue-500"
                                    />
                                    <span className="text-sm text-gray-300">{category}</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Status Filter */}
                    <div className="mb-6">
                        <h3 className="text-sm font-semibold text-white mb-3">Status</h3>
                        <div className="space-y-2">
                            {STATUSES.map(status => (
                                <label
                                    key={status}
                                    className="flex items-center gap-2 cursor-pointer hover:bg-gray-800 p-2 rounded"
                                >
                                    <input
                                        type="checkbox"
                                        checked={selectedStatuses.includes(status)}
                                        onChange={() => handleStatusToggle(status)}
                                        className="w-4 h-4 text-blue-500 bg-gray-700 border-gray-600 rounded focus:ring-blue-500"
                                    />
                                    <span className="text-sm text-gray-300">{status}</span>
                                </label>
                            ))}
                        </div>
                    </div>

                    {/* Zone Filter (placeholder - will be populated with actual zones) */}
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-3">
                            <MapPin className="w-4 h-4 text-gray-400" />
                            <h3 className="text-sm font-semibold text-white">Zone</h3>
                        </div>
                        <select
                            value={selectedZone}
                            onChange={(e) => setSelectedZone(e.target.value)}
                            className="w-full px-3 py-2 bg-gray-800 border border-gray-600 text-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        >
                            <option value="">All Zones</option>
                            {/* TODO: Fetch zones from API and populate */}
                            <option value="apron-a">Apron A</option>
                            <option value="apron-b">Apron B</option>
                            <option value="taxiway-1">Taxiway 1</option>
                            <option value="cargo-area">Cargo Area</option>
                        </select>
                    </div>

                    {/* Ground Handler Filter */}
                    <div className="mb-6">
                        <div className="flex items-center gap-2 mb-3">
                            <Truck className="w-4 h-4 text-gray-400" />
                            <h3 className="text-sm font-semibold text-white">Ground Handler</h3>
                        </div>
                        {isGhUser ? (
                            <p className="text-sm text-gray-300 bg-gray-800 px-3 py-2 rounded-lg border border-gray-700">
                                {userCompany || 'Your company'} (scoped to your assets)
                            </p>
                        ) : (
                            <select
                                value={selectedGroundHandler}
                                onChange={(e) => setSelectedGroundHandler(e.target.value)}
                                className="w-full px-3 py-2 bg-gray-800 border border-gray-600 text-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            >
                                <option value="">All Ground Handlers</option>
                                {groundHandlers.map(gh => (
                                    <option key={gh} value={gh}>{gh}</option>
                                ))}
                            </select>
                        )}
                    </div>

                    {/* Active Filters Summary */}
                    {hasActiveFilters && (
                        <div className="mt-6 p-3 bg-gray-800 rounded-lg border border-gray-700">
                            <p className="text-xs font-semibold text-gray-400 mb-2">Active Filters:</p>
                            <div className="flex flex-wrap gap-2">
                                {selectedCategories.map(cat => (
                                    <span
                                        key={cat}
                                        className="px-2 py-1 bg-blue-900/50 text-blue-300 text-xs rounded-full flex items-center gap-1 border border-blue-700/50"
                                    >
                                        {cat}
                                        <button
                                            onClick={() => handleCategoryToggle(cat)}
                                            className="hover:bg-blue-800 rounded-full"
                                        >
                                            <X className="w-3 h-3" />
                                        </button>
                                    </span>
                                ))}
                                {selectedStatuses.map(stat => (
                                    <span
                                        key={stat}
                                        className="px-2 py-1 bg-green-900/50 text-green-300 text-xs rounded-full flex items-center gap-1 border border-green-700/50"
                                    >
                                        {stat}
                                        <button
                                            onClick={() => handleStatusToggle(stat)}
                                            className="hover:bg-green-800 rounded-full"
                                        >
                                            <X className="w-3 h-3" />
                                        </button>
                                    </span>
                                ))}
                                {selectedZone && (
                                    <span className="px-2 py-1 bg-purple-900/50 text-purple-300 text-xs rounded-full flex items-center gap-1 border border-purple-700/50">
                                        Zone: {selectedZone}
                                        <button
                                            onClick={() => setSelectedZone('')}
                                            className="hover:bg-purple-800 rounded-full"
                                        >
                                            <X className="w-3 h-3" />
                                        </button>
                                    </span>
                                )}
                                {!isGhUser && selectedGroundHandler && (
                                    <span className="px-2 py-1 bg-orange-900/50 text-orange-300 text-xs rounded-full flex items-center gap-1 border border-orange-700/50">
                                        GH: {selectedGroundHandler}
                                        <button
                                            onClick={() => setSelectedGroundHandler('')}
                                            className="hover:bg-orange-800 rounded-full"
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
