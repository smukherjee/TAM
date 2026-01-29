import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import MapToolbar from '../../components/Map/MapToolbar';

describe('MapToolbar', () => {
    const mockOnLayerToggle = vi.fn();
    const mockOnHeatmapModeChange = vi.fn();
    const mockOnOpenFilters = vi.fn();

    const defaultLayers = {
        flights: true,
        vehicles: false,
        assets: false,
        heatmap: false,
        zones: false,
        alerts: false
    };

    const defaultProps = {
        layers: defaultLayers,
        onLayerToggle: mockOnLayerToggle,
        onHeatmapModeChange: mockOnHeatmapModeChange,
        onOpenFilters: mockOnOpenFilters,
        alertCount: 5
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should render all layer toggle buttons', () => {
        render(<MapToolbar {...defaultProps} />);
        
        expect(screen.getByTitle('Flights')).toBeInTheDocument();
        expect(screen.getByTitle('Vehicles')).toBeInTheDocument();
        expect(screen.getByTitle('Assets')).toBeInTheDocument();
        expect(screen.getByTitle('Zones')).toBeInTheDocument();
    });

    it('should display alert count badge', () => {
        render(<MapToolbar {...defaultProps} />);
        
        expect(screen.getByText('5')).toBeInTheDocument();
    });

    it('should call onLayerToggle when layer button is clicked', () => {
        render(<MapToolbar {...defaultProps} />);
        
        const flightsButton = screen.getByTitle('Flights');
        fireEvent.click(flightsButton);
        
        expect(mockOnLayerToggle).toHaveBeenCalledWith('flights');
    });

    it('should show heatmap button', () => {
        render(<MapToolbar {...defaultProps} />);
        
        const heatmapText = screen.getByText('Heatmap');
        expect(heatmapText).toBeInTheDocument();
    });

    it('should open heatmap dropdown on click', () => {
        render(<MapToolbar {...defaultProps} heatmapMode={null} />);
        
        const heatmapButton = screen.getByText('Heatmap').closest('button');
        if (heatmapButton) {
            fireEvent.click(heatmapButton);
            expect(screen.getByText('Activity')).toBeInTheDocument();
            expect(screen.getByText('Violations')).toBeInTheDocument();
            expect(screen.getByText('Dwell Time')).toBeInTheDocument();
        }
    });

    it('should display active heatmap mode', () => {
        render(<MapToolbar {...defaultProps} heatmapMode="activity" />);
        
        expect(screen.getByText('Activity')).toBeInTheDocument();
    });

    it('should call onHeatmapModeChange when mode is selected', () => {
        render(<MapToolbar {...defaultProps} heatmapMode={null} />);
        
        const heatmapButton = screen.getByText('Heatmap').closest('button');
        if (heatmapButton) {
            fireEvent.click(heatmapButton);
            
            const activityOption = screen.getByText('Activity').closest('button');
            if (activityOption) {
                fireEvent.click(activityOption);
                expect(mockOnHeatmapModeChange).toHaveBeenCalledWith('activity');
            }
        }
    });

    it('should show active indicator for enabled layers', () => {
        const layers = { ...defaultLayers, flights: true };
        render(<MapToolbar {...defaultProps} layers={layers} />);
        
        const flightsButton = screen.getByTitle('Flights');
        // Check that it has the active classes
        expect(flightsButton.className).toContain('bg-gray-700');
    });
});
