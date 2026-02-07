import React, { useState, useCallback, useEffect } from 'react';
import { Combobox } from '@headlessui/react';
import { useQuery } from '@tanstack/react-query';
import { Search, ChevronsUpDown, Check, Truck, Loader2 } from 'lucide-react';
import debounce from 'lodash/debounce';

interface Asset {
    id: string;
    assetId: string;
    identifier: string;
    name: string;
    category: string;
}

interface AssetSearchResponse {
    success: boolean;
    data: Asset[];
}

interface AssetSelectorProps {
    selectedAssetId: string | null;
    onSelect: (assetId: string) => void;
}

// Category colors matching AssetMarker
const CATEGORY_COLORS: Record<string, string> = {
    'EMERGENCY': 'bg-red-900/50 text-red-400',
    'FUELING': 'bg-orange-900/50 text-orange-400',
    'CARGO': 'bg-blue-900/50 text-blue-400',
    'GSE': 'bg-green-900/50 text-green-400',
    'TRANSPORT': 'bg-purple-900/50 text-purple-400',
    'POWER': 'bg-yellow-900/50 text-yellow-400',
    'SERVICES': 'bg-teal-900/50 text-teal-400',
};

/**
 * AssetSelector - Autocomplete dropdown for selecting an asset
 * Feature: 005-asset-tracking-security
 * Phase 11: Movement Trail Visualization
 */
const AssetSelector: React.FC<AssetSelectorProps> = ({
    selectedAssetId,
    onSelect
}) => {
    const [query, setQuery] = useState('');
    const [debouncedQuery, setDebouncedQuery] = useState('');

    // Debounce search query
    const debouncedSetQuery = useCallback(
        debounce((value: string) => {
            setDebouncedQuery(value);
        }, 300),
        []
    );

    useEffect(() => {
        debouncedSetQuery(query);
        return () => debouncedSetQuery.cancel();
    }, [query, debouncedSetQuery]);

    // Fetch assets based on search
    const { data: assets = [], isLoading } = useQuery<Asset[]>({
        queryKey: ['assetSearch', debouncedQuery],
        queryFn: async () => {
            const response = await fetch(
                `/api/assets/search?q=${encodeURIComponent(debouncedQuery)}&limit=20`
            );
            if (!response.ok) {
                throw new Error('Failed to search assets');
            }
            const json: AssetSearchResponse = await response.json();

            // Deduplicate by ID
            const uniqueAssets = Array.from(
                new Map((json.data || []).map(item => [item.id, item])).values()
            );

            // Map assetId to identifier for compatibility
            return uniqueAssets.map(a => ({ ...a, identifier: a.assetId || a.identifier }));
        },
        enabled: debouncedQuery.length >= 2 || debouncedQuery === '',
        staleTime: 30000,
    });

    // Fetch selected asset details
    const { data: selectedAsset } = useQuery<Asset>({
        queryKey: ['asset', selectedAssetId],
        queryFn: async () => {
            const response = await fetch(`/api/assets/${selectedAssetId}`);
            if (!response.ok) {
                throw new Error('Failed to fetch asset');
            }
            const json = await response.json();
            const asset = json.data || json;
            return { ...asset, identifier: asset.assetId || asset.identifier };
        },
        enabled: !!selectedAssetId,
    });

    // Handle selection
    const handleSelect = (asset: Asset | null) => {
        if (asset) {
            onSelect(asset.id);
        }
    };

    // Filter assets based on query (for displayed list)
    const filteredAssets = query === ''
        ? assets.slice(0, 10)
        : assets.filter(asset =>
            asset.identifier.toLowerCase().includes(query.toLowerCase()) ||
            asset.name.toLowerCase().includes(query.toLowerCase())
        );

    // Get recent searches from localStorage
    const getRecentSearches = (): Asset[] => {
        try {
            const stored = localStorage.getItem('recentAssetSearches');
            return stored ? JSON.parse(stored) : [];
        } catch {
            return [];
        }
    };

    // Save recent search to localStorage
    const saveRecentSearch = (asset: Asset) => {
        try {
            const recent = getRecentSearches();
            const filtered = recent.filter(a => a.id !== asset.id);
            const updated = [asset, ...filtered].slice(0, 5);
            localStorage.setItem('recentAssetSearches', JSON.stringify(updated));
        } catch {
            // Ignore localStorage errors
        }
    };

    const recentSearches = getRecentSearches();

    return (
        <Combobox
            value={selectedAsset || null}
            onChange={(asset: Asset | null) => {
                if (asset) {
                    saveRecentSearch(asset);
                }
                handleSelect(asset);
            }}
        >
            <div className="relative">
                <div className="relative w-full">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <Search className="h-5 w-5 text-gray-500" />
                    </div>
                    <Combobox.Input
                        className="w-full pl-10 pr-10 py-2 border border-gray-600 rounded-lg 
                            bg-gray-700 text-white
                            focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent
                            text-sm placeholder-gray-400"
                        displayValue={(asset: Asset | null) =>
                            asset ? `${asset.identifier} - ${asset.name}` : ''
                        }
                        onChange={(event) => setQuery(event.target.value)}
                        placeholder="Search by Asset ID or Name..."
                    />
                    <Combobox.Button className="absolute inset-y-0 right-0 flex items-center pr-2">
                        <ChevronsUpDown className="h-5 w-5 text-gray-400" />
                    </Combobox.Button>
                </div>

                <Combobox.Options className="absolute z-50 mt-1 w-full bg-gray-800 rounded-lg shadow-lg 
                    border border-gray-700 max-h-60 overflow-auto focus:outline-none">
                    {isLoading && (
                        <div className="px-4 py-3 text-sm text-gray-400 flex items-center">
                            <Loader2 className="h-4 w-4 animate-spin mr-2" />
                            Searching...
                        </div>
                    )}

                    {!isLoading && query.length >= 2 && filteredAssets.length === 0 && (
                        <div className="px-4 py-3 text-sm text-gray-400">
                            No assets found matching "{query}"
                        </div>
                    )}

                    {/* Recent Searches Section */}
                    {query === '' && recentSearches.length > 0 && (
                        <>
                            <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide bg-gray-700">
                                Recent Searches
                            </div>
                            {recentSearches.map((asset) => (
                                <Combobox.Option
                                    key={`recent-${asset.id}`}
                                    value={asset}
                                    className={({ active }) =>
                                        `relative cursor-pointer select-none py-2 pl-10 pr-4 ${active ? 'bg-blue-900/50 text-blue-200' : 'text-gray-300'
                                        }`
                                    }
                                >
                                    {({ selected, active }) => (
                                        <>
                                            <div className="flex items-center">
                                                <Truck className="h-4 w-4 text-gray-500 mr-2" />
                                                <span className={`block truncate ${selected ? 'font-medium' : 'font-normal'}`}>
                                                    <span className="font-medium">{asset.identifier}</span>
                                                    <span className="text-gray-400 ml-1">- {asset.name}</span>
                                                </span>
                                                <span className={`ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${CATEGORY_COLORS[asset.category] || 'bg-gray-700 text-gray-300'
                                                    }`}>
                                                    {asset.category}
                                                </span>
                                            </div>
                                            {selected && (
                                                <span className={`absolute inset-y-0 left-0 flex items-center pl-3 ${active ? 'text-blue-400' : 'text-blue-400'
                                                    }`}>
                                                    <Check className="h-5 w-5" />
                                                </span>
                                            )}
                                        </>
                                    )}
                                </Combobox.Option>
                            ))}
                            <div className="border-t border-gray-700" />
                        </>
                    )}

                    {/* Search Results Section */}
                    {(query !== '' || recentSearches.length === 0) && filteredAssets.length > 0 && (
                        <>
                            {query !== '' && (
                                <div className="px-3 py-2 text-xs font-semibold text-gray-500 uppercase tracking-wide bg-gray-700">
                                    Search Results
                                </div>
                            )}
                            {filteredAssets.map((asset) => (
                                <Combobox.Option
                                    key={asset.id}
                                    value={asset}
                                    className={({ active }) =>
                                        `relative cursor-pointer select-none py-2 pl-10 pr-4 ${active ? 'bg-blue-900/50 text-blue-200' : 'text-gray-300'
                                        }`
                                    }
                                >
                                    {({ selected, active }) => (
                                        <>
                                            <div className="flex items-center">
                                                <Truck className="h-4 w-4 text-gray-500 mr-2" />
                                                <span className={`block truncate ${selected ? 'font-medium' : 'font-normal'}`}>
                                                    <span className="font-medium">{asset.identifier}</span>
                                                    <span className="text-gray-400 ml-1">- {asset.name}</span>
                                                </span>
                                                <span className={`ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${CATEGORY_COLORS[asset.category] || 'bg-gray-700 text-gray-300'
                                                    }`}>
                                                    {asset.category}
                                                </span>
                                            </div>
                                            {selected && (
                                                <span className={`absolute inset-y-0 left-0 flex items-center pl-3 ${active ? 'text-blue-400' : 'text-blue-400'
                                                    }`}>
                                                    <Check className="h-5 w-5" />
                                                </span>
                                            )}
                                        </>
                                    )}
                                </Combobox.Option>
                            ))}
                        </>
                    )}
                </Combobox.Options>
            </div>
        </Combobox>
    );
};

export default AssetSelector;
