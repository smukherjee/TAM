import axios from 'axios';

const API_URL = '/api/flights';

export interface Flight {
  LivePlotId: string;
  CallSign: string;
  Lat: number;
  Lon: number;
  Speed: number;
  Heading: number;
  Altitude: number;
  Status: string;
  Time: string;
  TrackId: string;
  ModeSId: string;
  FlightLevel: number;
  ROC: number;
  SSR: string;
  SafetyAlert: boolean;
  SystemStatus: string;
  Spi: boolean;
  UpdateType: string;
}

// Mapped interface for component use (lowercase properties)
export interface FlightDisplay {
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

export const getActiveFlights = async (): Promise<FlightDisplay[]> => {
  try {
    const response = await axios.get(API_URL);
    if (response.data.success && Array.isArray(response.data.data)) {
      // Map API response to display format
      return response.data.data.map((f: Flight) => ({
        livePlotId: f.LivePlotId,
        callsign: f.CallSign,
        latitude: f.Lat,
        longitude: f.Lon,
        speed: f.Speed,
        heading: f.Heading,
        altitude: f.Altitude,
        status: f.Status,
        time: f.Time
      }));
    }
    return [];
  } catch (error) {
    console.error('Error fetching flights:', error);
    return [];
  }
};
