/**
 * Path API Service
 * Frontend service for vehicle path CRUD operations.
 * Implements FR-032 to FR-039: Admin path management.
 */

import { apiClient } from './apiClient';

export interface Waypoint {
  lat: number;
  lng: number;
  timestamp?: number;
  speed?: number;
  heading?: number;
}

export interface PathSchedule {
  startTime: string; // HH:mm format
  endTime: string;
  daysOfWeek: number[]; // 1-7 (Monday-Sunday)
  repeatIntervalMinutes?: number;
}

export interface VehiclePath {
  id?: number;
  tenantCode: string;
  name: string;
  description?: string;
  vehicleType?: string;      // Frontend naming
  vehicleTypeCode?: string;  // Backend naming (JPA entity uses this)
  waypoints: Waypoint[] | string;  // Can be parsed array or JSON string from backend
  schedule?: PathSchedule | string;
  loop: boolean;
  active: boolean;
  totalDistanceKm?: number;
  estimatedDurationSeconds?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PathPreview {
  waypoints: Waypoint[];
  totalDistanceKm: number;
  estimatedDurationSeconds: number;
  segmentDistances: number[];
  segmentDurations: number[];
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

// Note: apiClient uses /api as baseURL, so paths should NOT start with /api
const BASE_URL = '/admin/paths';

/**
 * Get all paths for a tenant.
 */
export async function getPaths(tenantCode: string, activeOnly = false): Promise<VehiclePath[]> {
  const params = new URLSearchParams({ tenantCode });
  if (activeOnly) {
    params.append('activeOnly', 'true');
  }
  const response = await apiClient.get<VehiclePath[]>(`${BASE_URL}?${params.toString()}`);
  return response;
}

/**
 * Get paths filtered by vehicle type.
 */
export async function getPathsByVehicleType(
  tenantCode: string,
  vehicleType: string
): Promise<VehiclePath[]> {
  const params = new URLSearchParams({ tenantCode, vehicleType });
  const response = await apiClient.get<VehiclePath[]>(`${BASE_URL}/by-vehicle-type?${params.toString()}`);
  return response;
}

/**
 * Get a single path by ID.
 */
export async function getPath(id: number): Promise<VehiclePath> {
  const response = await apiClient.get<VehiclePath>(`${BASE_URL}/${id}`);
  return response;
}

/**
 * Create a new path.
 */
export async function createPath(path: Omit<VehiclePath, 'id' | 'createdAt' | 'updatedAt'>): Promise<VehiclePath> {
  const response = await apiClient.post<VehiclePath>(BASE_URL, {
    ...path,
    waypoints: JSON.stringify(path.waypoints),
    schedule: path.schedule ? JSON.stringify(path.schedule) : null,
  });
  return response;
}

/**
 * Update an existing path.
 */
export async function updatePath(id: number, path: Partial<VehiclePath>): Promise<VehiclePath> {
  const payload = { ...path };
  if (path.waypoints) {
    (payload as Record<string, unknown>).waypoints = JSON.stringify(path.waypoints);
  }
  if (path.schedule) {
    (payload as Record<string, unknown>).schedule = JSON.stringify(path.schedule);
  }
  const response = await apiClient.put<VehiclePath>(`${BASE_URL}/${id}`, payload);
  return response;
}

/**
 * Delete a path.
 */
export async function deletePath(id: number): Promise<void> {
  await apiClient.delete(`${BASE_URL}/${id}`);
}

/**
 * Activate a path.
 */
export async function activatePath(id: number): Promise<VehiclePath> {
  const response = await apiClient.post<VehiclePath>(`${BASE_URL}/${id}/activate`);
  return response;
}

/**
 * Deactivate a path.
 */
export async function deactivatePath(id: number): Promise<VehiclePath> {
  const response = await apiClient.post<VehiclePath>(`${BASE_URL}/${id}/deactivate`);
  return response;
}

/**
 * Get preview data for path animation.
 */
export async function getPathPreview(id: number): Promise<PathPreview> {
  const response = await apiClient.get<PathPreview>(`${BASE_URL}/${id}/preview`);
  return response;
}

/**
 * Validate path waypoints against airport boundary.
 */
export async function validatePath(tenantCode: string, waypoints: Waypoint[]): Promise<ValidationResult> {
  const params = new URLSearchParams({ tenantCode });
  const response = await apiClient.post<ValidationResult>(
    `${BASE_URL}/validate?${params.toString()}`,
    JSON.stringify(waypoints)
  );
  return response;
}

/**
 * Parse waypoints from backend string format.
 */
export function parseWaypoints(waypointsJson: string): Waypoint[] {
  try {
    return JSON.parse(waypointsJson);
  } catch {
    return [];
  }
}

/**
 * Parse schedule from backend string format.
 */
export function parseSchedule(scheduleJson: string | null): PathSchedule | undefined {
  if (!scheduleJson) return undefined;
  try {
    return JSON.parse(scheduleJson);
  } catch {
    return undefined;
  }
}

/**
 * Calculate total distance from waypoints (Haversine formula).
 */
export function calculateTotalDistance(waypoints: Waypoint[]): number {
  if (waypoints.length < 2) return 0;

  let totalKm = 0;
  for (let i = 1; i < waypoints.length; i++) {
    totalKm += haversineDistance(waypoints[i - 1], waypoints[i]);
  }
  return totalKm;
}

/**
 * Calculate distance between two points using Haversine formula.
 */
function haversineDistance(p1: Waypoint, p2: Waypoint): number {
  const R = 6371; // Earth's radius in km
  const dLat = toRad(p2.lat - p1.lat);
  const dLng = toRad(p2.lng - p1.lng);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(p1.lat)) * Math.cos(toRad(p2.lat)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function toRad(deg: number): number {
  return (deg * Math.PI) / 180;
}

/**
 * Estimate duration based on distance and vehicle type.
 * Returns duration in seconds.
 */
export function estimateDuration(distanceKm: number, vehicleType: string): number {
  // Average speeds in km/h for different vehicle types
  const speeds: Record<string, number> = {
    FUEL: 15,
    CATERING: 12,
    BAGGAGE_TUG: 20,
    BAGGAGE_CART: 8,
    BELT_LOADER: 10,
    GPU: 10,
    PUSHBACK: 8,
    STAIRS: 10,
    WATER: 12,
    LAVATORY: 12,
    DEICING: 10,
    ASU: 10,
    BUS: 25,
    CARGO: 15,
    AMBULIFT: 12,
  };

  const speed = speeds[vehicleType] || 15; // Default 15 km/h
  return (distanceKm / speed) * 3600; // Convert hours to seconds
}

export const pathApi = {
  getPaths,
  getPathsByVehicleType,
  getPath,
  createPath,
  updatePath,
  deletePath,
  activatePath,
  deactivatePath,
  getPathPreview,
  validatePath,
  parseWaypoints,
  parseSchedule,
  calculateTotalDistance,
  estimateDuration,
};

export default pathApi;
