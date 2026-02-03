/**
 * useBrowserNotifications Hook
 * 
 * Feature: 005-asset-tracking-security
 * Task: T070a - Browser Push Notifications for CRITICAL Severity
 * 
 * Implements FR6.3: Browser notifications for CRITICAL severity only
 * 
 * This hook manages browser push notifications for critical security alerts,
 * including zone violations and movement discrepancies with CRITICAL severity.
 */

import { useEffect, useState, useCallback, useRef } from 'react';
import { webSocketService } from '../services/WebSocketService';

// Local storage keys
const NOTIFICATION_PERMISSION_KEY = 'tam_notification_permission';
const NOTIFICATION_ENABLED_KEY = 'tam_notifications_enabled';

/**
 * Alert payload from WebSocket
 */
interface CriticalAlert {
    id: string;
    type: 'ZONE_VIOLATION' | 'MOVEMENT_DISCREPANCY';
    severity: 'CRITICAL';
    title: string;
    message: string;
    assetId: string;
    assetName: string;
    zoneName?: string;
    timestamp: string;
    tenantCode: string;
}

/**
 * Hook return type
 */
interface UseBrowserNotificationsResult {
    /** Whether notifications are enabled and permission granted */
    isEnabled: boolean;
    /** Current notification permission status */
    permission: NotificationPermission;
    /** Whether sound is enabled for notifications */
    soundEnabled: boolean;
    /** Request notification permission from the browser */
    requestPermission: () => Promise<NotificationPermission>;
    /** Enable or disable notifications */
    setEnabled: (enabled: boolean) => void;
    /** Enable or disable sound */
    setSoundEnabled: (enabled: boolean) => void;
    /** Manually show a notification (for testing) */
    showNotification: (title: string, options?: NotificationOptions) => void;
    /** Number of notifications shown this session */
    notificationCount: number;
}

/**
 * Custom hook for browser push notifications on CRITICAL severity alerts
 * 
 * @param tenantCode - The tenant code to subscribe to for alerts
 * @returns Notification control functions and state
 */
export function useBrowserNotifications(tenantCode: string): UseBrowserNotificationsResult {
    // Permission state
    const [permission, setPermission] = useState<NotificationPermission>(
        typeof Notification !== 'undefined' ? Notification.permission : 'denied'
    );
    
    // User preference state (persisted to localStorage)
    const [enabled, setEnabledState] = useState<boolean>(() => {
        const stored = localStorage.getItem(NOTIFICATION_ENABLED_KEY);
        return stored === 'true';
    });
    
    const [soundEnabled, setSoundEnabledState] = useState<boolean>(() => {
        const stored = localStorage.getItem('tam_notification_sound');
        return stored !== 'false'; // Default to true
    });
    
    // Notification counter for this session
    const [notificationCount, setNotificationCount] = useState(0);
    
    // Audio ref for notification sound
    const audioRef = useRef<HTMLAudioElement | null>(null);

    /**
     * Initialize audio element for notification sound
     */
    useEffect(() => {
        // Create audio element for notification sound
        audioRef.current = new Audio('/sounds/critical-alert.mp3');
        audioRef.current.volume = 0.5;
        
        return () => {
            audioRef.current = null;
        };
    }, []);

    /**
     * Request notification permission from the browser
     */
    const requestPermission = useCallback(async (): Promise<NotificationPermission> => {
        if (typeof Notification === 'undefined') {
            console.warn('[Notifications] Browser does not support notifications');
            return 'denied';
        }

        try {
            const result = await Notification.requestPermission();
            setPermission(result);
            localStorage.setItem(NOTIFICATION_PERMISSION_KEY, result);
            
            if (result === 'granted') {
                setEnabledState(true);
                localStorage.setItem(NOTIFICATION_ENABLED_KEY, 'true');
            }
            
            return result;
        } catch (error) {
            console.error('[Notifications] Error requesting permission:', error);
            return 'denied';
        }
    }, []);

    /**
     * Enable or disable notifications
     */
    const setEnabled = useCallback((value: boolean) => {
        setEnabledState(value);
        localStorage.setItem(NOTIFICATION_ENABLED_KEY, String(value));
    }, []);

    /**
     * Enable or disable notification sound
     */
    const setSoundEnabled = useCallback((value: boolean) => {
        setSoundEnabledState(value);
        localStorage.setItem('tam_notification_sound', String(value));
    }, []);

    /**
     * Show a browser notification
     */
    const showNotification = useCallback((title: string, options?: NotificationOptions) => {
        if (typeof Notification === 'undefined') {
            console.warn('[Notifications] Browser does not support notifications');
            return;
        }

        if (Notification.permission !== 'granted') {
            console.warn('[Notifications] Permission not granted');
            return;
        }

        try {
            const notification = new Notification(title, {
                icon: '/icons/critical-alert.png',
                badge: '/icons/badge.png',
                tag: 'tam-critical-alert',
                requireInteraction: true, // Keep visible until user interacts
                ...options,
            });

            // Play sound if enabled
            if (soundEnabled && audioRef.current) {
                audioRef.current.play().catch(err => {
                    console.warn('[Notifications] Could not play sound:', err);
                });
            }

            // Track notification count
            setNotificationCount(prev => prev + 1);

            // Handle notification click
            notification.onclick = () => {
                window.focus();
                notification.close();
            };

            // Auto-close after 30 seconds if not interacted with
            setTimeout(() => {
                notification.close();
            }, 30000);

        } catch (error) {
            console.error('[Notifications] Error showing notification:', error);
        }
    }, [soundEnabled]);

    /**
     * Handle incoming critical alert from WebSocket
     */
    const handleCriticalAlert = useCallback((alert: CriticalAlert) => {
        console.log('[Notifications] Received critical alert:', alert);

        if (!enabled || permission !== 'granted') {
            console.log('[Notifications] Notifications disabled or permission not granted');
            return;
        }

        // Build notification content based on alert type
        let title: string;
        let body: string;

        if (alert.type === 'ZONE_VIOLATION') {
            title = `🚨 CRITICAL Zone Violation`;
            body = `${alert.assetName} entered restricted zone "${alert.zoneName}"`;
        } else if (alert.type === 'MOVEMENT_DISCREPANCY') {
            title = `⚠️ CRITICAL Movement Alert`;
            body = alert.message || `Critical discrepancy detected for ${alert.assetName}`;
        } else {
            title = `🔴 CRITICAL Alert`;
            body = alert.message || `Critical security event for ${alert.assetName}`;
        }

        showNotification(title, {
            body,
            data: {
                alertId: alert.id,
                assetId: alert.assetId,
                type: alert.type,
                timestamp: alert.timestamp,
            },
        });

    }, [enabled, permission, showNotification]);

    /**
     * Subscribe to critical alerts via WebSocket
     */
    useEffect(() => {
        if (!tenantCode) {
            return;
        }

        console.log('[Notifications] Subscribing to critical alerts for tenant:', tenantCode);
        
        const subscription = webSocketService.subscribeToCriticalAlerts(
            tenantCode,
            handleCriticalAlert
        );

        return () => {
            console.log('[Notifications] Unsubscribing from critical alerts');
            subscription.unsubscribe();
        };
    }, [tenantCode, handleCriticalAlert]);

    /**
     * Check and update permission status on mount
     */
    useEffect(() => {
        if (typeof Notification !== 'undefined') {
            setPermission(Notification.permission);
        }
    }, []);

    return {
        isEnabled: enabled && permission === 'granted',
        permission,
        soundEnabled,
        requestPermission,
        setEnabled,
        setSoundEnabled,
        showNotification,
        notificationCount,
    };
}

/**
 * Utility function to check if browser supports notifications
 */
export function supportsNotifications(): boolean {
    return typeof Notification !== 'undefined';
}

/**
 * Utility function to check if notifications are currently allowed
 */
export function notificationsAllowed(): boolean {
    return typeof Notification !== 'undefined' && Notification.permission === 'granted';
}

export default useBrowserNotifications;
