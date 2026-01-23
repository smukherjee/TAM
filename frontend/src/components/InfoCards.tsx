import React from 'react';
import { Truck, Plane, MapPin, Clock, Activity } from 'lucide-react';
import './MapIcons.css';

interface VehicleInfoCardProps {
  vehicle: {
    id: string;
    type: string;
    status: string;
    location: { lat: number; lng: number };
    speed?: number;
    lastUpdate: Date;
  };
  onClose: () => void;
}

interface FlightInfoCardProps {
  flight: {
    id: string;
    callsign: string;
    status: string;
    altitude?: number;
    speed?: number;
    heading?: number;
    origin?: string;
    destination?: string;
    lastUpdate: Date;
  };
  onClose: () => void;
}

export const VehicleInfoCard: React.FC<VehicleInfoCardProps> = ({ vehicle, onClose }) => {
  return (
    <div className="info-card glass-panel" style={{
      position: 'absolute',
      bottom: '20px',
      left: '20px',
      width: '320px',
      zIndex: 1000,
      padding: '16px',
    }}>
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <Truck className="w-5 h-5 text-blue-400" />
          <h3 className="text-white font-semibold">Vehicle Details</h3>
        </div>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-white transition-colors text-xl leading-none"
        >
          ×
        </button>
      </div>

      <div className="space-y-3">
        <div>
          <div className="text-xs text-gray-500 mb-1">Vehicle ID</div>
          <div className="text-sm text-white font-medium">{vehicle.id}</div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <div className="text-xs text-gray-500 mb-1">Type</div>
            <div className="text-sm text-white capitalize">{vehicle.type.replace('_', ' ')}</div>
          </div>
          <div>
            <div className="text-xs text-gray-500 mb-1">Status</div>
            <div className="flex items-center gap-2">
              <span className={`status-dot ${vehicle.status}`}></span>
              <span className="text-sm text-white capitalize">{vehicle.status}</span>
            </div>
          </div>
        </div>

        <div>
          <div className="flex items-center gap-2 text-xs text-gray-500 mb-1">
            <MapPin className="w-3 h-3" />
            <span>Location</span>
          </div>
          <div className="text-sm text-white">
            {vehicle.location.lat.toFixed(6)}, {vehicle.location.lng.toFixed(6)}
          </div>
        </div>

        {vehicle.speed !== undefined && (
          <div>
            <div className="flex items-center gap-2 text-xs text-gray-500 mb-1">
              <Activity className="w-3 h-3" />
              <span>Speed</span>
            </div>
            <div className="text-sm text-white">{vehicle.speed.toFixed(1)} km/h</div>
          </div>
        )}

        <div>
          <div className="flex items-center gap-2 text-xs text-gray-500 mb-1">
            <Clock className="w-3 h-3" />
            <span>Last Update</span>
          </div>
          <div className="text-sm text-white">
            {new Date(vehicle.lastUpdate).toLocaleTimeString()}
          </div>
        </div>
      </div>
    </div>
  );
};

export const FlightInfoCard: React.FC<FlightInfoCardProps> = ({ flight, onClose }) => {
  return (
    <div className="info-card glass-panel" style={{
      position: 'absolute',
      bottom: '20px',
      left: '20px',
      width: '320px',
      zIndex: 1000,
      padding: '16px',
    }}>
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <Plane className="w-5 h-5 text-blue-400" />
          <h3 className="text-white font-semibold">Flight Details</h3>
        </div>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-white transition-colors text-xl leading-none"
        >
          ×
        </button>
      </div>

      <div className="space-y-3">
        <div>
          <div className="text-xs text-gray-500 mb-1">Callsign</div>
          <div className="text-lg text-white font-bold">{flight.callsign}</div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <div className="text-xs text-gray-500 mb-1">Flight ID</div>
            <div className="text-sm text-white">{flight.id}</div>
          </div>
          <div>
            <div className="text-xs text-gray-500 mb-1">Status</div>
            <div className="flex items-center gap-2">
              <span className={`status-dot ${flight.status}`}></span>
              <span className="text-sm text-white capitalize">{flight.status}</span>
            </div>
          </div>
        </div>

        {flight.origin && flight.destination && (
          <div>
            <div className="text-xs text-gray-500 mb-1">Route</div>
            <div className="text-sm text-white">
              {flight.origin} → {flight.destination}
            </div>
          </div>
        )}

        <div className="grid grid-cols-3 gap-3">
          {flight.altitude !== undefined && (
            <div>
              <div className="text-xs text-gray-500 mb-1">Altitude</div>
              <div className="text-sm text-white">{flight.altitude.toLocaleString()} ft</div>
            </div>
          )}
          {flight.speed !== undefined && (
            <div>
              <div className="text-xs text-gray-500 mb-1">Speed</div>
              <div className="text-sm text-white">{flight.speed.toFixed(0)} kt</div>
            </div>
          )}
          {flight.heading !== undefined && (
            <div>
              <div className="text-xs text-gray-500 mb-1">Heading</div>
              <div className="text-sm text-white">{flight.heading.toFixed(0)}°</div>
            </div>
          )}
        </div>

        <div>
          <div className="flex items-center gap-2 text-xs text-gray-500 mb-1">
            <Clock className="w-3 h-3" />
            <span>Last Update</span>
          </div>
          <div className="text-sm text-white">
            {new Date(flight.lastUpdate).toLocaleTimeString()}
          </div>
        </div>
      </div>
    </div>
  );
};
