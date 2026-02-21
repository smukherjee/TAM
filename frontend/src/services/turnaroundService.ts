import api from './api';

export interface TaskSummary {
    type: string;
    status: string;
}

export interface TurnaroundSessionSummary {
    id: string;
    flightId: string;
    standId: string;
    status: string;
    tasks: TaskSummary[];
}

export interface TaskDetail {
    id: string;
    taskType: string;
    status: string;
    plannedStart: string;
    plannedEnd: string;
    actualStart: string;
    actualEnd: string;
}

export interface TurnaroundSessionDetail {
    id: string;
    flightId: string;
    icaoCode: string;
    standId: string;
    status: string;
    sirt: string;
    eibt: string;
    aibt: string;
    tobt: string;
    tsat: string;
    aobt: string;
    tasks: TaskDetail[];
}

export const getTurnaroundSessions = async (activeOnly: boolean = true): Promise<TurnaroundSessionSummary[]> => {
    const response = await api.get('/turnaround/sessions', { params: { activeOnly } });
    return response.data;
};

export const getSessionDetails = async (id: string, icaoCode?: string): Promise<TurnaroundSessionDetail> => {
    const response = await api.get(`/turnaround/sessions/${id}`, {
        params: icaoCode ? { icaoCode } : {}
    });
    return response.data;
};
