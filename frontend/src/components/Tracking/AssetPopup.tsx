import React from 'react';
import { Popup } from 'react-leaflet';
import { useNavigate } from 'react-router-dom';
import { formatDistanceToNow } from 'date-fns';
import { MapPin, Clock, Gauge, AlertTriangle, Eye, FileText } from 'lucide-react';
import { AssetLocation, CATEGORY_COLORS, STATUS_COLORS } from '../../types/assetTracking';

interface AssetPopupProps {
    asset: AssetLocation;
    onClose: () => void;
}

/**
 * Asset Popup Component - Detailed asset information popup
 * Feature: 005-asset-tracking-security
 * Task: T039
 */
const AssetPopup: React.FC<AssetPopupProps> = ({ asset, onClose }) => {
    const navigate = useNavigate();

    const handleViewTrail = () => {
        navigate(`/tracking/trail/${asset.assetId}`, {
            state: { asset }
        });
        onClose();
    };

    const handleViewRegister = () => {
        navigate(`/assets/${asset.assetId}`);
        onClose();
    };

    const categoryColor = CATEGORY_COLORS[asset.category] || CATEGORY_COLORS['Other'];
    const statusColor = STATUS_COLORS[asset.status] || '#6B7280';

    return (
        <Popup
            position={[asset.latitude, asset.longitude]}
            closeButton={true}
            className="asset-popup"
            eventHandlers={{
                remove: onClose
            }}
        >
            <div className="min-w-[280px] p-2">
                {/* Header */}
                <div className="mb-3">
                    <h3 className="text-lg font-bold text-gray-900 mb-1">
                        {asset.name}
                    </h3>
                    <p className="text-sm text-gray-600 font-mono">
                        {asset.assetIdentifier}
                    </p>
                </div>

                {/* Badges */}
                <div className="flex gap-2 mb-3">
                    <span
                        className="px-2 py-1 text-xs font-semibold rounded-full text-white"
                        style={{ backgroundColor: categoryColor }}
                    >
                        {asset.category}
                    </span>
                    <span
                        className="px-2 py-1 text-xs font-semibold rounded-full text-white"
                        style={{ backgroundColor: statusColor }}
                    >
                        {asset.status}
                    </span>
                    {asset.hasViolation && (
                        <span className="px-2 py-1 text-xs font-semibold rounded-full bg-red-100 text-red-800 flex items-center gap-1">
                            <AlertTriangle className="w-3 h-3" />
                            Violation
                        </span>
                    )}
                </div>

                {/* Details */}
                <div className="space-y-2 mb-3">
                    {/* Zone */}
                    {asset.currentZone && (
                        <div className="flex items-start gap-2 text-sm">
                            <MapPin className="w-4 h-4 text-gray-500 mt-0.5" />
                            <div>
                                <p className="text-gray-900 font-medium">{asset.currentZone}</p>
                                {asset.currentZoneType && (
                                    <p className="text-xs text-gray-600">{asset.currentZoneType}</p>
                                )}
                            </div>
                        </div>
                    )}

                    {/* Speed (if moving) */}
                    {asset.speed !== undefined && asset.speed > 0 && (
                        <div className="flex items-center gap-2 text-sm">
                            <Gauge className="w-4 h-4 text-gray-500" />
                            <span className="text-gray-900">
                                {asset.speed.toFixed(1)} km/h
                            </span>
                        </div>
                    )}

                    {/* Last Updated */}
                    <div className="flex items-center gap-2 text-sm">
                        <Clock className="w-4 h-4 text-gray-500" />
                        <span className="text-gray-600">
                            {formatDistanceToNow(new Date(asset.lastSeen), { addSuffix: true })}
                        </span>
                    </div>

                    {/* Coordinates */}
                    <div className="text-xs text-gray-500 font-mono">
                        {asset.latitude.toFixed(6)}, {asset.longitude.toFixed(6)}
                    </div>
                </div>

                {/* Action Buttons */}
                <div className="flex gap-2 pt-2 border-t border-gray-200">
                    <button
                        onClick={handleViewTrail}
                        className="flex-1 px-3 py-2 bg-blue-600 text-white text-sm font-medium rounded hover:bg-blue-700 transition-colors flex items-center justify-center gap-1"
                    >
                        <Eye className="w-4 h-4" />
                        View Trail
                    </button>
                    <button
                        onClick={handleViewRegister}
                        className="flex-1 px-3 py-2 bg-gray-600 text-white text-sm font-medium rounded hover:bg-gray-700 transition-colors flex items-center justify-center gap-1"
                    >
                        <FileText className="w-4 h-4" />
                        Details
                    </button>
                </div>
            </div>
        </Popup>
    );
};

export default AssetPopup;
