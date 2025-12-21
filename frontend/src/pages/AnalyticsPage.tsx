import { useState } from 'react';
import { BarChart3, TrendingUp, Activity, Users, Server, AlertTriangle, Building, Database } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const AnalyticsPage = () => {
    const { user } = useAuth();
    const [activeTab, setActiveTab] = useState<'executive' | 'flight' | 'turnaround' | 'vehicle' | 'pipeline' | 'infrastructure' | 'anomaly' | 'stand' | 'superset'>('executive');

    // Determine Superset Dashboard Slug based on User Context
    const getDashboardSlug = () => {
        if (!user || user.role === 'ADMIN') return 'tam_ops';
        const icao = user.icaoCode ? user.icaoCode.toLowerCase() : '';
        if (icao) return `tam_ops_${icao}`;
        return 'tam_ops';
    };

    const dashboardSlug = getDashboardSlug();
    const supersetUrl = `http://localhost:8089/superset/dashboard/${dashboardSlug}/?standalone=2&show_filters=0`;

    // Grafana dashboard configurations
    const dashboards = {
        executive: {
            name: 'Executive Operations',
            url: 'http://localhost:3001/d/tam-exec-ops/executive-operations-dashboard?orgId=1&refresh=30s&kiosk',
            icon: <BarChart3 className="w-4 h-4" />,
            description: 'High-level KPIs and critical metrics'
        },
        flight: {
            name: 'Flight Operations',
            url: 'http://localhost:3001/d/tam-flight-ops/flight-operations-dashboard?orgId=1&refresh=10s&kiosk',
            icon: <Activity className="w-4 h-4" />,
            description: 'Real-time flight tracking and status'
        },
        turnaround: {
            name: 'Turnaround Performance',
            url: 'http://localhost:3001/d/tam-turnaround-perf/turnaround-performance?orgId=1&refresh=30s&kiosk',
            icon: <TrendingUp className="w-4 h-4" />,
            description: 'Turnaround activities and efficiency'
        },
        vehicle: {
            name: 'Ground Vehicle Tracking',
            url: 'http://localhost:3001/d/tam-vehicle-tracking/ground-vehicle-tracking?orgId=1&refresh=10s&kiosk',
            icon: <Users className="w-4 h-4" />,
            description: 'Ground support vehicle monitoring'
        },
        pipeline: {
            name: 'Data Pipeline Health',
            url: 'http://localhost:3001/d/tam-pipeline-health/data-pipeline-health?orgId=1&refresh=10s&kiosk',
            icon: <Server className="w-4 h-4" />,
            description: 'Pipeline metrics and data flow'
        },
        infrastructure: {
            name: 'Infrastructure Metrics',
            url: 'http://localhost:3001/d/tam-infra-metrics/infrastructure-metrics?orgId=1&refresh=30s&kiosk',
            icon: <Server className="w-4 h-4" />,
            description: 'System health and performance'
        },
        anomaly: {
            name: 'Anomaly Detection',
            url: 'http://localhost:3001/d/tam-anomaly-detection/anomaly-detection?orgId=1&refresh=1m&kiosk',
            icon: <AlertTriangle className="w-4 h-4" />,
            description: 'Outliers and unusual patterns'
        },
        stand: {
            name: 'Stand Utilization',
            url: 'http://localhost:3001/d/tam-stand-utilization/stand-utilization-planning?orgId=1&refresh=1m&kiosk',
            icon: <Building className="w-4 h-4" />,
            description: 'Stand occupancy and planning'
        },
        superset: {
            name: 'Superset BI Reports',
            url: supersetUrl,
            icon: <Database className="w-4 h-4" />,
            description: 'Business intelligence dashboards'
        }
    };

    return (
        <div className="flex flex-col h-screen bg-gray-50 text-gray-900">
            {/* Header */}
            <div className="bg-white border-b border-gray-200 px-6 py-4 flex justify-between items-center shadow-sm z-10">
                <div>
                    <h1 className="text-2xl font-bold flex items-center gap-2 text-gray-800">
                        <BarChart3 className="w-6 h-6 text-blue-600" />
                        Analytics Dashboards
                    </h1>
                    <p className="text-sm text-gray-500 mt-1">Real-time operational insights and performance metrics.</p>
                </div>
            </div>

            {/* Content Area with Sidebar and Iframe */}
            <div className="flex flex-1 overflow-hidden">
                {/* Dashboard Sidebar */}
                <div className="w-64 bg-gray-100 border-r border-gray-200 flex flex-col p-4 overflow-y-auto">
                    <div className="space-y-1">
                        {(Object.keys(dashboards) as Array<keyof typeof dashboards>).map((key) => (
                            <button
                                key={key}
                                onClick={() => setActiveTab(key)}
                                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all ${activeTab === key
                                    ? 'bg-blue-100 text-blue-700 shadow-sm border border-blue-200'
                                    : 'text-gray-600 hover:bg-gray-200 hover:text-gray-900'
                                    }`}
                            >
                                {dashboards[key].icon}
                                <div className="text-left flex-1">
                                    <div>{dashboards[key].name}</div>
                                    <div className="text-[10px] opacity-70 mt-0.5">
                                        {dashboards[key].description}
                                    </div>
                                </div>
                            </button>
                        ))}
                    </div>

                    <div className="mt-auto pt-4 border-t border-gray-200">
                        <div className="bg-green-50 border border-green-100 rounded-lg p-3">
                            <h4 className="text-xs font-semibold text-green-800 mb-1 flex items-center gap-1">
                                <Activity className="w-3 h-3" />
                                Live Data
                            </h4>
                            <p className="text-[10px] text-green-600">
                                Dashboards auto-refresh every 10-30 seconds with real-time data from Prometheus and TimescaleDB.
                            </p>
                        </div>
                    </div>
                </div>

                {/* Main Dashboard View (Iframe) */}
                <div className="flex-1 bg-white relative">
                    <iframe
                        src={dashboards[activeTab].url}
                        title={dashboards[activeTab].name}
                        className="w-full h-full border-none"
                        sandbox="allow-same-origin allow-scripts allow-popups allow-forms"
                    />
                </div>
            </div>
        </div>
    );
};

export default AnalyticsPage;
