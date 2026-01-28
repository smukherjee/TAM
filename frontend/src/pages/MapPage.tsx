import React, { useEffect, useState } from 'react';
import MapComponent from '../components/Map/MapComponent';
import FlightLayer from '../components/Map/FlightLayer';
import VehicleLayer from '../components/Map/VehicleLayer';
import EnhancedAlertList from '../components/EnhancedAlertList';
import MapControlPanel from '../components/MapControlPanel';
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
    const [layers, setLayers] = useState({ vehicles: true, flights: true, alerts: true, assets: false });
    const [mapTheme, setMapTheme] = useState<'dark' | 'light'>('dark');
    const icao = user?.icaoCode || 'VIDP';

    const getCenter = (): [number, number] => {
        if (user?.icaoCode === 'VABB') return [19.0896, 72.8656];
        if (user?.icaoCode === 'LIRN') return [40.8844, 14.2908];
        if (user?.icaoCode === 'YBBN') return [-27.3842, 153.1175]; // Brisbane
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

    const handleLayerToggle = (layer: 'vehicles' | 'flights' | 'alerts' | 'assets') => {
        setLayers(prev => ({ ...prev, [layer]: !prev[layer] }));
    };

    const handleThemeToggle = () => {
        setMapTheme(prev => prev === 'dark' ? 'light' : 'dark');
    };

    const handleAlertDismiss = (alertId: string) => {
        setAlerts(prev => prev.filter(a => a.alertId !== alertId));
    };

    const enhancedAlerts = alerts.map(a => ({
        id: a.alertId,
        vehicleId: a.entityId,
        message: `${a.type}: ${a.value}`,
        severity: a.type.toLowerCase().includes('critical') ? 'critical' as const : 
                  a.type.toLowerCase().includes('warning') ? 'warning' as const : 'info' as const,
        timestamp: new Date(a.timestamp)
    }));

    return (
        <div className="h-full flex flex-col">
            <div className="flex-1 relative">
                <MapComponent center={getCenter()} zoom={14} theme={mapTheme}>
                    {layers.flights && <FlightLayer />}
                    {layers.vehicles && <VehicleLayer vehicles={vehicles} />}
                </MapComponent>
                
                {/* Map Control Panel */}
                <MapControlPanel
                    layers={layers}
                    onLayerToggle={handleLayerToggle}
                    theme={mapTheme}
                    onThemeToggle={handleThemeToggle}
                />
                
                {/* Enhanced Alert List */}
                {layers.alerts && (
                    <EnhancedAlertList
                        alerts={enhancedAlerts}
                        onDismiss={handleAlertDismiss}
                    />
                )}
            </div>
        </div>
    );
};

export default MapPage;
