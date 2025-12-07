import React from 'react';
import './AlertList.css';

interface Alert {
    alertId: string;
    type: string;
    entityId: string;
    value: number;
    timestamp: string;
    latitude: number;
    longitude: number;
}

interface AlertListProps {
    alerts: Alert[];
}

const AlertList: React.FC<AlertListProps> = ({ alerts }) => {
    return (
        <div className="alert-list">
            <h3>Alerts</h3>
            {alerts.length === 0 ? (
                <p>No alerts.</p>
            ) : (
                <ul>
                    {alerts.map(alert => (
                        <li key={alert.alertId} className="alert-item">
                            <strong>{alert.type}</strong>: {alert.entityId}
                            <br />
                            Value: {alert.value.toFixed(1)}
                            <br />
                            Location: {alert.latitude.toFixed(4)}, {alert.longitude.toFixed(4)}
                            <br />
                            Time: {new Date(alert.timestamp).toLocaleTimeString()}
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

export default AlertList;
