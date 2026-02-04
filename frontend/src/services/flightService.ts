import api from './api';

const API_URL = '/flights';

// Interface matching backend Flight.java with @JsonProperty mappings
export interface Flight {
  id?: string;
  FlightNumber: string;  // @JsonProperty("FlightNumber")
  CallSign: string;      // @JsonProperty("CallSign")
  Lat: number;           // @JsonProperty("Lat")
  Lon: number;           // @JsonProperty("Lon")
  Speed: number;         // @JsonProperty("Speed")
  Heading: number;       // @JsonProperty("Heading")
  Altitude: number;      // @JsonProperty("Altitude")
  Status: string;        // @JsonProperty("Status")
  Time: string;          // @JsonProperty("Time")
  // Optional fields that may come from external data sources
  LivePlotId?: string;
  TrackId?: string;
  ModeSId?: string;
  FlightLevel?: number;
  ROC?: number;
  SSR?: string;
  SafetyAlert?: boolean;
  SystemStatus?: string;
  Spi?: boolean;
  UpdateType?: string;
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
    // Don't pass headers here - the api interceptor already adds X-User-ICAO from localStorage
    const response = await api.get(API_URL);
    if (response.data.success && Array.isArray(response.data.data)) {
      // Map API response to display format
      return response.data.data.map((f: Flight) => ({
        livePlotId: f.LivePlotId || f.id || f.FlightNumber || '',
        callsign: f.CallSign || '',
        latitude: f.Lat || 0,
        longitude: f.Lon || 0,
        speed: f.Speed || 0,
        heading: f.Heading || 0,
        altitude: f.Altitude || 0,
        status: f.Status || '',
        time: f.Time || ''
      }));
    }
    return [];
  } catch (error) {
    console.error('Error fetching flights:', error);
    return [];
  }
};
