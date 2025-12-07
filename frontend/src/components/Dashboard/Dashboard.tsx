import React, { useEffect, useState } from 'react';
import DashboardLayout from './DashboardLayout';
import MapComponent from '../Map/MapComponent';
import FlightLayer from '../Map/FlightLayer';
import VehicleLayer from '../Map/VehicleLayer';
import AlertList from '../Alerts/AlertList';
import api from '../../services/api';

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

const Dashboard: React.FC = () => {
    const [flights, setFlights] = useState<Flight[]>([]);
    const [vehicles, setVehicles] = useState<Vehicle[]>([]);
    const [alerts, setAlerts] = useState<Alert[]>([]);

    const fetchFlights = async () => {
        try {
            const response = await api.get<Flight[]>('/flights');
            setFlights(response.data);
        } catch (error) {
            console.error('Error fetching flights:', error);
        }
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
            const response = await api.get<Alert[]>('/alerts');
            setAlerts(response.data);
        } catch (error) {
            console.error('Error fetching alerts:', error);
        }
    };

    useEffect(() => {
        fetchFlights();
        fetchVehicles();
        fetchAlerts();
        const interval = setInterval(() => {
            fetchFlights();
            fetchVehicles();
            fetchAlerts();
        }, 2000); // Poll every 2 seconds
        return () => clearInterval(interval);
    }, []);

    return (
        <DashboardLayout>
            <div style={{ position: 'relative', flex: 1, height: '100%', width: '100%' }}>
                <MapComponent>
                    <FlightLayer flights={flights} />
                    <VehicleLayer vehicles={vehicles} />
                </MapComponent>
                <div style={{ position: 'absolute', top: '10px', right: '10px', zIndex: 1000, width: '300px' }}>
                    <AlertList alerts={alerts} />
                </div>
            </div>
        </DashboardLayout>
    );
};

export default Dashboard;


