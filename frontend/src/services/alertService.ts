import api from './api';

export interface Alert {
    id: string;
    tenantCode?: string;   // Backend uses tenantCode
    icaoCode?: string;     // Frontend alias (may need to map from tenantCode)
    session?: { id: string };  // Backend returns session object
    sessionId?: string;    // Convenience alias
    type: string;          // Backend uses 'type' not 'alertType'
    alertType?: string;    // Frontend alias
    severity: string;
    message: string;
    isActive: boolean;
    timestamp?: string;    // Backend uses timestamp
    createdAt?: string;    // Frontend alias
}

export const getActiveAlerts = async (icaoCode?: string): Promise<Alert[]> => {
    const response = await api.get('/alerts', {
        params: icaoCode ? { icaoCode } : {}
    });
    // Map backend fields to frontend aliases for convenience
    return response.data.map((alert: Alert) => ({
        ...alert,
        alertType: alert.type,
        icaoCode: alert.tenantCode,
        sessionId: alert.session?.id,
        createdAt: alert.timestamp
    }));
};
