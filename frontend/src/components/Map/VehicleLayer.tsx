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
    vehicleNo: string;
    type: string;
    latitude: number;
    longitude: number;
    speed: number;
    altitude: number;
    status: string;
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
                <Marker key={vehicle.vehicleNo} position={[vehicle.latitude, vehicle.longitude]} icon={busIcon}>
                    <Popup>
                        <div>
                            <h3>{vehicle.vehicleNo}</h3>
                            <p><strong>Type:</strong> {vehicle.type}</p>
                            <p><strong>Lat/Lon:</strong> {vehicle.latitude.toFixed(4)}, {vehicle.longitude.toFixed(4)}</p>
                            <p><strong>Altitude:</strong> {vehicle.altitude?.toFixed(0)} ft</p>
                            <p><strong>Speed:</strong> {vehicle.speed.toFixed(0)} km/h</p>
                            <p><strong>Status:</strong> {vehicle.status}</p>
                        </div>
                    </Popup>
                </Marker>
            ))}
        </>
    );
};

export default VehicleLayer;
