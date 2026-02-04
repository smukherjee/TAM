/**
 * ZoneEditorPage
 * Admin page for creating and managing zones with GeoJSON support.
 * Implements FR-040 to FR-048: Admin zone management.
 */

import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import ZoneEditorMap from '../../components/admin/ZoneEditorMap';
import ZonePropertiesPanel from '../../components/admin/ZonePropertiesPanel';
import GeoJsonImportExport from '../../components/admin/GeoJsonImportExport';
import { Zone, ZoneType, zoneApi, ImportResult } from '../../services/zoneApi';
import { validatePolygon } from '../../utils/GeometryValidator';

const ZoneEditorPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const tenantCode = searchParams.get('tenant') || 'VIDP';

  // State
  const [zones, setZones] = useState<Zone[]>([]);
  const [zoneTypes, setZoneTypes] = useState<ZoneType[]>([]);
  const [selectedZone, setSelectedZone] = useState<Zone | null>(null);
  const [editingZone, setEditingZone] = useState<Partial<Zone> | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Load zones and types on mount
  useEffect(() => {
    loadData();
  }, [tenantCode]);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [zonesData, typesData] = await Promise.all([
        zoneApi.getZones(tenantCode),
        zoneApi.getZoneTypes(),
      ]);
      setZones(zonesData);
      setZoneTypes(typesData);
    } catch (err) {
      setError('Failed to load zones');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Handle zone selection
  const handleZoneSelect = (zone: Zone | null) => {
    setSelectedZone(zone);
    if (zone) {
      setEditingZone({ ...zone });
      setIsCreating(false);
      setEditMode(false);
    } else {
      setEditingZone(null);
    }
  };

  // Start creating new zone
  const handleCreate = () => {
    setSelectedZone(null);
    setEditingZone({
      tenantCode,
      name: '',
      code: '',
      type: 'CUSTOM',
      coordinates: [],
      color: '#6b7280',
      opacity: 0.3,
      active: true,
      restricted: false,
    });
    setIsCreating(true);
    setEditMode(false);
  };

  // Handle zone property changes
  const handleZoneChange = (updated: Partial<Zone>) => {
    setEditingZone(updated);
  };

  // Handle coordinates change from map
  const handleCoordinatesChange = (coordinates: number[][]) => {
    if (editingZone) {
      setEditingZone({ ...editingZone, coordinates });
    }
  };

  // Start editing mode
  const handleStartEdit = () => {
    if (selectedZone) {
      setEditingZone({ ...selectedZone });
      setEditMode(true);
    }
  };

  // Cancel editing
  const handleCancel = () => {
    setEditingZone(null);
    setIsCreating(false);
    setEditMode(false);
    if (selectedZone) {
      setEditingZone({ ...selectedZone });
    }
  };

  // Save zone
  const handleSave = async () => {
    if (!editingZone) return;

    // Validate
    if (!editingZone.name?.trim()) {
      setError('Zone name is required');
      return;
    }
    if (!editingZone.code?.trim()) {
      setError('Zone code is required');
      return;
    }
    if (!editingZone.coordinates || editingZone.coordinates.length < 3) {
      setError('Zone must have at least 3 coordinate points');
      return;
    }

    // Validate geometry
    const validation = validatePolygon(editingZone.coordinates);
    if (!validation.valid) {
      setError(`Invalid geometry: ${validation.errors.join(', ')}`);
      return;
    }

    setSaving(true);
    setError(null);

    try {
      if (isCreating) {
        await zoneApi.createZone(editingZone as Omit<Zone, 'id' | 'createdAt' | 'updatedAt'>);
        setSuccessMessage('Zone created successfully');
      } else if (selectedZone?.id) {
        await zoneApi.updateZone(selectedZone.id, editingZone);
        setSuccessMessage('Zone updated successfully');
      }

      await loadData();
      setIsCreating(false);
      setEditMode(false);
      setSelectedZone(null);
      setEditingZone(null);
    } catch (err) {
      setError('Failed to save zone');
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  // Delete zone
  const handleDelete = async () => {
    if (!selectedZone?.id) return;
    if (!confirm('Are you sure you want to delete this zone?')) return;

    try {
      await zoneApi.deleteZone(selectedZone.id);
      setSuccessMessage('Zone deleted successfully');
      await loadData();
      setSelectedZone(null);
      setEditingZone(null);
    } catch (err) {
      setError('Failed to delete zone');
      console.error(err);
    }
  };

  // Handle import success
  const handleImportSuccess = (result: ImportResult) => {
    setSuccessMessage(`Successfully imported ${result.imported} zones`);
    loadData();
  };

  // Clear messages after delay
  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  useEffect(() => {
    if (error) {
      const timer = setTimeout(() => setError(null), 10000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  return (
    <div className="min-h-screen bg-gray-100">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Zone Editor</h1>
            <p className="text-gray-600">
              Draw and manage zones for {tenantCode}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <GeoJsonImportExport
              tenantCode={tenantCode}
              onImportSuccess={handleImportSuccess}
              onExportSuccess={() => setSuccessMessage('Zones exported successfully')}
              onError={setError}
            />
            <button
              onClick={handleCreate}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              disabled={isCreating}
            >
              + New Zone
            </button>
          </div>
        </div>
      </div>

      {/* Messages */}
      {error && (
        <div className="mx-6 mt-4 p-4 bg-red-50 border border-red-200 rounded-lg text-red-700 flex justify-between items-center">
          <span>{error}</span>
          <button onClick={() => setError(null)} className="text-red-500 hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {successMessage && (
        <div className="mx-6 mt-4 p-4 bg-green-50 border border-green-200 rounded-lg text-green-700 flex justify-between items-center">
          <span>{successMessage}</span>
          <button onClick={() => setSuccessMessage(null)} className="text-green-500 hover:underline">
            Dismiss
          </button>
        </div>
      )}

      {/* Main Content */}
      <div className="p-6">
        <div className="grid grid-cols-12 gap-6 h-[calc(100vh-200px)]">
          {/* Zone List */}
          <div className="col-span-3 bg-white rounded-lg shadow overflow-hidden flex flex-col">
            <div className="p-4 border-b border-gray-200">
              <h2 className="text-lg font-semibold">Zones ({zones.length})</h2>
            </div>

            <div className="flex-1 overflow-y-auto p-4">
              {loading ? (
                <div className="text-center py-8 text-gray-500">Loading...</div>
              ) : zones.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  No zones yet. Click "New Zone" or import from GeoJSON.
                </div>
              ) : (
                <div className="space-y-2">
                  {zones.map((zone) => (
                    <div
                      key={zone.id}
                      onClick={() => handleZoneSelect(zone)}
                      className={`p-3 border rounded-lg cursor-pointer transition-colors ${
                        selectedZone?.id === zone.id
                          ? 'border-blue-500 bg-blue-50'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <div
                          className="w-4 h-4 rounded"
                          style={{ backgroundColor: zone.color || '#6b7280' }}
                        />
                        <div className="flex-1">
                          <div className="font-medium text-sm">{zone.name}</div>
                          <div className="text-xs text-gray-500">
                            {zone.code} • {zone.type}
                          </div>
                        </div>
                        <div className="flex items-center gap-1">
                          {zone.restricted && (
                            <span className="text-xs text-red-600" title="Restricted">
                              🚫
                            </span>
                          )}
                          <span
                            className={`w-2 h-2 rounded-full ${
                              zone.active ? 'bg-green-500' : 'bg-gray-300'
                            }`}
                          />
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Map */}
          <div className="col-span-6 bg-white rounded-lg shadow overflow-hidden">
            <ZoneEditorMap
              tenantCode={tenantCode}
              zones={zones}
              selectedZoneId={selectedZone?.id}
              onZoneSelect={handleZoneSelect}
              onZoneChange={handleCoordinatesChange}
              onDrawComplete={handleCoordinatesChange}
              editMode={editMode || isCreating}
              height="100%"
            />
          </div>

          {/* Properties Panel */}
          <div className="col-span-3 bg-white rounded-lg shadow overflow-hidden flex flex-col">
            {(editingZone || isCreating) ? (
              <ZonePropertiesPanel
                zone={editingZone}
                zoneTypes={zoneTypes}
                isCreating={isCreating}
                onChange={handleZoneChange}
                onSave={handleSave}
                onCancel={handleCancel}
                onDelete={!isCreating ? handleDelete : undefined}
                saving={saving}
              />
            ) : selectedZone ? (
              <div className="p-4">
                <div className="flex justify-between items-start mb-4">
                  <div>
                    <h3 className="text-lg font-semibold">{selectedZone.name}</h3>
                    <p className="text-sm text-gray-500">{selectedZone.code}</p>
                  </div>
                  <button
                    onClick={handleStartEdit}
                    className="px-3 py-1 bg-gray-100 hover:bg-gray-200 rounded transition-colors text-sm"
                  >
                    Edit
                  </button>
                </div>

                <div className="space-y-3 text-sm">
                  <div className="flex justify-between">
                    <span className="text-gray-500">Type</span>
                    <span>{selectedZone.type}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Status</span>
                    <span
                      className={selectedZone.active ? 'text-green-600' : 'text-gray-400'}
                    >
                      {selectedZone.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Restricted</span>
                    <span>{selectedZone.restricted ? 'Yes' : 'No'}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-gray-500">Points</span>
                    <span>{selectedZone.coordinates?.length || 0}</span>
                  </div>
                  {selectedZone.description && (
                    <div>
                      <span className="text-gray-500 block">Description</span>
                      <p className="mt-1">{selectedZone.description}</p>
                    </div>
                  )}
                </div>

                {/* Quick actions */}
                <div className="mt-6 space-y-2">
                  <button
                    onClick={() => handleZoneSelect(null)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm"
                  >
                    Deselect
                  </button>
                  <button
                    onClick={async () => {
                      try {
                        if (selectedZone.active) {
                          await zoneApi.deactivateZone(selectedZone.id!);
                        } else {
                          await zoneApi.activateZone(selectedZone.id!);
                        }
                        loadData();
                      } catch (err) {
                        setError('Failed to update zone status');
                      }
                    }}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm"
                  >
                    {selectedZone.active ? 'Deactivate' : 'Activate'}
                  </button>
                </div>
              </div>
            ) : (
              <div className="flex-1 flex items-center justify-center p-4 text-center text-gray-500">
                <div>
                  <div className="text-4xl mb-2">🗺️</div>
                  <p>Select a zone from the list or map, or draw a new one</p>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ZoneEditorPage;
