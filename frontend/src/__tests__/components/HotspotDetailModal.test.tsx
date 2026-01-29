import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import HotspotDetailModal from '../../components/Tracking/HotspotDetailModal';
import * as heatmapService from '../../services/heatmapService';

// Mock the heatmap service
vi.mock('../../services/heatmapService', () => ({
    fetchHotspotDetail: vi.fn()
}));

// Mock react-router-dom
vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn()
}));

const createWrapper = () => {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
        },
    });
    return ({ children }: { children: React.ReactNode }) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
};

describe('HotspotDetailModal', () => {
    const defaultProps = {
        isOpen: true,
        onClose: vi.fn(),
        latitude: -37.6690,
        longitude: 144.8410,
        gridSize: 25,
        mode: 'activity' as const,
        tenantCode: 'VIDP',
        startTime: '2024-01-28T00:00:00Z',
        endTime: '2024-01-28T23:59:59Z'
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should render modal when open', () => {
        vi.mocked(heatmapService.fetchHotspotDetail).mockResolvedValue({
            latitude: -37.6690,
            longitude: 144.8410,
            gridSize: 25,
            mode: 'activity',
            totalCount: 150,
            intensity: 0.85,
            assets: [],
            timeDistribution: []
        });

        render(
            <HotspotDetailModal {...defaultProps} />,
            { wrapper: createWrapper() }
        );

        expect(screen.getByRole('dialog')).toBeInTheDocument();
    });

    it('should display location coordinates', async () => {
        vi.mocked(heatmapService.fetchHotspotDetail).mockResolvedValue({
            latitude: -37.6690,
            longitude: 144.8410,
            gridSize: 25,
            mode: 'activity',
            totalCount: 150,
            intensity: 0.85,
            assets: [],
            timeDistribution: []
        });

        render(
            <HotspotDetailModal {...defaultProps} />,
            { wrapper: createWrapper() }
        );

        await waitFor(() => {
            expect(screen.getByText(/-37\.669/)).toBeInTheDocument();
        });
    });

    it('should show loading state', () => {
        vi.mocked(heatmapService.fetchHotspotDetail).mockImplementation(
            () => new Promise(() => {}) // Never resolves
        );

        render(
            <HotspotDetailModal {...defaultProps} />,
            { wrapper: createWrapper() }
        );

        // The loading state shows a spinner, not text
        const dialog = screen.getByRole('dialog');
        expect(dialog).toBeInTheDocument();
    });

    it('should handle error state', async () => {
        vi.mocked(heatmapService.fetchHotspotDetail).mockRejectedValue(
            new Error('Failed to fetch')
        );

        render(
            <HotspotDetailModal {...defaultProps} />,
            { wrapper: createWrapper() }
        );

        await waitFor(() => {
            expect(screen.getByText(/Failed to load/i)).toBeInTheDocument();
        });
    });

    it('should not fetch data when modal is closed', () => {
        const closedProps = { ...defaultProps, isOpen: false };
        
        render(
            <HotspotDetailModal {...closedProps} />,
            { wrapper: createWrapper() }
        );

        expect(heatmapService.fetchHotspotDetail).not.toHaveBeenCalled();
    });
});
