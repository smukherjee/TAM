import React, { useEffect, useState } from 'react';
import { Marker } from 'react-leaflet';
import { FlightDisplay, getActiveFlights } from '../../services/flightService';
import { useAuth } from '../../context/AuthContext';
import { webSocketService } from '../../services/WebSocketService';
import { createAircraftIcon, AircraftIconOptions } from '../MapIcons';
import { FlightInfoCard } from '../InfoCards';
import '../MapIcons.css';

interface FlightDisplayWithTimestamp extends FlightDisplay {
    lastUpdated: number;
}

const FlightLayer: React.FC = () => {
    const [flights, setFlights] = useState<FlightDisplayWithTimestamp[]>([]);
    const [selectedFlight, setSelectedFlight] = useState<FlightDisplayWithTimestamp | null>(null);
    const { user } = useAuth();
    const icao = user?.icaoCode || 'VIDP';

    useEffect(() => {
        const fetchFlights = async () => {
            const data = await getActiveFlights();
            const now = Date.now();
            console.log('FlightLayer: Fetched initial flights:', data.length);
            setFlights(data.map(f => ({ ...f, lastUpdated: now })));
        };

        fetchFlights();

        // Subscribe to WebSocket
        console.log('FlightLayer: Subscribing to /topic/flights/' + icao);
        const subscription = webSocketService.subscribe(`/topic/flights/${icao}`, (msg: any) => {
            console.log('FlightLayer: Received flight via WebSocket:', msg);
            const flightDisplay: FlightDisplayWithTimestamp = {
                livePlotId: msg.LivePlotId || msg.id || msg.livePlotId || 'unknown',
                callsign: msg.CallSign || msg.callsign,
                latitude: msg.Lat || msg.latitude,
                longitude: msg.Lon || msg.longitude,
                speed: msg.Speed || msg.speed,
                heading: msg.Heading || msg.heading,
                altitude: msg.Altitude || msg.altitude,
                status: msg.Status || msg.status || 'AIRBORNE',
                time: msg.Time || msg.time || msg.timestamp,
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
                    console.log('FlightLayer: Updated flight', flightDisplay.callsign, 'Total flights:', newFlights.length);
                    return newFlights;
                } else {
                    // Add new flight
                    console.log('FlightLayer: Added new flight', flightDisplay.callsign, 'Total flights:', prev.length + 1);
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

    console.log('FlightLayer: Rendering', flights.length, 'flights');
    
    return (
        <>
            {flights.map(flight => {
                console.log('FlightLayer: Rendering flight', flight.callsign, 'at', flight.latitude, flight.longitude);
                try {
                    const iconOptions: AircraftIconOptions = {
                        heading: flight.heading || 0,
                        status: flight.altitude > 1000 ? 'airborne' : flight.altitude > 100 ? 'taxiing' : 'landed'
                    };
                    const icon = createAircraftIcon(iconOptions);
                    console.log('FlightLayer: Created icon for', flight.callsign, icon);
                    return (
                        <Marker 
                            key={flight.callsign} 
                            position={[flight.latitude, flight.longitude]} 
                            icon={icon}
                            eventHandlers={{
                                mouseover: () => setSelectedFlight(flight),
                                mouseout: () => setSelectedFlight(null)
                            }}
                        />
                    );
                } catch (error) {
                    console.error('FlightLayer: Error rendering flight', flight.callsign, error);
                    return null;
                }
            })}
            {selectedFlight && (
                <FlightInfoCard
                    flight={{
                        id: selectedFlight.livePlotId.toString(),
                        callsign: selectedFlight.callsign,
                        status: selectedFlight.altitude > 1000 ? 'airborne' : 'landed',
                        altitude: selectedFlight.altitude,
                        speed: selectedFlight.speed,
                        heading: selectedFlight.heading,
                        lastUpdate: new Date(selectedFlight.time)
                    }}
                    onClose={() => setSelectedFlight(null)}
                />
            )}
        </>
    );
};

export default FlightLayer;
