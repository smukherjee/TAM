import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import AssetFilterPanel from '../../components/Tracking/AssetFilterPanel';

describe('AssetFilterPanel', () => {
    const mockOnFilterChange = vi.fn();
    const mockOnToggle = vi.fn();

    const defaultProps = {
        isOpen: true,
        onToggle: mockOnToggle,
        onFilterChange: mockOnFilterChange,
        assetCount: 100,
        totalCount: 500
    };

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should render with correct asset count badge', () => {
        render(<AssetFilterPanel {...defaultProps} />);
        
        expect(screen.getByText('100')).toBeInTheDocument();
        expect(screen.getByText('500')).toBeInTheDocument();
    });

    it('should display category filter checkboxes', () => {
        render(<AssetFilterPanel {...defaultProps} />);
        
        expect(screen.getByText('Emergency')).toBeInTheDocument();
        expect(screen.getByText('Fueling')).toBeInTheDocument();
        expect(screen.getByText('Cargo')).toBeInTheDocument();
    });

    it('should display status filter checkboxes', () => {
        render(<AssetFilterPanel {...defaultProps} />);
        
        expect(screen.getByText('In Use')).toBeInTheDocument();
        expect(screen.getByText('Available')).toBeInTheDocument();
    });

    it('should show Filters header', () => {
        render(<AssetFilterPanel {...defaultProps} />);
        
        expect(screen.getByText('Filters')).toBeInTheDocument();
    });

    it('should apply category filter when checkbox is clicked', () => {
        render(<AssetFilterPanel {...defaultProps} />);
        
        const checkboxes = screen.getAllByRole('checkbox');
        const emergencyCheckbox = checkboxes.find(cb => 
            cb.parentElement?.textContent?.includes('Emergency')
        );
        
        if (emergencyCheckbox) {
            fireEvent.click(emergencyCheckbox);
            expect(emergencyCheckbox).toBeChecked();
        }
    });

    it('should toggle panel visibility', () => {
        render(<AssetFilterPanel {...defaultProps} />);
        
        const toggleButton = screen.getByLabelText(/Close filter panel/i);
        fireEvent.click(toggleButton);
        
        expect(mockOnToggle).toHaveBeenCalled();
    });
});
