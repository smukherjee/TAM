import React, { useEffect, useState } from 'react';
import MapComponent from '../components/Map/MapComponent';
import FlightLayer from '../components/Map/FlightLayer';
import VehicleLayer from '../components/Map/VehicleLayer';
import AlertList from '../components/Alerts/AlertList';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { webSocketService } from '../services/WebSocketService';
import { Vehicle } from '../services/vehicleService';

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

        // WebSocket Subscription
        const handleVehicleUpdate = (vehicle: Vehicle) => {
            setVehicles(prev => {
                const index = prev.findIndex(v => v.vehicle_no === vehicle.vehicle_no);
                if (index !== -1) {
                    const newVehicles = [...prev];
                    newVehicles[index] = vehicle;
                    return newVehicles;
                } else {
                    return [...prev, vehicle];
                }
            });
        };

        const handleAlertUpdate = (alert: Alert) => {
            setAlerts(prev => [alert, ...prev].slice(0, 50));
        };

        webSocketService.connect(() => {
            webSocketService.subscribe('/topic/vehicles/' + icao, handleVehicleUpdate);
            webSocketService.subscribe('/topic/alerts/' + icao, handleAlertUpdate);
        });

        return () => {
            webSocketService.disconnect();
        };
    }, [icao]);

    return (
        <div className="h-full flex flex-col">
            <div className="flex-1 relative">
                <MapComponent center={getCenter()} zoom={14}>
                    <FlightLayer />
                    <VehicleLayer vehicles={vehicles} />
                </MapComponent>
                
                {/* Overlay Alerts */}
                <div className="absolute top-4 right-4 w-96 z-[1000]">
                    <AlertList alerts={alerts} />
                </div>
            </div>
        </div>
    );
};

export default MapPage;
