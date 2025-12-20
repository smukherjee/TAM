import React, { useEffect, useState } from 'react';
import MapComponent from '../components/Map/MapComponent';
import FlightLayer from '../components/Map/FlightLayer';
import VehicleLayer from '../components/Map/VehicleLayer';
import AlertList from '../components/Alerts/AlertList';
import api from '../services/api';

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
    const [vehicles, setVehicles] = useState<Vehicle[]>([]);
    const [alerts, setAlerts] = useState<Alert[]>([]);

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
            const response = await api.get<Alert[]>('/alerts');
            setAlerts(response.data);
        } catch (error) {
            console.error('Error fetching alerts:', error);
        }
    };

    useEffect(() => {
        fetchVehicles();
        fetchAlerts();
        const interval = setInterval(() => {
            fetchVehicles();
            fetchAlerts();
        }, 2000); // Poll every 2 seconds
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="relative w-full h-full">
            <MapComponent>
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
