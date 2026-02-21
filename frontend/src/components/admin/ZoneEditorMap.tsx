/**
 * ZoneEditorMap Component
 * Interactive map for drawing and editing zone polygons using Leaflet and leaflet-geoman.
 * Implements FR-040 to FR-048: Admin zone management.
 */

import React, { useEffect, useRef, useCallback, useState } from 'react';
import L from 'leaflet';
import '@geoman-io/leaflet-geoman-free';
import '@geoman-io/leaflet-geoman-free/dist/leaflet-geoman.css';
import 'leaflet/dist/leaflet.css';
import '../../types/leaflet-geoman.d';
import { Zone, getZoneColor } from '../../services/zoneApi';

export interface ZoneEditorMapProps {
  /** Tenant code for airport center */
  tenantCode: string;
  /** Existing zones to display */
  zones?: Zone[];
  /** Currently selected zone ID */
  selectedZoneId?: number | null;
  /** Callback when a zone is selected */
  onZoneSelect?: (zone: Zone | null) => void;
  /** Callback when zone polygon is created/updated */
  onZoneChange?: (coordinates: number[][]) => void;
  /** Callback when drawing is complete */
  onDrawComplete?: (coordinates: number[][]) => void;
  /** Whether to enable editing mode */
  editMode?: boolean;
  /** Read-only mode */
  readOnly?: boolean;
  /** Map height */
  height?: string;
}

// Airport center coordinates by tenant
const AIRPORT_CENTERS: Record<string, { lat: number; lng: number; zoom: number }> = {
  VIDP: { lat: 28.5665, lng: 77.1031, zoom: 15 },
  YBBN: { lat: -27.3942, lng: 153.1218, zoom: 15 },
};

const ZoneEditorMap: React.FC<ZoneEditorMapProps> = ({
  tenantCode,
  zones = [],
  selectedZoneId,
  onZoneSelect,
  onZoneChange,
  onDrawComplete,
  editMode = false,
  readOnly = false,
  height = '500px',
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const zoneLayersRef = useRef<Map<number, L.Polygon>>(new Map());
  const newZoneLayerRef = useRef<L.Polygon | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);

  // Initialize map
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    const center = AIRPORT_CENTERS[tenantCode] || AIRPORT_CENTERS.VIDP;

    const map = L.map(mapContainerRef.current, {
      center: [center.lat, center.lng],
      zoom: center.zoom,
      zoomControl: true,
    });

    // Add tile layers
    const darkLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution:
        '&copy; OpenStreetMap contributors &copy; CARTO',
      maxZoom: 19,
    }).addTo(map);

    const satelliteLayer = L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      {
        attribution: 'Tiles © Esri',
        maxZoom: 19,
      }
    );

    // Layer control
    L.control.layers(
      {
        'Dark Map': darkLayer,
        'Satellite': satelliteLayer,
      },
      {},
      { position: 'topright' }
    ).addTo(map);

    mapRef.current = map;

    // Initialize geoman for drawing tools
    if (!readOnly) {
      map.pm.addControls({
        position: 'topleft',
        drawCircle: false,
        drawCircleMarker: false,
        drawPolyline: false,
        drawRectangle: true,
        drawMarker: false,
        drawText: false,
        cutPolygon: true,
        rotateMode: true,
        drawPolygon: true,
        editMode: true,
        dragMode: true,
        removalMode: true,
      });

      map.pm.setGlobalOptions({
        snappable: true,
        snapDistance: 20,
        templineStyle: { color: '#3b82f6', weight: 2 },
        hintlineStyle: { color: '#3b82f6', dashArray: '5,5' },
      });
    }

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, [tenantCode, readOnly]);

  // Draw existing zones
  useEffect(() => {
    if (!mapRef.current) return;

    const map = mapRef.current;

    // Clear existing zone layers
    zoneLayersRef.current.forEach((layer) => {
      map.removeLayer(layer);
    });
    zoneLayersRef.current.clear();

    // Draw each zone
    zones.forEach((zone) => {
      if (!zone.coordinates || zone.coordinates.length < 3) return;

      const latLngs: L.LatLngExpression[] = zone.coordinates.map(([lng, lat]) => [lat, lng]);

      const color = zone.color || getZoneColor(zone.type);
      const isSelected = zone.id === selectedZoneId;

      const polygon = L.polygon(latLngs, {
        color: isSelected ? '#000000' : color,
        weight: isSelected ? 3 : 2,
        fillColor: color,
        fillOpacity: zone.opacity ?? 0.3,
        dashArray: zone.restricted ? '10,5' : undefined,
      }).addTo(map);

      // Add tooltip
      polygon.bindTooltip(
        `<div class="font-medium">${zone.name}</div>
         <div class="text-xs">${zone.type}${zone.restricted ? ' (Restricted)' : ''}</div>`,
        {
          permanent: false,
          direction: 'center',
        }
      );

      // Add click handler
      polygon.on('click', () => {
        if (!isDrawing && onZoneSelect) {
          onZoneSelect(zone);
        }
      });

      if (zone.id) {
        zoneLayersRef.current.set(zone.id, polygon);
      }
    });
  }, [zones, selectedZoneId, isDrawing, onZoneSelect]);

  // Enable/disable edit mode for selected zone
  useEffect(() => {
    if (!mapRef.current || readOnly) return;

    // Disable editing on all zones first
    zoneLayersRef.current.forEach((layer) => {
      layer.pm.disable();
    });

    // Enable editing on selected zone if in edit mode
    if (editMode && selectedZoneId) {
      const selectedLayer = zoneLayersRef.current.get(selectedZoneId);
      if (selectedLayer) {
        selectedLayer.pm.enable({
          allowSelfIntersection: false,
        });

        // Listen for edit events
        selectedLayer.on('pm:edit', () => {
          const newLatLngs = (selectedLayer.getLatLngs()[0] as L.LatLng[]);
          const newCoords = newLatLngs.map((ll) => [ll.lng, ll.lat]);
          if (onZoneChange) {
            onZoneChange(newCoords);
          }
        });
      }
    }
  }, [editMode, selectedZoneId, readOnly, onZoneChange]);

  // Extract coordinates from polygon
  const extractCoordinates = useCallback((layer: L.Polygon): number[][] => {
    const latLngs = layer.getLatLngs()[0] as L.LatLng[];
    return latLngs.map((ll) => [ll.lng, ll.lat]);
  }, []);

  // Handle geoman events
  useEffect(() => {
    if (!mapRef.current || readOnly) return;

    const map = mapRef.current;

    // Drawing started
    map.on('pm:drawstart', () => {
      setIsDrawing(true);
      // Remove any existing new zone layer
      if (newZoneLayerRef.current) {
        map.removeLayer(newZoneLayerRef.current);
        newZoneLayerRef.current = null;
      }
    });

    // Drawing complete
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    map.on('pm:create', (e: any) => {
      setIsDrawing(false);
      if (e.layer instanceof L.Polygon) {
        // Remove previous new zone layer
        if (newZoneLayerRef.current) {
          map.removeLayer(newZoneLayerRef.current);
        }
        newZoneLayerRef.current = e.layer;

        const coordinates = extractCoordinates(e.layer);

        if (onDrawComplete) {
          onDrawComplete(coordinates);
        }
        if (onZoneChange) {
          onZoneChange(coordinates);
        }
      }
    });

    // Layer edited
    map.on('pm:edit', () => {
      if (newZoneLayerRef.current) {
        const coordinates = extractCoordinates(newZoneLayerRef.current);
        if (onZoneChange) {
          onZoneChange(coordinates);
        }
      }
    });

    // Layer removed
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    map.on('pm:remove', (e: any) => {
      if (e.layer === newZoneLayerRef.current) {
        newZoneLayerRef.current = null;
        if (onZoneChange) {
          onZoneChange([]);
        }
      }
    });

    return () => {
      map.off('pm:drawstart');
      map.off('pm:create');
      map.off('pm:edit');
      map.off('pm:remove');
    };
  }, [readOnly, extractCoordinates, onZoneChange, onDrawComplete]);

  // Clear new zone drawing
  const clearNewZone = useCallback(() => {
    if (mapRef.current && newZoneLayerRef.current) {
      mapRef.current.removeLayer(newZoneLayerRef.current);
      newZoneLayerRef.current = null;
      if (onZoneChange) {
        onZoneChange([]);
      }
    }
  }, [onZoneChange]);

  // Fit map to show a specific zone
  const focusOnZone = useCallback((zoneId: number) => {
    const layer = zoneLayersRef.current.get(zoneId);
    if (layer && mapRef.current) {
      mapRef.current.fitBounds(layer.getBounds(), { padding: [50, 50] });
    }
  }, []);

  // Fit map to show all zones
  const fitToAllZones = useCallback(() => {
    if (!mapRef.current || zoneLayersRef.current.size === 0) return;

    const bounds = L.latLngBounds([]);
    zoneLayersRef.current.forEach((layer) => {
      bounds.extend(layer.getBounds());
    });

    if (bounds.isValid()) {
      mapRef.current.fitBounds(bounds, { padding: [50, 50] });
    }
  }, []);

  return (
    <div className="zone-editor-map">
      <div
        ref={mapContainerRef}
        style={{ height, width: '100%' }}
        className="rounded-lg border border-gray-700 bg-gray-900"
      />

      {!readOnly && (
        <div className="mt-2 flex items-center justify-between text-sm text-gray-400">
          <div className="flex items-center gap-4">
            <span className="inline-flex items-center gap-1">
              <span className="w-3 h-3 bg-blue-500 rounded" />
              Operational
            </span>
            <span className="inline-flex items-center gap-1">
              <span className="w-3 h-3 bg-red-500 rounded border-dashed border-2 border-red-700" />
              Restricted
            </span>
            <span className="inline-flex items-center gap-1">
              <span className="w-3 h-3 bg-green-500 rounded" />
              Parking
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span>Zones: {zones.length}</span>
            {isDrawing && (
              <span className="text-blue-400 font-medium">Drawing...</span>
            )}
          </div>
        </div>
      )}

      {/* Methods exposed for parent component */}
      <input type="hidden" data-clear-new-zone={clearNewZone.toString()} />
      <input type="hidden" data-focus-on-zone={focusOnZone.toString()} />
      <input type="hidden" data-fit-to-all={fitToAllZones.toString()} />
    </div>
  );
};

export default ZoneEditorMap;
