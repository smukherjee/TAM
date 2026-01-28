import L from 'leaflet';

export interface VehicleIconOptions {
  type: 'bus' | 'fuel_truck' | 'tug' | 'belt_loader' | 'catering' | 'other' | 'emergency' | 'gpu' | 'cargo_loader' | 'power_unit' | 'cleaning' | 'maintenance';
  status: 'active' | 'alert' | 'warning' | 'idle';
}

export interface AircraftIconOptions {
  heading: number;
  status: 'airborne' | 'landed' | 'taxiing' | 'alert';
}

const getStatusColor = (status: string): string => {
  switch (status) {
    case 'active':
    case 'airborne':
      return '#3b82f6'; // blue
    case 'alert':
      return '#ef4444'; // red
    case 'warning':
      return '#f59e0b'; // amber
    case 'idle':
    case 'landed':
      return '#6b7280'; // gray
    case 'taxiing':
      return '#10b981'; // green
    default:
      return '#3b82f6';
  }
};

const getVehicleSVG = (type: string, color: string): string => {
  const svgs: Record<string, string> = {
    bus: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="6" y="8" width="20" height="16" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <rect x="8" y="10" width="7" height="6" fill="white" opacity="0.3"/>
        <rect x="17" y="10" width="7" height="6" fill="white" opacity="0.3"/>
        <circle cx="10" cy="24" r="2" fill="white"/>
        <circle cx="22" cy="24" r="2" fill="white"/>
        <rect x="12" y="20" width="8" height="2" fill="white" opacity="0.5"/>
      </svg>
    `,
    fuel_truck: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="4" y="12" width="24" height="10" rx="1" fill="${color}" stroke="white" stroke-width="1.5"/>
        <ellipse cx="16" cy="10" rx="8" ry="4" fill="${color}" stroke="white" stroke-width="1.5"/>
        <circle cx="10" cy="22" r="2" fill="white"/>
        <circle cx="22" cy="22" r="2" fill="white"/>
        <rect x="14" y="8" width="4" height="6" fill="white" opacity="0.3"/>
      </svg>
    `,
    tug: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="8" y="10" width="16" height="12" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <rect x="10" y="12" width="5" height="4" fill="white" opacity="0.3"/>
        <circle cx="12" cy="22" r="2" fill="white"/>
        <circle cx="20" cy="22" r="2" fill="white"/>
        <rect x="22" y="14" width="4" height="4" fill="white" opacity="0.5"/>
      </svg>
    `,
    belt_loader: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="6" y="14" width="14" height="8" rx="1" fill="${color}" stroke="white" stroke-width="1.5"/>
        <rect x="18" y="8" width="8" height="14" rx="1" fill="${color}" stroke="white" stroke-width="1.5"/>
        <circle cx="10" cy="22" r="2" fill="white"/>
        <circle cx="16" cy="22" r="2" fill="white"/>
        <line x1="20" y1="10" x2="20" y2="18" stroke="white" stroke-width="1" opacity="0.5"/>
      </svg>
    `,
    catering: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="6" y="10" width="20" height="12" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <rect x="8" y="8" width="16" height="4" fill="${color}" stroke="white" stroke-width="1.5"/>
        <circle cx="10" cy="22" r="2" fill="white"/>
        <circle cx="22" cy="22" r="2" fill="white"/>
        <path d="M 12 14 L 14 14 L 14 18 L 12 18 Z" fill="white" opacity="0.3"/>
        <path d="M 18 14 L 20 14 L 20 18 L 18 18 Z" fill="white" opacity="0.3"/>
      </svg>
    `,
    emergency: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="6" y="10" width="20" height="12" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <polygon points="16,12 18,16 14,16" fill="white"/>
        <rect x="15" y="16" width="2" height="4" fill="white"/>
        <circle cx="16" cy="21" r="0.8" fill="white"/>
        <circle cx="10" cy="22" r="2" fill="white"/>
        <circle cx="22" cy="22" r="2" fill="white"/>
      </svg>
    `,
    gpu: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="8" y="8" width="16" height="16" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <rect x="10" y="10" width="5" height="5" fill="white" opacity="0.3"/>
        <rect x="17" y="10" width="5" height="5" fill="white" opacity="0.3"/>
        <rect x="10" y="17" width="5" height="5" fill="white" opacity="0.3"/>
        <rect x="17" y="17" width="5" height="5" fill="white" opacity="0.3"/>
        <circle cx="12" cy="24" r="1.5" fill="white"/>
        <circle cx="20" cy="24" r="1.5" fill="white"/>
      </svg>
    `,
    cargo_loader: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="4" y="14" width="18" height="8" rx="1" fill="${color}" stroke="white" stroke-width="1.5"/>
        <rect x="20" y="8" width="8" height="14" rx="1" fill="${color}" stroke="white" stroke-width="1.5"/>
        <rect x="6" y="16" width="4" height="4" fill="white" opacity="0.4"/>
        <rect x="11" y="16" width="4" height="4" fill="white" opacity="0.4"/>
        <circle cx="8" cy="22" r="2" fill="white"/>
        <circle cx="18" cy="22" r="2" fill="white"/>
      </svg>
    `,
    power_unit: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="7" y="10" width="18" height="12" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <polygon points="16,13 18,17 14,17" fill="white"/>
        <polygon points="16,17 14,21 18,21" fill="white"/>
        <circle cx="11" cy="22" r="2" fill="white"/>
        <circle cx="21" cy="22" r="2" fill="white"/>
      </svg>
    `,
    cleaning: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="6" y="10" width="20" height="12" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <circle cx="13" cy="14" r="2" fill="white" opacity="0.4"/>
        <circle cx="19" cy="14" r="2" fill="white" opacity="0.4"/>
        <circle cx="16" cy="18" r="2" fill="white" opacity="0.4"/>
        <circle cx="10" cy="22" r="2" fill="white"/>
        <circle cx="22" cy="22" r="2" fill="white"/>
      </svg>
    `,
    maintenance: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="6" y="10" width="20" height="12" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <rect x="12" y="13" width="2" height="6" fill="white" opacity="0.5"/>
        <circle cx="18" cy="16" r="2.5" stroke="white" stroke-width="1.5" fill="none"/>
        <circle cx="10" cy="22" r="2" fill="white"/>
        <circle cx="22" cy="22" r="2" fill="white"/>
      </svg>
    `,
    other: `
      <svg width="32" height="32" viewBox="0 0 32 32" xmlns="http://www.w3.org/2000/svg">
        <rect x="8" y="12" width="16" height="8" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <circle cx="12" cy="22" r="2" fill="white"/>
        <circle cx="20" cy="22" r="2" fill="white"/>
      </svg>
    `
  };
  return svgs[type] || svgs.other;
};

export const createVehicleIcon = (options: VehicleIconOptions): L.DivIcon => {
  const color = getStatusColor(options.status);
  const svg = getVehicleSVG(options.type, color);
  const pulseClass = options.status === 'alert' ? 'map-icon-pulse' : '';
  
  return L.divIcon({
    className: `vehicle-icon ${pulseClass}`,
    html: `<div class="vehicle-icon-wrapper">${svg}</div>`,
    iconSize: [32, 32],
    iconAnchor: [16, 16],
  });
};

export const createAircraftIcon = (options: AircraftIconOptions): L.DivIcon => {
  const color = getStatusColor(options.status);
  const pulseClass = options.status === 'alert' ? 'map-icon-pulse' : '';
  
  const svg = `
    <svg width="40" height="40" viewBox="0 0 40 40" xmlns="http://www.w3.org/2000/svg" style="transform: rotate(${options.heading}deg)">
      <g transform="translate(20,20)">
        <!-- Fuselage -->
        <ellipse cx="0" cy="0" rx="3" ry="10" fill="${color}" stroke="white" stroke-width="1.5"/>
        <!-- Wings -->
        <rect x="-15" y="-2" width="30" height="4" rx="2" fill="${color}" stroke="white" stroke-width="1.5"/>
        <!-- Tail -->
        <polygon points="0,-10 -4,-14 4,-14" fill="${color}" stroke="white" stroke-width="1.5"/>
        <!-- Nose -->
        <circle cx="0" cy="10" r="2" fill="white"/>
      </g>
    </svg>
  `;
  
  return L.divIcon({
    className: `aircraft-icon ${pulseClass}`,
    html: `<div class="aircraft-icon-wrapper">${svg}</div>`,
    iconSize: [40, 40],
    iconAnchor: [20, 20],
  });
};
