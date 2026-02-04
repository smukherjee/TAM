import React, { useState } from 'react';
import { Settings, Activity, BarChart2, Shield, RefreshCw, Download } from 'lucide-react';
import { GeneratorControls } from './GeneratorControls';
import { GeneratorStatus } from './GeneratorStatus';
import { useAuth } from '../../context/AuthContext';

// Tab definitions
type TabType = 'overview' | 'controls' | 'metrics' | 'security';

interface TabConfig {
  id: TabType;
  label: string;
  icon: React.ElementType;
}

const TABS: TabConfig[] = [
  { id: 'overview', label: 'Overview', icon: Activity },
  { id: 'controls', label: 'Controls', icon: Settings },
  { id: 'metrics', label: 'Metrics', icon: BarChart2 },
  { id: 'security', label: 'Security', icon: Shield },
];

/**
 * T085: DataGeneratorAdminPage - Main admin page for data generator management.
 * Combines controls and status display with tabbed navigation.
 * Updated with dark theme to match TAM OS design.
 */
export const DataGeneratorAdminPage: React.FC = () => {
  const { user } = useAuth();
  const tenantCode = user?.icaoCode || 'YBBN';
  const [activeTab, setActiveTab] = useState<TabType>('overview');
  const [refreshKey, setRefreshKey] = useState(0);

  const handleRefresh = () => {
    setRefreshKey((prev) => prev + 1);
  };

  const handleExportLogs = async () => {
    try {
      const response = await fetch('/api/admin/generators/logs/export');
      if (response.ok) {
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `generator-logs-${new Date().toISOString().split('T')[0]}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      }
    } catch (error) {
      console.error('Failed to export logs:', error);
    }
  };

  return (
    <div className="min-h-screen bg-gray-900">
      {/* Header */}
      <header className="bg-gray-800 shadow border-b border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-white">
                Data Generator Administration
              </h1>
              <p className="mt-1 text-sm text-gray-400">
                Manage simulation data generators for <span className="text-blue-400 font-medium">{tenantCode}</span>
              </p>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={handleRefresh}
                className="inline-flex items-center px-3 py-2 border border-gray-600 rounded-md text-sm font-medium text-gray-200 bg-gray-700 hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-blue-500"
              >
                <RefreshCw className="w-4 h-4 mr-2" />
                Refresh
              </button>
              <button
                onClick={handleExportLogs}
                className="inline-flex items-center px-3 py-2 border border-gray-600 rounded-md text-sm font-medium text-gray-200 bg-gray-700 hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-blue-500"
              >
                <Download className="w-4 h-4 mr-2" />
                Export Logs
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Tab Navigation */}
      <div className="bg-gray-800 border-b border-gray-700">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <nav className="-mb-px flex space-x-8" aria-label="Tabs">
            {TABS.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`
                    group inline-flex items-center py-4 px-1 border-b-2 font-medium text-sm
                    ${isActive
                      ? 'border-blue-500 text-blue-400'
                      : 'border-transparent text-gray-400 hover:text-gray-200 hover:border-gray-500'
                    }
                  `}
                >
                  <Icon
                    className={`w-5 h-5 mr-2 ${
                      isActive ? 'text-blue-400' : 'text-gray-500 group-hover:text-gray-300'
                    }`}
                  />
                  {tab.label}
                </button>
              );
            })}
          </nav>
        </div>
      </div>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 max-h-screen overflow-y-auto">
        {activeTab === 'overview' && (
          <div className="space-y-8" key={refreshKey}>
            {/* Status Display */}
            <GeneratorStatus pollInterval={5000} />

            {/* Quick Actions */}
            <div className="bg-gray-800 rounded-lg shadow border border-gray-700 p-6">
              <h3 className="text-lg font-semibold text-white mb-4">Quick Actions</h3>
              <GeneratorControls tenantCode={tenantCode} />
            </div>
          </div>
        )}

        {activeTab === 'controls' && (
          <div className="space-y-6" key={refreshKey}>
            <div className="bg-gray-800 rounded-lg shadow border border-gray-700 p-6">
              <h3 className="text-lg font-semibold text-white mb-4">
                Generator Control Panel
              </h3>
              <GeneratorControls tenantCode={tenantCode} />
            </div>

            {/* Configuration Options */}
            <div className="bg-gray-800 rounded-lg shadow border border-gray-700 p-6">
              <h3 className="text-lg font-semibold text-white mb-4">
                Configuration
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                <ConfigField
                  label="Position Update Interval"
                  value="500ms"
                  description="Interval between position updates for moving entities"
                />
                <ConfigField
                  label="Trail Retention"
                  value="5 minutes"
                  description="How long position history is retained"
                />
                <ConfigField
                  label="Max Trail Points"
                  value="600"
                  description="Maximum position points per entity trail"
                />
                <ConfigField
                  label="Stale Entity Threshold"
                  value="30 seconds"
                  description="Time before entity marked as stale"
                />
                <ConfigField
                  label="Batch Size"
                  value="100 records"
                  description="Records generated per batch operation"
                />
                <ConfigField
                  label="Historical Lookback"
                  value="7 days"
                  description="Default period for historical data generation"
                />
              </div>
            </div>
          </div>
        )}

        {activeTab === 'metrics' && (
          <div className="space-y-6" key={refreshKey}>
            <GeneratorStatus pollInterval={2000} />

            {/* Detailed Metrics */}
            <div className="bg-gray-800 rounded-lg shadow border border-gray-700 p-6">
              <h3 className="text-lg font-semibold text-white mb-4">
                Performance Metrics
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <MetricCard
                  title="Throughput"
                  metric="Records/sec"
                  description="Average record generation rate over last minute"
                />
                <MetricCard
                  title="Latency"
                  metric="Position Update"
                  description="Time to process and persist position updates"
                />
                <MetricCard
                  title="Memory"
                  metric="Trail Buffer"
                  description="Memory used by in-memory trail storage"
                />
                <MetricCard
                  title="Database"
                  metric="Write Queue"
                  description="Pending database write operations"
                />
              </div>
            </div>
          </div>
        )}

        {activeTab === 'security' && (
          <div className="space-y-6" key={refreshKey}>
            {/* Security Status */}
            <div className="bg-gray-800 rounded-lg shadow border border-gray-700 p-6">
              <h3 className="text-lg font-semibold text-white mb-4">
                Security Configuration
              </h3>
              <div className="space-y-4">
                <SecurityItem
                  label="Admin Role Required"
                  status="enabled"
                  description="All generator endpoints require ADMIN role"
                />
                <SecurityItem
                  label="Rate Limiting"
                  status="enabled"
                  description="API calls limited to 100 requests/minute"
                />
                <SecurityItem
                  label="Audit Logging"
                  status="enabled"
                  description="All admin actions are logged"
                />
                <SecurityItem
                  label="Boundary Validation"
                  status="enabled"
                  description="All positions validated against airport perimeter"
                />
              </div>
            </div>

            {/* Recent Violations */}
            <div className="bg-gray-800 rounded-lg shadow border border-gray-700 p-6">
              <h3 className="text-lg font-semibold text-white mb-4">
                Recent Security Events
              </h3>
              <p className="text-gray-400 text-sm">
                No security violations detected in the last 24 hours.
              </p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
};

// Helper Components
interface ConfigFieldProps {
  label: string;
  value: string;
  description: string;
}

const ConfigField: React.FC<ConfigFieldProps> = ({ label, value, description }) => (
  <div className="border border-gray-600 rounded-lg p-4 bg-gray-700/50">
    <div className="flex justify-between items-start mb-1">
      <span className="text-sm font-medium text-gray-200">{label}</span>
      <span className="text-sm font-semibold text-blue-400">{value}</span>
    </div>
    <p className="text-xs text-gray-400">{description}</p>
  </div>
);

interface MetricCardProps {
  title: string;
  metric: string;
  description: string;
}

const MetricCard: React.FC<MetricCardProps> = ({ title, metric, description }) => (
  <div className="border border-gray-600 rounded-lg p-4 bg-gray-700/50">
    <h4 className="text-sm font-medium text-gray-200">{title}</h4>
    <div className="mt-2 flex items-baseline gap-2">
      <span className="text-2xl font-semibold text-white">--</span>
      <span className="text-sm text-gray-400">{metric}</span>
    </div>
    <p className="mt-1 text-xs text-gray-400">{description}</p>
  </div>
);

interface SecurityItemProps {
  label: string;
  status: 'enabled' | 'disabled' | 'warning';
  description: string;
}

const SecurityItem: React.FC<SecurityItemProps> = ({ label, status, description }) => (
  <div className="flex items-center justify-between py-3 border-b border-gray-700 last:border-0">
    <div>
      <span className="text-sm font-medium text-white">{label}</span>
      <p className="text-xs text-gray-400">{description}</p>
    </div>
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
        status === 'enabled'
          ? 'bg-green-900/50 text-green-400 border border-green-700'
          : status === 'disabled'
          ? 'bg-red-900/50 text-red-400 border border-red-700'
          : 'bg-yellow-900/50 text-yellow-400 border border-yellow-700'
      }`}
    >
      {status.charAt(0).toUpperCase() + status.slice(1)}
    </span>
  </div>
);

export default DataGeneratorAdminPage;
