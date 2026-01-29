import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { webSocketService } from '../services/WebSocketService';
import {
    fetchMovementDiscrepancies,
    acknowledgeDiscrepancy,
    fetchDiscrepancyStatistics,
    downloadDiscrepanciesExport
} from '../services/trackingService';
import { 
    MovementDiscrepancy, 
    DiscrepancyFilters, 
    DiscrepancyStatistics, 
    DISCREPANCY_TYPE_LABELS 
} from '../types/tracking';
import DiscrepancyTable from '../components/Tracking/DiscrepancyTable';
import DiscrepancyFiltersPanel from '../components/Tracking/DiscrepancyFilters';
import DiscrepancyMapView from '../components/Tracking/DiscrepancyMapView';
import AcknowledgeDiscrepancyModal from '../components/Tracking/AcknowledgeDiscrepancyModal';
import { 
    AlertTriangle, 
    Download, 
    RefreshCw, 
    Table,
    Map,
    CheckCircle,
    Loader2,
    Activity
} from 'lucide-react';
import { toast } from 'react-hot-toast';

type ViewMode = 'table' | 'map';

/**
 * MovementDiscrepancyReportPage - Display and manage movement discrepancies
 * Feature: 005-asset-tracking-security
 * Phase 10: Movement Discrepancy Report
 */
const MovementDiscrepancyReportPage: React.FC = () => {
    const { user } = useAuth();
    const queryClient = useQueryClient();
    const tenantCode = user?.icaoCode || 'YBBN';

    // State
    const [viewMode, setViewMode] = useState<ViewMode>('table');
    const [filters, setFilters] = useState<DiscrepancyFilters>({
        tenantCode,
        page: 0,
        size: 20,
        sort: 'timestamp,desc'
    });
    const [selectedDiscrepancy, setSelectedDiscrepancy] = useState<MovementDiscrepancy | null>(null);
    const [showAcknowledgeModal, setShowAcknowledgeModal] = useState(false);

    // Calculate date range for statistics (last 30 days)
    const now = new Date();
    const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);

    // Fetch discrepancies
    const { data: discrepanciesData, isLoading, error, refetch } = useQuery({
        queryKey: ['discrepancies', filters],
        queryFn: () => fetchMovementDiscrepancies(filters),
        refetchInterval: 30000
    });

    // Fetch statistics
    const { data: statistics } = useQuery<DiscrepancyStatistics>({
        queryKey: ['discrepancyStats', tenantCode],
        queryFn: () => fetchDiscrepancyStatistics(
            tenantCode,
            thirtyDaysAgo.toISOString(),
            now.toISOString()
        ),
        refetchInterval: 60000
    });

    // Acknowledge mutation
    const acknowledgeMutation = useMutation({
        mutationFn: ({ id, notes }: { id: string; notes: string }) => 
            acknowledgeDiscrepancy(id, { notes }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['discrepancies'] });
            queryClient.invalidateQueries({ queryKey: ['discrepancyStats'] });
            setShowAcknowledgeModal(false);
            setSelectedDiscrepancy(null);
            toast.success('Discrepancy acknowledged successfully');
        },
        onError: (error: Error) => {
            toast.error(`Failed to acknowledge: ${error.message}`);
        }
    });

    // WebSocket subscription for real-time updates
    useEffect(() => {
        const subscription = webSocketService.subscribeToDiscrepancies(tenantCode, (newDiscrepancy) => {
            toast.custom((t: { visible: boolean; id: string }) => (
                <div className={`${t.visible ? 'animate-enter' : 'animate-leave'} 
                    max-w-md w-full bg-white shadow-lg rounded-lg pointer-events-auto 
                    flex ring-1 ring-black ring-opacity-5`}>
                    <div className="flex-1 w-0 p-4">
                        <div className="flex items-start">
                            <AlertTriangle className="h-6 w-6 text-orange-500" />
                            <div className="ml-3 flex-1">
                                <p className="text-sm font-medium text-gray-900">
                                    New Movement Discrepancy
                                </p>
                                <p className="mt-1 text-sm text-gray-500">
                                    {newDiscrepancy.assetIdentifier}: {DISCREPANCY_TYPE_LABELS[newDiscrepancy.discrepancyType as keyof typeof DISCREPANCY_TYPE_LABELS]}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            ), { duration: 5000 });
            
            queryClient.invalidateQueries({ queryKey: ['discrepancies'] });
            queryClient.invalidateQueries({ queryKey: ['discrepancyStats'] });
        });

        return () => subscription.unsubscribe();
    }, [tenantCode, queryClient]);

    // Handle filter changes
    const handleFilterChange = (newFilters: Partial<DiscrepancyFilters>) => {
        setFilters(prev => ({ ...prev, ...newFilters, page: 0 }));
    };

    // Handle page change
    const handlePageChange = (page: number) => {
        setFilters(prev => ({ ...prev, page }));
    };

    // Handle acknowledge click
    const handleAcknowledge = (discrepancy: MovementDiscrepancy) => {
        setSelectedDiscrepancy(discrepancy);
        setShowAcknowledgeModal(true);
    };

    // Handle view on map
    const handleViewOnMap = (discrepancy: MovementDiscrepancy) => {
        setSelectedDiscrepancy(discrepancy);
        setViewMode('map');
    };

    // Handle export
    const handleExport = async (format: 'csv' | 'pdf') => {
        try {
            await downloadDiscrepanciesExport(filters, format);
            toast.success(`Discrepancies exported as ${format.toUpperCase()}`);
        } catch (error) {
            toast.error('Failed to export discrepancies');
        }
    };

    return (
        <div className="h-full flex flex-col bg-gray-900">
            {/* Header */}
            <div className="bg-gray-900 border-b border-gray-700 px-6 py-4">
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <AlertTriangle className="h-8 w-8 text-orange-500" />
                        <div>
                            <h1 className="text-2xl font-bold text-white">Movement Discrepancies</h1>
                            <p className="text-sm text-gray-400">
                                Detect and investigate asset movement anomalies
                            </p>
                        </div>
                    </div>
                    <div className="flex items-center space-x-3">
                        {/* View Toggle */}
                        <div className="inline-flex rounded-md" role="group">
                            <button
                                onClick={() => setViewMode('table')}
                                className={`inline-flex items-center px-3 py-2 text-sm font-medium 
                                    rounded-l-md border ${viewMode === 'table' 
                                        ? 'bg-blue-600 text-white border-blue-600' 
                                        : 'bg-gray-800 text-gray-300 border-gray-600 hover:bg-gray-700'}`}
                            >
                                <Table className="h-4 w-4 mr-2" />
                                Table
                            </button>
                            <button
                                onClick={() => setViewMode('map')}
                                className={`inline-flex items-center px-3 py-2 text-sm font-medium 
                                    rounded-r-md border-t border-b border-r ${viewMode === 'map' 
                                        ? 'bg-blue-600 text-white border-blue-600' 
                                        : 'bg-gray-800 text-gray-300 border-gray-600 hover:bg-gray-700'}`}
                            >
                                <Map className="h-4 w-4 mr-2" />
                                Map
                            </button>
                        </div>
                        <button
                            onClick={() => refetch()}
                            className="inline-flex items-center px-3 py-2 border border-gray-600 
                                text-sm font-medium rounded-md text-gray-300 bg-gray-800 
                                hover:bg-gray-700"
                        >
                            <RefreshCw className="h-4 w-4 mr-2" />
                            Refresh
                        </button>
                        <button
                            onClick={() => handleExport('csv')}
                            className="inline-flex items-center px-3 py-2 border border-gray-600 
                                text-sm font-medium rounded-md text-gray-300 bg-gray-800 
                                hover:bg-gray-700"
                        >
                            <Download className="h-4 w-4 mr-2" />
                            Export
                        </button>
                    </div>
                </div>

                {/* Statistics Cards */}
                <div className="mt-4 grid grid-cols-5 gap-4">
                    <div className="bg-gray-800 rounded-lg border border-gray-700 p-4">
                        <div className="flex items-center">
                            <div className="p-2 bg-orange-900/50 rounded-lg">
                                <AlertTriangle className="h-5 w-5 text-orange-500" />
                            </div>
                            <div className="ml-3">
                                <p className="text-sm text-gray-400">Total</p>
                                <p className="text-2xl font-semibold text-white">
                                    {statistics?.total || 0}
                                </p>
                            </div>
                        </div>
                    </div>
                    <div className="bg-gray-800 rounded-lg border border-gray-700 p-4">
                        <div className="flex items-center">
                            <div className="p-2 bg-red-900/50 rounded-lg">
                                <Activity className="h-5 w-5 text-red-500" />
                            </div>
                            <div className="ml-3">
                                <p className="text-sm text-gray-400">Unexpected Movement</p>
                                <p className="text-2xl font-semibold text-white">
                                    {statistics?.byType?.UNEXPECTED_MOVEMENT || 0}
                                </p>
                            </div>
                        </div>
                    </div>
                    <div className="bg-gray-800 rounded-lg border border-gray-700 p-4">
                        <div className="flex items-center">
                            <div className="p-2 bg-yellow-900/50 rounded-lg">
                                <AlertTriangle className="h-5 w-5 text-yellow-500" />
                            </div>
                            <div className="ml-3">
                                <p className="text-sm text-gray-400">Status Mismatch</p>
                                <p className="text-2xl font-semibold text-white">
                                    {statistics?.byType?.STATUS_MISMATCH || 0}
                                </p>
                            </div>
                        </div>
                    </div>
                    <div className="bg-gray-800 rounded-lg border border-gray-700 p-4">
                        <div className="flex items-center">
                            <div className="p-2 bg-purple-900/50 rounded-lg">
                                <Activity className="h-5 w-5 text-purple-500" />
                            </div>
                            <div className="ml-3">
                                <p className="text-sm text-gray-400">Speed Anomaly</p>
                                <p className="text-2xl font-semibold text-white">
                                    {statistics?.byType?.SPEED_ANOMALY || 0}
                                </p>
                            </div>
                        </div>
                    </div>
                    <div className="bg-gray-800 rounded-lg border border-gray-700 p-4">
                        <div className="flex items-center">
                            <div className="p-2 bg-green-900/50 rounded-lg">
                                <CheckCircle className="h-5 w-5 text-green-500" />
                            </div>
                            <div className="ml-3">
                                <p className="text-sm text-gray-400">Acknowledged</p>
                                <p className="text-2xl font-semibold text-white">
                                    {(statistics?.total || 0) - (statistics?.unacknowledged || 0)}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 flex overflow-hidden">
                {/* Filters Panel */}
                <div className="w-72 bg-gray-800 border-r border-gray-700 p-4 overflow-y-auto">
                    <DiscrepancyFiltersPanel
                        filters={filters}
                        onFilterChange={handleFilterChange}
                        tenantCode={tenantCode}
                    />
                </div>

                {/* Content Area */}
                <div className="flex-1 p-4 overflow-auto bg-gray-900">
                    {isLoading ? (
                        <div className="flex items-center justify-center h-64">
                            <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
                            <span className="ml-2 text-gray-400">Loading discrepancies...</span>
                        </div>
                    ) : error ? (
                        <div className="flex items-center justify-center h-64">
                            <AlertTriangle className="h-8 w-8 text-red-500" />
                            <span className="ml-2 text-red-400">Failed to load discrepancies</span>
                        </div>
                    ) : viewMode === 'table' ? (
                        discrepanciesData && discrepanciesData.content.length > 0 ? (
                            <DiscrepancyTable
                                discrepancies={discrepanciesData.content}
                                totalPages={discrepanciesData.totalPages}
                                currentPage={filters.page || 0}
                                onPageChange={handlePageChange}
                                onAcknowledge={handleAcknowledge}
                                onViewOnMap={handleViewOnMap}
                            />
                        ) : (
                            <div className="flex flex-col items-center justify-center h-64 text-gray-400">
                                <CheckCircle className="h-12 w-12 text-green-500 mb-3" />
                                <p className="text-lg font-medium text-white">No discrepancies found</p>
                                <p className="text-sm">All asset movements are normal</p>
                            </div>
                        )
                    ) : (
                        <DiscrepancyMapView
                            discrepancies={discrepanciesData?.content || []}
                            selectedDiscrepancy={selectedDiscrepancy}
                            onSelectDiscrepancy={setSelectedDiscrepancy}
                            onAcknowledge={handleAcknowledge}
                            tenantCode={tenantCode}
                        />
                    )}
                </div>
            </div>

            {/* Acknowledge Modal */}
            {showAcknowledgeModal && selectedDiscrepancy && (
                <AcknowledgeDiscrepancyModal
                    discrepancy={selectedDiscrepancy}
                    isOpen={showAcknowledgeModal}
                    onClose={() => {
                        setShowAcknowledgeModal(false);
                        setSelectedDiscrepancy(null);
                    }}
                    onAcknowledge={(notes) => 
                        acknowledgeMutation.mutate({ id: selectedDiscrepancy.id, notes })
                    }
                    isSubmitting={acknowledgeMutation.isPending}
                />
            )}
        </div>
    );
};

export default MovementDiscrepancyReportPage;
