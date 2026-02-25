import { Fragment, useState } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import { X, Copy, MapPin, TrendingUp, AlertTriangle, Clock, ExternalLink } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { fetchHotspotDetail } from '../../services/heatmapService';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

/**
 * HotspotDetailModal Component
 * Shows detailed statistics for a clicked heatmap cell
 * Feature: 005-asset-tracking-security (Task T051)
 */

interface HotspotDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    latitude: number;
    longitude: number;
    gridSize: number;
    mode: 'activity' | 'violations' | 'dwell';
    tenantCode: string;
    startTime: string;
    endTime: string;
}

const HotspotDetailModal: React.FC<HotspotDetailModalProps> = ({
    isOpen,
    onClose,
    latitude,
    longitude,
    gridSize,
    mode,
    tenantCode,
    startTime,
    endTime
}) => {
    const navigate = useNavigate();
    const [copied, setCopied] = useState(false);

    // Fetch hotspot details
    const { data, isLoading, error } = useQuery({
        queryKey: ['hotspot-detail', latitude, longitude, mode, gridSize, startTime],
        queryFn: () => fetchHotspotDetail(latitude, longitude, gridSize, mode, tenantCode, startTime, endTime),
        enabled: isOpen
    });

    const handleCopyCoordinates = () => {
        navigator.clipboard.writeText(`${latitude?.toFixed(6) ?? 'N/A'}, ${longitude?.toFixed(6) ?? 'N/A'}`);
        // Assuming 'toast' is available globally or imported elsewhere
        // If not, this line will cause an error.
        // For example, if using react-toastify, you would need: import { toast } from 'react-toastify';
        // toast.success('Coordinates copied'); 
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const handleViewAssets = () => {
        // Navigate to asset map with location filter
        const params = new URLSearchParams({
            lat: latitude.toString(),
            lng: longitude.toString(),
            radius: (gridSize / 2).toString()
        });
        navigate(`/tracking/assets?${params.toString()}`);
        onClose();
    };

    const handleViewViolations = () => {
        // Navigate to violation report with location filter
        const params = new URLSearchParams({
            lat: latitude.toString(),
            lng: longitude.toString(),
            startTime,
            endTime
        });
        navigate(`/reports/violations?${params.toString()}`);
        onClose();
    };

    // Prepare time distribution chart data
    const timeDistributionData = data?.timeDistribution && Array.isArray(data.timeDistribution)
        ? data.timeDistribution.map(point => ({
            hour: `${point.hour}:00`,
            count: point.count
        }))
        : [];

    return (
        <Transition appear show={isOpen} as={Fragment}>
            <Dialog className="relative z-[2000]" onClose={onClose}>
                <Transition.Child
                    as={Fragment}
                    enter="ease-out duration-300"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="ease-in duration-200"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                >
                    <div className="fixed inset-0 bg-black bg-opacity-25" />
                </Transition.Child>

                <div className="fixed inset-0 overflow-y-auto">
                    <div className="flex min-h-full items-center justify-center p-4">
                        <Transition.Child
                            as={Fragment}
                            enter="ease-out duration-300"
                            enterFrom="opacity-0 scale-95"
                            enterTo="opacity-100 scale-100"
                            leave="ease-in duration-200"
                            leaveFrom="opacity-100 scale-100"
                            leaveTo="opacity-0 scale-95"
                        >
                            <Dialog.Panel className="w-full max-w-2xl transform overflow-hidden rounded-2xl bg-white shadow-xl transition-all">
                                {/* Header */}
                                <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
                                    <Dialog.Title className="text-lg font-semibold text-gray-900 flex items-center gap-2">
                                        <MapPin className="w-5 h-5 text-blue-600" />
                                        Hotspot Details
                                    </Dialog.Title>
                                    <button
                                        onClick={onClose}
                                        className="text-gray-400 hover:text-gray-600 transition"
                                    >
                                        <X className="w-5 h-5" />
                                    </button>
                                </div>

                                {/* Content */}
                                <div className="px-6 py-4 max-h-[70vh] overflow-y-auto">
                                    {isLoading && (
                                        <div className="flex items-center justify-center py-12">
                                            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div>
                                        </div>
                                    )}

                                    {error && (
                                        <div className="text-center py-12">
                                            <p className="text-red-600">Failed to load hotspot details</p>
                                            <p className="text-sm text-gray-500 mt-2">{(error as Error).message}</p>
                                        </div>
                                    )}

                                    {data && (
                                        <div className="space-y-6">
                                            {/* Location */}
                                            <div className="bg-gray-50 rounded-lg p-4">
                                                <h3 className="text-sm font-medium text-gray-700 mb-2">Location</h3>
                                                <div className="flex items-center justify-between">
                                                    <span className="font-mono text-gray-300">
                                                        {latitude?.toFixed(6) ?? 'N/A'}, {longitude?.toFixed(6) ?? 'N/A'}
                                                    </span>
                                                    <button
                                                        onClick={handleCopyCoordinates}
                                                        className="flex items-center gap-1 px-3 py-1 text-sm text-blue-600 hover:bg-blue-50 rounded transition"
                                                    >
                                                        <Copy className="w-4 h-4" />
                                                        {copied ? 'Copied!' : 'Copy'}
                                                    </button>
                                                </div>
                                                <p className="text-xs text-gray-500 mt-1">Grid Size: {gridSize}m</p>
                                            </div>

                                            {/* Statistics by Mode */}
                                            {mode === 'activity' && (
                                                <div className="grid grid-cols-3 gap-4">
                                                    <div className="bg-blue-50 rounded-lg p-4">
                                                        <div className="flex items-center gap-2 mb-1">
                                                            <TrendingUp className="w-4 h-4 text-blue-600" />
                                                            <span className="text-xs font-medium text-blue-900">Total Movements</span>
                                                        </div>
                                                        <p className="text-2xl font-bold text-blue-600">{data.totalCount}</p>
                                                    </div>
                                                    <div className="bg-green-50 rounded-lg p-4">
                                                        <div className="flex items-center gap-2 mb-1">
                                                            <TrendingUp className="w-4 h-4 text-green-600" />
                                                            <span className="text-xs font-medium text-green-900">Unique Assets</span>
                                                        </div>
                                                        <p className="text-2xl font-bold text-green-600">{data.assets?.length || 0}</p>
                                                    </div>
                                                    <div className="bg-amber-50 rounded-lg p-4">
                                                        <div className="flex items-center gap-2 mb-1">
                                                            <TrendingUp className="w-4 h-4 text-amber-600" />
                                                            <span className="text-xs font-medium text-amber-900">Avg Speed</span>
                                                        </div>
                                                        <p className="text-2xl font-bold text-amber-600">
                                                            {data.assets?.[0]?.avgSpeed?.toFixed(1) || '0'}<span className="text-sm ml-1">km/h</span>
                                                        </p>
                                                    </div>
                                                </div>
                                            )}

                                            {mode === 'violations' && data.violations && Array.isArray(data.violations) && data.violations.length > 0 && (
                                                <div>
                                                    <h3 className="text-sm font-medium text-gray-700 mb-3 flex items-center gap-2">
                                                        <AlertTriangle className="w-4 h-4 text-red-600" />
                                                        Violations by Severity
                                                    </h3>
                                                    <div className="grid grid-cols-4 gap-3">
                                                        {data.violations.map(v => (
                                                            <div key={v.severity} className={`rounded-lg p-3 ${v.severity === 'CRITICAL' ? 'bg-red-50 border border-red-200' :
                                                                    v.severity === 'HIGH' ? 'bg-orange-50 border border-orange-200' :
                                                                        v.severity === 'MEDIUM' ? 'bg-yellow-50 border border-yellow-200' :
                                                                            'bg-gray-50 border border-gray-200'
                                                                }`}>
                                                                <p className="text-xs font-medium text-gray-600">{v.severity}</p>
                                                                <p className="text-xl font-bold mt-1">{v.count}</p>
                                                            </div>
                                                        ))}
                                                    </div>
                                                </div>
                                            )}

                                            {mode === 'dwell' && data.dwellStats && (
                                                <div className="grid grid-cols-3 gap-4">
                                                    <div className="bg-purple-50 rounded-lg p-4">
                                                        <div className="flex items-center gap-2 mb-1">
                                                            <Clock className="w-4 h-4 text-purple-600" />
                                                            <span className="text-xs font-medium text-purple-900">Total Dwell</span>
                                                        </div>
                                                        <p className="text-2xl font-bold text-purple-600">
                                                            {Math.floor(data.dwellStats.totalDwellTime / 60)}<span className="text-sm ml-1">min</span>
                                                        </p>
                                                    </div>
                                                    <div className="bg-indigo-50 rounded-lg p-4">
                                                        <div className="flex items-center gap-2 mb-1">
                                                            <Clock className="w-4 h-4 text-indigo-600" />
                                                            <span className="text-xs font-medium text-indigo-900">Avg Dwell</span>
                                                        </div>
                                                        <p className="text-2xl font-bold text-indigo-600">
                                                            {Math.floor(data.dwellStats.avgDwellTime / 60)}<span className="text-sm ml-1">min</span>
                                                        </p>
                                                    </div>
                                                    <div className="bg-cyan-50 rounded-lg p-4">
                                                        <div className="flex items-center gap-2 mb-1">
                                                            <Clock className="w-4 h-4 text-cyan-600" />
                                                            <span className="text-xs font-medium text-cyan-900">Asset Count</span>
                                                        </div>
                                                        <p className="text-2xl font-bold text-cyan-600">{data.dwellStats.assetCount}</p>
                                                    </div>
                                                </div>
                                            )}

                                            {/* Asset List */}
                                            {data.assets && Array.isArray(data.assets) && data.assets.length > 0 && (
                                                <div>
                                                    <h3 className="text-sm font-medium text-gray-700 mb-2">Assets in this Area</h3>
                                                    <div className="bg-gray-50 rounded-lg max-h-40 overflow-y-auto">
                                                        <table className="min-w-full text-sm">
                                                            <thead className="bg-gray-100 sticky top-0">
                                                                <tr>
                                                                    <th className="px-3 py-2 text-left font-medium text-gray-600">ID</th>
                                                                    <th className="px-3 py-2 text-left font-medium text-gray-600">Name</th>
                                                                    <th className="px-3 py-2 text-left font-medium text-gray-600">Category</th>
                                                                    <th className="px-3 py-2 text-right font-medium text-gray-600">Count</th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>
                                                                {data.assets.map((asset, idx) => (
                                                                    <tr key={idx} className="border-t border-gray-200">
                                                                        <td className="px-3 py-2 text-gray-700 font-mono">{asset.assetIdentifier}</td>
                                                                        <td className="px-3 py-2 text-gray-700">{asset.name}</td>
                                                                        <td className="px-3 py-2 text-gray-600">{asset.category}</td>
                                                                        <td className="px-3 py-2 text-right text-gray-700 font-medium">{asset.count}</td>
                                                                    </tr>
                                                                ))}
                                                            </tbody>
                                                        </table>
                                                    </div>
                                                </div>
                                            )}

                                            {/* Time Distribution Chart */}
                                            {timeDistributionData.length > 0 && (
                                                <div>
                                                    <h3 className="text-sm font-medium text-gray-700 mb-3">Hourly Distribution</h3>
                                                    <ResponsiveContainer width="100%" height={200}>
                                                        <BarChart data={timeDistributionData}>
                                                            <CartesianGrid strokeDasharray="3 3" />
                                                            <XAxis dataKey="hour" tick={{ fontSize: 11 }} />
                                                            <YAxis tick={{ fontSize: 11 }} />
                                                            <Tooltip />
                                                            <Bar dataKey="count" fill="#3B82F6" />
                                                        </BarChart>
                                                    </ResponsiveContainer>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>

                                {/* Footer Actions */}
                                {data && (
                                    <div className="border-t border-gray-200 px-6 py-4 bg-gray-50 flex items-center justify-between">
                                        <button
                                            onClick={onClose}
                                            className="px-4 py-2 text-sm text-gray-700 hover:bg-gray-200 rounded-md transition"
                                        >
                                            Close
                                        </button>
                                        <div className="flex gap-2">
                                            <button
                                                onClick={handleViewAssets}
                                                className="flex items-center gap-2 px-4 py-2 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 transition"
                                            >
                                                <ExternalLink className="w-4 h-4" />
                                                View Assets
                                            </button>
                                            {mode === 'violations' && (
                                                <button
                                                    onClick={handleViewViolations}
                                                    className="flex items-center gap-2 px-4 py-2 text-sm bg-red-600 text-white rounded-md hover:bg-red-700 transition"
                                                >
                                                    <ExternalLink className="w-4 h-4" />
                                                    View Violations
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                )}
                            </Dialog.Panel>
                        </Transition.Child>
                    </div>
                </div>
            </Dialog>
        </Transition>
    );
};

export default HotspotDetailModal;
