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

    subscribe(topic: string, callback: (message: any) => void): { unsubscribe: () => void } {
        // Always track the subscription so we can re-subscribe on reconnect
        const subscriptionRecord = { 
            topic, 
            callback, 
            subscription: undefined as StompSubscription | undefined 
        };
        this.pendingSubscriptions.push(subscriptionRecord);

        if (this.client.connected) {
            subscriptionRecord.subscription = this.client.subscribe(topic, (message: IMessage) => {
                try {
                    const body = JSON.parse(message.body);
                    callback(body);
                } catch (e) {
                    console.error('Error parsing WebSocket message', e);
                }
            });
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
}

export const webSocketService = new WebSocketService();
