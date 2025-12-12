import React, { useEffect, useState } from 'react';
import { Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { Flight, getActiveFlights } from '../../services/flightService';

const planeIcon = L.divIcon({
    html: '<div style="font-size: 24px; line-height: 1;">✈️</div>',
    className: 'custom-plane-icon',
    iconSize: [30, 30],
    iconAnchor: [15, 15]
});

const FlightLayer: React.FC = () => {
    const [flights, setFlights] = useState<Flight[]>([]);

    useEffect(() => {
        const fetchFlights = async () => {
            const data = await getActiveFlights();
            setFlights(data);
        };

        fetchFlights();
        const interval = setInterval(fetchFlights, 3000);
        return () => clearInterval(interval);
    }, []);

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
                        </div>
                    </Popup>
                </Marker>
            ))}
        </>
    );
};

export default FlightLayer;
