import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Server, Database, Activity, BarChart, Settings, ExternalLink, Map, Route, Zap, MapPin } from 'lucide-react';

const PlatformAdminPage = () => {
    const [activeTab, setActiveTab] = useState<'admin-tools' | 'kafka' | 'nifi' | 'prometheus' | 'grafana' | 'superset'>('admin-tools');

    // Admin tool cards for internal pages
    const adminTools = [
        {
            name: 'Zone Editor',
            path: '/admin/zones',
            icon: <MapPin className="w-8 h-8 text-blue-400" />,
            description: 'Create and manage geofence zones for restricted areas, terminals, and aprons.',
            color: 'blue'
        },
        {
            name: 'Path Editor',
            path: '/admin/paths',
            icon: <Route className="w-8 h-8 text-green-400" />,
            description: 'Define vehicle movement paths for simulation and routing.',
            color: 'green'
        },
        {
            name: 'Data Generators',
            path: '/admin/generators',
            icon: <Zap className="w-8 h-8 text-yellow-400" />,
            description: 'Configure and control simulation data generators for flights, vehicles, and events.',
            color: 'yellow'
        }
    ];

    // Configuration for external tools - using actual ports from docker-compose.dev.yml
    const externalTools = {
        kafka: {
            name: 'Redpanda (Kafka)',
            url: 'http://localhost:8090',
            icon: <Server className="w-4 h-4" />,
            description: 'Manage topics, consumer groups, and schemas.'
        },
        nifi: {
            name: 'Apache NiFi',
            url: 'http://localhost:8091/nifi',
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
            url: 'http://localhost:8089',
            icon: <Database className="w-4 h-4" />,
            description: 'Business intelligence and data exploration.'
        }
    };

    return (
        <div className="flex flex-col h-screen bg-gray-900 text-white">
            {/* Header */}
            <div className="bg-gray-800 border-b border-gray-700 px-6 py-4 flex justify-between items-center shadow-sm z-10">
                <div>
                    <h1 className="text-2xl font-bold flex items-center gap-2 text-white">
                        <Settings className="w-6 h-6 text-purple-400" />
                        Platform Administration
                    </h1>
                    <p className="text-sm text-gray-400 mt-1">Manage zones, paths, data generators, and infrastructure services.</p>
                </div>
            </div>

            {/* Content Area with Sidebar */}
            <div className="flex flex-1 overflow-hidden">
                {/* Secondary Sidebar */}
                <div className="w-64 bg-gray-800 border-r border-gray-700 flex flex-col p-4 overflow-y-auto">
                    {/* Admin Tools Section */}
                    <div className="mb-4">
                        <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2 px-2">Admin Tools</h3>
                        <button
                            onClick={() => setActiveTab('admin-tools')}
                            className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all ${activeTab === 'admin-tools'
                                ? 'bg-purple-900/50 text-purple-300 shadow-sm border border-purple-700'
                                : 'text-gray-400 hover:bg-gray-700 hover:text-white'
                                }`}
                        >
                            <Map className="w-4 h-4" />
                            <span>Data Management</span>
                        </button>
                    </div>

                    {/* Infrastructure Section */}
                    <div className="mb-4">
                        <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2 px-2">Infrastructure</h3>
                        <div className="space-y-1">
                            {(Object.keys(externalTools) as Array<keyof typeof externalTools>).map((key) => (
                                <button
                                    key={key}
                                    onClick={() => setActiveTab(key)}
                                    className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all ${activeTab === key
                                        ? 'bg-purple-900/50 text-purple-300 shadow-sm border border-purple-700'
                                        : 'text-gray-400 hover:bg-gray-700 hover:text-white'
                                        }`}
                                >
                                    {externalTools[key].icon}
                                    <span>{externalTools[key].name}</span>
                                </button>
                            ))}
                        </div>
                    </div>

                    {activeTab !== 'admin-tools' && (
                        <div className="mt-auto pt-4 border-t border-gray-700">
                            <div className="bg-blue-900/30 border border-blue-700/50 rounded-lg p-3">
                                <h4 className="text-xs font-semibold text-blue-300 mb-1">Access Issues?</h4>
                                <p className="text-[10px] text-blue-400 mb-2">
                                    If the tool doesn't load below, open it in a new tab.
                                </p>
                                <a
                                    href={externalTools[activeTab as keyof typeof externalTools]?.url}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="block w-full text-center bg-blue-800/50 hover:bg-blue-700/50 text-blue-300 text-xs py-1.5 rounded transition-colors flex items-center justify-center gap-1"
                                >
                                    Open in New Tab <ExternalLink className="w-3 h-3" />
                                </a>
                            </div>
                        </div>
                    )}
                </div>

                {/* Main View */}
                <div className="flex-1 bg-gray-900 relative overflow-auto">
                    {activeTab === 'admin-tools' ? (
                        <div className="p-8">
                            <div className="max-w-4xl mx-auto">
                                <h2 className="text-xl font-bold text-white mb-6">Data Management Tools</h2>
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                                    {adminTools.map((tool) => (
                                        <Link
                                            key={tool.path}
                                            to={tool.path}
                                            className="bg-gray-800 border border-gray-700 rounded-xl p-6 hover:shadow-lg transition-shadow hover:border-gray-600"
                                        >
                                            <div className="w-16 h-16 bg-gray-700 rounded-xl flex items-center justify-center mb-4">
                                                {tool.icon}
                                            </div>
                                            <h3 className="text-lg font-semibold text-white mb-2">{tool.name}</h3>
                                            <p className="text-sm text-gray-400">{tool.description}</p>
                                        </Link>
                                    ))}
                                </div>

                                <div className="mt-8 p-6 bg-gradient-to-r from-purple-900/30 to-blue-900/30 rounded-xl border border-purple-700/50">
                                    <h3 className="text-lg font-semibold text-white mb-2">Quick Start</h3>
                                    <ul className="text-sm text-gray-300 space-y-2">
                                        <li className="flex items-start gap-2">
                                            <span className="text-purple-400 font-bold">1.</span>
                                            <span>Use <strong className="text-white">Zone Editor</strong> to define restricted zones, terminals, and operational areas.</span>
                                        </li>
                                        <li className="flex items-start gap-2">
                                            <span className="text-purple-400 font-bold">2.</span>
                                            <span>Use <strong className="text-white">Path Editor</strong> to create vehicle movement routes for simulation.</span>
                                        </li>
                                        <li className="flex items-start gap-2">
                                            <span className="text-purple-400 font-bold">3.</span>
                                            <span>Use <strong className="text-white">Data Generators</strong> to start simulating flights, vehicles, and turnaround events.</span>
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    ) : activeTab === 'nifi' ? (
                        <div className="h-full flex items-center justify-center bg-gradient-to-br from-gray-900 to-gray-800">
                            <div className="max-w-md mx-auto p-8">
                                <div className="bg-gray-800 rounded-xl shadow-lg p-8 border border-gray-700">
                                    <div className="flex items-center justify-center w-16 h-16 bg-orange-900/50 rounded-full mx-auto mb-4">
                                        <Settings className="w-8 h-8 text-orange-400" />
                                    </div>
                                    <h2 className="text-2xl font-bold text-white text-center mb-2">Apache NiFi</h2>
                                    <p className="text-gray-400 text-center mb-6">
                                        NiFi cannot be embedded due to security restrictions. Please open it in a new tab.
                                    </p>
                                    <a
                                        href={externalTools.nifi.url}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="block w-full bg-orange-600 hover:bg-orange-700 text-white font-semibold py-3 px-6 rounded-lg transition-colors flex items-center justify-center gap-2 shadow-md hover:shadow-lg"
                                    >
                                        <ExternalLink className="w-5 h-5" />
                                        Open NiFi in New Tab
                                    </a>
                                </div>
                            </div>
                        </div>
                    ) : (
                        <iframe
                            src={externalTools[activeTab as keyof typeof externalTools]?.url}
                            title={externalTools[activeTab as keyof typeof externalTools]?.name}
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
