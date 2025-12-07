import React from 'react';
import { Marker, Popup } from 'react-leaflet';
import L from 'leaflet';

// Fix for default marker icon
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});

L.Marker.prototype.options.icon = DefaultIcon;

interface Flight {
    livePlotId: string;
    callsign: string;
    latitude: number;
    longitude: number;
    speed: number;
    heading: number;
    altitude: number;
    status: string;
}

const planeIcon = L.divIcon({
    html: '<div style="font-size: 24px; line-height: 1;">✈️</div>',
    className: 'custom-plane-icon',
    iconSize: [30, 30],
    iconAnchor: [15, 15]
});

interface FlightLayerProps {
    flights: Flight[];
}

const FlightLayer: React.FC<FlightLayerProps> = ({ flights }) => {
    return (
        <>
            {flights.map(flight => (
                <Marker key={flight.livePlotId} position={[flight.latitude, flight.longitude]} icon={planeIcon}>
                    <Popup>
                        <div>
                            <h3>{flight.callsign}</h3>
                            <p><strong>Lat/Lon:</strong> {flight.latitude.toFixed(4)}, {flight.longitude.toFixed(4)}</p>
                            <p><strong>Altitude:</strong> {flight.altitude?.toFixed(0)} ft</p>
                            <p><strong>Speed:</strong> {flight.speed.toFixed(0)} kts</p>
                            <p><strong>Heading:</strong> {flight.heading.toFixed(0)}°</p>
                            <p><strong>Status:</strong> {flight.status}</p>
                        </div>
                    </Popup>
                </Marker>
            ))}
        </>
    );
};

export default FlightLayer;
