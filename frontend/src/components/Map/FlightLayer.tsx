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
    LivePlotId: string;
    CallSign: string;
    Lat: number;
    Lon: number;
    Speed: number;
    Heading: number;
    Altitude: number;
    Status: string;
    TrackId: string;
    ModeSId: string;
    FlightLevel: number;
    ROC: number;
    SSR: string;
    SafetyAlert: boolean;
    SystemStatus: string;
    Spi: boolean;
    UpdateType: string;
    Time: string;
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
                <Marker key={flight.LivePlotId} position={[flight.Lat, flight.Lon]} icon={planeIcon}>
                    <Popup>
                        <div>
                            <h3>{flight.CallSign}</h3>
                            <p><strong>Track ID:</strong> {flight.TrackId}</p>
                            <p><strong>Lat/Lon:</strong> {flight.Lat.toFixed(4)}, {flight.Lon.toFixed(4)}</p>
                            <p><strong>Altitude:</strong> {flight.Altitude?.toFixed(0)} ft</p>
                            <p><strong>Speed:</strong> {flight.Speed.toFixed(0)} kts</p>
                            <p><strong>Heading:</strong> {flight.Heading.toFixed(0)}°</p>
                            <p><strong>Mode S:</strong> {flight.ModeSId}</p>
                            <p><strong>SSR:</strong> {flight.SSR}</p>
                        </div>
                    </Popup>
                </Marker>
            ))}
        </>
    );
};

export default FlightLayer;
