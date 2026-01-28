import React, { useState } from 'react';
import { Layers, Filter, ChevronDown, ChevronUp, Eye, EyeOff, Sun, Moon } from 'lucide-react';
import { useDraggable } from '../hooks/useDraggable';
import './MapIcons.css';

interface MapControlPanelProps {
  layers: {
    vehicles: boolean;
    flights: boolean;
    alerts: boolean;
    assets?: boolean;
  };
  onLayerToggle: (layer: 'vehicles' | 'flights' | 'alerts' | 'assets') => void;
  theme?: 'dark' | 'light';
  onThemeToggle?: () => void;
  vehicleTypes?: string[]; // Reserved for future use
  onVehicleFilter?: (types: string[]) => void;
  timeRange?: { start: Date; end: Date }; // Reserved for future use
  onTimeRangeChange?: (start: Date, end: Date) => void; // Reserved for future use
}

const MapControlPanel: React.FC<MapControlPanelProps> = ({
  layers,
  onLayerToggle,
  theme = 'dark',
  onThemeToggle,
  // vehicleTypes, // Reserved for future use
  onVehicleFilter,
  // timeRange, // Reserved for future use
  // onTimeRangeChange, // Reserved for future use
}) => {
  const [collapsed, setCollapsed] = useState(true);
  const [selectedVehicleTypes, setSelectedVehicleTypes] = useState<string[]>([]);
  const { handleMouseDown, style } = useDraggable(20, 20);

  const allVehicleTypes = ['bus', 'fuel_truck', 'tug', 'belt_loader', 'catering', 'other'];

  const handleVehicleTypeToggle = (type: string) => {
    const newTypes = selectedVehicleTypes.includes(type)
      ? selectedVehicleTypes.filter(t => t !== type)
      : [...selectedVehicleTypes, type];
    setSelectedVehicleTypes(newTypes);
    onVehicleFilter?.(newTypes);
  };

  return (
    <div 
      className="map-control-panel glass-panel" 
      style={{
        ...style,
        width: '280px',
        zIndex: 1000,
        padding: '16px',
      }}
      onMouseDown={handleMouseDown}
    >
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
          {/* Theme Toggle */}
          {onThemeToggle && (
            <div>
              <div className="flex items-center gap-2 mb-2">
                {theme === 'dark' ? <Moon className="w-4 h-4 text-gray-400" /> : <Sun className="w-4 h-4 text-gray-400" />}
                <span className="text-sm font-medium text-gray-300">Map Theme</span>
              </div>
              <button
                onClick={onThemeToggle}
                className="w-full flex items-center justify-between p-2 rounded bg-gray-800/50 hover:bg-gray-700/50 transition-colors"
              >
                <span className="text-sm text-gray-300 flex items-center gap-2">
                  {theme === 'dark' ? (
                    <><Moon className="w-4 h-4" /> Dark Mode</>
                  ) : (
                    <><Sun className="w-4 h-4" /> Light Mode</>
                  )}
                </span>
                <div className={`w-10 h-5 rounded-full transition-colors ${
                  theme === 'dark' ? 'bg-blue-600' : 'bg-gray-600'
                } relative`}>
                  <div className={`absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white transition-transform ${
                    theme === 'dark' ? 'translate-x-5' : 'translate-x-0'
                  }`} />
                </div>
              </button>
            </div>
          )}

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
