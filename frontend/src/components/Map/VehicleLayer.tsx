import React, { useState, useEffect } from 'react';
import { Marker } from 'react-leaflet';
import L from 'leaflet';
import { Vehicle, fetchAssetStatusByVehicleId } from '../../services/vehicleService';
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

    if (lowerType.includes('bus') || lowerType.includes('passenger bus')) type = 'bus';
    else if (lowerType.includes('fuel') || lowerType.includes('fuel truck')) type = 'fuel_truck';
    else if (lowerType.includes('tug') || lowerType.includes('baggage') || lowerType.includes('tractor')) type = 'tug';
    else if (lowerType.includes('compactor') || lowerType.includes('belt')) type = 'belt_loader';
    else if (lowerType.includes('catering') || lowerType.includes('headunit')) type = 'catering';
    else if (lowerType.includes('deic') || lowerType.includes('de-ic')) type = 'deicing';
    else if (lowerType.includes('stair')) type = 'stairs';
    else if (lowerType.includes('gpu') || lowerType.includes('ground')) type = 'gpu';

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
    const [assetStatus, setAssetStatus] = useState<string | null>(null);
    const [loadingStatus, setLoadingStatus] = useState(false);

    // Fetch asset status when vehicle is selected
    useEffect(() => {
        if (selectedVehicle) {
            setLoadingStatus(true);
            fetchAssetStatusByVehicleId(selectedVehicle.vehicle_no)
                .then(statusData => {
                    if (statusData) {
                        setAssetStatus(statusData.status);
                    } else {
                        // No asset linked - fall back to calculated status
                        setAssetStatus(selectedVehicle.ign === 'ON' ? 'active' : 'idle');
                    }
                })
                .catch(() => {
                    // Error - fall back to calculated status
                    setAssetStatus(selectedVehicle.ign === 'ON' ? 'active' : 'idle');
                })
                .finally(() => setLoadingStatus(false));
        }
    }, [selectedVehicle]);

    // Guard against undefined or non-array vehicles
    if (!vehicles || !Array.isArray(vehicles)) {
        console.warn('VehicleLayer: vehicles is not an array', vehicles);
        return null;
    }

    return (
        <>
            {vehicles.map(vehicle => {
                const isSelected = selectedVehicle?.vehicle_no === vehicle.vehicle_no;
                const iconOptions = getVehicleTypeAndStatus(vehicle);

                // Change status to 'warning' for selected vehicle to make it stand out
                if (isSelected) {
                    iconOptions.status = 'warning';
                }

                return (
                    <Marker
                        key={vehicle.vehicle_no}
                        position={[vehicle.latitude, vehicle.longitude]}
                        icon={createVehicleIcon(iconOptions)}
                        eventHandlers={{
                            click: () => setSelectedVehicle(vehicle)
                        }}
                        zIndexOffset={isSelected ? 1000 : 0}
                    />
                );
            })}
            {selectedVehicle && (
                <VehicleInfoCard
                    vehicle={{
                        id: selectedVehicle.vehicle_no,
                        type: selectedVehicle.vehicletype,
                        status: assetStatus || 'Unknown',
                        location: { lat: selectedVehicle.latitude, lng: selectedVehicle.longitude },
                        speed: selectedVehicle.speed,
                        lastUpdate: new Date(selectedVehicle.gpsactualtime)
                    }}
                    loading={loadingStatus}
                    onClose={() => {
                        setSelectedVehicle(null);
                        setAssetStatus(null);
                    }}
                />
            )}
        </>
    );
};

export default VehicleLayer;
