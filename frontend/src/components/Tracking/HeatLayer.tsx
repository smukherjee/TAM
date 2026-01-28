import { useEffect } from 'react';
import { useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet.heat';

interface HeatLayerProps {
    data: [number, number, number][]; // [lat, lng, intensity]
    gridSize?: number;
    intensity?: number;
}

/**
 * HeatLayer - Renders a heatmap overlay on Leaflet map
 * Uses leaflet.heat plugin for visualization
 */
const HeatLayer: React.FC<HeatLayerProps> = ({ 
    data, 
    gridSize = 25,
    intensity = 75 
}) => {
    const map = useMap();

    useEffect(() => {
        if (!data || data.length === 0) return;

        // Create heat layer with configuration
        const heat = (L as any).heatLayer(data, {
            radius: gridSize,
            blur: 15,
            maxZoom: 18,
            max: 1.0,
            gradient: {
                0.0: '#000033',
                0.25: '#0000ff',
                0.5: '#00ff00',
                0.75: '#ffff00',
                1.0: '#ff0000'
            },
            minOpacity: 0.4
        });

        heat.addTo(map);

        return () => {
            map.removeLayer(heat);
        };
    }, [map, data, gridSize, intensity]);

    return null;
};

export default HeatLayer;
