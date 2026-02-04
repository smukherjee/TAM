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

export const getVehicles = async (): Promise<Vehicle[]> => {
  try {
    const response = await api.get<Vehicle[]>(API_URL);
    return response.data;
  } catch (error) {
    console.error('Error fetching vehicles:', error);
    return [];
  }
};
