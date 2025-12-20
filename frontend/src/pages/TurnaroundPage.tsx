import React from 'react';
import TurnaroundGantt from '../components/Turnaround/TurnaroundGantt';

const TurnaroundPage: React.FC = () => {
    return (
        <div className="w-full h-full p-6 bg-gray-50 overflow-hidden flex flex-col">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold text-gray-800">Turnaround Process View</h2>
                <div className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">Real-time</div>
            </div>
            <div className="flex-1 overflow-hidden bg-white rounded-xl shadow-lg border border-gray-200 p-4">
                <TurnaroundGantt />
            </div>
        </div>
    );
};

export default TurnaroundPage;
