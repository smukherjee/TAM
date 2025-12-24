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
    Luggage,
    Clock,
    CheckCircle,
    Plane
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

const getStatusIcon = (status: string) => {
    switch (status) {
        case 'ON_BLOCK': return <PlaneLanding className="text-blue-300" size={20} />;
        case 'OFF_BLOCK': return <PlaneTakeoff className="text-green-300" size={20} />;
        case 'SCHEDULED': return <Clock className="text-gray-300" size={20} />;
        case 'DEPARTED': return <CheckCircle className="text-green-400" size={20} />;
        default: return <Plane className="text-gray-400" size={20} />;
    }
};

const getBackgroundImage = (status: string) => {
    // Randomly select from available onblock images for variety
    const images = ['onblock1.png', 'onblock2.webp', 'onblock3.jpeg', 'onblock4.jpg', 'onblock5.webp'];
    const randomIndex = Math.floor(Math.random() * images.length);
    
    switch (status) {
        case 'ON_BLOCK': 
            return `/assets/images/${images[randomIndex]}`;
        case 'OFF_BLOCK':
        case 'DEPARTED':
            return '/assets/images/onblock5.webp'; // Use a specific image for departed
        case 'SCHEDULED':
        default:
            return '/assets/images/onblock1.png'; // Use a specific image for scheduled
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
                        {/* Background Image - Status Specific */}
                        <img 
                            src={getBackgroundImage(session.status)} 
                            alt={`Turnaround - ${session.status}`} 
                            className="absolute inset-0 w-full h-full object-cover opacity-50"
                        />
                        
                        {/* Content Overlay */}
                        <div className="absolute inset-0 p-4 flex flex-col justify-between">
                            <div className="flex justify-between items-start">
                                <div className="bg-black bg-opacity-70 p-2 rounded">
                                    <div className="flex items-center space-x-2">
                                        {getStatusIcon(session.status)}
                                        <div>
                                            <h3 className="text-xl font-bold text-white">{session.flightId}</h3>
                                            <p className="text-sm text-gray-300">Stand {session.standId}</p>
                                        </div>
                                    </div>
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
        case 'DEPARTED': return 'bg-green-600 text-white';
        case 'SCHEDULED': return 'bg-gray-600 text-white';
        case 'DELAYED': return 'bg-red-500 text-white';
        case 'COMPLETED': return 'bg-green-700 text-white';
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
