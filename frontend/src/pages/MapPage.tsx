import React, { useEffect, useState } from 'react';
import MapComponent from '../components/Map/MapComponent';
import FlightLayer from '../components/Map/FlightLayer';
import VehicleLayer from '../components/Map/VehicleLayer';
import AlertList from '../components/Alerts/AlertList';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { webSocketService } from '../services/WebSocketService';

interface Vehicle {
    vehicle_no: string;
    vehicletype: string;
    latitude: string;
    longitude: string;
    speed: string;
    status: string;
    vehicle_name: string;
    company: string;
    location: string;
    gpsactualtime: string;
    ign: string;
}

interface Alert {
    alertId: string;
    type: string;
    entityId: string;
    value: number;
    timestamp: string;
    latitude: number;
    longitude: number;
}

const MapPage: React.FC = () => {
    const { user } = useAuth();
    const [vehicles, setVehicles] = useState<Vehicle[]>([]);
    const [alerts, setAlerts] = useState<Alert[]>([]);
    const icao = user?.icaoCode || 'VIDP';

    const getCenter = (): [number, number] => {
        if (user?.icaoCode === 'VABB') return [19.0896, 72.8656];
        if (user?.icaoCode === 'LIRN') return [40.8844, 14.2908];
        return [28.5562, 77.1000]; // Default VIDP
    };

    const fetchVehicles = async () => {
        try {
            const response = await api.get<Vehicle[]>('/vehicles');
            setVehicles(response.data);
        } catch (error) {
            console.error('Error fetching vehicles:', error);
        }
    };

    const fetchAlerts = async () => {
        try {
            const response = await api.get<Alert[]>('/vehicle-alerts');
            setAlerts(response.data);
        } catch (error) {
            console.error('Error fetching alerts:', error);
        }
    };

    useEffect(() => {
        fetchVehicles();
        fetchAlerts();

        // Subscribe to Vehicles via WebSocket
        const vehicleSub = webSocketService.subscribe(`/topic/vehicles/${icao}`, (vehicle: Vehicle) => {
            setVehicles(prev => {
                const index = prev.findIndex(v => v.vehicle_no === vehicle.vehicle_no);
                if (index >= 0) {
                    const newVehicles = [...prev];
                    newVehicles[index] = vehicle;
                    return newVehicles;
                } else {
                    return [...prev, vehicle];
                }
            });
        });

        const interval = setInterval(() => {
            // Only poll alerts, vehicles are real-time now
            fetchAlerts();
        }, 2000); 

        return () => {
            clearInterval(interval);
            vehicleSub.unsubscribe();
        };
    }, [icao]);

    return (
        <div className="relative w-full h-full">
            <MapComponent center={getCenter()}>
                <FlightLayer />
                <VehicleLayer vehicles={vehicles} />
            </MapComponent>
            <div className="absolute top-4 right-4 z-[500] w-[400px]">
                <AlertList alerts={alerts} />
            </div>
        </div>
    );
};

export default MapPage;
