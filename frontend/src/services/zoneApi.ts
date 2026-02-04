/**
 * Zone API Service
 * Frontend service for zone CRUD operations and GeoJSON import/export.
 * Implements FR-040 to FR-048: Admin zone management.
 */

import { apiClient } from './apiClient';

export interface ZoneCoordinate {
  lng: number;
  lat: number;
}

export interface Zone {
  id?: number;
  tenantCode: string;
  name: string;
  code: string;
  type: string;
  description?: string;
  coordinates: number[][]; // [[lng, lat], [lng, lat], ...]
  color?: string;
  opacity?: number;
  restricted?: boolean;
  active?: boolean;
  allowedVehicleTypes?: string[];
  allowedRoles?: string[];
  accessSchedule?: string;
  alertOnEntry?: boolean;
  alertOnExit?: boolean;
  dwellTimeAlertMinutes?: number;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface ZoneType {
  code: string;
  name: string;
  color: string;
}

export interface GeoJsonFeature {
  type: 'Feature';
  properties: Record<string, unknown>;
  geometry: {
    type: 'Polygon';
    coordinates: number[][][];
  };
}

export interface GeoJsonFeatureCollection {
  type: 'FeatureCollection';
  features: GeoJsonFeature[];
}

export interface ImportResult {
  success: boolean;
  imported: number;
  zones: Zone[];
}

export interface PointCheckResult {
  point: { lng: number; lat: number };
  inRestrictedZone: boolean;
  restrictedZones: Zone[];
}

// Note: apiClient uses /api as baseURL, so paths should NOT start with /api
const BASE_URL = '/admin/zones';

/**
 * Get all zones for a tenant.
 */
export async function getZones(
  tenantCode: string,
  options: { activeOnly?: boolean; type?: string } = {}
): Promise<Zone[]> {
  const params = new URLSearchParams({ tenantCode });
  if (options.activeOnly) {
    params.append('activeOnly', 'true');
  }
  if (options.type) {
    params.append('type', options.type);
  }
  const response = await apiClient.get<Zone[]>(`${BASE_URL}?${params.toString()}`);
  return response;
}

/**
 * Get a single zone by ID.
 */
export async function getZone(id: number): Promise<Zone> {
  const response = await apiClient.get<Zone>(`${BASE_URL}/${id}`);
  return response;
}

/**
 * Get zone by code.
 */
export async function getZoneByCode(tenantCode: string, code: string): Promise<Zone> {
  const params = new URLSearchParams({ tenantCode, code });
  const response = await apiClient.get<Zone>(`${BASE_URL}/by-code?${params.toString()}`);
  return response;
}

/**
 * Create a new zone.
 */
export async function createZone(zone: Omit<Zone, 'id' | 'createdAt' | 'updatedAt'>): Promise<Zone> {
  const response = await apiClient.post<Zone>(BASE_URL, zone);
  return response;
}

/**
 * Update an existing zone.
 */
export async function updateZone(id: number, zone: Partial<Zone>): Promise<Zone> {
  const response = await apiClient.put<Zone>(`${BASE_URL}/${id}`, zone);
  return response;
}

/**
 * Delete a zone.
 */
export async function deleteZone(id: number): Promise<void> {
  await apiClient.delete(`${BASE_URL}/${id}`);
}

/**
 * Activate a zone.
 */
export async function activateZone(id: number): Promise<Zone> {
  const response = await apiClient.post<Zone>(`${BASE_URL}/${id}/activate`);
  return response;
}

/**
 * Deactivate a zone.
 */
export async function deactivateZone(id: number): Promise<Zone> {
  const response = await apiClient.post<Zone>(`${BASE_URL}/${id}/deactivate`);
  return response;
}

/**
 * Export zones as GeoJSON.
 */
export async function exportGeoJson(tenantCode: string): Promise<GeoJsonFeatureCollection> {
  const params = new URLSearchParams({ tenantCode });
  const response = await apiClient.get<GeoJsonFeatureCollection>(
    `${BASE_URL}/geojson?${params.toString()}`
  );
  return response;
}

/**
 * Download zones as GeoJSON file.
 */
export async function downloadGeoJson(tenantCode: string): Promise<void> {
  const params = new URLSearchParams({ tenantCode });
  const response = await apiClient.get<Blob>(`${BASE_URL}/export?${params.toString()}`, {
    responseType: 'blob',
  });

  // Create download link
  const url = window.URL.createObjectURL(new Blob([response]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', `${tenantCode}_zones.geojson`);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Import zones from GeoJSON file.
 */
export async function importGeoJsonFile(tenantCode: string, file: File): Promise<ImportResult> {
  const formData = new FormData();
  formData.append('file', file);

  const params = new URLSearchParams({ tenantCode });
  const response = await apiClient.post<ImportResult>(
    `${BASE_URL}/import?${params.toString()}`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    }
  );
  return response;
}

/**
 * Import zones from GeoJSON string.
 */
export async function importGeoJson(
  tenantCode: string,
  geoJson: string | GeoJsonFeatureCollection
): Promise<ImportResult> {
  const params = new URLSearchParams({ tenantCode });
  const body = typeof geoJson === 'string' ? geoJson : JSON.stringify(geoJson);

  const response = await apiClient.post<ImportResult>(
    `${BASE_URL}/import/json?${params.toString()}`,
    body,
    {
      headers: {
        'Content-Type': 'application/json',
      },
    }
  );
  return response;
}

/**
 * Check if a point is within any restricted zone.
 */
export async function checkPointInZones(
  tenantCode: string,
  lng: number,
  lat: number
): Promise<PointCheckResult> {
  const params = new URLSearchParams({
    tenantCode,
    lng: lng.toString(),
    lat: lat.toString(),
  });
  const response = await apiClient.get<PointCheckResult>(
    `${BASE_URL}/check-point?${params.toString()}`
  );
  return response;
}

/**
 * Get available zone types.
 */
export async function getZoneTypes(): Promise<ZoneType[]> {
  const response = await apiClient.get<ZoneType[]>(`${BASE_URL}/types`);
  return response;
}

/**
 * Convert Zone coordinates to Leaflet LatLng format.
 */
export function coordinatesToLatLng(coordinates: number[][]): [number, number][] {
  return coordinates.map(([lng, lat]) => [lat, lng]);
}

/**
 * Convert Leaflet LatLng to Zone coordinates format.
 */
export function latLngToCoordinates(latLngs: [number, number][]): number[][] {
  return latLngs.map(([lat, lng]) => [lng, lat]);
}

/**
 * Calculate approximate area of polygon in square meters.
 */
export function calculatePolygonArea(coordinates: number[][]): number {
  if (coordinates.length < 3) return 0;

  let area = 0;
  const n = coordinates.length;

  for (let i = 0; i < n; i++) {
    const j = (i + 1) % n;
    const [lng1, lat1] = coordinates[i];
    const [lng2, lat2] = coordinates[j];

    // Approximate conversion at this latitude
    const avgLat = ((lat1 + lat2) / 2) * (Math.PI / 180);
    const metersPerDegreeLat = 111320;
    const metersPerDegreeLng = 111320 * Math.cos(avgLat);

    const x1 = lng1 * metersPerDegreeLng;
    const y1 = lat1 * metersPerDegreeLat;
    const x2 = lng2 * metersPerDegreeLng;
    const y2 = lat2 * metersPerDegreeLat;

    area += x1 * y2 - x2 * y1;
  }

  return Math.abs(area) / 2;
}

/**
 * Check if a point is inside a polygon.
 */
export function isPointInPolygon(
  point: { lng: number; lat: number },
  coordinates: number[][]
): boolean {
  if (coordinates.length < 3) return false;

  let inside = false;
  const n = coordinates.length;

  for (let i = 0, j = n - 1; i < n; j = i++) {
    const [xi, yi] = coordinates[i];
    const [xj, yj] = coordinates[j];

    if (
      yi > point.lat !== yj > point.lat &&
      point.lng < ((xj - xi) * (point.lat - yi)) / (yj - yj) + xi
    ) {
      inside = !inside;
    }
  }

  return inside;
}

/**
 * Default colors for zone types.
 */
export const ZONE_TYPE_COLORS: Record<string, string> = {
  RESTRICTED: '#ef4444',
  OPERATIONAL: '#3b82f6',
  PARKING: '#22c55e',
  TAXIWAY: '#f59e0b',
  RUNWAY: '#8b5cf6',
  TERMINAL: '#06b6d4',
  CARGO: '#84cc16',
  MAINTENANCE: '#f97316',
  SECURITY: '#dc2626',
  CUSTOM: '#6b7280',
};

/**
 * Get color for zone type.
 */
export function getZoneColor(type: string): string {
  return ZONE_TYPE_COLORS[type] || ZONE_TYPE_COLORS.CUSTOM;
}

export const zoneApi = {
  getZones,
  getZone,
  getZoneByCode,
  createZone,
  updateZone,
  deleteZone,
  activateZone,
  deactivateZone,
  exportGeoJson,
  downloadGeoJson,
  importGeoJsonFile,
  importGeoJson,
  checkPointInZones,
  getZoneTypes,
  coordinatesToLatLng,
  latLngToCoordinates,
  calculatePolygonArea,
  isPointInPolygon,
  getZoneColor,
  ZONE_TYPE_COLORS,
};

export default zoneApi;
