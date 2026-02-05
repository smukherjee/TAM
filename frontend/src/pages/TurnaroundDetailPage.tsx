import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getSessionDetails, TurnaroundSessionDetail } from '../services/turnaroundService';
import { GanttChart } from '../components/Turnaround/Detail/GanttChart';
import { VideoTimeline } from '../components/Turnaround/Detail/VideoTimeline';
import TurnaroundDetailSkeleton from '../components/Turnaround/TurnaroundDetailSkeleton';
import { ErrorMessage } from '../components/ui/ErrorMessage';
import { Task } from 'gantt-task-react';
import { ArrowLeft, Plane, Clock } from 'lucide-react';

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
                progressColor: task.status === 'DELAYED' ? '#ef4444' : task.status === 'COMPLETED' ? '#10B981' : '#3B82F6',
                progressSelectedColor: '#059669',
                backgroundColor: task.status === 'DELAYED' ? '#fca5a5' : task.status === 'COMPLETED' ? '#6ee7b7' : '#93c5fd',
            },
        }));
    };

    const formatTime = (dateString: string | null | undefined) => {
        if (!dateString) return '-';
        return new Date(dateString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    };

    if (loading) return <TurnaroundDetailSkeleton />;
    if (error) return (
        <div className="p-4 h-screen bg-slate-900">
            <button 
                onClick={() => navigate('/turnaround')}
                className="text-blue-400 hover:text-blue-300 mb-4 flex items-center gap-1 text-sm"
            >
                <ArrowLeft size={14} /> Back to Dashboard
            </button>
            <ErrorMessage message={error} onRetry={() => id && fetchSessionDetails(id)} />
        </div>
    );
    if (!session) return <div className="p-6 text-center bg-slate-900 h-screen text-gray-400">Session not found</div>;

    const ganttTasks = mapTasksToGantt(session);

    return (
        <div className="h-screen bg-slate-900 flex flex-col overflow-hidden">
            {/* Compact Header */}
            <div className="flex-shrink-0 px-4 py-3 border-b border-slate-700 bg-slate-800/50">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-4">
                        <button 
                            onClick={() => navigate('/turnaround')}
                            className="text-blue-400 hover:text-blue-300 flex items-center gap-1 text-sm"
                        >
                            <ArrowLeft size={14} /> Back to Dashboard
                        </button>
                        <div className="h-4 w-px bg-slate-600" />
                        <div className="flex items-center gap-2">
                            <Plane size={18} className="text-blue-400" />
                            <h1 className="text-lg font-bold text-gray-100">
                                Flight {session.flightId}
                            </h1>
                            <span className="text-sm text-gray-400">({session.icaoCode})</span>
                            <span className={`ml-2 px-2 py-0.5 text-xs font-medium rounded ${
                                session.status === 'ON_BLOCK' ? 'bg-green-500/20 text-green-400' :
                                session.status === 'OFF_BLOCK' ? 'bg-blue-500/20 text-blue-400' :
                                session.status === 'SCHEDULED' ? 'bg-yellow-500/20 text-yellow-400' :
                                'bg-gray-500/20 text-gray-400'
                            }`}>
                                {session.status}
                            </span>
                        </div>
                    </div>
                    <div className="flex items-center gap-3 text-xs">
                        <div className="flex items-center gap-1 bg-slate-700/50 px-2 py-1 rounded">
                            <span className="text-gray-500">Stand</span>
                            <span className="text-gray-200 font-medium">{session.standId}</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Main Content - Fits in remaining viewport */}
            <div className="flex-1 p-4 flex flex-col gap-3 min-h-0">
                {/* Top Row: Video + Session Info */}
                <div className="grid grid-cols-3 gap-3" style={{ height: '35%', minHeight: '200px' }}>
                    {/* Video Feed - Takes 1/3 */}
                    <div className="col-span-1 h-full">
                        <VideoTimeline />
                    </div>
                    
                    {/* Session Times - Takes 2/3 */}
                    <div className="col-span-2 bg-slate-800 border border-slate-700 rounded-lg shadow-lg p-3">
                        <h3 className="text-sm font-semibold text-gray-100 mb-3 flex items-center gap-2">
                            <Clock size={14} className="text-blue-400" />
                            Session Timing
                        </h3>
                        <div className="grid grid-cols-4 gap-3">
                            <TimeBlock label="AIBT" sublabel="Actual In-Block" value={formatTime(session.aibt)} highlight />
                            <TimeBlock label="AOBT" sublabel="Actual Off-Block" value={formatTime(session.aobt)} />
                            <TimeBlock label="TSAT" sublabel="Target Start-up" value={formatTime(session.tsat)} />
                            <TimeBlock label="EIBT" sublabel="Estimated In-Block" value={formatTime(session.eibt)} />
                            <TimeBlock label="SIBT" sublabel="Scheduled In-Block" value={formatTime(session.sirt)} />
                            <TimeBlock label="SOBT" sublabel="Scheduled Off-Block" value={formatTime(session.tobt)} />
                            <TimeBlock label="TOBT" sublabel="Target Off-Block" value={formatTime(session.tobt)} highlight />
                            <TimeBlock label="COBT" sublabel="Calculated Off-Block" value={formatTime(session.tobt)} />
                        </div>
                    </div>
                </div>

                {/* Gantt Chart - Takes remaining space */}
                <div className="flex-1 min-h-0 flex flex-col">
                    <GanttChart tasks={ganttTasks} />
                </div>
            </div>
        </div>
    );
};

// Helper component for time display blocks
const TimeBlock: React.FC<{ label: string; sublabel: string; value: string; highlight?: boolean }> = ({ label, sublabel, value, highlight }) => (
    <div className={`p-2 rounded ${highlight ? 'bg-blue-500/10 border border-blue-500/30' : 'bg-slate-700/30'}`}>
        <div className="flex items-baseline gap-1">
            <span className={`text-xs font-semibold ${highlight ? 'text-blue-400' : 'text-gray-400'}`}>{label}</span>
        </div>
        <span className={`text-sm font-medium ${highlight ? 'text-blue-300' : 'text-gray-200'}`}>{value}</span>
        <div className="text-[10px] text-gray-500 truncate">{sublabel}</div>
    </div>
);

export default TurnaroundDetailPage;
