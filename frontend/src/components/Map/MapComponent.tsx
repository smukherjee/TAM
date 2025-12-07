import React from 'react';
import { MapContainer, TileLayer, Circle } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';

interface MapComponentProps {
  children?: React.ReactNode;
}

const MapComponent: React.FC<MapComponentProps> = ({ children }) => {
  const center: [number, number] = [28.5562, 77.1000]; // Indira Gandhi International Airport

  return (
    <MapContainer center={center} zoom={8} style={{ height: '100%', width: '100%' }}>
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      />
      <Circle center={center} radius={18520} pathOptions={{ color: 'blue', fill: false }} />
      <Circle center={center} radius={74080} pathOptions={{ color: 'blue', fill: false }} />
      <Circle center={center} radius={129640} pathOptions={{ color: 'blue', fill: false }} />
      {children}
    </MapContainer>
  );
};

export default MapComponent;
