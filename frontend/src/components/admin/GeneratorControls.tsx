import React, { useState, useEffect, useCallback } from 'react';
import { Play, Pause, Square, RefreshCw, AlertTriangle, CheckCircle, Clock, Settings, ChevronDown, ChevronUp, Trash2, Database, Globe } from 'lucide-react';

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

interface TenantInfo {
  icaoCode: string;
  name: string;
  location: string;
  timezone: string;
}

// Supported airports
const SUPPORTED_TENANTS: TenantInfo[] = [
  { icaoCode: 'VIDP', name: 'Indira Gandhi International Airport', location: 'Delhi, India', timezone: 'Asia/Kolkata' },
  { icaoCode: 'LIRN', name: 'Naples International Airport', location: 'Naples, Italy', timezone: 'Europe/Rome' },
  { icaoCode: 'YBBN', name: 'Brisbane Airport', location: 'Brisbane, Australia', timezone: 'Australia/Brisbane' },
];

interface GeneratorControlsProps {
  tenantCode?: string;
  onStatusChange?: (status: OrchestratorStatus) => void;
}

/**
 * T083: GeneratorControls component for admin control panel.
 * Provides UI controls for starting/stopping data generators.
 * Supports multi-tenant data generation for VIDP, LIRN, YBBN.
 */
export const GeneratorControls: React.FC<GeneratorControlsProps> = ({
  tenantCode: initialTenantCode = 'YBBN',
  onStatusChange,
}) => {
  const [selectedTenant, setSelectedTenant] = useState(initialTenantCode);
  const [status, setStatus] = useState<OrchestratorStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [actionInProgress, setActionInProgress] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
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

  // Clear success message after 5 seconds
  useEffect(() => {
    if (successMessage) {
      const timer = setTimeout(() => setSuccessMessage(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [successMessage]);

  // Control actions
  const startBatchPopulation = async (tenant: string = selectedTenant) => {
    setLoading(true);
    setActionInProgress('batch');
    setError(null);
    try {
      const response = await fetch(`/api/admin/generators/batch/${tenant}?batchSize=${config.batchSize}`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error('Failed to start batch');
      const result = await response.json();
      setSuccessMessage(`Generated batch data for ${tenant}: ${Object.values(result.results || {}).reduce((a: number, b: unknown) => a + (b as number), 0)} records`);
      await fetchStatus();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to start batch');
    } finally {
      setLoading(false);
      setActionInProgress(null);
    }
  };

  const clearData = async (tenant: string = selectedTenant) => {
    if (!confirm(`Are you sure you want to clear all simulation data for ${tenant}? This cannot be undone.`)) {
      return;
    }
    setLoading(true);
    setActionInProgress('clear');
    setError(null);
    try {
      const response = await fetch(`/api/admin/generators/clear/${tenant}`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error('Failed to clear data');
      const result = await response.json();
      setSuccessMessage(`Cleared ${result.deletedCount} records for ${tenant}`);
      await fetchStatus();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to clear data');
    } finally {
      setLoading(false);
      setActionInProgress(null);
    }
  };

  const generateFreshData = async (tenant: string = selectedTenant) => {
    setLoading(true);
    setActionInProgress('fresh');
    setError(null);
    try {
      // Step 1: Clear existing data
      setSuccessMessage(`Clearing existing data for ${tenant}...`);
      const clearResponse = await fetch(`/api/admin/generators/clear/${tenant}`, {
        method: 'POST',
      });
      if (!clearResponse.ok) throw new Error('Failed to clear data');

      // Step 2: Generate batch data
      setSuccessMessage(`Generating batch data for ${tenant}...`);
      const batchResponse = await fetch(`/api/admin/generators/batch/${tenant}?batchSize=${config.batchSize}`, {
        method: 'POST',
      });
      if (!batchResponse.ok) throw new Error('Failed to generate batch data');

      // Step 3: Generate historical data
      setSuccessMessage(`Generating historical data for ${tenant}...`);
      const histResponse = await fetch(`/api/admin/generators/historical/${tenant}?days=7&samplesPerDay=24`, {
        method: 'POST',
      });
      if (!histResponse.ok) throw new Error('Failed to generate historical data');

      // Step 4: Refresh views
      setSuccessMessage(`Refreshing materialized views...`);
      await fetch('/api/admin/generators/refresh-views', { method: 'POST' });

      setSuccessMessage(`Successfully generated fresh data for ${tenant}!`);
      await fetchStatus();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate fresh data');
    } finally {
      setLoading(false);
      setActionInProgress(null);
    }
  };

  const generateForAllTenants = async () => {
    if (!confirm('Generate fresh data for ALL airports (VIDP, LIRN, YBBN)? This will clear and regenerate all simulation data.')) {
      return;
    }
    setLoading(true);
    setActionInProgress('all');
    setError(null);
    try {
      for (const tenant of SUPPORTED_TENANTS) {
        setSuccessMessage(`Processing ${tenant.icaoCode} (${tenant.location})...`);
        await generateFreshDataForTenant(tenant.icaoCode);
      }
      setSuccessMessage('Successfully generated data for all airports!');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate data for all tenants');
    } finally {
      setLoading(false);
      setActionInProgress(null);
    }
  };

  const generateFreshDataForTenant = async (tenant: string) => {
    const clearResponse = await fetch(`/api/admin/generators/clear/${tenant}`, { method: 'POST' });
    if (!clearResponse.ok) throw new Error(`Failed to clear data for ${tenant}`);
    
    const batchResponse = await fetch(`/api/admin/generators/batch/${tenant}?batchSize=${config.batchSize}`, { method: 'POST' });
    if (!batchResponse.ok) throw new Error(`Failed to generate batch for ${tenant}`);
    
    const histResponse = await fetch(`/api/admin/generators/historical/${tenant}?days=7&samplesPerDay=24`, { method: 'POST' });
    if (!histResponse.ok) throw new Error(`Failed to generate history for ${tenant}`);
  };

  const refreshViews = async () => {
    setLoading(true);
    setActionInProgress('refresh');
    setError(null);
    try {
      const response = await fetch('/api/admin/generators/refresh-views', { method: 'POST' });
      if (!response.ok) throw new Error('Failed to refresh views');
      setSuccessMessage('Materialized views refreshed successfully');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to refresh views');
    } finally {
      setLoading(false);
      setActionInProgress(null);
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

  const generateHistoricalData = async (days: number, tenant: string = selectedTenant) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`/api/admin/generators/historical/${tenant}?days=${days}`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error('Failed to generate historical data');
      setSuccessMessage(`Generated ${days} days of historical data for ${tenant}`);
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

      {/* Success message */}
      {successMessage && (
        <div className="mb-4 p-3 bg-green-900/50 text-green-300 rounded-lg flex items-center gap-2 border border-green-700">
          <CheckCircle className="w-5 h-5" />
          {successMessage}
        </div>
      )}

      {/* Error display */}
      {error && (
        <div className="mb-4 p-3 bg-red-900/50 text-red-300 rounded-lg flex items-center gap-2 border border-red-700">
          <AlertTriangle className="w-5 h-5" />
          {error}
        </div>
      )}

      {/* Tenant Selection */}
      <div className="mb-6 p-4 bg-gray-700/50 rounded-lg border border-gray-600">
        <div className="flex items-center gap-2 mb-3">
          <Globe className="w-5 h-5 text-blue-400" />
          <span className="font-medium text-white">Select Airport</span>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          {SUPPORTED_TENANTS.map((tenant) => (
            <button
              key={tenant.icaoCode}
              onClick={() => setSelectedTenant(tenant.icaoCode)}
              className={`p-3 rounded-lg border transition-all text-left ${
                selectedTenant === tenant.icaoCode
                  ? 'bg-blue-600/30 border-blue-500 text-white'
                  : 'bg-gray-700/50 border-gray-600 text-gray-300 hover:border-gray-500'
              }`}
            >
              <div className="font-bold text-lg">{tenant.icaoCode}</div>
              <div className="text-xs opacity-75">{tenant.name}</div>
              <div className="text-xs opacity-50">{tenant.location}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Fresh Data Generation */}
      <div className="mb-6 p-4 bg-gradient-to-r from-blue-900/30 to-purple-900/30 rounded-lg border border-blue-700/50">
        <div className="flex items-center gap-2 mb-3">
          <Database className="w-5 h-5 text-blue-400" />
          <span className="font-medium text-white">Data Generation</span>
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={() => generateFreshData(selectedTenant)}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-blue-600 to-purple-600 text-white rounded-lg 
                     hover:from-blue-700 hover:to-purple-700 disabled:from-gray-600 disabled:to-gray-600 disabled:cursor-not-allowed"
          >
            {actionInProgress === 'fresh' ? (
              <RefreshCw className="w-4 h-4 animate-spin" />
            ) : (
              <Database className="w-4 h-4" />
            )}
            Generate Fresh Data ({selectedTenant})
          </button>

          <button
            onClick={generateForAllTenants}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-green-600 to-teal-600 text-white rounded-lg 
                     hover:from-green-700 hover:to-teal-700 disabled:from-gray-600 disabled:to-gray-600 disabled:cursor-not-allowed"
          >
            {actionInProgress === 'all' ? (
              <RefreshCw className="w-4 h-4 animate-spin" />
            ) : (
              <Globe className="w-4 h-4" />
            )}
            Generate All Airports
          </button>

          <button
            onClick={() => clearData(selectedTenant)}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-lg 
                     hover:bg-red-700 disabled:bg-gray-600 disabled:cursor-not-allowed"
          >
            {actionInProgress === 'clear' ? (
              <RefreshCw className="w-4 h-4 animate-spin" />
            ) : (
              <Trash2 className="w-4 h-4" />
            )}
            Clear Data ({selectedTenant})
          </button>

          <button
            onClick={refreshViews}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg 
                     hover:bg-gray-500 disabled:bg-gray-700 disabled:cursor-not-allowed"
          >
            {actionInProgress === 'refresh' ? (
              <RefreshCw className="w-4 h-4 animate-spin" />
            ) : (
              <RefreshCw className="w-4 h-4" />
            )}
            Refresh Views
          </button>
        </div>
        <p className="mt-3 text-xs text-gray-400">
          "Generate Fresh Data" will clear existing data, generate batch data, create 7 days of historical data, and refresh views.
        </p>
      </div>

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
            onClick={() => startBatchPopulation(selectedTenant)}
            disabled={loading || status?.batchMode}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg 
                     hover:bg-blue-700 disabled:bg-gray-600 disabled:cursor-not-allowed"
          >
            <Play className="w-4 h-4" />
            Run Batch ({selectedTenant})
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
            onClick={() => generateHistoricalData(7, selectedTenant)}
            disabled={loading}
            className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg 
                     hover:bg-purple-700 disabled:bg-gray-600"
          >
            <Clock className="w-4 h-4" />
            Generate 7 Days History ({selectedTenant})
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
