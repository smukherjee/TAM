import React, { useState } from 'react';
import { Marker, Popup } from 'react-leaflet';
import L from 'leaflet';
import { Vehicle } from '../../services/vehicleService';
import { createVehicleIcon, VehicleIconOptions } from '../MapIcons';
import { VehicleInfoCard } from '../InfoCards';
import '../MapIcons.css';

// Fix for default marker icon (if not already handled globally)
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});

L.Marker.prototype.options.icon = DefaultIcon;

const getVehicleTypeAndStatus = (vehicle: Vehicle): VehicleIconOptions => {
    const lowerType = (vehicle.vehicletype || '').toLowerCase();
    let type: VehicleIconOptions['type'] = 'other';
    
    if (lowerType.includes('bus')) type = 'bus';
    else if (lowerType.includes('truck') || lowerType.includes('fuel')) type = 'fuel_truck';
    else if (lowerType.includes('tug')) type = 'tug';
    else if (lowerType.includes('compactor') || lowerType.includes('belt')) type = 'belt_loader';
    else if (lowerType.includes('catering') || lowerType.includes('headunit')) type = 'catering';
    
    // Determine status based on vehicle data
    let status: VehicleIconOptions['status'] = 'idle';
    if (vehicle.ign === 'ON' && vehicle.speed > 5) status = 'active';
    else if (vehicle.speed > 20) status = 'warning'; // Example: speeding
    
    return { type, status };
};

interface VehicleLayerProps {
    vehicles: Vehicle[];
}

const VehicleLayer: React.FC<VehicleLayerProps> = ({ vehicles }) => {
    const [selectedVehicle, setSelectedVehicle] = useState<Vehicle | null>(null);

    return (
        <>
            {vehicles.map(vehicle => {
                const iconOptions = getVehicleTypeAndStatus(vehicle);
                return (
                    <Marker 
                        key={vehicle.vehicle_no} 
                        position={[vehicle.latitude, vehicle.longitude]} 
                        icon={createVehicleIcon(iconOptions)}
                        eventHandlers={{
                            click: () => setSelectedVehicle(vehicle)
                        }}
                    >
                        <Popup>
                            <div>
                                <h3>{vehicle.vehicle_name} ({vehicle.vehicle_no})</h3>
                                <p><strong>Type:</strong> {vehicle.vehicletype}</p>
                                <p><strong>Company:</strong> {vehicle.company}</p>
                                <p><strong>Status:</strong> {vehicle.status}</p>
                                <p><strong>Speed:</strong> {vehicle.speed.toFixed(1)} km/h</p>
                                <p><strong>Ignition:</strong> {vehicle.ign}</p>
                                <p><strong>Location:</strong> {vehicle.location}</p>
                                <p><strong>Last Update:</strong> {vehicle.gpsactualtime}</p>
                            </div>
                        </Popup>
                </Marker>
                );
            })}
            {selectedVehicle && (
                <VehicleInfoCard
                    vehicle={{
                        id: selectedVehicle.vehicle_no,
                        type: selectedVehicle.vehicletype,
                        status: selectedVehicle.ign === 'ON' ? 'active' : 'idle',
                        location: { lat: selectedVehicle.latitude, lng: selectedVehicle.longitude },
                        speed: selectedVehicle.speed,
                        lastUpdate: new Date(selectedVehicle.gpsactualtime)
                    }}
                    onClose={() => setSelectedVehicle(null)}
                />
            )}
        </>
    );
};

export default VehicleLayer;
