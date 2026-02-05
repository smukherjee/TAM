import React from 'react';
import { MapContainer, TileLayer } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import FlightLayer from './FlightLayer';
import VehicleLayer from './VehicleLayer';
import { useQuery } from '@tanstack/react-query';
import { getVehicles } from '../../services/vehicleService';

const IGIA_COORDINATES: [number, number] = [28.5562, 77.1000];
const DEFAULT_ZOOM = 13;

const Map: React.FC = () => {
  const { data: vehicles = [] } = useQuery({
    queryKey: ['vehicles'],
    queryFn: getVehicles,
    refetchInterval: 5000
  });

  return (
    <MapContainer
      center={IGIA_COORDINATES}
      zoom={DEFAULT_ZOOM}
      style={{ height: '100vh', width: '100%' }}
    >
      <TileLayer
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      <FlightLayer />
      <VehicleLayer vehicles={vehicles} />
    </MapContainer>
  );
};

export default Map;
