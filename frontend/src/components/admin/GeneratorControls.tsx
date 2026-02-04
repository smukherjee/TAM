import React, { useState, useEffect, useCallback } from 'react';
import { Play, Pause, Square, RefreshCw, AlertTriangle, CheckCircle, Clock, Settings, ChevronDown, ChevronUp } from 'lucide-react';

// Types - matches backend BaseDataGenerator.GeneratorStats record
interface GeneratorStatus {
  generatorName: string;  // Backend returns generatorName, not name
  entityType: string;
  running: boolean;
  recordsGenerated: number;
  errorCount: number;
  startedAt: string | null;
}

interface OrchestratorStatus {
  batchMode: boolean;
  continuousMode: boolean;
  generatorCount: number;
  runningCount: number;
  generators: GeneratorStatus[];
}

interface GeneratorConfig {
  flightsPerHour: number;
  groundVehicleCount: number;
  batchSize: number;
  vehicleUpdatesPerSecond: number;
  flightUpdatesPerSecond: number;
  alertsPerHour: number;
}

interface GeneratorControlsProps {
  tenantCode?: string;
  onStatusChange?: (status: OrchestratorStatus) => void;
}

/**
 * T083: GeneratorControls component for admin control panel.
 * Provides UI controls for starting/stopping data generators.
 */
export const GeneratorControls: React.FC<GeneratorControlsProps> = ({
  tenantCode = 'YBBN',
  onStatusChange,
}) => {
  const [status, setStatus] = useState<OrchestratorStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showConfig, setShowConfig] = useState(false);
  const [config, setConfig] = useState<GeneratorConfig>({
    flightsPerHour: 20,
    groundVehicleCount: 100,
    batchSize: 100,
    vehicleUpdatesPerSecond: 5,
    flightUpdatesPerSecond: 2,
    alertsPerHour: 50,
  });

  // Fetch current status
  const fetchStatus = useCallback(async () => {
    try {
      const response = await fetch('/api/admin/generators/status');
      if (!response.ok) throw new Error('Failed to fetch status');
      const data = await response.json();
      setStatus(data);
      onStatusChange?.(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    }
  }, [onStatusChange]);

  useEffect(() => {
    fetchStatus();
    const interval = setInterval(fetchStatus, 5000); // Poll every 5 seconds
    return () => clearInterval(interval);
  }, [fetchStatus]);

  // Control actions
  const startBatchPopulation = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`/api/admin/generators/batch/${tenantCode}`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error('Failed to start batch');
      await fetchStatus();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start batch');
    } finally {
      setLoading(false);
    }
  };

  const startContinuous = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch('/api/admin/generators/continuous/start', {
        method: 'POST',
      });
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || errorData.error || `Server returned ${response.status}`);
      }
      await fetchStatus();
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to start continuous mode';
      setError(message || 'Failed to start continuous mode');
    } finally {
      setLoading(false);
    }
  };

  const stopContinuous = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch('/api/admin/generators/continuous/stop', {
        method: 'POST',
      });
      if (!response.ok) throw new Error('Failed to stop continuous mode');
      await fetchStatus();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to stop');
    } finally {
      setLoading(false);
    }
  };

  const startGenerator = async (name: string) => {
    setLoading(true);
    try {
      const response = await fetch(`/api/admin/generators/${name}/start`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error(`Failed to start ${name}`);
      await fetchStatus();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start generator');
    } finally {
      setLoading(false);
    }
  };

  const stopGenerator = async (name: string) => {
    setLoading(true);
    try {
      const response = await fetch(`/api/admin/generators/${name}/stop`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error(`Failed to stop ${name}`);
      await fetchStatus();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to stop generator');
    } finally {
      setLoading(false);
    }
  };

  const generateHistoricalData = async (days: number) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`/api/admin/generators/historical/${tenantCode}?days=${days}`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error('Failed to generate historical data');
      await fetchStatus();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate historical data');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-gray-800 rounded-lg shadow border border-gray-700 p-6">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-lg font-semibold text-white">Generator Controls</h2>
        <button
          onClick={fetchStatus}
          className="p-2 text-gray-400 hover:text-white"
          title="Refresh status"
        >
          <RefreshCw className={`w-5 h-5 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Error display */}
      {error && (
        <div className="mb-4 p-3 bg-red-900/50 text-red-300 rounded-lg flex items-center gap-2 border border-red-700">
          <AlertTriangle className="w-5 h-5" />
          {error}
        </div>
      )}

      {/* Configuration Panel */}
      <div className="mb-6 border border-gray-700 rounded-lg overflow-hidden">
        <button
          onClick={() => setShowConfig(!showConfig)}
          className="w-full flex items-center justify-between p-4 bg-gray-700/50 hover:bg-gray-700 transition-colors"
        >
          <div className="flex items-center gap-2">
            <Settings className="w-5 h-5 text-gray-400" />
            <span className="font-medium text-white">Generation Configuration</span>
          </div>
          {showConfig ? (
            <ChevronUp className="w-5 h-5 text-gray-400" />
          ) : (
            <ChevronDown className="w-5 h-5 text-gray-400" />
          )}
        </button>
        
        {showConfig && (
          <div className="p-4 bg-gray-800/50 space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {/* Flights Per Hour */}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">
                  Flights Per Hour
                </label>
                <input
                  type="number"
                  min="1"
                  max="100"
                  value={config.flightsPerHour}
                  onChange={(e) => setConfig({ ...config, flightsPerHour: parseInt(e.target.value) || 20 })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">Number of flights generated per hour</p>
              </div>

              {/* Ground Vehicle Count */}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">
                  Ground Vehicles
                </label>
                <input
                  type="number"
                  min="10"
                  max="500"
                  value={config.groundVehicleCount}
                  onChange={(e) => setConfig({ ...config, groundVehicleCount: parseInt(e.target.value) || 100 })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">Total GSE vehicles in fleet</p>
              </div>

              {/* Batch Size */}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">
                  Batch Size
                </label>
                <input
                  type="number"
                  min="10"
                  max="500"
                  value={config.batchSize}
                  onChange={(e) => setConfig({ ...config, batchSize: parseInt(e.target.value) || 100 })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">Records per batch operation</p>
              </div>

              {/* Vehicle Updates Per Second */}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">
                  Vehicle Update Rate
                </label>
                <input
                  type="number"
                  min="1"
                  max="20"
                  value={config.vehicleUpdatesPerSecond}
                  onChange={(e) => setConfig({ ...config, vehicleUpdatesPerSecond: parseInt(e.target.value) || 5 })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">Position updates per second</p>
              </div>

              {/* Flight Updates Per Second */}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">
                  Flight Update Rate
                </label>
                <input
                  type="number"
                  min="1"
                  max="10"
                  value={config.flightUpdatesPerSecond}
                  onChange={(e) => setConfig({ ...config, flightUpdatesPerSecond: parseInt(e.target.value) || 2 })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">Position updates per second</p>
              </div>

              {/* Alerts Per Hour */}
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-1">
                  Alerts Per Hour
                </label>
                <input
                  type="number"
                  min="0"
                  max="200"
                  value={config.alertsPerHour}
                  onChange={(e) => setConfig({ ...config, alertsPerHour: parseInt(e.target.value) || 50 })}
                  className="w-full px-3 py-2 bg-gray-700 border border-gray-600 rounded-lg text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
                <p className="text-xs text-gray-500 mt-1">Simulation alerts generated</p>
              </div>
            </div>

            <div className="flex justify-end pt-2 border-t border-gray-700">
              <p className="text-xs text-gray-500 italic">
                Note: Configuration changes will apply on next batch or continuous restart
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <div className="space-y-4 mb-6">
        <h3 className="text-sm font-medium text-gray-300">Quick Actions</h3>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={startBatchPopulation}
            disabled={loading || status?.batchMode}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg 
                     hover:bg-blue-700 disabled:bg-gray-600 disabled:cursor-not-allowed"
          >
            <Play className="w-4 h-4" />
            Run Batch ({tenantCode})
          </button>

          {status?.continuousMode ? (
            <button
              onClick={stopContinuous}
              disabled={loading}
              className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg 
                       hover:bg-red-700 disabled:bg-gray-600"
            >
              <Square className="w-4 h-4" />
              Stop Continuous
            </button>
          ) : (
            <button
              onClick={startContinuous}
              disabled={loading}
              className="flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg 
                       hover:bg-green-700 disabled:bg-gray-600"
            >
              <Play className="w-4 h-4" />
              Start Continuous
            </button>
          )}

          <button
            onClick={() => generateHistoricalData(7)}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg 
                     hover:bg-purple-700 disabled:bg-gray-600"
          >
            <Clock className="w-4 h-4" />
            Generate 7 Days History
          </button>
        </div>
      </div>

      {/* Status Summary */}
      {status && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="bg-gray-700/50 rounded-lg p-4 border border-gray-600">
            <div className="text-2xl font-bold text-white">{status.generatorCount}</div>
            <div className="text-sm text-gray-400">Total Generators</div>
          </div>
          <div className="bg-gray-700/50 rounded-lg p-4 border border-gray-600">
            <div className="text-2xl font-bold text-green-400">{status.runningCount}</div>
            <div className="text-sm text-gray-400">Running</div>
          </div>
          <div className="bg-gray-700/50 rounded-lg p-4 border border-gray-600">
            <div className="flex items-center gap-2">
              {status.batchMode ? (
                <CheckCircle className="w-6 h-6 text-blue-400" />
              ) : (
                <div className="w-6 h-6 rounded-full bg-gray-600" />
              )}
              <span className="text-sm text-gray-300">Batch Mode</span>
            </div>
          </div>
          <div className="bg-gray-700/50 rounded-lg p-4 border border-gray-600">
            <div className="flex items-center gap-2">
              {status.continuousMode ? (
                <CheckCircle className="w-6 h-6 text-green-400" />
              ) : (
                <div className="w-6 h-6 rounded-full bg-gray-600" />
              )}
              <span className="text-sm text-gray-300">Continuous Mode</span>
            </div>
          </div>
        </div>
      )}

      {/* Individual Generator Controls */}
      {status?.generators && (
        <div>
          <h3 className="text-sm font-medium text-gray-300 mb-3">Individual Generators</h3>
          <div className="space-y-2">
            {status.generators.map((gen) => (
              <div
                key={gen.generatorName}
                className="flex items-center justify-between p-3 bg-gray-700/50 rounded-lg border border-gray-600"
              >
                <div className="flex items-center gap-3">
                  <div
                    className={`w-3 h-3 rounded-full ${
                      gen.running ? 'bg-green-500' : 'bg-gray-500'
                    }`}
                  />
                  <div>
                    <div className="font-medium text-white">{gen.entityType}</div>
                    <div className="text-xs text-gray-400">{gen.generatorName}</div>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-right">
                    <div className="text-sm font-medium text-white">
                      {gen.recordsGenerated.toLocaleString()}
                    </div>
                    <div className="text-xs text-gray-400">records</div>
                  </div>
                  {gen.errorCount > 0 && (
                    <div className="text-right">
                      <div className="text-sm font-medium text-red-400">
                        {gen.errorCount}
                      </div>
                      <div className="text-xs text-gray-400">errors</div>
                    </div>
                  )}
                  <button
                    onClick={() => (gen.running ? stopGenerator(gen.generatorName) : startGenerator(gen.generatorName))}
                    disabled={loading}
                    className={`p-2 rounded-lg ${
                      gen.running
                        ? 'text-red-400 hover:bg-red-900/50'
                        : 'text-green-400 hover:bg-green-900/50'
                    }`}
                  >
                    {gen.running ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default GeneratorControls;
