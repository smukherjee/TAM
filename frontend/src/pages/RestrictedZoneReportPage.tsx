import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { webSocketService } from '../services/WebSocketService';
import {
    fetchZoneViolations,
    acknowledgeViolation,
    fetchViolationStatistics,
    downloadViolationsExport
} from '../services/trackingService';
import { ZoneViolation, ViolationFilters, ViolationStatistics, SEVERITY_COLORS } from '../types/tracking';
import ViolationTable from '../components/Tracking/ViolationTable';
import ViolationFiltersPanel from '../components/Tracking/ViolationFilters';
import AcknowledgeViolationModal from '../components/Tracking/AcknowledgeViolationModal';
import { 
    AlertOctagon, 
    Download, 
    RefreshCw, 
    AlertTriangle,
    CheckCircle,
    Loader2
} from 'lucide-react';
import { toast } from 'react-hot-toast';

/**
 * RestrictedZoneReportPage - Display and manage zone violations
 * Feature: 005-asset-tracking-security
 * Phase 9: Zone Violations Report
 */
const RestrictedZoneReportPage: React.FC = () => {
    const { user } = useAuth();
    const queryClient = useQueryClient();
    const tenantCode = user?.icaoCode ?? '';

    // State
    const [filters, setFilters] = useState<ViolationFilters>({
        tenantCode,
        page: 0,
        size: 20,
        sort: 'timestamp,desc'
    });
    const [selectedViolation, setSelectedViolation] = useState<ZoneViolation | null>(null);
    const [showAcknowledgeModal, setShowAcknowledgeModal] = useState(false);

    // Calculate date range for statistics (last 30 days)
    const now = new Date();
    const thirtyDaysAgo = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);

    // Fetch violations
    const { data: violationsData, isLoading, error, refetch } = useQuery({
        queryKey: ['violations', filters],
        queryFn: () => fetchZoneViolations(filters),
        enabled: Boolean(tenantCode),
        refetchInterval: 30000 // Refetch every 30 seconds
    });

    // Fetch statistics
    const { data: statistics } = useQuery<ViolationStatistics>({
        queryKey: ['violationStats', tenantCode],
        queryFn: () => fetchViolationStatistics(
            tenantCode,
            thirtyDaysAgo.toISOString(),
            now.toISOString()
        ),
        enabled: Boolean(tenantCode),
        refetchInterval: 60000
    });

    useEffect(() => {
        setFilters(prev => ({ ...prev, tenantCode }));
    }, [tenantCode]);

    // Acknowledge mutation
    const acknowledgeMutation = useMutation({
        mutationFn: ({ id, notes }: { id: string; notes: string }) => 
            acknowledgeViolation(id, { notes }),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['violations'] });
            queryClient.invalidateQueries({ queryKey: ['violationStats'] });
            setShowAcknowledgeModal(false);
            setSelectedViolation(null);
            toast.success('Violation acknowledged successfully');
        },
        onError: (error: Error) => {
            toast.error(`Failed to acknowledge: ${error.message}`);
        }
    });

    // WebSocket subscription for real-time updates
    useEffect(() => {
        if (!tenantCode) return;
        const subscription = webSocketService.subscribeToViolations(tenantCode, (newViolation) => {
            // Show toast for new violations
            toast.custom((t: { visible: boolean; id: string }) => (
                <div className={`${t.visible ? 'animate-enter' : 'animate-leave'} 
                    max-w-md w-full bg-white shadow-lg rounded-lg pointer-events-auto 
                    flex ring-1 ring-black ring-opacity-5`}>
                    <div className="flex-1 w-0 p-4">
                        <div className="flex items-start">
                            <AlertOctagon className="h-6 w-6 text-red-500" />
                            <div className="ml-3 flex-1">
                                <p className="text-sm font-medium text-gray-900">
                                    New Zone Violation
                                </p>
                                <p className="mt-1 text-sm text-gray-500">
                                    {newViolation.assetIdentifier} entered {newViolation.zoneName}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            ), { duration: 5000 });
            
            // Refetch data
            queryClient.invalidateQueries({ queryKey: ['violations'] });
            queryClient.invalidateQueries({ queryKey: ['violationStats'] });
        });

        return () => subscription.unsubscribe();
    }, [tenantCode, queryClient]);

    // Handle filter changes
    const handleFilterChange = (newFilters: Partial<ViolationFilters>) => {
        setFilters(prev => ({ ...prev, ...newFilters, page: 0 }));
    };

    // Handle page change
    const handlePageChange = (page: number) => {
        setFilters(prev => ({ ...prev, page }));
    };

    // Handle acknowledge click
    const handleAcknowledge = (violation: ZoneViolation) => {
        setSelectedViolation(violation);
        setShowAcknowledgeModal(true);
    };

    // Handle export
    const handleExport = async (format: 'csv' | 'pdf') => {
        try {
            await downloadViolationsExport(filters, format);
            toast.success(`Violations exported as ${format.toUpperCase()}`);
        } catch (error) {
            toast.error('Failed to export violations');
        }
    };

    return (
        <div className="h-full flex flex-col bg-gray-900">
            {/* Header */}
            <div className="bg-gray-900 border-b border-gray-700 px-6 py-4">
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <AlertOctagon className="h-8 w-8 text-red-500" />
                        <div>
                            <h1 className="text-2xl font-bold text-white">Zone Violations</h1>
                            <p className="text-sm text-gray-400">
                                Monitor and manage restricted zone violations
                            </p>
                        </div>
                    </div>
                    <div className="flex items-center space-x-3">
                        <button
                            onClick={() => refetch()}
                            className="inline-flex items-center px-3 py-2 border border-gray-600 
                                text-sm font-medium rounded-md text-gray-300 bg-gray-800 
                                hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 
                                focus:ring-blue-500"
                        >
                            <RefreshCw className="h-4 w-4 mr-2" />
                            Refresh
                        </button>
                        <div className="relative inline-block">
                            <button
                                onClick={() => handleExport('csv')}
                                className="inline-flex items-center px-3 py-2 border border-gray-600 
                                    text-sm font-medium rounded-md text-gray-300 bg-gray-800 
                                    hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-offset-2 
                                    focus:ring-blue-500"
                            >
                                <Download className="h-4 w-4 mr-2" />
                                Export
                            </button>
                        </div>
                    </div>
                </div>

                {/* Statistics Cards */}
                <div className="mt-4 grid grid-cols-4 gap-4">
                    <div className="bg-gray-800 rounded-lg border border-gray-700 p-4">
                        <div className="flex items-center">
                            <div className="p-2 bg-red-900/50 rounded-lg">
                                <AlertOctagon className="h-5 w-5 text-red-500" />
                            </div>
                            <div className="ml-3">
                                <p className="text-sm text-gray-400">Total Violations</p>
                                <p className="text-2xl font-semibold text-white">
                                    {statistics?.total || 0}
                                </p>
                            </div>
                        </div>
                    </div>
                    <div className="bg-gray-800 rounded-lg border border-gray-700 p-4">
                        <div className="flex items-center">
                            <div className="p-2 bg-orange-900/50 rounded-lg">
                                <AlertTriangle className="h-5 w-5 text-orange-500" />
                            </div>
                            <div className="ml-3">
                                <p className="text-sm text-gray-400">Unacknowledged</p>
                                <p className="text-2xl font-semibold text-white">
                                    {statistics?.unacknowledged || 0}
                                </p>
                            </div>
                        </div>
                    </div>
                    <div className="bg-gray-800 rounded-lg border border-gray-700 p-4">
                        <div className="flex items-center">
                            <div className="p-2 bg-red-900/50 rounded-lg">
                                <AlertOctagon className="h-5 w-5" style={{ color: SEVERITY_COLORS.CRITICAL }} />
                            </div>
                            <div className="ml-3">
                                <p className="text-sm text-gray-400">Critical</p>
                                <p className="text-2xl font-semibold text-white">
                                    {statistics?.bySeverity?.CRITICAL || 0}
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
                    <ViolationFiltersPanel
                        filters={filters}
                        onFilterChange={handleFilterChange}
                        tenantCode={tenantCode}
                    />
                </div>

                {/* Table */}
                <div className="flex-1 p-4 overflow-auto bg-gray-900">
                    {isLoading ? (
                        <div className="flex items-center justify-center h-64">
                            <Loader2 className="h-8 w-8 animate-spin text-blue-500" />
                            <span className="ml-2 text-gray-400">Loading violations...</span>
                        </div>
                    ) : error ? (
                        <div className="flex items-center justify-center h-64">
                            <AlertTriangle className="h-8 w-8 text-red-500" />
                            <span className="ml-2 text-red-400">Failed to load violations</span>
                        </div>
                    ) : violationsData && violationsData.content.length > 0 ? (
                        <ViolationTable
                            violations={violationsData.content}
                            totalPages={violationsData.totalPages}
                            currentPage={filters.page || 0}
                            onPageChange={handlePageChange}
                            onAcknowledge={handleAcknowledge}
                        />
                    ) : (
                        <div className="flex flex-col items-center justify-center h-64 text-gray-400">
                            <CheckCircle className="h-12 w-12 text-green-500 mb-3" />
                            <p className="text-lg font-medium text-white">No violations found</p>
                            <p className="text-sm">All zones are secure</p>
                        </div>
                    )}
                </div>
            </div>

            {/* Acknowledge Modal */}
            {showAcknowledgeModal && selectedViolation && (
                <AcknowledgeViolationModal
                    violation={selectedViolation}
                    isOpen={showAcknowledgeModal}
                    onClose={() => {
                        setShowAcknowledgeModal(false);
                        setSelectedViolation(null);
                    }}
                    onAcknowledge={(notes) => 
                        acknowledgeMutation.mutate({ id: selectedViolation.id, notes })
                    }
                    isSubmitting={acknowledgeMutation.isPending}
                />
            )}
        </div>
    );
};

export default RestrictedZoneReportPage;
