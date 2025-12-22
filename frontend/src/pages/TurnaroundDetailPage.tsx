import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getSessionDetails, TurnaroundSessionDetail } from '../services/turnaroundService';
import { GanttChart } from '../components/Turnaround/Detail/GanttChart';
import { VideoTimeline } from '../components/Turnaround/Detail/VideoTimeline';
import TurnaroundDetailSkeleton from '../components/Turnaround/TurnaroundDetailSkeleton';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { Task } from 'gantt-task-react';

const TurnaroundDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [session, setSession] = useState<TurnaroundSessionDetail | null>(null);
    const [loading, setLoading] = useState<boolean>(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        if (id) {
            fetchSessionDetails(id);
        }
    }, [id]);

    const fetchSessionDetails = async (sessionId: string) => {
        try {
            setLoading(true);
            const data = await getSessionDetails(sessionId);
            setSession(data);
            setError(null);
        } catch (err) {
            console.error("Failed to fetch session details", err);
            setError("Failed to load session details. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    const mapTasksToGantt = (session: TurnaroundSessionDetail): Task[] => {
        return session.tasks.map(task => ({
            start: new Date(task.actualStart || task.plannedStart),
            end: new Date(task.actualEnd || task.plannedEnd),
            name: task.taskType,
            id: task.id,
            type: 'task',
            progress: task.status === 'COMPLETED' ? 100 : (task.status === 'IN_PROGRESS' ? 50 : 0),
            isDisabled: true,
            styles: {
                progressColor: task.status === 'COMPLETED' ? '#10B981' : '#3B82F6',
                progressSelectedColor: '#059669',
            },
        }));
    };

    if (loading) return <TurnaroundDetailSkeleton />;
    if (error) return (
        <div className="p-6">
            <button 
                onClick={() => navigate('/turnaround')}
                className="text-blue-600 hover:underline mb-4"
            >
                &larr; Back to Dashboard
            </button>
            <ErrorMessage message={error} onRetry={() => id && fetchSessionDetails(id)} />
        </div>
    );
    if (!session) return <div className="p-8 text-center">Session not found</div>;

    const ganttTasks = mapTasksToGantt(session);

    return (
        <div className="p-6 bg-gray-100 min-h-screen">
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <button 
                        onClick={() => navigate('/turnaround')}
                        className="text-blue-600 hover:underline mb-2"
                    >
                        &larr; Back to Dashboard
                    </button>
                    <h1 className="text-2xl font-bold text-gray-800">
                        Flight {session.flightId} ({session.icaoCode})
                    </h1>
                    <p className="text-gray-600">Stand: {session.standId} | Status: {session.status}</p>
                </div>
                <div className="text-right">
                    <div className="text-sm text-gray-500">SIBT: {session.sirt ? new Date(session.sirt).toLocaleTimeString() : '-'}</div>
                    <div className="text-sm text-gray-500">TOBT: {session.tobt ? new Date(session.tobt).toLocaleTimeString() : '-'}</div>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                <VideoTimeline />
                <div className="bg-white rounded-lg shadow p-4">
                    <h3 className="text-lg font-semibold mb-4">Session Info</h3>
                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <span className="block text-sm text-gray-500">AIBT</span>
                            <span className="font-medium">{session.aibt ? new Date(session.aibt).toLocaleTimeString() : '-'}</span>
                        </div>
                        <div>
                            <span className="block text-sm text-gray-500">AOBT</span>
                            <span className="font-medium">{session.aobt ? new Date(session.aobt).toLocaleTimeString() : '-'}</span>
                        </div>
                        <div>
                            <span className="block text-sm text-gray-500">TSAT</span>
                            <span className="font-medium">{session.tsat ? new Date(session.tsat).toLocaleTimeString() : '-'}</span>
                        </div>
                        <div>
                            <span className="block text-sm text-gray-500">EIBT</span>
                            <span className="font-medium">{session.eibt ? new Date(session.eibt).toLocaleTimeString() : '-'}</span>
                        </div>
                    </div>
                </div>
            </div>

            <GanttChart tasks={ganttTasks} />
        </div>
    );
};

export default TurnaroundDetailPage;
