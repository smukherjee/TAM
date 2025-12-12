import axios from 'axios';

const API_URL = 'http://localhost:8080/api/flights';

export interface Flight {
  livePlotId: string;
  callsign: string;
  latitude: number;
  longitude: number;
  speed: number;
  heading: number;
  altitude: number;
  status: string;
  time: string;
}

export const getActiveFlights = async (): Promise<Flight[]> => {
  try {
    const response = await axios.get(API_URL);
    if (response.data.success) {
      return response.data.data;
    }
    return [];
  } catch (error) {
    console.error('Error fetching flights:', error);
    return [];
  }
};
