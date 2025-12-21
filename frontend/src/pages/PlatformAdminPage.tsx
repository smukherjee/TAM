import { useState } from 'react';
import { Server, Database, Activity, BarChart, Settings, ExternalLink } from 'lucide-react';

const PlatformAdminPage = () => {
    const [activeTab, setActiveTab] = useState<'kafka' | 'nifi' | 'prometheus' | 'grafana' | 'superset'>('kafka');

    // Configuration for tools - using actual ports from docker-compose.dev.yml
    const tools = {
        kafka: {
            name: 'Redpanda (Kafka)',
            url: 'http://localhost:8090', // Redpanda Console mapped to 8090:8080
            icon: <Server className="w-4 h-4" />,
            description: 'Manage topics, consumer groups, and schemas.'
        },
        nifi: {
            name: 'Apache NiFi',
            url: 'http://localhost:8091/nifi', // NiFi mapped to 8091:8080 (HTTP not HTTPS)
            icon: <Settings className="w-4 h-4" />,
            description: 'Data flow management and ingestion pipelines.'
        },
        prometheus: {
            name: 'Prometheus',
            url: 'http://localhost:9090',
            icon: <Activity className="w-4 h-4" />,
            description: 'Time-series metric collection and querying.'
        },
        grafana: {
            name: 'Grafana',
            url: 'http://localhost:3001',
            icon: <BarChart className="w-4 h-4" />,
            description: 'Metric visualization and dashboarding.'
        },
        superset: {
            name: 'Superset',
            url: 'http://localhost:8089', // Superset mapped to 8089:8088
            icon: <Database className="w-4 h-4" />,
            description: 'Business intelligence and data exploration.'
        }
    };

    return (
        <div className="flex flex-col h-screen bg-gray-50 text-gray-900">
            {/* Header */}
            <div className="bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center shadow-sm z-10">
                <div>
                    <h1 className="text-2xl font-bold flex items-center gap-2 text-gray-800">
                        <Settings className="w-6 h-6 text-purple-600" />
                        Platform Administration
                    </h1>
                    <p className="text-sm text-gray-500 mt-1">Manage infrastructure, data pipelines, and analytics services.</p>
                </div>
                <div className="flex gap-2">
                    {/* Toolbar actions if needed */}
                </div>
            </div>

            {/* Content Area with Sidebar and Iframe */}
            <div className="flex flex-1 overflow-hidden">
                {/* Secondary Sidebar */}
                <div className="w-64 bg-gray-100 border-r border-gray-200 flex flex-col p-4 overflow-y-auto">
                    <div className="space-y-1">
                        {(Object.keys(tools) as Array<keyof typeof tools>).map((key) => (
                            <button
                                key={key}
                                onClick={() => setActiveTab(key)}
                                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all ${activeTab === key
                                    ? 'bg-purple-100 text-purple-700 shadow-sm border border-purple-200'
                                    : 'text-gray-600 hover:bg-gray-200 hover:text-gray-900'
                                    }`}
                            >
                                {tools[key].icon}
                                <span>{tools[key].name}</span>
                            </button>
                        ))}
                    </div>

                    <div className="mt-auto pt-4 border-t border-gray-200">
                        <div className="bg-blue-50 border border-blue-100 rounded-lg p-3">
                            <h4 className="text-xs font-semibold text-blue-800 mb-1">Access Issues?</h4>
                            <p className="text-[10px] text-blue-600 mb-2">
                                If the tool doesn't load below (e.g., due to SSL or embedding restrictions), open it in a new tab.
                            </p>
                            <a
                                href={tools[activeTab].url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="block w-full text-center bg-blue-100 hover:bg-blue-200 text-blue-700 text-xs py-1.5 rounded transition-colors flex items-center justify-center gap-1"
                            >
                                Open in New Tab <ExternalLink className="w-3 h-3" />
                            </a>
                        </div>
                    </div>
                </div>

                {/* Main View (Iframe or NiFi Card) */}
                <div className="flex-1 bg-white relative">
                    {activeTab === 'nifi' ? (
                        // Special view for NiFi since it blocks iframe embedding
                        <div className="h-full flex items-center justify-center bg-gradient-to-br from-gray-50 to-gray-100">
                            <div className="max-w-md mx-auto p-8">
                                <div className="bg-white rounded-xl shadow-lg p-8 border border-gray-200">
                                    <div className="flex items-center justify-center w-16 h-16 bg-orange-100 rounded-full mx-auto mb-4">
                                        <Settings className="w-8 h-8 text-orange-600" />
                                    </div>
                                    <h2 className="text-2xl font-bold text-gray-900 text-center mb-2">
                                        Apache NiFi
                                    </h2>
                                    <p className="text-gray-600 text-center mb-6">
                                        NiFi cannot be embedded due to security restrictions (X-Frame-Options).
                                        Please open it in a new tab to access the data flow management interface.
                                    </p>
                                    <a
                                        href={tools[activeTab].url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="block w-full bg-orange-600 hover:bg-orange-700 text-white font-semibold py-3 px-6 rounded-lg transition-colors flex items-center justify-center gap-2 shadow-md hover:shadow-lg"
                                    >
                                        <ExternalLink className="w-5 h-5" />
                                        Open NiFi in New Tab
                                    </a>
                                    <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-100">
                                        <p className="text-xs text-blue-800 font-medium mb-1">Default Credentials:</p>
                                        <p className="text-xs text-blue-700">
                                            Username: <code className="bg-blue-100 px-1 rounded">admin</code><br />
                                            Password: <code className="bg-blue-100 px-1 rounded">admin123456789</code>
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    ) : (
                        // Standard iframe for other tools
                        <iframe
                            src={tools[activeTab].url}
                            title={tools[activeTab].name}
                            className="w-full h-full border-none"
                            sandbox="allow-same-origin allow-scripts allow-popups allow-forms"
                        />
                    )}
                </div>
            </div>
        </div>
    );
};

export default PlatformAdminPage;
