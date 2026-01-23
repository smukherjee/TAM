import React from 'react';
import { MapContainer, TileLayer, Circle } from 'react-leaflet';
import MapThemeController from './MapThemeController';
import 'leaflet/dist/leaflet.css';

interface MapComponentProps {
  children?: React.ReactNode;
  center?: [number, number];
  zoom?: number;
  theme?: 'dark' | 'light';
}

const MapComponent: React.FC<MapComponentProps> = ({ children, center = [28.5562, 77.1000], zoom = 10, theme = 'dark' }) => {
  const isDark = theme === 'dark';
  
  return (
    <MapContainer 
      center={center} 
      zoom={zoom} 
      style={{ 
        height: '100%', 
        width: '100%',
        background: isDark ? '#0f172a' : '#f8fafc'
      }} 
      key={center.toString()}
      preferCanvas={true} // Better performance for many markers
    >
      <MapThemeController theme={theme} />
      
      {/* Conditional tile layer based on theme */}
      {isDark ? (
        <TileLayer
          key="dark"
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>'
          maxZoom={20}
          subdomains='abcd'
        />
      ) : (
        <TileLayer
          key="light"
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          maxZoom={20}
        />
      )}
      
      {/* Airport boundary circles with theme-aware colors */}
      <Circle 
        center={center} 
        radius={18520} 
        pathOptions={{ 
          color: isDark ? '#3b82f6' : '#2563eb', 
          weight: 2,
          opacity: isDark ? 0.6 : 0.8,
          fill: false,
          dashArray: '5, 5'
        }} 
      />
      <Circle 
        center={center} 
        radius={74080} 
        pathOptions={{ 
          color: isDark ? '#6366f1' : '#4f46e5', 
          weight: 2,
          opacity: isDark ? 0.4 : 0.6,
          fill: false,
          dashArray: '10, 5'
        }} 
      />
      <Circle 
        center={center} 
        radius={129640} 
        pathOptions={{ 
          color: isDark ? '#8b5cf6' : '#7c3aed', 
          weight: 2,
          opacity: isDark ? 0.3 : 0.5,
          fill: false,
          dashArray: '15, 10'
        }} 
      />
      {children}
    </MapContainer>
  );
};

export default MapComponent;
