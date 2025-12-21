import React, { useEffect, useState } from 'react';
import { Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { FlightDisplay, getActiveFlights, Flight } from '../../services/flightService';
import { useAuth } from '../../context/AuthContext';
import { webSocketService } from '../../services/WebSocketService';

const planeIcon = L.divIcon({
    html: '<div style="font-size: 24px; line-height: 1;">✈️</div>',
    className: 'custom-plane-icon',
    iconSize: [30, 30],
    iconAnchor: [15, 15]
});

interface FlightDisplayWithTimestamp extends FlightDisplay {
    lastUpdated: number;
}

const FlightLayer: React.FC = () => {
    const [flights, setFlights] = useState<FlightDisplayWithTimestamp[]>([]);
    const { user } = useAuth();
    const icao = user?.icaoCode || 'VIDP';

    useEffect(() => {
        const fetchFlights = async () => {
            const data = await getActiveFlights();
            const now = Date.now();
            setFlights(data.map(f => ({ ...f, lastUpdated: now })));
        };

        fetchFlights();

        // Subscribe to WebSocket
        const subscription = webSocketService.subscribe(`/topic/flights/${icao}`, (msg: Flight) => {
            const flightDisplay: FlightDisplayWithTimestamp = {
                livePlotId: msg.LivePlotId,
                callsign: msg.CallSign,
                latitude: msg.Lat,
                longitude: msg.Lon,
                speed: msg.Speed,
                heading: msg.Heading,
                altitude: msg.Altitude,
                status: msg.Status,
                time: msg.Time,
                lastUpdated: Date.now()
            };

            setFlights(prev => {
                // Use callsign as the unique key instead of livePlotId
                // livePlotId changes with every update in the simulation
                const index = prev.findIndex(f => f.callsign === flightDisplay.callsign);
                if (index >= 0) {
                    // Update existing flight
                    const newFlights = [...prev];
                    newFlights[index] = flightDisplay;
                    return newFlights;
                } else {
                    // Add new flight
                    return [...prev, flightDisplay];
                }
            });
        });

        // Prune stale flights every 10 seconds
        const interval = setInterval(() => {
            const now = Date.now();
            setFlights(prev => prev.filter(f => now - f.lastUpdated < 120000)); // Remove if older than 2 minutes
        }, 10000);

        return () => {
            subscription.unsubscribe();
            clearInterval(interval);
        };
    }, [icao]);

    return (
        <>
            {flights.map(flight => (
                <Marker key={flight.callsign} position={[flight.latitude, flight.longitude]} icon={planeIcon}>
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
