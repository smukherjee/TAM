import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert } from '../../../services/alertService';
import { ExternalLink } from 'lucide-react';

interface AlertSidebarProps {
    alerts: Alert[];
}

const AlertSidebar: React.FC<AlertSidebarProps> = ({ alerts }) => {
    const navigate = useNavigate();

    const handleAlertClick = (alert: Alert) => {
        const sessionId = alert.sessionId || alert.session?.id;
        if (sessionId) {
            navigate(`/turnaround/${sessionId}`);
        }
    };

    return (
        <div className="w-80 bg-gray-800 border-l border-gray-700 flex flex-col h-full">
            <div className="p-4 border-b border-gray-700">
                <h2 className="text-lg font-semibold text-white">Active Alerts</h2>
                <p className="text-xs text-gray-400 mt-1">Click to view turnaround details</p>
            </div>
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {alerts.length === 0 ? (
                    <div className="text-gray-400 text-center">No active alerts</div>
                ) : (
                    alerts.map(alert => {
                        const hasSession = alert.sessionId || alert.session?.id;
                        return (
                            <div 
                                key={alert.id} 
                                onClick={() => handleAlertClick(alert)}
                                className={`p-3 rounded border-l-4 ${getSeverityColor(alert.severity)} bg-gray-700 ${hasSession ? 'cursor-pointer hover:bg-gray-600 transition-colors' : ''}`}
                            >
                                <div className="flex justify-between items-start mb-1">
                                    <div className="flex items-center gap-2">
                                        <span className="font-bold text-white text-sm">{alert.alertType || alert.type}</span>
                                        {hasSession && <ExternalLink size={12} className="text-blue-400" />}
                                    </div>
                                    <span className="text-xs text-gray-400">{alert.createdAt || alert.timestamp ? new Date(alert.createdAt || alert.timestamp || '').toLocaleTimeString() : ''}</span>
                                </div>
                                <p className="text-sm text-gray-300">{alert.message}</p>
                                {hasSession && (
                                    <p className="text-xs text-blue-400 mt-2">View turnaround →</p>
                                )}
                            </div>
                        );
                    })
                )}
            </div>
        </div>
    );
};

const getSeverityColor = (severity: string) => {
    switch (severity) {
        case 'CRITICAL': return 'border-red-500';
        case 'WARNING': return 'border-yellow-500';
        case 'INFO': return 'border-blue-500';
        default: return 'border-gray-500';
    }
};

export default AlertSidebar;
