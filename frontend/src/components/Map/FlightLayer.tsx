import React, { useEffect, useState, useMemo } from 'react';
import { Marker } from 'react-leaflet';
import { FlightDisplay, getActiveFlights } from '../../services/flightService';
import { useAuth } from '../../context/AuthContext';
import { webSocketService } from '../../services/WebSocketService';
import { createAircraftIcon, AircraftIconOptions } from '../MapIcons';
import { FlightInfoCard } from '../InfoCards';
import { useThrottle } from '../../hooks/useDebounce';
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
            setFlights(data.map(f => ({ ...f, lastUpdated: now })));
        };

        fetchFlights();

        // Subscribe to WebSocket
        const subscription = webSocketService.subscribe(`/topic/flights/${icao}`, (msg: any) => {
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

    // Throttle flight updates to prevent excessive re-renders
    const throttledFlights = useThrottle(flights, 1000); // Update UI max once per second

    // Memoize flight markers to prevent unnecessary re-creation
    const flightMarkers = useMemo(() => {
        // Defensive check: ensure throttledFlights is an array
        if (!throttledFlights || !Array.isArray(throttledFlights)) {
            return [];
        }
        
        return throttledFlights.map(flight => {
            try {
                // Null-safe property access
                if (!flight.latitude || !flight.longitude || !flight.callsign) {
                    console.warn('FlightLayer: Skipping flight with missing data', flight);
                    return null;
                }
                
                const isSelected = selectedFlight?.callsign === flight.callsign;
                const iconOptions: AircraftIconOptions = {
                    heading: flight.heading || 0,
                    status: isSelected ? 'taxiing' : // Highlight selected flight with different status
                           flight.altitude > 1000 ? 'airborne' : 
                           flight.altitude > 100 ? 'taxiing' : 'landed'
                };
                const icon = createAircraftIcon(iconOptions);
                
                return (
                    <Marker 
                        key={flight.callsign} 
                        position={[flight.latitude, flight.longitude]} 
                        icon={icon}
                        zIndexOffset={isSelected ? 1000 : 0}
                        eventHandlers={{
                            click: () => setSelectedFlight(flight),
                        }}
                    />
                );
            } catch (error) {
                console.error('FlightLayer: Error rendering flight', flight?.callsign, error);
                return null;
            }
        }).filter(marker => marker !== null);
    }, [throttledFlights, selectedFlight]);
    
    return (
        <>
            {flightMarkers}
            {selectedFlight && (
                <FlightInfoCard
                    flight={{
                        id: selectedFlight.livePlotId?.toString() || selectedFlight.callsign || 'unknown',
                        callsign: selectedFlight.callsign || 'Unknown',
                        status: selectedFlight.altitude > 1000 ? 'airborne' : 'landed',
                        altitude: selectedFlight.altitude || 0,
                        speed: selectedFlight.speed || 0,
                        heading: selectedFlight.heading || 0,
                        lastUpdate: new Date(selectedFlight.time || Date.now())
                    }}
                    onClose={() => setSelectedFlight(null)}
                />
            )}
        </>
    );
};

export default FlightLayer;
