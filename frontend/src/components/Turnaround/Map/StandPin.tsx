import React from 'react';

interface StandPinProps {
    standId: string;
    status: string;
}

const StandPin: React.FC<StandPinProps> = ({ standId, status }) => {
    const getColor = (status: string) => {
        switch (status) {
            case 'ON_BLOCK': return 'bg-blue-500';
            case 'OFF_BLOCK': return 'bg-green-500';
            case 'DELAYED': return 'bg-red-500';
            default: return 'bg-gray-500';
        }
    };

    return (
        <div className="flex flex-col items-center transform -translate-x-1/2 -translate-y-full cursor-pointer hover:scale-110 transition-transform">
            <div className={`w-8 h-8 rounded-full border-2 border-white flex items-center justify-center text-white font-bold text-xs shadow-lg ${getColor(status)}`}>
                {standId}
            </div>
            <div className="w-0 h-0 border-l-4 border-l-transparent border-r-4 border-r-transparent border-t-4 border-t-white mt-[-2px]"></div>
        </div>
    );
};

export default StandPin;
