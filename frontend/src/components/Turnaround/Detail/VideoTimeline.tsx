import React, { useState } from 'react';
import { Maximize2, X } from 'lucide-react';

export const VideoTimeline: React.FC = () => {
    const [isFullscreen, setIsFullscreen] = useState(false);

    const toggleFullscreen = () => {
        setIsFullscreen(!isFullscreen);
    };

    // Fullscreen modal overlay
    if (isFullscreen) {
        return (
            <div className="fixed inset-0 z-50 bg-black/95 flex items-center justify-center p-4">
                <div className="relative w-full max-w-6xl">
                    <div className="flex items-center justify-between mb-3">
                        <h3 className="text-lg font-semibold text-gray-100 flex items-center gap-2">
                            <span className="relative flex h-2 w-2">
                                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                                <span className="relative inline-flex rounded-full h-2 w-2 bg-red-500"></span>
                            </span>
                            Live Feed
                        </h3>
                        <button
                            onClick={toggleFullscreen}
                            className="p-2 hover:bg-slate-700 rounded-lg transition-colors text-gray-400 hover:text-white"
                            title="Exit fullscreen"
                        >
                            <X size={24} />
                        </button>
                    </div>
                    <div className="relative aspect-video bg-slate-900 rounded-lg overflow-hidden border border-slate-600">
                        <video 
                            src="/assets/video/mock_feed.mp4" 
                            controls 
                            autoPlay 
                            loop 
                            muted 
                            className="w-full h-full object-cover"
                        />
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="bg-slate-800 border border-slate-700 rounded-lg shadow-lg p-4 h-full flex flex-col">
            <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-semibold text-gray-100 flex items-center gap-2">
                    <span className="relative flex h-2 w-2">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
                        <span className="relative inline-flex rounded-full h-2 w-2 bg-red-500"></span>
                    </span>
                    Live Feed
                </h3>
                <button
                    onClick={toggleFullscreen}
                    className="p-1.5 hover:bg-slate-700 rounded transition-colors text-gray-400 hover:text-white"
                    title="Expand to fullscreen"
                >
                    <Maximize2 size={14} />
                </button>
            </div>
            <div className="relative bg-slate-900 rounded-lg overflow-hidden flex-1 border border-slate-600">
                <video 
                    src="/assets/video/mock_feed.mp4" 
                    controls 
                    autoPlay 
                    loop 
                    muted 
                    className="w-full h-full object-cover"
                />
            </div>
        </div>
    );
};
