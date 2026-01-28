import React, { useState, useEffect, useCallback } from 'react';
import { Search, X, Clock } from 'lucide-react';
import { AssetLocation } from '../../types/assetTracking';

interface AssetSearchBarProps {
    assets: AssetLocation[];
    onSelect: (asset: AssetLocation) => void;
}

interface RecentSearch {
    assetId: string;
    assetIdentifier: string;
    name: string;
    timestamp: number;
}

const RECENT_SEARCHES_KEY = 'assetSearchHistory';
const MAX_RECENT_SEARCHES = 5;

/**
 * Asset Search Bar with Autocomplete
 * Feature: 005-asset-tracking-security (Task T042)
 */
const AssetSearchBar: React.FC<AssetSearchBarProps> = ({ assets, onSelect }) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [suggestions, setSuggestions] = useState<AssetLocation[]>([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [recentSearches, setRecentSearches] = useState<RecentSearch[]>([]);
    const [showRecentSearches, setShowRecentSearches] = useState(false);

    // Load recent searches from localStorage
    useEffect(() => {
        const saved = localStorage.getItem(RECENT_SEARCHES_KEY);
        if (saved) {
            try {
                setRecentSearches(JSON.parse(saved));
            } catch (error) {
                console.error('Failed to parse recent searches:', error);
            }
        }
    }, []);

    // Filter suggestions based on search term (debounced)
    useEffect(() => {
        if (searchTerm.trim().length === 0) {
            setSuggestions([]);
            setShowSuggestions(false);
            return;
        }

        const timeoutId = setTimeout(() => {
            const term = searchTerm.toLowerCase();
            const filtered = assets.filter(asset =>
                asset.assetIdentifier.toLowerCase().includes(term) ||
                asset.name.toLowerCase().includes(term) ||
                asset.category.toLowerCase().includes(term)
            ).slice(0, 10); // Limit to 10 suggestions

            setSuggestions(filtered);
            setShowSuggestions(filtered.length > 0);
        }, 300); // Debounce by 300ms

        return () => clearTimeout(timeoutId);
    }, [searchTerm, assets]);

    // Save search to recent searches
    const saveRecentSearch = useCallback((asset: AssetLocation) => {
        const newSearch: RecentSearch = {
            assetId: asset.assetId,
            assetIdentifier: asset.assetIdentifier,
            name: asset.name,
            timestamp: Date.now()
        };

        setRecentSearches(prev => {
            // Remove duplicate if exists
            const filtered = prev.filter(s => s.assetId !== asset.assetId);
            // Add to front
            const updated = [newSearch, ...filtered].slice(0, MAX_RECENT_SEARCHES);
            // Save to localStorage
            localStorage.setItem(RECENT_SEARCHES_KEY, JSON.stringify(updated));
            return updated;
        });
    }, []);

    // Handle asset selection
    const handleSelect = (asset: AssetLocation) => {
        saveRecentSearch(asset);
        onSelect(asset);
        setSearchTerm('');
        setShowSuggestions(false);
        setShowRecentSearches(false);
    };

    // Handle recent search click
    const handleRecentClick = (recent: RecentSearch) => {
        const asset = assets.find(a => a.assetId === recent.assetId);
        if (asset) {
            handleSelect(asset);
        }
    };

    // Clear all recent searches
    const clearRecentSearches = () => {
        setRecentSearches([]);
        localStorage.removeItem(RECENT_SEARCHES_KEY);
    };

    // Clear search input
    const clearSearch = () => {
        setSearchTerm('');
        setSuggestions([]);
        setShowSuggestions(false);
    };

    return (
        <div className="relative w-full max-w-md">
            {/* Search Input */}
            <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Search className="h-5 w-5 text-gray-400" />
                </div>
                <input
                    type="text"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    onFocus={() => {
                        if (searchTerm.length === 0 && recentSearches.length > 0) {
                            setShowRecentSearches(true);
                        }
                    }}
                    onBlur={() => {
                        // Delay to allow click on suggestions
                        setTimeout(() => {
                            setShowSuggestions(false);
                            setShowRecentSearches(false);
                        }, 200);
                    }}
                    placeholder="Search assets by ID or name..."
                    className="block w-full pl-10 pr-10 py-2 border border-gray-300 rounded-lg leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-blue-500 focus:border-blue-500 text-sm"
                />
                {searchTerm && (
                    <button
                        onClick={clearSearch}
                        className="absolute inset-y-0 right-0 pr-3 flex items-center"
                    >
                        <X className="h-5 w-5 text-gray-400 hover:text-gray-600" />
                    </button>
                )}
            </div>

            {/* Suggestions Dropdown */}
            {showSuggestions && suggestions.length > 0 && (
                <div className="absolute z-10 mt-1 w-full bg-white shadow-lg max-h-60 rounded-md py-1 text-base ring-1 ring-black ring-opacity-5 overflow-auto focus:outline-none sm:text-sm">
                    {suggestions.map((asset) => (
                        <button
                            key={asset.assetId}
                            onClick={() => handleSelect(asset)}
                            className="w-full text-left px-4 py-2 hover:bg-gray-100 transition-colors"
                        >
                            <div className="font-medium text-gray-900">{asset.assetIdentifier}</div>
                            <div className="text-sm text-gray-600">{asset.name}</div>
                            <div className="text-xs text-gray-500">{asset.category}</div>
                        </button>
                    ))}
                </div>
            )}

            {/* Recent Searches Dropdown */}
            {showRecentSearches && recentSearches.length > 0 && (
                <div className="absolute z-10 mt-1 w-full bg-white shadow-lg max-h-60 rounded-md py-1 text-base ring-1 ring-black ring-opacity-5 overflow-auto focus:outline-none sm:text-sm">
                    <div className="flex items-center justify-between px-4 py-2 border-b border-gray-200">
                        <div className="flex items-center text-xs text-gray-500">
                            <Clock className="h-4 w-4 mr-1" />
                            <span>Recent Searches</span>
                        </div>
                        <button
                            onClick={clearRecentSearches}
                            className="text-xs text-blue-600 hover:text-blue-800"
                        >
                            Clear
                        </button>
                    </div>
                    {recentSearches.map((recent) => (
                        <button
                            key={recent.assetId}
                            onClick={() => handleRecentClick(recent)}
                            className="w-full text-left px-4 py-2 hover:bg-gray-100 transition-colors"
                        >
                            <div className="font-medium text-gray-900">{recent.assetIdentifier}</div>
                            <div className="text-sm text-gray-600">{recent.name}</div>
                        </button>
                    ))}
                </div>
            )}
        </div>
    );
};

export default AssetSearchBar;
