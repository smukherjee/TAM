/**
 * PathDrawingMap Component
 * Interactive map for drawing vehicle movement paths using Leaflet and leaflet-geoman.
 * Implements FR-032 to FR-039: Admin path drawing interface.
 */

import React, { useEffect, useRef, useCallback, useState } from 'react';
import L from 'leaflet';
import '@geoman-io/leaflet-geoman-free';
import '@geoman-io/leaflet-geoman-free/dist/leaflet-geoman.css';
import 'leaflet/dist/leaflet.css';
import '../../types/leaflet-geoman.d';
import { Waypoint } from '../../services/pathApi';

export interface PathDrawingMapProps {
  /** Tenant code for airport center */
  tenantCode: string;
  /** Initial waypoints to display */
  initialWaypoints?: Waypoint[];
  /** Airport boundary polygon for reference */
  boundary?: { lat: number; lng: number }[];
  /** Callback when path is updated */
  onPathChange?: (waypoints: Waypoint[]) => void;
  /** Callback when drawing is complete */
  onDrawComplete?: (waypoints: Waypoint[]) => void;
  /** Callback on validation error */
  onValidationError?: (errors: string[]) => void;
  /** Whether to show validation warnings */
  showValidation?: boolean;
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

const PathDrawingMap: React.FC<PathDrawingMapProps> = ({
  tenantCode,
  initialWaypoints = [],
  boundary,
  onPathChange,
  onDrawComplete,
  onValidationError,
  showValidation = true,
  readOnly = false,
  height = '500px',
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const drawnLayerRef = useRef<L.Polyline | null>(null);
  const boundaryLayerRef = useRef<L.Polygon | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [currentWaypoints, setCurrentWaypoints] = useState<Waypoint[]>(initialWaypoints);

  // Initialize map
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    const center = AIRPORT_CENTERS[tenantCode] || AIRPORT_CENTERS.VIDP;

    const map = L.map(mapContainerRef.current, {
      center: [center.lat, center.lng],
      zoom: center.zoom,
      zoomControl: true,
    });

    // Add tile layer
    const darkLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution:
        '&copy; OpenStreetMap contributors &copy; CARTO',
      maxZoom: 19,
    }).addTo(map);

    // Add satellite layer option
    const satellite = L.tileLayer(
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
        'Satellite': satellite,
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
        drawPolygon: false,
        drawRectangle: false,
        drawMarker: false,
        drawText: false,
        cutPolygon: false,
        rotateMode: false,
        drawPolyline: true,
        editMode: true,
        dragMode: true,
        removalMode: true,
      });

      // Custom button styles
      map.pm.setGlobalOptions({
        snappable: true,
        snapDistance: 20,
        templineStyle: { color: '#3b82f6', weight: 3 },
        hintlineStyle: { color: '#3b82f6', dashArray: '5,5' },
      });
    }

    return () => {
      map.remove();
      mapRef.current = null;
    };
  }, [tenantCode, readOnly]);

  // Draw boundary polygon
  useEffect(() => {
    if (!mapRef.current || !boundary || boundary.length < 3) return;

    // Remove existing boundary
    if (boundaryLayerRef.current) {
      mapRef.current.removeLayer(boundaryLayerRef.current);
    }

    // Draw new boundary
    const latLngs: L.LatLngExpression[] = boundary.map((p) => [p.lat, p.lng]);
    boundaryLayerRef.current = L.polygon(latLngs, {
      color: '#ef4444',
      weight: 2,
      fill: false,
      dashArray: '10,5',
      opacity: 0.7,
    }).addTo(mapRef.current);

  }, [boundary]);

  // Draw initial waypoints
  useEffect(() => {
    if (!mapRef.current || initialWaypoints.length === 0) return;

    // Remove existing path
    if (drawnLayerRef.current) {
      mapRef.current.removeLayer(drawnLayerRef.current);
    }

    // Draw path
    const latLngs: L.LatLngExpression[] = initialWaypoints.map((w) => [w.lat, w.lng]);
    drawnLayerRef.current = L.polyline(latLngs, {
      color: '#3b82f6',
      weight: 4,
      opacity: 0.8,
    }).addTo(mapRef.current);

    // Add waypoint markers
    initialWaypoints.forEach((waypoint, index) => {
      const isStart = index === 0;
      const isEnd = index === initialWaypoints.length - 1;

      const marker = L.circleMarker([waypoint.lat, waypoint.lng], {
        radius: isStart || isEnd ? 8 : 5,
        fillColor: isStart ? '#22c55e' : isEnd ? '#ef4444' : '#3b82f6',
        color: '#ffffff',
        weight: 2,
        fillOpacity: 0.9,
      }).addTo(mapRef.current!);

      marker.bindTooltip(`Point ${index + 1}${isStart ? ' (Start)' : isEnd ? ' (End)' : ''}`, {
        permanent: false,
        direction: 'top',
      });
    });

    // Fit bounds to path
    if (latLngs.length > 0) {
      mapRef.current.fitBounds(L.latLngBounds(latLngs), { padding: [50, 50] });
    }

    setCurrentWaypoints(initialWaypoints);
  }, [initialWaypoints]);

  // Extract waypoints from polyline
  const extractWaypoints = useCallback((layer: L.Polyline): Waypoint[] => {
    const latLngs = layer.getLatLngs() as L.LatLng[];
    return latLngs.map((ll, index) => ({
      lat: ll.lat,
      lng: ll.lng,
      timestamp: index * 1000, // 1 second between points initially
    }));
  }, []);

  // Validate waypoints against boundary
  const validateWaypoints = useCallback(
    (waypoints: Waypoint[]): string[] => {
      if (!boundary || boundary.length < 3 || !showValidation) return [];

      const errors: string[] = [];
      const boundaryPolygon = L.polygon(boundary.map((p) => [p.lat, p.lng]));

      waypoints.forEach((waypoint, index) => {
        const point = L.latLng(waypoint.lat, waypoint.lng);
        if (!isPointInPolygon(point, boundaryPolygon)) {
          errors.push(`Point ${index + 1} is outside airport boundary`);
        }
      });

      return errors;
    },
    [boundary, showValidation]
  );

  // Check if point is inside polygon
  const isPointInPolygon = (point: L.LatLng, polygon: L.Polygon): boolean => {
    const bounds = polygon.getBounds();
    if (!bounds.contains(point)) return false;

    // Ray casting algorithm
    const polyPoints = (polygon.getLatLngs()[0] as L.LatLng[]);
    let inside = false;
    for (let i = 0, j = polyPoints.length - 1; i < polyPoints.length; j = i++) {
      const xi = polyPoints[i].lat, yi = polyPoints[i].lng;
      const xj = polyPoints[j].lat, yj = polyPoints[j].lng;

      if (
        yi > point.lng !== yj > point.lng &&
        point.lat < ((xj - xi) * (point.lng - yi)) / (yj - yi) + xi
      ) {
        inside = !inside;
      }
    }
    return inside;
  };

  // Handle geoman events
  useEffect(() => {
    if (!mapRef.current || readOnly) return;

    const map = mapRef.current;

    // Drawing started
    map.on('pm:drawstart', () => {
      setIsDrawing(true);
      // Remove existing path when starting new draw
      if (drawnLayerRef.current) {
        map.removeLayer(drawnLayerRef.current);
        drawnLayerRef.current = null;
      }
    });

    // Drawing complete
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    map.on('pm:create', (e: any) => {
      setIsDrawing(false);
      if (e.layer instanceof L.Polyline) {
        drawnLayerRef.current = e.layer;
        const waypoints = extractWaypoints(e.layer);
        setCurrentWaypoints(waypoints);

        const errors = validateWaypoints(waypoints);
        if (errors.length > 0 && onValidationError) {
          onValidationError(errors);
        }

        if (onDrawComplete) {
          onDrawComplete(waypoints);
        }
        if (onPathChange) {
          onPathChange(waypoints);
        }
      }
    });

    // Path edited
    map.on('pm:edit', () => {
      if (drawnLayerRef.current) {
        const waypoints = extractWaypoints(drawnLayerRef.current);
        setCurrentWaypoints(waypoints);

        const errors = validateWaypoints(waypoints);
        if (errors.length > 0 && onValidationError) {
          onValidationError(errors);
        }

        if (onPathChange) {
          onPathChange(waypoints);
        }
      }
    });

    // Layer removed
    map.on('pm:remove', () => {
      drawnLayerRef.current = null;
      setCurrentWaypoints([]);
      if (onPathChange) {
        onPathChange([]);
      }
    });

    return () => {
      map.off('pm:drawstart');
      map.off('pm:create');
      map.off('pm:edit');
      map.off('pm:remove');
    };
  }, [readOnly, extractWaypoints, validateWaypoints, onPathChange, onDrawComplete, onValidationError]);

  // Clear the current path
  const clearPath = useCallback(() => {
    if (mapRef.current && drawnLayerRef.current) {
      mapRef.current.removeLayer(drawnLayerRef.current);
      drawnLayerRef.current = null;
      setCurrentWaypoints([]);
      if (onPathChange) {
        onPathChange([]);
      }
    }
  }, [onPathChange]);

  // Start draw mode programmatically
  const startDrawing = useCallback(() => {
    if (mapRef.current && !readOnly) {
      mapRef.current.pm.enableDraw('Line');
    }
  }, [readOnly]);

  // Stop draw mode
  const stopDrawing = useCallback(() => {
    if (mapRef.current) {
      mapRef.current.pm.disableDraw();
      setIsDrawing(false);
    }
  }, []);

  return (
    <div className="path-drawing-map">
      <div
        ref={mapContainerRef}
        style={{ height, width: '100%' }}
        className="rounded-lg border border-gray-700 bg-gray-900"
      />
      
      {!readOnly && (
        <div className="mt-2 flex items-center gap-2 text-sm text-gray-400">
          <span className="inline-flex items-center gap-1">
            <span className="w-3 h-3 rounded-full bg-green-500" />
            Start
          </span>
          <span className="inline-flex items-center gap-1">
            <span className="w-3 h-3 rounded-full bg-blue-500" />
            Waypoint
          </span>
          <span className="inline-flex items-center gap-1">
            <span className="w-3 h-3 rounded-full bg-red-500" />
            End
          </span>
          <span className="ml-auto">
            Points: {currentWaypoints.length}
          </span>
          {isDrawing && (
            <span className="text-blue-400 font-medium">
              Drawing...
            </span>
          )}
        </div>
      )}

      {/* Expose methods via ref would be done in a forwardRef version */}
      <input type="hidden" data-clear-path={clearPath.toString()} />
      <input type="hidden" data-start-drawing={startDrawing.toString()} />
      <input type="hidden" data-stop-drawing={stopDrawing.toString()} />
    </div>
  );
};

export default PathDrawingMap;
