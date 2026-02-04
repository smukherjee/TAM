/**
 * GeoJsonImportExport Component
 * Component for importing and exporting zones as GeoJSON.
 * Implements FR-045: GeoJSON import/export functionality.
 */

import React, { useRef, useState } from 'react';
import { zoneApi, ImportResult } from '../../services/zoneApi';

export interface GeoJsonImportExportProps {
  /** Tenant code */
  tenantCode: string;
  /** Callback when import is successful */
  onImportSuccess?: (result: ImportResult) => void;
  /** Callback when export is successful */
  onExportSuccess?: () => void;
  /** Callback on error */
  onError?: (error: string) => void;
}

const GeoJsonImportExport: React.FC<GeoJsonImportExportProps> = ({
  tenantCode,
  onImportSuccess,
  onExportSuccess,
  onError,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [importing, setImporting] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [pasteContent, setPasteContent] = useState('');
  const [importMode, setImportMode] = useState<'file' | 'paste'>('file');

  // Handle file selection
  const handleFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.name.endsWith('.geojson') && !file.name.endsWith('.json')) {
      onError?.('Please select a .geojson or .json file');
      return;
    }

    setImporting(true);
    try {
      const result = await zoneApi.importGeoJsonFile(tenantCode, file);
      onImportSuccess?.(result);
      setShowImportModal(false);
    } catch (err) {
      onError?.(`Import failed: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setImporting(false);
      // Reset file input
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  // Handle paste import
  const handlePasteImport = async () => {
    if (!pasteContent.trim()) {
      onError?.('Please paste GeoJSON content');
      return;
    }

    // Validate JSON
    try {
      JSON.parse(pasteContent);
    } catch {
      onError?.('Invalid JSON format');
      return;
    }

    setImporting(true);
    try {
      const result = await zoneApi.importGeoJson(tenantCode, pasteContent);
      onImportSuccess?.(result);
      setShowImportModal(false);
      setPasteContent('');
    } catch (err) {
      onError?.(`Import failed: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setImporting(false);
    }
  };

  // Handle export
  const handleExport = async () => {
    setExporting(true);
    try {
      await zoneApi.downloadGeoJson(tenantCode);
      onExportSuccess?.();
    } catch (err) {
      onError?.(`Export failed: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setExporting(false);
    }
  };

  // Handle geojson.io open
  const handleOpenGeoJsonIo = async () => {
    try {
      const geoJson = await zoneApi.exportGeoJson(tenantCode);
      // Encode and open in geojson.io
      const encoded = encodeURIComponent(JSON.stringify(geoJson));
      window.open(`https://geojson.io/#data=data:application/json,${encoded}`, '_blank');
    } catch (err) {
      onError?.(`Failed to open in geojson.io: ${err instanceof Error ? err.message : 'Unknown error'}`);
    }
  };

  return (
    <>
      {/* Action buttons */}
      <div className="flex gap-2">
        <button
          onClick={() => setShowImportModal(true)}
          className="px-3 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors flex items-center gap-1"
          disabled={importing}
        >
          📥 Import
        </button>
        <button
          onClick={handleExport}
          className="px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-1"
          disabled={exporting}
        >
          {exporting ? '⏳' : '📤'} Export
        </button>
        <button
          onClick={handleOpenGeoJsonIo}
          className="px-3 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors flex items-center gap-1"
          title="Open in geojson.io"
        >
          🌐 geojson.io
        </button>
      </div>

      {/* Import Modal */}
      {showImportModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-lg mx-4">
            {/* Modal Header */}
            <div className="flex justify-between items-center p-4 border-b border-gray-200">
              <h3 className="text-lg font-semibold">Import GeoJSON</h3>
              <button
                onClick={() => setShowImportModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                ✕
              </button>
            </div>

            {/* Modal Content */}
            <div className="p-4">
              {/* Import mode tabs */}
              <div className="flex gap-2 mb-4">
                <button
                  onClick={() => setImportMode('file')}
                  className={`flex-1 py-2 px-4 rounded-lg transition-colors ${
                    importMode === 'file'
                      ? 'bg-blue-100 text-blue-700 border-2 border-blue-500'
                      : 'bg-gray-100 text-gray-700 border-2 border-transparent'
                  }`}
                >
                  📁 Upload File
                </button>
                <button
                  onClick={() => setImportMode('paste')}
                  className={`flex-1 py-2 px-4 rounded-lg transition-colors ${
                    importMode === 'paste'
                      ? 'bg-blue-100 text-blue-700 border-2 border-blue-500'
                      : 'bg-gray-100 text-gray-700 border-2 border-transparent'
                  }`}
                >
                  📋 Paste JSON
                </button>
              </div>

              {/* File upload */}
              {importMode === 'file' && (
                <div>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".geojson,.json"
                    onChange={handleFileSelect}
                    className="hidden"
                    title="Select GeoJSON file"
                    aria-label="Select GeoJSON file to import"
                  />
                  <div
                    onClick={() => fileInputRef.current?.click()}
                    className="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center cursor-pointer hover:border-blue-500 hover:bg-blue-50 transition-colors"
                  >
                    <div className="text-4xl mb-2">📄</div>
                    <p className="text-gray-600 mb-1">Click to select a GeoJSON file</p>
                    <p className="text-sm text-gray-400">or drag and drop here</p>
                  </div>
                  <p className="text-xs text-gray-500 mt-2">
                    Supported formats: .geojson, .json (FeatureCollection or Feature)
                  </p>
                </div>
              )}

              {/* Paste mode */}
              {importMode === 'paste' && (
                <div>
                  <textarea
                    value={pasteContent}
                    onChange={(e) => setPasteContent(e.target.value)}
                    placeholder='Paste GeoJSON content here...&#10;&#10;{&#10;  "type": "FeatureCollection",&#10;  "features": [...]&#10;}'
                    className="w-full h-48 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-sm"
                  />
                  <div className="flex justify-end mt-2">
                    <button
                      onClick={handlePasteImport}
                      className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50"
                      disabled={importing || !pasteContent.trim()}
                    >
                      {importing ? 'Importing...' : 'Import'}
                    </button>
                  </div>
                </div>
              )}

              {/* Help text */}
              <div className="mt-4 p-3 bg-gray-50 rounded-lg text-sm text-gray-600">
                <p className="font-medium mb-1">Supported GeoJSON structure:</p>
                <ul className="list-disc list-inside text-xs space-y-1">
                  <li>FeatureCollection with Polygon features</li>
                  <li>Single Feature with Polygon geometry</li>
                  <li>Properties: name, code, type, color, restricted, description</li>
                </ul>
                <p className="mt-2">
                  <a
                    href="https://geojson.io"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:underline"
                  >
                    Create zones at geojson.io →
                  </a>
                </p>
              </div>
            </div>

            {/* Modal Footer */}
            <div className="flex justify-end gap-2 p-4 border-t border-gray-200">
              <button
                onClick={() => setShowImportModal(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default GeoJsonImportExport;
