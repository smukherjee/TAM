import React from 'react';
import { 
    Play, 
    Pause, 
    SkipBack, 
    SkipForward,
    FastForward
} from 'lucide-react';
import { format } from 'date-fns';

interface TrailPlaybackControlsProps {
    isPlaying: boolean;
    playbackSpeed: number;
    currentIndex: number;
    totalPoints: number;
    currentTimestamp?: string;
    onPlayPause: () => void;
    onSpeedChange: (speed: number) => void;
}

const SPEED_OPTIONS = [
    { value: 1, label: '1x' },
    { value: 5, label: '5x' },
    { value: 10, label: '10x' },
    { value: 30, label: '30x' },
    { value: 60, label: '60x' },
];

/**
 * TrailPlaybackControls - Play/Pause and speed controls for trail playback
 * Feature: 005-asset-tracking-security
 * Phase 11: Movement Trail Visualization
 */
const TrailPlaybackControls: React.FC<TrailPlaybackControlsProps> = ({
    isPlaying,
    playbackSpeed,
    currentIndex,
    totalPoints,
    currentTimestamp,
    onPlayPause,
    onSpeedChange
}) => {
    // Calculate progress percentage
    const progressPercent = totalPoints > 0 
        ? Math.round((currentIndex / (totalPoints - 1)) * 100) 
        : 0;

    // Format current timestamp
    const formattedTime = currentTimestamp 
        ? format(new Date(currentTimestamp), 'MMM d, yyyy HH:mm:ss')
        : '—';

    return (
        <div className="px-6 py-3 flex items-center justify-between bg-gray-800">
            {/* Left: Current Time Display */}
            <div className="flex items-center space-x-4 min-w-[200px]">
                <div className="text-sm">
                    <div className="text-gray-500 text-xs">Current Time</div>
                    <div className="font-medium text-white">{formattedTime}</div>
                </div>
            </div>

            {/* Center: Playback Controls */}
            <div className="flex items-center space-x-4">
                {/* Skip to Start */}
                <button
                    onClick={() => onSpeedChange(1)}
                    disabled={currentIndex === 0}
                    className="p-2 text-gray-500 hover:text-gray-300 hover:bg-gray-700 
                        rounded-full disabled:opacity-50 disabled:cursor-not-allowed
                        transition-colors"
                    title="Back to start"
                >
                    <SkipBack className="h-5 w-5" />
                </button>

                {/* Play/Pause */}
                <button
                    onClick={onPlayPause}
                    disabled={totalPoints === 0}
                    className={`p-4 rounded-full transition-colors ${
                        isPlaying 
                            ? 'bg-blue-600 text-white hover:bg-blue-700' 
                            : 'bg-blue-900/50 text-blue-400 hover:bg-blue-900/70'
                    } disabled:opacity-50 disabled:cursor-not-allowed`}
                    title={isPlaying ? 'Pause' : 'Play'}
                >
                    {isPlaying ? (
                        <Pause className="h-6 w-6" />
                    ) : (
                        <Play className="h-6 w-6 ml-0.5" />
                    )}
                </button>

                {/* Skip to End */}
                <button
                    onClick={() => {}}
                    disabled={currentIndex >= totalPoints - 1}
                    className="p-2 text-gray-500 hover:text-gray-300 hover:bg-gray-700 
                        rounded-full disabled:opacity-50 disabled:cursor-not-allowed
                        transition-colors"
                    title="Skip to end"
                >
                    <SkipForward className="h-5 w-5" />
                </button>
            </div>

            {/* Right: Speed Controls & Progress */}
            <div className="flex items-center space-x-6 min-w-[300px] justify-end">
                {/* Speed Selector */}
                <div className="flex items-center space-x-2">
                    <FastForward className="h-4 w-4 text-gray-500" />
                    <div className="flex bg-gray-700 rounded-lg p-0.5">
                        {SPEED_OPTIONS.map((option) => (
                            <button
                                key={option.value}
                                onClick={() => onSpeedChange(option.value)}
                                className={`px-3 py-1 text-xs font-medium rounded-md transition-colors ${
                                    playbackSpeed === option.value
                                        ? 'bg-gray-600 text-blue-400 shadow-sm'
                                        : 'text-gray-400 hover:text-white'
                                }`}
                            >
                                {option.label}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Progress Indicator */}
                <div className="flex items-center space-x-3">
                    <div className="w-32 h-2 bg-gray-700 rounded-full overflow-hidden">
                        <div 
                            className="h-full bg-blue-500 transition-all duration-150"
                            style={{ width: `${progressPercent}%` }}
                        />
                    </div>
                    <span className="text-sm font-medium text-gray-400 min-w-[50px] text-right">
                        {progressPercent}%
                    </span>
                </div>
            </div>
        </div>
    );
};

export default TrailPlaybackControls;
