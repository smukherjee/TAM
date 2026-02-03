/**
 * NotificationSettings Component
 * 
 * Feature: 005-asset-tracking-security
 * Task: T070a - Browser Push Notifications for CRITICAL Severity
 * 
 * Settings panel for managing browser notification preferences.
 * Shows permission status, enable/disable toggle, and sound settings.
 */

import React from 'react';
import { useBrowserNotifications, supportsNotifications } from '../../hooks/useBrowserNotifications';

// Simple SVG icons to avoid external dependency
const BellIcon = ({ className }: { className?: string }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
    </svg>
);

const BellSlashIcon = ({ className }: { className?: string }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" d="M9.143 17.082a24.248 24.248 0 003.844.148m-3.844-.148a23.856 23.856 0 01-5.455-1.31 8.964 8.964 0 002.3-5.542m3.155 6.852a3 3 0 005.667 1.097M9.143 17.082a23.856 23.856 0 005.454 1.31m-3.143 0a3 3 0 001.143-1.31M12 9.75V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
);

const SpeakerIcon = ({ on, className }: { on: boolean; className?: string }) => (
    <svg className={className} fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor">
        {on ? (
            <path strokeLinecap="round" strokeLinejoin="round" d="M19.114 5.636a9 9 0 010 12.728M16.463 8.288a5.25 5.25 0 010 7.424M6.75 8.25l4.72-4.72a.75.75 0 011.28.53v15.88a.75.75 0 01-1.28.53l-4.72-4.72H4.51c-.88 0-1.704-.507-1.938-1.354A9.01 9.01 0 012.25 12c0-.83.112-1.633.322-2.396C2.806 8.756 3.63 8.25 4.51 8.25H6.75z" />
        ) : (
            <path strokeLinecap="round" strokeLinejoin="round" d="M17.25 9.75L19.5 12m0 0l2.25 2.25M19.5 12l2.25-2.25M19.5 12l-2.25 2.25m-10.5-6l4.72-4.72a.75.75 0 011.28.53v15.88a.75.75 0 01-1.28.53l-4.72-4.72H4.51c-.88 0-1.704-.507-1.938-1.354A9.01 9.01 0 012.25 12c0-.83.112-1.633.322-2.396C2.806 8.756 3.63 8.25 4.51 8.25H6.75z" />
        )}
    </svg>
);

interface NotificationSettingsProps {
    tenantCode: string;
    className?: string;
}

export const NotificationSettings: React.FC<NotificationSettingsProps> = ({ 
    tenantCode,
    className = '' 
}) => {
    const {
        isEnabled,
        permission,
        soundEnabled,
        requestPermission,
        setEnabled,
        setSoundEnabled,
        notificationCount,
    } = useBrowserNotifications(tenantCode);

    // Check if browser supports notifications
    if (!supportsNotifications()) {
        return (
            <div className={`p-4 bg-gray-100 rounded-lg ${className}`}>
                <div className="flex items-center gap-2 text-gray-500">
                    <BellSlashIcon className="h-5 w-5" />
                    <span>Browser notifications not supported</span>
                </div>
            </div>
        );
    }

    const handleEnableToggle = async () => {
        if (permission === 'default') {
            // Request permission first
            const result = await requestPermission();
            if (result === 'granted') {
                setEnabled(true);
            }
        } else if (permission === 'granted') {
            setEnabled(!isEnabled);
        }
    };

    return (
        <div className={`p-4 bg-white border border-gray-200 rounded-lg shadow-sm ${className}`}>
            <h3 className="text-sm font-semibold text-gray-700 mb-3 flex items-center gap-2">
                <BellIcon className="h-5 w-5" />
                Critical Alert Notifications
            </h3>

            {/* Permission Status */}
            {permission === 'denied' && (
                <div className="mb-3 p-2 bg-red-50 border border-red-200 rounded text-sm text-red-700">
                    ⚠️ Notifications blocked. Enable in browser settings.
                </div>
            )}

            {/* Enable/Disable Toggle */}
            <div className="flex items-center justify-between mb-3">
                <div>
                    <p className="text-sm font-medium text-gray-700">
                        Enable notifications
                    </p>
                    <p className="text-xs text-gray-500">
                        Receive browser alerts for CRITICAL violations
                    </p>
                </div>
                <button
                    onClick={handleEnableToggle}
                    disabled={permission === 'denied'}
                    className={`
                        relative inline-flex h-6 w-11 items-center rounded-full transition-colors
                        ${isEnabled ? 'bg-red-600' : 'bg-gray-300'}
                        ${permission === 'denied' ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                    `}
                    role="switch"
                    aria-checked={isEnabled}
                    aria-label="Enable critical alert notifications"
                >
                    <span
                        className={`
                            inline-block h-4 w-4 transform rounded-full bg-white transition-transform
                            ${isEnabled ? 'translate-x-6' : 'translate-x-1'}
                        `}
                    />
                </button>
            </div>

            {/* Sound Toggle */}
            <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                    <SpeakerIcon on={soundEnabled} className={`h-4 w-4 ${soundEnabled ? 'text-gray-500' : 'text-gray-400'}`} />
                    <p className="text-sm text-gray-700">
                        Alert sound
                    </p>
                </div>
                <button
                    onClick={() => setSoundEnabled(!soundEnabled)}
                    disabled={!isEnabled}
                    className={`
                        relative inline-flex h-5 w-9 items-center rounded-full transition-colors
                        ${soundEnabled && isEnabled ? 'bg-blue-500' : 'bg-gray-200'}
                        ${!isEnabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
                    `}
                    role="switch"
                    aria-checked={soundEnabled}
                    aria-label="Enable notification sound"
                >
                    <span
                        className={`
                            inline-block h-3 w-3 transform rounded-full bg-white transition-transform
                            ${soundEnabled ? 'translate-x-5' : 'translate-x-1'}
                        `}
                    />
                </button>
            </div>

            {/* Session Stats */}
            {notificationCount > 0 && (
                <div className="pt-2 border-t border-gray-100 text-xs text-gray-500">
                    {notificationCount} notification{notificationCount !== 1 ? 's' : ''} this session
                </div>
            )}

            {/* Request Permission Button (if needed) */}
            {permission === 'default' && !isEnabled && (
                <button
                    onClick={requestPermission}
                    className="mt-2 w-full px-3 py-2 bg-red-600 text-white text-sm font-medium rounded-md hover:bg-red-700 transition-colors"
                >
                    Enable Critical Alerts
                </button>
            )}
        </div>
    );
};

export default NotificationSettings;
