import React from 'react';
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch';
import { Link } from 'react-router-dom';
import StandPin from './StandPin';
import { TurnaroundSessionSummary } from '../../../services/turnaroundService';

interface AirportMapProps {
    sessions: TurnaroundSessionSummary[];
}

const AirportMap: React.FC<AirportMapProps> = ({ sessions }) => {
    // Mock coordinates mapping for stands
    const standCoordinates: Record<string, { top: string, left: string }> = {
        'S1': { top: '20%', left: '30%' },
        'S2': { top: '20%', left: '40%' },
        'S3': { top: '20%', left: '50%' },
        'S4': { top: '40%', left: '30%' },
        'S5': { top: '40%', left: '40%' },
        // ... add more as needed
    };

    return (
        <div className="w-full h-full bg-gray-900 overflow-hidden">
            <TransformWrapper
                initialScale={1}
                minScale={0.5}
                maxScale={4}
            >
                <TransformComponent wrapperClass="w-full h-full" contentClass="w-full h-full">
                    <div className="relative">
                        <img 
                            src="/assets/images/airport_map.png" 
                            alt="Airport Map" 
                            className="max-w-none"
                        />
                        {sessions.map(session => {
                            const coords = standCoordinates[session.standId];
                            if (!coords) return null;
                            return (
                                <div key={session.id} className="absolute" style={{ top: coords.top, left: coords.left }}>
                                    <Link to={`/turnaround/${session.id}`}>
                                        <StandPin standId={session.standId} status={session.status} />
                                    </Link>
                                </div>
                            );
                        })}
                    </div>
                </TransformComponent>
            </TransformWrapper>
        </div>
    );
};

export default AirportMap;
