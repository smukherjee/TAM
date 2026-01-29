import React, { useState, useRef, useEffect } from 'react';
import { format } from 'date-fns';
import { Calendar, ChevronDown } from 'lucide-react';

interface TrailDateRangePickerProps {
    startDate: Date;
    endDate: Date;
    onDateRangeChange: (start: Date, end: Date) => void;
    onQuickSelect: (preset: string) => void;
}

const QUICK_PRESETS = [
    { value: '1h', label: 'Last 1 Hour' },
    { value: '6h', label: 'Last 6 Hours' },
    { value: '24h', label: 'Last 24 Hours' },
    { value: '7d', label: 'Last 7 Days' },
    { value: '30d', label: 'Last 30 Days' },
];

/**
 * TrailDateRangePicker - Date range selector with quick presets
 * Feature: 005-asset-tracking-security
 * Phase 11: Movement Trail Visualization
 */
const TrailDateRangePicker: React.FC<TrailDateRangePickerProps> = ({
    startDate,
    endDate,
    onDateRangeChange,
    onQuickSelect
}) => {
    const [isOpen, setIsOpen] = useState(false);
    const [tempStartDate, setTempStartDate] = useState(format(startDate, "yyyy-MM-dd'T'HH:mm"));
    const [tempEndDate, setTempEndDate] = useState(format(endDate, "yyyy-MM-dd'T'HH:mm"));
    const dropdownRef = useRef<HTMLDivElement>(null);

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // Sync temp dates with props
    useEffect(() => {
        setTempStartDate(format(startDate, "yyyy-MM-dd'T'HH:mm"));
        setTempEndDate(format(endDate, "yyyy-MM-dd'T'HH:mm"));
    }, [startDate, endDate]);

    // Handle apply custom range
    const handleApply = () => {
        const start = new Date(tempStartDate);
        const end = new Date(tempEndDate);
        
        if (start < end) {
            onDateRangeChange(start, end);
            setIsOpen(false);
        }
    };

    // Handle quick preset selection
    const handlePresetSelect = (preset: string) => {
        onQuickSelect(preset);
        setIsOpen(false);
    };

    // Format display text
    const displayText = `${format(startDate, 'MMM d, HH:mm')} - ${format(endDate, 'MMM d, HH:mm')}`;

    return (
        <div className="relative" ref={dropdownRef}>
            {/* Trigger Button */}
            <button
                onClick={() => setIsOpen(!isOpen)}
                className="inline-flex items-center px-4 py-2 text-sm font-medium text-gray-300 
                    bg-gray-800 border border-gray-600 rounded-lg hover:bg-gray-700 
                    focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
                <Calendar className="h-4 w-4 mr-2 text-gray-400" />
                <span>{displayText}</span>
                <ChevronDown className={`h-4 w-4 ml-2 text-gray-400 transition-transform ${
                    isOpen ? 'rotate-180' : ''
                }`} />
            </button>

            {/* Dropdown */}
            {isOpen && (
                <div className="absolute z-50 mt-2 w-80 bg-gray-800 rounded-lg shadow-lg border 
                    border-gray-700 right-0">
                    {/* Quick Presets */}
                    <div className="p-3 border-b border-gray-700">
                        <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">
                            Quick Select
                        </div>
                        <div className="flex flex-wrap gap-2">
                            {QUICK_PRESETS.map((preset) => (
                                <button
                                    key={preset.value}
                                    onClick={() => handlePresetSelect(preset.value)}
                                    className="px-3 py-1 text-xs font-medium text-gray-300 
                                        bg-gray-700 rounded-full hover:bg-blue-900/50 
                                        hover:text-blue-400 transition-colors"
                                >
                                    {preset.label}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Custom Range */}
                    <div className="p-3">
                        <div className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-3">
                            Custom Range
                        </div>
                        
                        <div className="space-y-3">
                            {/* Start Date */}
                            <div>
                                <label className="block text-xs text-gray-500 mb-1">Start</label>
                                <input
                                    type="datetime-local"
                                    value={tempStartDate}
                                    onChange={(e) => setTempStartDate(e.target.value)}
                                    className="w-full px-3 py-2 text-sm border border-gray-600 
                                        bg-gray-700 text-white rounded-md focus:outline-none focus:ring-1 
                                        focus:ring-blue-500 focus:border-blue-500"
                                />
                            </div>

                            {/* End Date */}
                            <div>
                                <label className="block text-xs text-gray-500 mb-1">End</label>
                                <input
                                    type="datetime-local"
                                    value={tempEndDate}
                                    onChange={(e) => setTempEndDate(e.target.value)}
                                    className="w-full px-3 py-2 text-sm border border-gray-600 
                                        bg-gray-700 text-white rounded-md focus:outline-none focus:ring-1 
                                        focus:ring-blue-500 focus:border-blue-500"
                                />
                            </div>

                            {/* Validation message */}
                            {new Date(tempStartDate) >= new Date(tempEndDate) && (
                                <p className="text-xs text-red-400">
                                    Start date must be before end date
                                </p>
                            )}

                            {/* Max range warning */}
                            {(new Date(tempEndDate).getTime() - new Date(tempStartDate).getTime()) > 30 * 24 * 60 * 60 * 1000 && (
                                <p className="text-xs text-orange-400">
                                    Maximum range is 30 days
                                </p>
                            )}
                        </div>
                    </div>

                    {/* Actions */}
                    <div className="flex justify-end space-x-2 p-3 border-t border-gray-700 bg-gray-750 rounded-b-lg">
                        <button
                            onClick={() => setIsOpen(false)}
                            className="px-3 py-1.5 text-sm font-medium text-gray-400 
                                hover:text-gray-200 transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={handleApply}
                            disabled={new Date(tempStartDate) >= new Date(tempEndDate)}
                            className="px-4 py-1.5 text-sm font-medium text-white bg-blue-600 
                                rounded-md hover:bg-blue-700 disabled:opacity-50 
                                disabled:cursor-not-allowed transition-colors"
                        >
                            Apply
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default TrailDateRangePicker;
