/**
 * ZonePropertiesPanel Component
 * Side panel for editing zone metadata and properties.
 * Implements FR-040 to FR-048: Admin zone management.
 */

import React, { useState, useEffect } from 'react';
import { Zone, ZoneType, calculatePolygonArea, ZONE_TYPE_COLORS } from '../../services/zoneApi';

export interface ZonePropertiesPanelProps {
  /** Zone being edited */
  zone: Partial<Zone> | null;
  /** Zone types available */
  zoneTypes: ZoneType[];
  /** Whether in create mode */
  isCreating?: boolean;
  /** Callback when zone properties change */
  onChange?: (zone: Partial<Zone>) => void;
  /** Callback to save */
  onSave?: () => void;
  /** Callback to cancel */
  onCancel?: () => void;
  /** Callback to delete */
  onDelete?: () => void;
  /** Whether saving is in progress */
  saving?: boolean;
}

// Vehicle types for access control
const VEHICLE_TYPES = [
  'FUEL',
  'CATERING',
  'BAGGAGE_TUG',
  'BAGGAGE_CART',
  'BELT_LOADER',
  'GPU',
  'PUSHBACK',
  'STAIRS',
  'WATER',
  'LAVATORY',
  'DEICING',
  'ASU',
  'BUS',
  'CARGO',
  'AMBULIFT',
];

// Roles for access control
const ACCESS_ROLES = [
  'ADMIN',
  'OPERATOR',
  'SUPERVISOR',
  'SECURITY',
  'MAINTENANCE',
  'GROUND_CREW',
  'PILOT',
  'DISPATCHER',
];

const ZonePropertiesPanel: React.FC<ZonePropertiesPanelProps> = ({
  zone,
  zoneTypes,
  isCreating = false,
  onChange,
  onSave,
  onCancel,
  onDelete,
  saving = false,
}) => {
  const [activeTab, setActiveTab] = useState<'general' | 'access' | 'alerts'>('general');
  const [area, setArea] = useState<number>(0);

  // Calculate area when coordinates change
  useEffect(() => {
    if (zone?.coordinates && zone.coordinates.length >= 3) {
      const areaSqM = calculatePolygonArea(zone.coordinates);
      setArea(areaSqM);
    } else {
      setArea(0);
    }
  }, [zone?.coordinates]);

  if (!zone) {
    return (
      <div className="p-4 text-center text-gray-500">
        <div className="text-4xl mb-2">🗺️</div>
        <p>Select a zone or draw a new one to edit its properties</p>
      </div>
    );
  }

  const handleChange = (field: keyof Zone, value: unknown) => {
    if (onChange) {
      onChange({ ...zone, [field]: value });
    }
  };

  const handleVehicleTypeToggle = (type: string) => {
    const current = zone.allowedVehicleTypes || [];
    const updated = current.includes(type)
      ? current.filter((t) => t !== type)
      : [...current, type];
    handleChange('allowedVehicleTypes', updated);
  };

  const handleRoleToggle = (role: string) => {
    const current = zone.allowedRoles || [];
    const updated = current.includes(role)
      ? current.filter((r) => r !== role)
      : [...current, role];
    handleChange('allowedRoles', updated);
  };

  const formatArea = (sqM: number): string => {
    if (sqM >= 1000000) {
      return `${(sqM / 1000000).toFixed(2)} km²`;
    } else if (sqM >= 10000) {
      return `${(sqM / 10000).toFixed(2)} ha`;
    } else {
      return `${sqM.toFixed(0)} m²`;
    }
  };

  return (
    <div className="h-full flex flex-col bg-white">
      {/* Header */}
      <div className="p-4 border-b border-gray-200">
        <h3 className="text-lg font-semibold">
          {isCreating ? 'Create New Zone' : 'Edit Zone'}
        </h3>
        {zone.coordinates && zone.coordinates.length > 0 && (
          <div className="text-sm text-gray-500 mt-1">
            {zone.coordinates.length} points • {formatArea(area)}
          </div>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-gray-200">
        {(['general', 'access', 'alerts'] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`flex-1 py-2 px-4 text-sm font-medium ${
              activeTab === tab
                ? 'text-blue-600 border-b-2 border-blue-600'
                : 'text-gray-500 hover:text-gray-700'
            }`}
          >
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto p-4">
        {/* General Tab */}
        {activeTab === 'general' && (
          <div className="space-y-4">
            {/* Name */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Zone Name *
              </label>
              <input
                type="text"
                value={zone.name || ''}
                onChange={(e) => handleChange('name', e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g., Terminal 1 Apron"
              />
            </div>

            {/* Code */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Zone Code *
              </label>
              <input
                type="text"
                value={zone.code || ''}
                onChange={(e) => handleChange('code', e.target.value.toUpperCase())}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 uppercase"
                placeholder="e.g., T1_APRON"
              />
            </div>

            {/* Type */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Zone Type
              </label>
              <select
                value={zone.type || 'CUSTOM'}
                onChange={(e) => {
                  handleChange('type', e.target.value);
                  // Auto-set color based on type
                  handleChange('color', ZONE_TYPE_COLORS[e.target.value] || '#6b7280');
                }}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                title="Zone Type"
                aria-label="Select zone type"
              >
                {zoneTypes.map((type) => (
                  <option key={type.code} value={type.code}>
                    {type.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Description */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Description
              </label>
              <textarea
                value={zone.description || ''}
                onChange={(e) => handleChange('description', e.target.value)}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Optional description..."
              />
            </div>

            {/* Color */}
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Color
                </label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    value={zone.color || '#3b82f6'}
                    onChange={(e) => handleChange('color', e.target.value)}
                    className="w-12 h-10 rounded border border-gray-300 cursor-pointer"
                    title="Pick zone color"
                    aria-label="Zone color picker"
                  />
                  <input
                    type="text"
                    value={zone.color || '#3b82f6'}
                    onChange={(e) => handleChange('color', e.target.value)}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    title="Zone color hex value"
                    placeholder="#3b82f6"
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Opacity
                </label>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  value={zone.opacity ?? 0.3}
                  onChange={(e) => handleChange('opacity', parseFloat(e.target.value))}
                  className="w-full"
                  title={`Opacity: ${((zone.opacity ?? 0.3) * 100).toFixed(0)}%`}
                  aria-label="Adjust zone opacity"
                />
                <div className="text-xs text-gray-500 text-center">
                  {((zone.opacity ?? 0.3) * 100).toFixed(0)}%
                </div>
              </div>
            </div>

            {/* Flags */}
            <div className="flex gap-4">
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={zone.restricted || false}
                  onChange={(e) => handleChange('restricted', e.target.checked)}
                  className="rounded border-gray-300"
                />
                <span className="text-sm text-gray-700">Restricted Zone</span>
              </label>
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={zone.active !== false}
                  onChange={(e) => handleChange('active', e.target.checked)}
                  className="rounded border-gray-300"
                />
                <span className="text-sm text-gray-700">Active</span>
              </label>
            </div>
          </div>
        )}

        {/* Access Tab */}
        {activeTab === 'access' && (
          <div className="space-y-4">
            {/* Allowed Vehicle Types */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Allowed Vehicle Types
              </label>
              <div className="grid grid-cols-2 gap-2">
                {VEHICLE_TYPES.map((type) => (
                  <label key={type} className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={zone.allowedVehicleTypes?.includes(type) || false}
                      onChange={() => handleVehicleTypeToggle(type)}
                      className="rounded border-gray-300"
                    />
                    <span className="text-sm">{type.replace(/_/g, ' ')}</span>
                  </label>
                ))}
              </div>
              <p className="text-xs text-gray-500 mt-1">
                Leave empty to allow all vehicle types
              </p>
            </div>

            {/* Allowed Roles */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Allowed Roles
              </label>
              <div className="grid grid-cols-2 gap-2">
                {ACCESS_ROLES.map((role) => (
                  <label key={role} className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      checked={zone.allowedRoles?.includes(role) || false}
                      onChange={() => handleRoleToggle(role)}
                      className="rounded border-gray-300"
                    />
                    <span className="text-sm">{role.replace(/_/g, ' ')}</span>
                  </label>
                ))}
              </div>
              <p className="text-xs text-gray-500 mt-1">
                Leave empty to allow all roles
              </p>
            </div>
          </div>
        )}

        {/* Alerts Tab */}
        {activeTab === 'alerts' && (
          <div className="space-y-4">
            {/* Alert flags */}
            <div className="space-y-3">
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={zone.alertOnEntry || false}
                  onChange={(e) => handleChange('alertOnEntry', e.target.checked)}
                  className="rounded border-gray-300"
                />
                <span className="text-sm text-gray-700">Alert on Entry</span>
              </label>

              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={zone.alertOnExit || false}
                  onChange={(e) => handleChange('alertOnExit', e.target.checked)}
                  className="rounded border-gray-300"
                />
                <span className="text-sm text-gray-700">Alert on Exit</span>
              </label>
            </div>

            {/* Dwell time alert */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Dwell Time Alert (minutes)
              </label>
              <input
                type="number"
                min="0"
                value={zone.dwellTimeAlertMinutes || ''}
                onChange={(e) =>
                  handleChange('dwellTimeAlertMinutes', e.target.value ? parseInt(e.target.value) : null)
                }
                className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="e.g., 15"
              />
              <p className="text-xs text-gray-500 mt-1">
                Alert if vehicle stays longer than this duration. Leave empty to disable.
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Actions */}
      <div className="p-4 border-t border-gray-200 space-y-2">
        <div className="flex gap-2">
          <button
            onClick={onCancel}
            className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            disabled={saving}
          >
            Cancel
          </button>
          <button
            onClick={onSave}
            className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
            disabled={saving || !zone.name?.trim() || !zone.code?.trim() || !zone.coordinates?.length}
          >
            {saving ? 'Saving...' : isCreating ? 'Create Zone' : 'Save Changes'}
          </button>
        </div>

        {!isCreating && onDelete && (
          <button
            onClick={onDelete}
            className="w-full px-4 py-2 text-red-600 border border-red-200 rounded-lg hover:bg-red-50 transition-colors"
            disabled={saving}
          >
            Delete Zone
          </button>
        )}
      </div>
    </div>
  );
};

export default ZonePropertiesPanel;
