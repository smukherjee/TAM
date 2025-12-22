import api from './api';

export interface Alert {
    id: string;
    icaoCode: string;
    sessionId: string;
    alertType: string;
    severity: string;
    message: string;
    isActive: boolean;
    createdAt: string;
}

export const getActiveAlerts = async (icaoCode: string = 'VIDP'): Promise<Alert[]> => {
    const response = await api.get('/alerts', {
        params: { icaoCode }
    });
    return response.data;
};
