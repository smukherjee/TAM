import { useEffect, useState, useRef } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Activity, Plane, Truck, ArrowRight, Settings, AlertTriangle, AlertOctagon, CheckCircle2 } from 'lucide-react';
import axios from 'axios';
import { Client } from '@stomp/stompjs';

// Define the shape of the pipeline status data
interface PipelineStatus {
    flowName: string;
    sourceRate: number;
    sinkRate: number;
    health: 'GREEN' | 'AMBER' | 'RED';
    lastDataTimestamp: number;
    timeSinceLastData: string;
    avgLatency?: number;
    errorRate?: number;
}

interface AlertConfig {
    greenThresholdSeconds: number;
    amberThresholdSeconds: number;
}

const PipelinePage = () => {
    const [statuses, setStatuses] = useState<PipelineStatus[]>([]);
    const [connected, setConnected] = useState(false);
    const [showConfig, setShowConfig] = useState(false);
    const [config, setConfig] = useState<AlertConfig>({ greenThresholdSeconds: 60, amberThresholdSeconds: 120 });
    const stompClientRef = useRef<Client | null>(null);

    // Initial Fetch & Config Fetch & WebSocket Init
    useEffect(() => {
        fetchPipelineStatus();
        fetchAlertConfig();

        // Initialize WebSocket
        const client = new Client({
            brokerURL: 'ws://localhost:8080/ws',
            onConnect: () => {
                setConnected(true);
                client.subscribe('/topic/pipeline-status', (message) => {
                    const data = JSON.parse(message.body);
                    if (Array.isArray(data)) {
                        setStatuses(data);
                    } else {
                        console.error("Received invalid data from WebSocket", data);
                    }
                });
            },
            onDisconnect: () => {
                setConnected(false);
            },
        });

        client.activate();
        stompClientRef.current = client;

        return () => {
            client.deactivate();
        };
    }, []);

    const fetchPipelineStatus = async () => {
        try {
            const response = await axios.get(`${import.meta.env.VITE_API_URL}/api/admin/pipeline-status`);
            if (Array.isArray(response.data)) {
                setStatuses(response.data);
            } else {
                console.error("Received invalid data from API", response.data);
            }
        } catch (error) {
            console.error('Error fetching pipeline status:', error);
        }
    };

    const fetchAlertConfig = async () => {
        try {
            const response = await axios.get(`${import.meta.env.VITE_API_URL}/api/admin/config/alerts`);
            setConfig(response.data);
        } catch (error) {
            console.error('Error fetching alert config:', error);
        }
    }

    const updateConfig = async () => {
        try {
            await axios.post(`${import.meta.env.VITE_API_URL}/api/admin/config/alerts`, config);
            setShowConfig(false);
            // Re-fetch to confirm
            fetchAlertConfig();
        } catch (error) {
            console.error('Error updating alert config:', error);
        }
    }

    const getStatusBg = (health: string) => {
        switch (health) {
            case 'GREEN': return 'bg-green-500/10 border-green-500/20';
            case 'AMBER': return 'bg-yellow-500/10 border-yellow-500/20';
            case 'RED': return 'bg-red-500/10 border-red-500/20';
            default: return 'bg-gray-800 border-gray-700';
        }
    };

    const getIcon = (name: string) => {
        if (name.toLowerCase().includes('flight')) return <Plane className="h-6 w-6" />;
        if (name.toLowerCase().includes('vehicle')) return <Truck className="h-6 w-6" />;
        return <Activity className="h-6 w-6" />;
    };

    return (
        <div className="p-6 space-y-6 bg-black min-h-screen text-white font-sans">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-3xl font-bold tracking-tight bg-gradient-to-r from-blue-400 to-purple-500 bg-clip-text text-transparent">
                        Observability Dashboard
                    </h1>
                    <p className="text-gray-400 mt-1 flex items-center gap-2">
                        <span className={`inline-block w-2 h-2 rounded-full ${connected ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`}></span>
                        {connected ? 'Real-Time Updates Active' : 'Connecting to Live Stream...'}
                    </p>
                </div>
                <div className="flex gap-3">
                    <button
                        onClick={() => setShowConfig(!showConfig)}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-800 hover:bg-gray-700 rounded-lg border border-gray-700 transition-colors"
                    >
                        <Settings className="w-4 h-4" />
                        Alert Config
                    </button>
                    <span className="px-3 py-1 bg-purple-900/30 border border-purple-500/30 rounded-full text-xs font-mono text-purple-300">
                        v2.1 WebSocket
                    </span>
                </div>
            </div>

            {/* Config Modal */}
            {showConfig && (
                <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center">
                    <div className="bg-gray-900 border border-gray-700 rounded-xl p-6 w-full max-w-md shadow-2xl">
                        <h2 className="text-xl font-bold mb-4 flex items-center gap-2">
                            <Settings className="w-5 h-5 text-purple-400" />
                            Health Thresholds
                        </h2>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm text-gray-400 mb-1">Green Threshold (seconds)</label>
                                <input
                                    type="number"
                                    value={config.greenThresholdSeconds}
                                    onChange={(e) => setConfig({ ...config, greenThresholdSeconds: Number(e.target.value) })}
                                    className="w-full bg-gray-800 border border-gray-700 rounded p-2 text-white focus:border-purple-500 outline-none"
                                />
                                <p className="text-xs text-gray-500 mt-1">If data is received within this time, status is GREEN.</p>
                            </div>
                            <div>
                                <label className="block text-sm text-gray-400 mb-1">Amber Threshold (seconds)</label>
                                <input
                                    type="number"
                                    value={config.amberThresholdSeconds}
                                    onChange={(e) => setConfig({ ...config, amberThresholdSeconds: Number(e.target.value) })}
                                    className="w-full bg-gray-800 border border-gray-700 rounded p-2 text-white focus:border-yellow-500 outline-none"
                                />
                                <p className="text-xs text-gray-500 mt-1">If data is delayed beyond Green but within this, status is AMBER.</p>
                            </div>
                        </div>
                        <div className="flex justify-end gap-3 mt-6">
                            <button
                                onClick={() => setShowConfig(false)}
                                className="px-4 py-2 text-gray-400 hover:text-white transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={updateConfig}
                                className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg font-medium transition-colors"
                            >
                                Save Changes
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
                {statuses.map((status) => (
                    <Card key={status.flowName} className={`${getStatusBg(status.health)} border-2 transition-all duration-300 hover:scale-[1.02] shadow-xl relative overflow-hidden group`}>
                        <div className={`absolute inset-0 bg-gradient-to-br from-transparent via-transparent to-white/5 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none`} />
                        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                            <CardTitle className="text-lg font-bold flex items-center gap-2">
                                {getIcon(status.flowName)}
                                <span className="truncate" title={status.flowName}>{status.flowName}</span>
                            </CardTitle>
                            <div className={`flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-bold border ${status.health === 'GREEN' ? 'bg-green-500/20 border-green-500 text-green-400' :
                                status.health === 'AMBER' ? 'bg-yellow-500/20 border-yellow-500 text-yellow-400' :
                                    'bg-red-500/20 border-red-500 text-red-500 animate-pulse'
                                }`}>
                                {status.health === 'GREEN' && <CheckCircle2 className="w-3 h-3" />}
                                {status.health === 'AMBER' && <AlertOctagon className="w-3 h-3" />}
                                {status.health === 'RED' && <AlertTriangle className="w-3 h-3" />}
                                {status.health}
                            </div>
                        </CardHeader>
                        <CardContent>
                            <div className="mt-4 flex items-center justify-between text-sm">
                                <span className="text-gray-400">Last Data:</span>
                                <span className="font-mono text-gray-300">
                                    {status.timeSinceLastData}
                                </span>
                            </div>

                            <div className="my-6 relative">
                                <div className="absolute left-0 top-1/2 w-full h-0.5 bg-gray-700/50 -z-10"></div>
                                <div className="flex justify-between items-center relative z-0">
                                    <div className="flex flex-col items-center bg-black/40 p-2 rounded backdrop-blur-sm border border-gray-800">
                                        <div className="h-10 w-10 rounded-full bg-blue-900/30 border border-blue-500/50 flex items-center justify-center mb-1 shadow-[0_0_10px_rgba(59,130,246,0.3)]">
                                            <Activity className="h-5 w-5 text-blue-400" />
                                        </div>
                                        <span className="text-[10px] text-gray-500 uppercase tracking-wider">Source</span>
                                        <span className="text-sm font-bold text-white tabular-nums">{status.sourceRate.toFixed(1)}</span>
                                    </div>

                                    <div className="flex flex-col items-center">
                                        <ArrowRight className={`h-5 w-5 ${status.sourceRate > 0 ? 'text-green-500 animate-pulse' : 'text-gray-600'}`} />
                                        {status.sourceRate === 0 && (status.sinkRate || 0) > 0 && (
                                            <span className="text-[10px] text-yellow-500 mt-1 animate-bounce">Lag?</span>
                                        )}
                                    </div>

                                    <div className="flex flex-col items-center bg-black/40 p-2 rounded backdrop-blur-sm border border-gray-800">
                                        <div className="h-10 w-10 rounded-full bg-purple-900/30 border border-purple-500/50 flex items-center justify-center mb-1 shadow-[0_0_10px_rgba(168,85,247,0.3)]">
                                            <ArrowRight className="h-5 w-5 text-purple-400 rotate-90" />
                                        </div>
                                        <span className="text-[10px] text-gray-500 uppercase tracking-wider">Sink</span>
                                        <span className="text-sm font-bold text-white tabular-nums">{status.sinkRate.toFixed(1)}</span>
                                    </div>
                                </div>
                            </div>

                            {/* Detailed Metrics Footer */}
                            <div className="pt-3 border-t border-gray-800 grid grid-cols-2 gap-3">
                                <div className="bg-gray-900/40 rounded p-2 flex flex-col border border-gray-800/50">
                                    <span className="text-[10px] text-gray-500 uppercase tracking-wider font-semibold mb-1">Latency</span>
                                    <div className="flex items-end gap-1">
                                        <span className={`text-lg font-mono font-bold leading-none ${(status.avgLatency || 0) > 1000 ? 'text-red-400' :
                                            (status.avgLatency || 0) > 500 ? 'text-yellow-400' : 'text-green-400'
                                            }`}>
                                            {(status.avgLatency || 0).toFixed(0)}
                                        </span>
                                        <span className="text-xs text-gray-500 mb-0.5">ms</span>
                                    </div>
                                </div>
                                <div className="bg-gray-900/40 rounded p-2 flex flex-col border border-gray-800/50">
                                    <span className="text-[10px] text-gray-500 uppercase tracking-wider font-semibold mb-1">Errors</span>
                                    <div className="flex items-end gap-1">
                                        <span className={`text-lg font-mono font-bold leading-none ${(status.errorRate || 0) > 0 ? 'text-red-500 animate-pulse' : 'text-gray-500'
                                            }`}>
                                            {(status.errorRate || 0).toFixed(2)}
                                        </span>
                                        <span className="text-xs text-gray-500 mb-0.5">/s</span>
                                    </div>
                                </div>
                            </div>
                        </CardContent>
                    </Card>
                ))}
            </div>
        </div>
    );
};

export default PipelinePage;
