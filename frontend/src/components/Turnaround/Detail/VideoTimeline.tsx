import React from 'react';

export const VideoTimeline: React.FC = () => {
    return (
        <div className="bg-white rounded-lg shadow p-4">
            <h3 className="text-lg font-semibold mb-4">Live Feed</h3>
            <div className="relative aspect-video bg-gray-900 rounded overflow-hidden flex items-center justify-center">
                <video 
                    src="/assets/video/mock_feed.mp4" 
                    controls 
                    autoPlay 
                    loop 
                    muted 
                    className="w-full h-full object-cover"
                />
                <div className="absolute top-2 right-2 bg-red-600 text-white text-xs px-2 py-1 rounded animate-pulse">
                    LIVE
                </div>
            </div>
        </div>
    );
};
