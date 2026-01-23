import { useEffect } from 'react';
import { useMap } from 'react-leaflet';

interface MapThemeControllerProps {
  theme: 'dark' | 'light';
}

const MapThemeController: React.FC<MapThemeControllerProps> = ({ theme }) => {
  const map = useMap();

  useEffect(() => {
    // Update map background color
    const container = map.getContainer();
    container.style.background = theme === 'dark' ? '#0f172a' : '#f8fafc';
  }, [theme, map]);

  return null;
};

export default MapThemeController;
