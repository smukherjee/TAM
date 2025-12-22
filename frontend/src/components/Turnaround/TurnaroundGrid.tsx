import React from 'react';
import { TurnaroundSessionSummary } from '../../services/turnaroundService';
import { Link } from 'react-router-dom';
import { 
    Users, 
    Package, 
    Fuel, 
    Utensils, 
    Sparkles, 
    PlaneLanding, 
    PlaneTakeoff,
    AlertCircle,
    Luggage
} from 'lucide-react';

interface TurnaroundGridProps {
    sessions: TurnaroundSessionSummary[];
}

const getTaskIcon = (type: string) => {
    switch (type) {
        case 'DISEMBARKATION': return <Users size={14} />;
        case 'UNLOADING': return <Luggage size={14} />;
        case 'FUELING': return <Fuel size={14} />;
        case 'CATERING': return <Utensils size={14} />;
        case 'CLEANING': return <Sparkles size={14} />;
        case 'BOARDING': return <Users size={14} />;
        case 'LOADING': return <Package size={14} />;
        case 'BRIDGE_CONNECT': return <PlaneLanding size={14} />;
        case 'PUSHBACK': return <PlaneTakeoff size={14} />;
        default: return <AlertCircle size={14} />;
    }
};

const TurnaroundGrid: React.FC<TurnaroundGridProps> = ({ sessions }) => {
    if (sessions.length === 0) {
        return <div className="p-4 text-white">No active sessions found.</div>;
    }

    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 p-4">
            {sessions.map(session => (
                <Link to={`/turnaround/${session.id}`} key={session.id} className="block">
                    <div className="bg-gray-800 rounded-lg overflow-hidden shadow-lg hover:shadow-xl transition-shadow relative h-64">
                        {/* Background Image */}
                        <img 
                            src="/assets/images/assaiturnaroundallcard.jpg" 
                            alt="Turnaround" 
                            className="absolute inset-0 w-full h-full object-cover opacity-50"
                        />
                        
                        {/* Content Overlay */}
                        <div className="absolute inset-0 p-4 flex flex-col justify-between">
                            <div className="flex justify-between items-start">
                                <div className="bg-black bg-opacity-70 p-2 rounded">
                                    <h3 className="text-xl font-bold text-white">{session.flightId}</h3>
                                    <p className="text-sm text-gray-300">Stand {session.standId}</p>
                                </div>
                                <span className={`px-2 py-1 rounded text-xs font-bold ${getStatusColor(session.status)}`}>
                                    {session.status}
                                </span>
                            </div>

                            <div className="bg-black bg-opacity-70 p-2 rounded">
                                <div className="flex flex-wrap gap-2">
                                    {session.tasks?.map((task, idx) => (
                                        <div 
                                            key={idx} 
                                            className={`p-1.5 rounded-full ${getTaskColor(task.status)} text-white flex items-center justify-center`} 
                                            title={`${task.type} - ${task.status}`}
                                        >
                                            {getTaskIcon(task.type)}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </div>
                </Link>
            ))}
        </div>
    );
};

const getStatusColor = (status: string) => {
    switch (status) {
        case 'ON_BLOCK': return 'bg-blue-500 text-white';
        case 'OFF_BLOCK': return 'bg-green-500 text-white';
        case 'DELAYED': return 'bg-red-500 text-white';
        default: return 'bg-gray-500 text-white';
    }
};

const getTaskColor = (status: string) => {
    switch (status) {
        case 'COMPLETED': return 'bg-green-500';
        case 'IN_PROGRESS': return 'bg-blue-500';
        case 'DELAYED': return 'bg-red-500';
        default: return 'bg-gray-500';
    }
};

export default TurnaroundGrid;
