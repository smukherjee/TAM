import React from 'react';
import { Alert } from '../../../services/alertService';

interface AlertSidebarProps {
    alerts: Alert[];
}

const AlertSidebar: React.FC<AlertSidebarProps> = ({ alerts }) => {
    return (
        <div className="w-80 bg-gray-800 border-l border-gray-700 flex flex-col h-full">
            <div className="p-4 border-b border-gray-700">
                <h2 className="text-lg font-semibold text-white">Active Alerts</h2>
            </div>
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
                {alerts.length === 0 ? (
                    <div className="text-gray-400 text-center">No active alerts</div>
                ) : (
                    alerts.map(alert => (
                        <div key={alert.id} className={`p-3 rounded border-l-4 ${getSeverityColor(alert.severity)} bg-gray-700`}>
                            <div className="flex justify-between items-start mb-1">
                                <span className="font-bold text-white text-sm">{alert.alertType}</span>
                                <span className="text-xs text-gray-400">{new Date(alert.createdAt).toLocaleTimeString()}</span>
                            </div>
                            <p className="text-sm text-gray-300">{alert.message}</p>
                        </div>
                    ))
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
