import React, { useState, useEffect } from 'react';
import TurnaroundGrid from '../components/Turnaround/TurnaroundGrid';
import TurnaroundGridSkeleton from '../components/Turnaround/TurnaroundGridSkeleton';
import AlertSidebar from '../components/Turnaround/Alerts/AlertSidebar';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { getTurnaroundSessions, TurnaroundSessionSummary } from '../services/turnaroundService';
import { getActiveAlerts, Alert } from '../services/alertService';
import { useAuth } from '../context/AuthContext';

const TurnaroundPage: React.FC = () => {
    const { user } = useAuth();
    const [sessions, setSessions] = useState<TurnaroundSessionSummary[]>([]);
    const [alerts, setAlerts] = useState<Alert[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);
    const [showAlerts, setShowAlerts] = useState(true);

    const fetchData = async () => {
        try {
            const [sessionsData, alertsData] = await Promise.all([
                getTurnaroundSessions(),
                getActiveAlerts(user?.icaoCode)
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
    }, [user?.icaoCode]);

    return (
        <div className="flex flex-col h-full">
            <div className="flex justify-between items-center p-4 bg-gray-900 border-b border-gray-700">
                <h1 className="text-2xl font-bold text-white">Turnaround Operations</h1>
                <button
                    onClick={() => setShowAlerts(!showAlerts)}
                    className={`px-3 py-1 rounded border ${showAlerts ? 'bg-red-600 border-red-600 text-white' : 'border-gray-600 text-gray-400 hover:text-white'}`}
                >
                    Alerts ({alerts.length})
                </button>
            </div>

            <div className="flex flex-1 overflow-hidden">
                <div className="flex-1 overflow-auto bg-gray-900 relative">
                    {error ? (
                        <ErrorMessage message={error} onRetry={fetchData} />
                    ) : loading && sessions.length === 0 ? (
                        <TurnaroundGridSkeleton />
                    ) : (
                        <TurnaroundGrid sessions={sessions} />
                    )}
                </div>
                {showAlerts && <AlertSidebar alerts={alerts} />}
            </div>
        </div>
    );
};

export default TurnaroundPage;
