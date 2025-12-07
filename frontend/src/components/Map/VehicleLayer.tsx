import React from 'react';
import { Marker, Popup } from 'react-leaflet';
import L from 'leaflet';

// Fix for default marker icon (if not already handled globally)
import icon from 'leaflet/dist/images/marker-icon.png';
import iconShadow from 'leaflet/dist/images/marker-shadow.png';

// Use a different icon color or style if possible, but for now standard marker
// Maybe we can use a custom icon later.

let DefaultIcon = L.icon({
    iconUrl: icon,
    shadowUrl: iconShadow,
    iconSize: [25, 41],
    iconAnchor: [12, 41]
});

L.Marker.prototype.options.icon = DefaultIcon;

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

const busIcon = L.divIcon({
    html: '<div style="font-size: 24px; line-height: 1;">🚌</div>',
    className: 'custom-bus-icon',
    iconSize: [30, 30],
    iconAnchor: [15, 15]
});

interface VehicleLayerProps {
    vehicles: Vehicle[];
}

const VehicleLayer: React.FC<VehicleLayerProps> = ({ vehicles }) => {
    return (
        <>
            {vehicles.map(vehicle => (
                <Marker 
                    key={vehicle.vehicle_no} 
                    position={[parseFloat(vehicle.latitude), parseFloat(vehicle.longitude)]} 
                    icon={busIcon}
                >
                    <Popup>
                        <div>
                            <h3>{vehicle.vehicle_name} ({vehicle.vehicle_no})</h3>
                            <p><strong>Type:</strong> {vehicle.vehicletype}</p>
                            <p><strong>Company:</strong> {vehicle.company}</p>
                            <p><strong>Status:</strong> {vehicle.status}</p>
                            <p><strong>Speed:</strong> {vehicle.speed} km/h</p>
                            <p><strong>Ignition:</strong> {vehicle.ign}</p>
                            <p><strong>Location:</strong> {vehicle.location}</p>
                            <p><strong>Last Update:</strong> {vehicle.gpsactualtime}</p>
                        </div>
                    </Popup>
                </Marker>
            ))}
        </>
    );
};

export default VehicleLayer;
