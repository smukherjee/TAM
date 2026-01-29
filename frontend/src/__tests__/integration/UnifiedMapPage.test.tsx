import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import UnifiedMapPage from '../../pages/UnifiedMapPage';

// Mock AuthContext
vi.mock('../../context/AuthContext', () => ({
    useAuth: () => ({
        user: { id: '1', email: 'test@example.com', tenantCode: 'VIDP' },
        isAuthenticated: true,
        login: vi.fn(),
        logout: vi.fn(),
    }),
}));

// Mock Leaflet map
vi.mock('react-leaflet', () => ({
    MapContainer: ({ children }: any) => <div data-testid="map-container">{children}</div>,
    TileLayer: () => <div data-testid="tile-layer" />,
    Circle: () => <div data-testid="circle" />,
    Marker: () => <div data-testid="marker" />,
    Popup: () => <div data-testid="popup" />,
    useMap: () => ({
        setView: vi.fn(),
        flyTo: vi.fn(),
        getContainer: vi.fn(() => ({
            style: {},
        })),
    }),
}));

const createWrapper = () => {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
        },
    });
    return ({ children }: { children: React.ReactNode }) => (
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>{children}</BrowserRouter>
        </QueryClientProvider>
    );
};

describe('UnifiedMapPage Integration', () => {
    it('should render the page with map container', () => {
        render(<UnifiedMapPage />, { wrapper: createWrapper() });
        
        expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });

    it('should render map toolbar', () => {
        render(<UnifiedMapPage />, { wrapper: createWrapper() });
        
        // MapToolbar should be present
        const toolbar = screen.queryByTitle('Flights') || screen.queryByTitle('Vehicles');
        expect(toolbar).toBeTruthy();
    });
});
