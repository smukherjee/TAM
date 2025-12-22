import React, { useState, useEffect } from 'react';
import TurnaroundGrid from '../components/Turnaround/TurnaroundGrid';
import TurnaroundGridSkeleton from '../components/Turnaround/TurnaroundGridSkeleton';
import AirportMap from '../components/Turnaround/Map/AirportMap';
import AlertSidebar from '../components/Turnaround/Alerts/AlertSidebar';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { getTurnaroundSessions, TurnaroundSessionSummary } from '../services/turnaroundService';
import { getActiveAlerts, Alert } from '../services/alertService';

const TurnaroundPage: React.FC = () => {
    const [viewMode, setViewMode] = useState<'grid' | 'map'>('grid');
    const [sessions, setSessions] = useState<TurnaroundSessionSummary[]>([]);
    const [alerts, setAlerts] = useState<Alert[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showAlerts, setShowAlerts] = useState(true);

    const fetchData = async () => {
        try {
            const [sessionsData, alertsData] = await Promise.all([
                getTurnaroundSessions(),
                getActiveAlerts()
            ]);
            setSessions(sessionsData);
            setAlerts(alertsData);
            setError(null);
        } catch (error) {
            console.error("Failed to fetch data", error);
            setError("Failed to load turnaround data. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 5000);
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="flex flex-col h-full">
            <div className="flex justify-between items-center p-4 bg-gray-900 border-b border-gray-700">
                <h1 className="text-2xl font-bold text-white">Turnaround Operations</h1>
                <div className="flex items-center space-x-4">
                    <div className="flex space-x-2 bg-gray-800 rounded p-1">
                        <button
                            onClick={() => setViewMode('grid')}
                            className={`px-3 py-1 rounded ${viewMode === 'grid' ? 'bg-blue-600 text-white' : 'text-gray-400 hover:text-white'}`}
                        >
                            Grid
                        </button>
                        <button
                            onClick={() => setViewMode('map')}
                            className={`px-3 py-1 rounded ${viewMode === 'map' ? 'bg-blue-600 text-white' : 'text-gray-400 hover:text-white'}`}
                        >
                            Map
                        </button>
                    </div>
                    <button
                        onClick={() => setShowAlerts(!showAlerts)}
                        className={`px-3 py-1 rounded border ${showAlerts ? 'bg-red-600 border-red-600 text-white' : 'border-gray-600 text-gray-400 hover:text-white'}`}
                    >
                        Alerts ({alerts.length})
                    </button>
                </div>
            </div>

            <div className="flex flex-1 overflow-hidden">
                <div className="flex-1 overflow-auto bg-gray-900 relative">
                    {error ? (
                        <ErrorMessage message={error} onRetry={fetchData} />
                    ) : loading && sessions.length === 0 ? (
                        viewMode === 'grid' ? <TurnaroundGridSkeleton /> : <div className="p-4 text-white">Loading Map...</div>
                    ) : viewMode === 'grid' ? (
                        <TurnaroundGrid sessions={sessions} />
                    ) : (
                        <AirportMap sessions={sessions} />
                    )}
                </div>
                {showAlerts && <AlertSidebar alerts={alerts} />}
            </div>
        </div>
    );
};

export default TurnaroundPage;
