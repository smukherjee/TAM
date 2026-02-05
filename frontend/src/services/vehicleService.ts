import api from './api';

const API_URL = '/vehicles';

export interface Vehicle {
  id: string;
  gpsactualtime: string;
  vehicle_no: string;
  vehicletype: string;
  latitude: number;
  longitude: number;
  speed: number;
  status: string;
  vehicle_name: string;
  // Fields below are optional - may not be present from backend
  company?: string;
  location?: string;
  ign?: string;
}

export interface AssetStatus {
  status: string;
  assetId: string;
  name: string;
  category: string;
}

// Client-side cache for asset status (5 minutes TTL)
const assetStatusCache = new Map<string, { status: AssetStatus; timestamp: number }>();
const CACHE_TTL = 5 * 60 * 1000; // 5 minutes in milliseconds

export const getVehicles = async (): Promise<Vehicle[]> => {
  try {
    const response = await api.get<Vehicle[]>(API_URL);
    return response.data;
  } catch (error) {
    console.error('Error fetching vehicles:', error);
    return [];
  }
};

/**
 * Fetch asset status for a vehicle by vehicle ID.
 * Uses client-side caching (5 min TTL) to minimize API calls.
 * 
 * @param vehicleId The vehicle identifier
 * @returns Asset status or null if not found
 */
export const fetchAssetStatusByVehicleId = async (vehicleId: string): Promise<AssetStatus | null> => {
  try {
    // Check cache first
    const cached = assetStatusCache.get(vehicleId);
    if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
      console.debug(`Using cached asset status for vehicle ${vehicleId}`);
      return cached.status;
    }

    // Fetch from API
    console.debug(`Fetching asset status for vehicle ${vehicleId}`);
    const response = await api.get<AssetStatus>(`${API_URL}/${vehicleId}/asset-status`);
    const status = response.data;

    // Update cache
    assetStatusCache.set(vehicleId, { status, timestamp: Date.now() });

    return status;
  } catch (error: any) {
    if (error.response?.status === 404) {
      console.debug(`No asset linked to vehicle ${vehicleId}`);
      return null;
    }
    console.error(`Error fetching asset status for vehicle ${vehicleId}:`, error);
    return null;
  }
};
