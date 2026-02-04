/**
 * PathEditorPage
 * Admin page for creating and managing vehicle movement paths.
 * Implements FR-032 to FR-039: Admin path drawing interface.
 */

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import PathDrawingMap from '../../components/admin/PathDrawingMap';
import PathPreview from '../../components/admin/PathPreview';
import {
  VehiclePath,
  Waypoint,
  pathApi,
} from '../../services/pathApi';

// Vehicle types available for paths
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

// Vehicle type labels
const VEHICLE_LABELS: Record<string, string> = {
  FUEL: '⛽ Fuel Truck',
  CATERING: '🍽️ Catering',
  BAGGAGE_TUG: '🚜 Baggage Tug',
  BAGGAGE_CART: '📦 Baggage Cart',
  BELT_LOADER: '📤 Belt Loader',
  GPU: '⚡ GPU',
  PUSHBACK: '🚗 Pushback',
  STAIRS: '🪜 Passenger Stairs',
  WATER: '💧 Water Service',
  LAVATORY: '🚽 Lavatory Service',
  DEICING: '❄️ Deicing',
  ASU: '🌬️ Air Starter Unit',
  BUS: '🚌 Passenger Bus',
  CARGO: '📦 Cargo Loader',
  AMBULIFT: '🚑 Ambulift',
};

const PathEditorPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const tenantCode = searchParams.get('tenant') || 'VIDP';

  // State
  const [paths, setPaths] = useState<VehiclePath[]>([]);
  const [selectedPath, setSelectedPath] = useState<VehiclePath | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [showPreview, setShowPreview] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [validationErrors, setValidationErrors] = useState<string[]>([]);

  // Form state for new/edit path
  const [formData, setFormData] = useState<Partial<VehiclePath>>({
    tenantCode,
    name: '',
    description: '',
    vehicleType: 'FUEL',
    waypoints: [],
    loop: false,
    active: true,
  });

  // Load paths on mount
  useEffect(() => {
    loadPaths();
  }, [tenantCode]);

  const loadPaths = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await pathApi.getPaths(tenantCode);
      setPaths(data);
    } catch (err) {
      setError('Failed to load paths');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Handle form input changes
  const handleInputChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? (e.target as HTMLInputElement).checked : value,
    }));
  };

  // Handle waypoints from map
  const handleWaypointsChange = (waypoints: Waypoint[]) => {
    setFormData((prev) => ({ ...prev, waypoints }));
  };

  // Handle validation errors from map
  const handleValidationError = (errors: string[]) => {
    setValidationErrors(errors);
  };

  // Start creating new path
  const handleCreate = () => {
    setSelectedPath(null);
    setFormData({
      tenantCode,
      name: '',
      description: '',
      vehicleType: 'FUEL',
      waypoints: [],
      loop: false,
      active: true,
    });
    setIsCreating(true);
    setIsEditing(false);
    setShowPreview(false);
    setValidationErrors([]);
  };

  // Start editing existing path
  const handleEdit = (path: VehiclePath) => {
    setSelectedPath(path);
    setFormData({
      ...path,
      waypoints: pathApi.parseWaypoints(path.waypoints as unknown as string),
    });
    setIsEditing(true);
    setIsCreating(false);
    setShowPreview(false);
    setValidationErrors([]);
  };

  // Cancel create/edit
  const handleCancel = () => {
    setIsCreating(false);
    setIsEditing(false);
    setSelectedPath(null);
    setFormData({
      tenantCode,
      name: '',
      description: '',
      vehicleType: 'FUEL',
      waypoints: [],
      loop: false,
      active: true,
    });
    setValidationErrors([]);
  };

  // Save path
  const handleSave = async () => {
    if (!formData.name?.trim()) {
      setError('Path name is required');
      return;
    }
    if (!formData.waypoints || formData.waypoints.length < 2) {
      setError('Path must have at least 2 waypoints');
      return;
    }

    setSaving(true);
    setError(null);

    try {
      if (isCreating) {
        await pathApi.createPath(formData as Omit<VehiclePath, 'id' | 'createdAt' | 'updatedAt'>);
      } else if (isEditing && selectedPath?.id) {
        await pathApi.updatePath(selectedPath.id, formData);
      }

      await loadPaths();
      handleCancel();
    } catch (err) {
      setError('Failed to save path');
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  // Delete path
  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this path?')) return;

    try {
      await pathApi.deletePath(id);
      await loadPaths();
      if (selectedPath?.id === id) {
        handleCancel();
      }
    } catch (err) {
      setError('Failed to delete path');
      console.error(err);
    }
  };

  // Toggle path active status
  const handleToggleActive = async (path: VehiclePath) => {
    try {
      if (path.active) {
        await pathApi.deactivatePath(path.id!);
      } else {
        await pathApi.activatePath(path.id!);
      }
      await loadPaths();
    } catch (err) {
      setError('Failed to update path status');
      console.error(err);
    }
  };

  // View path details
  const handleView = (path: VehiclePath) => {
    setSelectedPath(path);
    setShowPreview(true);
    setIsCreating(false);
    setIsEditing(false);
  };

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-6 flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Vehicle Path Editor</h1>
            <p className="text-gray-600">
              Draw and manage vehicle movement paths for {tenantCode}
            </p>
          </div>
          <button
            onClick={handleCreate}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            disabled={isCreating || isEditing}
          >
            + New Path
          </button>
        </div>

        {/* Error display */}
        {error && (
          <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700">
            {error}
            <button onClick={() => setError(null)} className="ml-4 text-red-500 hover:underline">
              Dismiss
            </button>
          </div>
        )}

        <div className="grid grid-cols-12 gap-6">
          {/* Path List */}
          <div className="col-span-4">
            <div className="bg-white rounded-lg shadow p-4">
              <h2 className="text-lg font-semibold mb-4">Saved Paths</h2>

              {loading ? (
                <div className="text-center py-8 text-gray-500">Loading...</div>
              ) : paths.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  No paths created yet. Click "New Path" to create one.
                </div>
              ) : (
                <div className="space-y-2">
                  {paths.map((path) => (
                    <div
                      key={path.id}
                      className={`p-3 border rounded-lg cursor-pointer transition-colors ${
                        selectedPath?.id === path.id
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                      onClick={() => handleView(path)}
                    >
                      <div className="flex justify-between items-start">
                        <div>
                          <div className="font-medium">{path.name}</div>
                          <div className="text-sm text-gray-500">
                            {VEHICLE_LABELS[path.vehicleType || path.vehicleTypeCode || ''] || path.vehicleType || path.vehicleTypeCode || 'Unknown'}
                          </div>
                          {path.totalDistanceKm && (
                            <div className="text-xs text-gray-400">
                              {path.totalDistanceKm.toFixed(2)} km •{' '}
                              {Math.round((path.estimatedDurationSeconds || 0) / 60)} min
                            </div>
                          )}
                        </div>
                        <div className="flex items-center gap-1">
                          <span
                            className={`w-2 h-2 rounded-full ${
                              path.active ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                          />
                          {path.loop && (
                            <span className="text-xs text-gray-400" title="Loops">
                              🔁
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Actions */}
                      <div className="mt-2 flex gap-2">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleEdit(path);
                          }}
                          className="text-xs px-2 py-1 bg-gray-100 hover:bg-gray-200 rounded"
                        >
                          Edit
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleToggleActive(path);
                          }}
                          className="text-xs px-2 py-1 bg-gray-100 hover:bg-gray-200 rounded"
                        >
                          {path.active ? 'Deactivate' : 'Activate'}
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDelete(path.id!);
                          }}
                          className="text-xs px-2 py-1 bg-red-50 text-red-600 hover:bg-red-100 rounded"
                        >
                          Delete
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Main Content */}
          <div className="col-span-8">
            {/* Create/Edit Form */}
            {(isCreating || isEditing) && (
              <div className="bg-white rounded-lg shadow p-4 mb-4">
                <h2 className="text-lg font-semibold mb-4">
                  {isCreating ? 'Create New Path' : 'Edit Path'}
                </h2>

                {/* Form fields */}
                <div className="grid grid-cols-2 gap-4 mb-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Path Name *
                    </label>
                    <input
                      type="text"
                      name="name"
                      value={formData.name || ''}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="e.g., Fuel Route Terminal 1"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Vehicle Type
                    </label>
                    <select
                      name="vehicleType"
                      value={formData.vehicleType || 'FUEL'}
                      onChange={handleInputChange}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      title="Vehicle Type"
                      aria-label="Select vehicle type"
                    >
                      {VEHICLE_TYPES.map((type) => (
                        <option key={type} value={type}>
                          {VEHICLE_LABELS[type] || type}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Description
                  </label>
                  <textarea
                    name="description"
                    value={formData.description || ''}
                    onChange={handleInputChange}
                    rows={2}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="Optional description of this path"
                  />
                </div>

                <div className="flex gap-4 mb-4">
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      name="loop"
                      checked={formData.loop || false}
                      onChange={handleInputChange}
                      className="rounded border-gray-300"
                    />
                    <span className="text-sm text-gray-700">Loop (return to start)</span>
                  </label>
                  <label className="flex items-center gap-2">
                    <input
                      type="checkbox"
                      name="active"
                      checked={formData.active !== false}
                      onChange={handleInputChange}
                      className="rounded border-gray-300"
                    />
                    <span className="text-sm text-gray-700">Active</span>
                  </label>
                </div>

                {/* Validation errors */}
                {validationErrors.length > 0 && (
                  <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                    <div className="font-medium text-yellow-800 mb-1">Validation Warnings:</div>
                    <ul className="text-sm text-yellow-700 list-disc list-inside">
                      {validationErrors.map((err, i) => (
                        <li key={i}>{err}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Map for drawing */}
                <div className="mb-4">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Draw Path on Map (use the line tool)
                  </label>
                  <PathDrawingMap
                    tenantCode={tenantCode}
                    initialWaypoints={formData.waypoints as Waypoint[]}
                    onPathChange={handleWaypointsChange}
                    onValidationError={handleValidationError}
                    height="400px"
                  />
                </div>

                {/* Action buttons */}
                <div className="flex gap-3 justify-end">
                  <button
                    onClick={handleCancel}
                    className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
                    disabled={saving}
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleSave}
                    className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
                    disabled={saving || !formData.name?.trim() || !formData.waypoints?.length}
                  >
                    {saving ? 'Saving...' : isCreating ? 'Create Path' : 'Save Changes'}
                  </button>
                </div>
              </div>
            )}

            {/* Preview */}
            {showPreview && selectedPath && (
              <div className="bg-white rounded-lg shadow p-4">
                <div className="flex justify-between items-center mb-4">
                  <div>
                    <h2 className="text-lg font-semibold">{selectedPath.name}</h2>
                    <p className="text-sm text-gray-500">{selectedPath.description}</p>
                  </div>
                  <button
                    onClick={() => handleEdit(selectedPath)}
                    className="px-4 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
                  >
                    Edit Path
                  </button>
                </div>

                <PathPreview
                  waypoints={pathApi.parseWaypoints(selectedPath.waypoints as unknown as string)}
                  vehicleType={selectedPath.vehicleType || selectedPath.vehicleTypeCode || ''}
                  loop={selectedPath.loop}
                  height="500px"
                />
              </div>
            )}

            {/* Empty state */}
            {!isCreating && !isEditing && !showPreview && (
              <div className="bg-white rounded-lg shadow p-8 text-center text-gray-500">
                <div className="text-6xl mb-4">🛣️</div>
                <h3 className="text-lg font-medium mb-2">Select or Create a Path</h3>
                <p>
                  Click on a path from the list to preview it, or click "New Path" to create a new
                  vehicle movement path.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default PathEditorPage;
