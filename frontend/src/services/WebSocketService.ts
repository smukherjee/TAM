import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

class WebSocketService {
    private client: Client;
    private pendingSubscriptions: Array<{ topic: string, callback: (message: any) => void, subscription?: StompSubscription }> = [];

    constructor() {
        this.client = new Client({
            brokerURL: 'ws://localhost:8080/ws',
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.client.onConnect = () => {
            console.log('Connected to WebSocket');
            this.processPendingSubscriptions();
        };

        this.client.onStompError = (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
        };

        this.client.activate();
    }

    connect(onConnect?: () => void) {
        if (onConnect) {
            if (this.client.connected) {
                onConnect();
            } else {
                const originalOnConnect = this.client.onConnect;
                this.client.onConnect = (frame) => {
                    if (originalOnConnect) originalOnConnect(frame);
                    onConnect();
                };
            }
        }
        if (!this.client.active) {
            this.client.activate();
        }
    }

    subscribe(topic: string, callback: (message: any) => void): { unsubscribe: () => void } {
        console.log('[WebSocket] Subscribing to topic:', topic);
        // Always track the subscription so we can re-subscribe on reconnect
        const subscriptionRecord = { 
            topic, 
            callback, 
            subscription: undefined as StompSubscription | undefined 
        };
        this.pendingSubscriptions.push(subscriptionRecord);

        if (this.client.connected) {
            subscriptionRecord.subscription = this.client.subscribe(topic, (message: IMessage) => {
                console.log('[WebSocket] Received message on topic:', topic, 'Body:', message.body);
                try {
                    const body = JSON.parse(message.body);
                    console.log('[WebSocket] Parsed message:', body);
                    callback(body);
                } catch (e) {
                    console.error('Error parsing WebSocket message', e);
                }
            });
            console.log('[WebSocket] Successfully subscribed to:', topic);
        } else {
            console.log('[WebSocket] Not connected yet, subscription will be processed when connected');
        }

        return {
            unsubscribe: () => {
                this.pendingSubscriptions = this.pendingSubscriptions.filter(p => p !== subscriptionRecord);
                if (subscriptionRecord.subscription) {
                    subscriptionRecord.subscription.unsubscribe();
                }
            }
        };
    }

    private processPendingSubscriptions() {
        this.pendingSubscriptions.forEach(p => {
            // Always subscribe (or re-subscribe) when connected
            // If we are reconnecting, the old subscription is invalid anyway
            p.subscription = this.client.subscribe(p.topic, (message: IMessage) => {
                try {
                    const body = JSON.parse(message.body);
                    p.callback(body);
                } catch (e) {
                    console.error('Error parsing WebSocket message', e);
                }
            });
        });
    }

    disconnect() {
        this.client.deactivate();
    }

    // ========================================================================
    // Tracking Subscriptions (Feature: 005-asset-tracking-security)
    // ========================================================================

    /**
     * Subscribe to zone violation events for a tenant
     */
    subscribeToViolations(tenantCode: string, callback: (violation: any) => void): { unsubscribe: () => void } {
        return this.subscribe(`/topic/violations/${tenantCode}`, callback);
    }

    /**
     * Subscribe to movement discrepancy events for a tenant
     */
    subscribeToDiscrepancies(tenantCode: string, callback: (discrepancy: any) => void): { unsubscribe: () => void } {
        return this.subscribe(`/topic/discrepancies/${tenantCode}`, callback);
    }

    /**
     * Subscribe to violation acknowledgment events
     */
    subscribeToViolationAcknowledgments(tenantCode: string, callback: (event: any) => void): { unsubscribe: () => void } {
        return this.subscribe(`/topic/violations/${tenantCode}/acknowledged`, callback);
    }

    /**
     * Subscribe to discrepancy acknowledgment events
     */
    subscribeToDiscrepancyAcknowledgments(tenantCode: string, callback: (event: any) => void): { unsubscribe: () => void } {
        return this.subscribe(`/topic/discrepancies/${tenantCode}/acknowledged`, callback);
    }

    /**
     * Subscribe to critical alerts that require immediate attention
     */
    subscribeToCriticalAlerts(tenantCode: string, callback: (alert: any) => void): { unsubscribe: () => void } {
        return this.subscribe(`/topic/alerts/critical/${tenantCode}`, callback);
    }
}

export const webSocketService = new WebSocketService();
