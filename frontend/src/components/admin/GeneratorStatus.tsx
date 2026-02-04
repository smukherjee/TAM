import React, { useState, useEffect } from 'react';
import { Activity, Database, Clock, AlertCircle, TrendingUp, Server } from 'lucide-react';

// Types - matches backend BaseDataGenerator.GeneratorStats record
interface GeneratorStats {
  generatorName: string;  // Backend returns generatorName, not name
  entityType: string;
  running: boolean;
  recordsGenerated: number;
  errorCount: number;
  startedAt: string | null;
}

interface TrailStatistics {
  vehicleTrailCount: number;
  flightTrailCount: number;
  totalTrailPoints: number;
  maxTrailPointsPerEntity: number;
  retentionMinutes: number;
}

interface RegisterStatistics {
  vehicleCount: number;
  flightCount: number;
  staleThresholdMs: number;
}

interface SystemMetrics {
  totalRecords: number;
  recordsPerSecond: number;
  errorRate: number;
  uptime: string;
  memoryUsage: number;
  cpuUsage: number;
}

interface GeneratorStatusProps {
  pollInterval?: number; // in milliseconds, default 5000
}

/**
 * T084: GeneratorStatus component for displaying generator health and metrics.
 * Shows real-time status, throughput, and error information.
 */
export const GeneratorStatus: React.FC<GeneratorStatusProps> = ({
  pollInterval = 5000,
}) => {
  const [generators, setGenerators] = useState<GeneratorStats[]>([]);
  const [trailStats, setTrailStats] = useState<TrailStatistics | null>(null);
  const [registerStats, setRegisterStats] = useState<RegisterStatistics | null>(null);
  const [_systemMetrics, setSystemMetrics] = useState<SystemMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  // Fetch all status data
  useEffect(() => {
    const fetchStatus = async () => {
      try {
        // Fetch generator status
        const genResponse = await fetch('/api/admin/generators/status');
        if (genResponse.ok) {
          const data = await genResponse.json();
          setGenerators(data.generators || []);
        }

        // Fetch trail statistics
        const trailResponse = await fetch('/api/admin/generators/trails/stats');
        if (trailResponse.ok) {
          setTrailStats(await trailResponse.json());
        }

        // Fetch register statistics
        const regResponse = await fetch('/api/admin/generators/register/stats');
        if (regResponse.ok) {
          setRegisterStats(await regResponse.json());
        }

        // Fetch system metrics
        const metricsResponse = await fetch('/api/actuator/metrics');
        if (metricsResponse.ok) {
          // Parse Prometheus-style metrics
          const metricsData = await metricsResponse.json();
          setSystemMetrics(parseSystemMetrics(metricsData));
        }

        setLastUpdated(new Date());
        setLoading(false);
      } catch (error) {
        console.error('Failed to fetch status:', error);
        setLoading(false);
      }
    };

    fetchStatus();
    const interval = setInterval(fetchStatus, pollInterval);
    return () => clearInterval(interval);
  }, [pollInterval]);

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const parseSystemMetrics = (_data: unknown): SystemMetrics => {
    // Extract metrics from actuator data
    const totalRecords = generators.reduce((sum, g) => sum + g.recordsGenerated, 0);
    const totalErrors = generators.reduce((sum, g) => sum + g.errorCount, 0);
    
    return {
      totalRecords,
      recordsPerSecond: Math.round(totalRecords / 60), // Approximate
      errorRate: totalRecords > 0 ? (totalErrors / totalRecords) * 100 : 0,
      uptime: '00:00:00', // Would come from actual metrics
      memoryUsage: 0,
      cpuUsage: 0,
    };
  };

  const formatNumber = (num: number): string => {
    if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`;
    if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
    return num.toString();
  };

  const formatUptime = (startedAt: string | null): string => {
    if (!startedAt) return '-';
    const start = new Date(startedAt);
    const now = new Date();
    const diffMs = now.getTime() - start.getTime();
    const hours = Math.floor(diffMs / 3600000);
    const minutes = Math.floor((diffMs % 3600000) / 60000);
    const seconds = Math.floor((diffMs % 60000) / 1000);
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  if (loading) {
    return (
      <div className="animate-pulse space-y-4">
        <div className="h-32 bg-gray-700 rounded-lg" />
        <div className="h-64 bg-gray-700 rounded-lg" />
      </div>
    );
  }

  const runningCount = generators.filter((g) => g.running).length;
  const totalRecords = generators.reduce((sum, g) => sum + g.recordsGenerated, 0);
  const totalErrors = generators.reduce((sum, g) => sum + g.errorCount, 0);

  return (
    <div className="space-y-6 max-h-screen overflow-y-auto">
      {/* Overall Status Summary */}
      <div className="bg-gray-800 rounded-lg shadow border border-gray-700 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-white">System Status</h2>
          <span className="text-sm text-gray-400">
            Last updated: {lastUpdated.toLocaleTimeString()}
          </span>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
          {/* Running Generators */}
          <div className="bg-green-900/30 border border-green-700/50 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <Activity className="w-5 h-5 text-green-400" />
              <span className="text-sm text-green-300">Running</span>
            </div>
            <div className="text-2xl font-bold text-green-100">
              {runningCount}/{generators.length}
            </div>
          </div>

          {/* Total Records */}
          <div className="bg-blue-900/30 border border-blue-700/50 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <Database className="w-5 h-5 text-blue-400" />
              <span className="text-sm text-blue-300">Records</span>
            </div>
            <div className="text-2xl font-bold text-blue-100">
              {formatNumber(totalRecords)}
            </div>
          </div>

          {/* Active Trails */}
          {trailStats && (
            <div className="bg-purple-900/30 border border-purple-700/50 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-2">
                <TrendingUp className="w-5 h-5 text-purple-400" />
                <span className="text-sm text-purple-300">Trails</span>
              </div>
              <div className="text-2xl font-bold text-purple-100">
                {trailStats.vehicleTrailCount + trailStats.flightTrailCount}
              </div>
            </div>
          )}

          {/* Live Entities */}
          {registerStats && (
            <div className="bg-indigo-900/30 border border-indigo-700/50 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-2">
                <Server className="w-5 h-5 text-indigo-400" />
                <span className="text-sm text-indigo-300">Live Entities</span>
              </div>
              <div className="text-2xl font-bold text-indigo-100">
                {registerStats.vehicleCount + registerStats.flightCount}
              </div>
            </div>
          )}

          {/* Trail Points */}
          {trailStats && (
            <div className="bg-teal-900/30 border border-teal-700/50 rounded-lg p-4">
              <div className="flex items-center gap-2 mb-2">
                <Clock className="w-5 h-5 text-teal-400" />
                <span className="text-sm text-teal-300">Trail Points</span>
              </div>
              <div className="text-2xl font-bold text-teal-100">
                {formatNumber(trailStats.totalTrailPoints)}
              </div>
            </div>
          )}

          {/* Errors */}
          <div className={`rounded-lg p-4 border ${
            totalErrors > 0 
              ? 'bg-red-900/30 border-red-700/50' 
              : 'bg-gray-700/30 border-gray-600/50'
          }`}>
            <div className="flex items-center gap-2 mb-2">
              <AlertCircle className={`w-5 h-5 ${totalErrors > 0 ? 'text-red-400' : 'text-gray-400'}`} />
              <span className={`text-sm ${totalErrors > 0 ? 'text-red-300' : 'text-gray-300'}`}>
                Errors
              </span>
            </div>
            <div className={`text-2xl font-bold ${totalErrors > 0 ? 'text-red-100' : 'text-gray-100'}`}>
              {totalErrors}
            </div>
          </div>
        </div>
      </div>

      {/* Generator Details Table */}
      <div className="bg-gray-800 rounded-lg shadow border border-gray-700 overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-700">
          <h3 className="text-lg font-semibold text-white">Generator Details</h3>
        </div>
        <div className="overflow-auto max-h-48" style={{scrollbarWidth: 'thin', scrollbarColor: '#374151 #1f2937'}}>
          <table className="min-w-full divide-y divide-gray-700">
            <thead className="bg-gray-700/50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                  Generator
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                  Entity Type
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-300 uppercase tracking-wider">
                  Records
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-300 uppercase tracking-wider">
                  Errors
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-300 uppercase tracking-wider">
                  Uptime
                </th>
              </tr>
            </thead>
            <tbody className="bg-gray-800 divide-y divide-gray-700">
              {generators.map((gen) => (
                <tr key={gen.generatorName} className={gen.running ? '' : 'bg-gray-750'}>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <div className="flex items-center">
                      <div
                        className={`w-2.5 h-2.5 rounded-full mr-3 ${
                          gen.running ? 'bg-green-500 animate-pulse' : 'bg-gray-500'
                        }`}
                      />
                      <span className="font-medium text-white">{gen.entityType}</span>
                    </div>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                    {gen.entityType}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    <span
                      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                        gen.running
                          ? 'bg-green-900/50 text-green-300 border border-green-700'
                          : 'bg-gray-700 text-gray-300 border border-gray-600'
                      }`}
                    >
                      {gen.running ? 'Running' : 'Stopped'}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-white">
                    {formatNumber(gen.recordsGenerated)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right">
                    <span className={gen.errorCount > 0 ? 'text-red-400 font-medium' : 'text-gray-400'}>
                      {gen.errorCount}
                    </span>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-right text-gray-400">
                    {formatUptime(gen.startedAt)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default GeneratorStatus;
