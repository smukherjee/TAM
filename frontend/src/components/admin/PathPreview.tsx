/**
 * PathPreview Component
 * Animates vehicle movement along a path for preview purposes.
 * Implements FR-036: Path preview before saving.
 */

import React, { useEffect, useRef, useState, useCallback } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { Waypoint, calculateTotalDistance, estimateDuration } from '../../services/pathApi';

export interface PathPreviewProps {
  /** Waypoints defining the path */
  waypoints: Waypoint[];
  /** Vehicle type for speed calculation */
  vehicleType: string;
  /** Whether the path loops */
  loop?: boolean;
  /** Playback speed multiplier */
  speed?: number;
  /** Auto-start animation */
  autoPlay?: boolean;
  /** Callback when animation completes */
  onComplete?: () => void;
  /** Map height */
  height?: string;
}

// Vehicle icons by type
const VEHICLE_ICONS: Record<string, string> = {
  FUEL: '⛽',
  CATERING: '🍽️',
  BAGGAGE_TUG: '🚜',
  BAGGAGE_CART: '📦',
  BELT_LOADER: '📤',
  GPU: '⚡',
  PUSHBACK: '🚗',
  STAIRS: '🪜',
  WATER: '💧',
  LAVATORY: '🚽',
  DEICING: '❄️',
  ASU: '🌬️',
  BUS: '🚌',
  CARGO: '📦',
  AMBULIFT: '🚑',
};

const PathPreview: React.FC<PathPreviewProps> = ({
  waypoints,
  vehicleType,
  loop = false,
  speed = 1,
  autoPlay = false,
  onComplete,
  height = '400px',
}) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const vehicleMarkerRef = useRef<L.Marker | null>(null);
  const animationRef = useRef<number | null>(null);
  const trailLayerRef = useRef<L.Polyline | null>(null);

  const [isPlaying, setIsPlaying] = useState(autoPlay);
  const [progress, setProgress] = useState(0);
  const [currentSpeed, setCurrentSpeed] = useState(speed);
  const [pathStats, setPathStats] = useState<{ distanceKm: number; durationSec: number }>({
    distanceKm: 0,
    durationSec: 0,
  });

  // Initialize map
  useEffect(() => {
    if (!mapContainerRef.current || mapRef.current) return;

    // Calculate center from waypoints
    const centerLat = waypoints.reduce((sum, w) => sum + w.lat, 0) / Math.max(waypoints.length, 1);
    const centerLng = waypoints.reduce((sum, w) => sum + w.lng, 0) / Math.max(waypoints.length, 1);

    const map = L.map(mapContainerRef.current, {
      center: [centerLat || 0, centerLng || 0],
      zoom: 16,
      zoomControl: true,
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map);

    mapRef.current = map;

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
      map.remove();
      mapRef.current = null;
    };
  }, []);

  // Draw path and calculate stats
  useEffect(() => {
    if (!mapRef.current || waypoints.length < 2) return;

    // Clear existing layers
    if (trailLayerRef.current) {
      mapRef.current.removeLayer(trailLayerRef.current);
    }
    if (vehicleMarkerRef.current) {
      mapRef.current.removeLayer(vehicleMarkerRef.current);
    }

    // Draw full path
    const latLngs: L.LatLngExpression[] = waypoints.map((w) => [w.lat, w.lng]);
    trailLayerRef.current = L.polyline(latLngs, {
      color: '#94a3b8',
      weight: 3,
      opacity: 0.5,
      dashArray: '5,10',
    }).addTo(mapRef.current);

    // Add start/end markers
    L.circleMarker([waypoints[0].lat, waypoints[0].lng], {
      radius: 8,
      fillColor: '#22c55e',
      color: '#fff',
      weight: 2,
      fillOpacity: 0.9,
    }).addTo(mapRef.current).bindTooltip('Start', { permanent: true, direction: 'top' });

    if (!loop) {
      L.circleMarker([waypoints[waypoints.length - 1].lat, waypoints[waypoints.length - 1].lng], {
        radius: 8,
        fillColor: '#ef4444',
        color: '#fff',
        weight: 2,
        fillOpacity: 0.9,
      }).addTo(mapRef.current).bindTooltip('End', { permanent: true, direction: 'top' });
    }

    // Create vehicle marker
    const icon = L.divIcon({
      html: `<div class="vehicle-icon" style="font-size: 24px; filter: drop-shadow(2px 2px 2px rgba(0,0,0,0.5));">${VEHICLE_ICONS[vehicleType] || '🚗'}</div>`,
      className: 'custom-vehicle-icon',
      iconSize: [32, 32],
      iconAnchor: [16, 16],
    });

    vehicleMarkerRef.current = L.marker([waypoints[0].lat, waypoints[0].lng], { icon }).addTo(
      mapRef.current
    );

    // Fit bounds
    mapRef.current.fitBounds(L.latLngBounds(latLngs), { padding: [50, 50] });

    // Calculate stats
    const distanceKm = calculateTotalDistance(waypoints);
    const durationSec = estimateDuration(distanceKm, vehicleType);
    setPathStats({ distanceKm, durationSec });
  }, [waypoints, vehicleType, loop]);

  // Animation loop
  const animate = useCallback(() => {
    if (!isPlaying || waypoints.length < 2) return;

    const startTime = Date.now();
    const totalDuration = (pathStats.durationSec * 1000) / currentSpeed;

    const step = () => {
      if (!isPlaying) return;

      const elapsed = Date.now() - startTime;
      let t = Math.min(elapsed / totalDuration, 1);

      if (loop && t >= 1) {
        t = t % 1;
      }

      setProgress(t * 100);

      // Calculate current position along path
      const position = getPositionAlongPath(waypoints, t);
      if (vehicleMarkerRef.current && position) {
        vehicleMarkerRef.current.setLatLng([position.lat, position.lng]);
      }

      if (t < 1 || loop) {
        animationRef.current = requestAnimationFrame(step);
      } else {
        setIsPlaying(false);
        setProgress(100);
        if (onComplete) {
          onComplete();
        }
      }
    };

    animationRef.current = requestAnimationFrame(step);
  }, [isPlaying, waypoints, pathStats.durationSec, currentSpeed, loop, onComplete]);

  // Start/stop animation
  useEffect(() => {
    if (isPlaying) {
      animate();
    } else if (animationRef.current) {
      cancelAnimationFrame(animationRef.current);
    }

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [isPlaying, animate]);

  // Get position along path at time t (0-1)
  const getPositionAlongPath = (points: Waypoint[], t: number): Waypoint | null => {
    if (points.length < 2) return null;

    const totalDistance = calculateTotalDistance(points);
    const targetDistance = totalDistance * t;

    let accumulated = 0;
    for (let i = 1; i < points.length; i++) {
      const segmentDist = haversineDistance(points[i - 1], points[i]);
      if (accumulated + segmentDist >= targetDistance) {
        const segmentT = (targetDistance - accumulated) / segmentDist;
        return {
          lat: points[i - 1].lat + (points[i].lat - points[i - 1].lat) * segmentT,
          lng: points[i - 1].lng + (points[i].lng - points[i - 1].lng) * segmentT,
        };
      }
      accumulated += segmentDist;
    }

    return points[points.length - 1];
  };

  // Haversine distance between two points
  const haversineDistance = (p1: Waypoint, p2: Waypoint): number => {
    const R = 6371;
    const dLat = toRad(p2.lat - p1.lat);
    const dLng = toRad(p2.lng - p1.lng);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(toRad(p1.lat)) * Math.cos(toRad(p2.lat)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  };

  const toRad = (deg: number): number => (deg * Math.PI) / 180;

  // Control handlers
  const handlePlay = () => {
    if (progress >= 100) {
      setProgress(0);
      if (vehicleMarkerRef.current && waypoints.length > 0) {
        vehicleMarkerRef.current.setLatLng([waypoints[0].lat, waypoints[0].lng]);
      }
    }
    setIsPlaying(true);
  };

  const handlePause = () => {
    setIsPlaying(false);
  };

  const handleReset = () => {
    setIsPlaying(false);
    setProgress(0);
    if (vehicleMarkerRef.current && waypoints.length > 0) {
      vehicleMarkerRef.current.setLatLng([waypoints[0].lat, waypoints[0].lng]);
    }
  };

  const handleSpeedChange = (newSpeed: number) => {
    setCurrentSpeed(newSpeed);
  };

  // Format duration
  const formatDuration = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return mins > 0 ? `${mins}m ${secs}s` : `${secs}s`;
  };

  return (
    <div className="path-preview">
      <div
        ref={mapContainerRef}
        style={{ height, width: '100%' }}
        className="rounded-lg border border-gray-200"
      />

      {/* Controls */}
      <div className="mt-3 p-3 bg-gray-50 rounded-lg">
        <div className="flex items-center gap-3 mb-2">
          {/* Play/Pause */}
          <button
            onClick={isPlaying ? handlePause : handlePlay}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            disabled={waypoints.length < 2}
          >
            {isPlaying ? '⏸ Pause' : '▶ Play'}
          </button>

          {/* Reset */}
          <button
            onClick={handleReset}
            className="px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
            disabled={waypoints.length < 2}
          >
            ⏹ Reset
          </button>

          {/* Speed control */}
          <div className="flex items-center gap-2 ml-4">
            <span className="text-sm text-gray-600">Speed:</span>
            {[0.5, 1, 2, 5].map((s) => (
              <button
                key={s}
                onClick={() => handleSpeedChange(s)}
                className={`px-2 py-1 text-sm rounded ${
                  currentSpeed === s
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
                }`}
              >
                {s}x
              </button>
            ))}
          </div>
        </div>

        {/* Progress bar */}
        <div className="w-full bg-gray-200 rounded-full h-2 mb-2">
          <div
            className="bg-blue-600 h-2 rounded-full transition-all duration-100"
            style={{ width: `${progress}%` }}
          />
        </div>

        {/* Stats */}
        <div className="flex justify-between text-sm text-gray-600">
          <span>
            {VEHICLE_ICONS[vehicleType] || '🚗'} {vehicleType}
          </span>
          <span>Distance: {pathStats.distanceKm.toFixed(2)} km</span>
          <span>Duration: {formatDuration(pathStats.durationSec)}</span>
          <span>Progress: {progress.toFixed(0)}%</span>
        </div>
      </div>
    </div>
  );
};

export default PathPreview;
