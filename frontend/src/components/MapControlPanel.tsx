import React, { useState } from 'react';
import { Layers, Filter, ChevronDown, ChevronUp, Eye, EyeOff } from 'lucide-react';
import './MapIcons.css';

interface MapControlPanelProps {
  layers: {
    vehicles: boolean;
    flights: boolean;
    alerts: boolean;
  };
  onLayerToggle: (layer: 'vehicles' | 'flights' | 'alerts') => void;
  vehicleTypes?: string[]; // Reserved for future use
  onVehicleFilter?: (types: string[]) => void;
  timeRange?: { start: Date; end: Date }; // Reserved for future use
  onTimeRangeChange?: (start: Date, end: Date) => void; // Reserved for future use
}

const MapControlPanel: React.FC<MapControlPanelProps> = ({
  layers,
  onLayerToggle,
  // vehicleTypes, // Reserved for future use
  onVehicleFilter,
  // timeRange, // Reserved for future use
  // onTimeRangeChange, // Reserved for future use
}) => {
  const [collapsed, setCollapsed] = useState(false);
  const [selectedVehicleTypes, setSelectedVehicleTypes] = useState<string[]>([]);

  const allVehicleTypes = ['bus', 'fuel_truck', 'tug', 'belt_loader', 'catering', 'other'];

  const handleVehicleTypeToggle = (type: string) => {
    const newTypes = selectedVehicleTypes.includes(type)
      ? selectedVehicleTypes.filter(t => t !== type)
      : [...selectedVehicleTypes, type];
    setSelectedVehicleTypes(newTypes);
    onVehicleFilter?.(newTypes);
  };

  return (
    <div className="map-control-panel glass-panel" style={{
      position: 'absolute',
      top: '20px',
      left: '20px',
      width: '280px',
      zIndex: 1000,
      padding: '16px',
    }}>
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Layers className="w-5 h-5 text-blue-400" />
          <h3 className="text-white font-semibold">Map Controls</h3>
        </div>
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="text-gray-400 hover:text-white transition-colors"
        >
          {collapsed ? <ChevronDown className="w-5 h-5" /> : <ChevronUp className="w-5 h-5" />}
        </button>
      </div>

      {!collapsed && (
        <div className="space-y-4">
          {/* Layer Toggles */}
          <div>
            <div className="flex items-center gap-2 mb-2">
              <Layers className="w-4 h-4 text-gray-400" />
              <span className="text-sm font-medium text-gray-300">Layers</span>
            </div>
            <div className="space-y-2">
              {(Object.keys(layers) as Array<keyof typeof layers>).map(layer => (
                <label key={layer} className="flex items-center justify-between p-2 rounded bg-gray-800/50 hover:bg-gray-700/50 transition-colors cursor-pointer">
                  <span className="text-sm text-gray-300 capitalize flex items-center gap-2">
                    {layers[layer] ? <Eye className="w-4 h-4" /> : <EyeOff className="w-4 h-4" />}
                    {layer}
                  </span>
                  <input
                    type="checkbox"
                    checked={layers[layer]}
                    onChange={() => onLayerToggle(layer)}
                    className="w-4 h-4 rounded border-gray-600 bg-gray-700 text-blue-600 focus:ring-blue-500 focus:ring-offset-gray-900"
                  />
                </label>
              ))}
            </div>
          </div>

          {/* Vehicle Type Filter */}
          {onVehicleFilter && (
            <div>
              <div className="flex items-center gap-2 mb-2">
                <Filter className="w-4 h-4 text-gray-400" />
                <span className="text-sm font-medium text-gray-300">Vehicle Types</span>
              </div>
              <div className="space-y-1">
                {allVehicleTypes.map(type => (
                  <label key={type} className="flex items-center justify-between p-2 rounded bg-gray-800/50 hover:bg-gray-700/50 transition-colors cursor-pointer">
                    <span className="text-xs text-gray-300 capitalize">
                      {type.replace('_', ' ')}
                    </span>
                    <input
                      type="checkbox"
                      checked={selectedVehicleTypes.includes(type)}
                      onChange={() => handleVehicleTypeToggle(type)}
                      className="w-3.5 h-3.5 rounded border-gray-600 bg-gray-700 text-blue-600 focus:ring-blue-500 focus:ring-offset-gray-900"
                    />
                  </label>
                ))}
              </div>
            </div>
          )}

          {/* Quick Actions */}
          <div className="pt-3 border-t border-gray-700">
            <button
              onClick={() => {
                setSelectedVehicleTypes([]);
                onVehicleFilter?.([]);
              }}
              className="w-full px-3 py-2 text-sm bg-gray-700 hover:bg-gray-600 text-gray-300 rounded transition-colors"
            >
              Clear All Filters
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default MapControlPanel;
