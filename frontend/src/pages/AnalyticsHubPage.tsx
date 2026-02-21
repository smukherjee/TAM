import { useState, useMemo } from 'react';
import {
  BarChart3, TrendingUp, Activity, Shield, Truck, Brain,
  Gauge, AlertTriangle, Timer, Package, Zap, Map,
  ChevronDown, ChevronRight, ExternalLink, Building, Server,
  Users, RefreshCw
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

// Chart type definitions
type ChartType = 'table' | 'line' | 'kpi' | 'map' | 'bar';

interface Chart {
  id: string;
  sliceId: number; // Actual Superset chart ID
  name: string;
  type: ChartType;
  description?: string;
}

interface ChartCategory {
  name: string;
  icon: React.ReactNode;
  description: string;
  color: string;
  charts: Chart[];
  supersetDashboard: string;
}

// Define all 28 Superset charts organized by category
const chartCategories: Record<string, ChartCategory> = {
  operations: {
    name: 'Operations Overview',
    icon: <Activity className="w-4 h-4" />,
    description: 'Daily operations, flights, and throughput metrics',
    color: 'blue',
    supersetDashboard: 'tam_ops_full',
    charts: [
      { id: 'ops_overview_daily', sliceId: 1, name: 'Ops Overview Daily', type: 'table', description: 'Daily flight and vehicle counts' },
      { id: 'flight_movements_hourly', sliceId: 2, name: 'Flight Movements Hourly', type: 'table', description: 'Hourly flight position data' },
      { id: 'flight_movements_line', sliceId: 22, name: 'Flight Movements (Trend)', type: 'line', description: 'Flight activity over time' },
      { id: 'vehicle_activity_daily', sliceId: 3, name: 'Vehicle Activity Daily', type: 'table', description: 'Ground vehicle telemetry summary' },
      { id: 'throughput_today', sliceId: 15, name: 'Throughput Today', type: 'kpi', description: 'Today\'s operational volume' },
    ]
  },
  safety: {
    name: 'Safety & Security',
    icon: <Shield className="w-4 h-4" />,
    description: 'Zone violations, breaches, and security alerts',
    color: 'red',
    supersetDashboard: 'tam_safety',
    charts: [
      { id: 'violations_by_zone', sliceId: 7, name: 'Violations by Zone', type: 'table', description: 'Speed and zone violations' },
      { id: 'breach_dwell_stats', sliceId: 8, name: 'Breach Dwell Stats', type: 'table', description: 'Time spent in restricted zones' },
      { id: 'discrepancy_trends', sliceId: 9, name: 'Discrepancy Trends Daily', type: 'table', description: 'Movement discrepancy patterns' },
      { id: 'repeat_offenders', sliceId: 14, name: 'Repeat Offenders', type: 'table', description: 'Assets with frequent violations' },
      { id: 'alerts_by_type_line', sliceId: 23, name: 'Alerts by Type (Trend)', type: 'line', description: 'Alert frequency over time' },
      { id: 'violation_heatmap', sliceId: 21, name: 'Violation Heatmap', type: 'map', description: 'Geographic violation density' },
    ]
  },
  turnaround: {
    name: 'Turnaround Performance',
    icon: <Timer className="w-4 h-4" />,
    description: 'Stand occupancy, SLA compliance, and delays',
    color: 'green',
    supersetDashboard: 'tam_turnaround',
    charts: [
      { id: 'stand_occupancy', sliceId: 4, name: 'Stand Occupancy', type: 'table', description: 'Gate/stand utilization metrics' },
      { id: 'sla_compliance', sliceId: 5, name: 'SLA Compliance by Task', type: 'table', description: 'On-time task completion rates' },
      { id: 'delay_root_causes', sliceId: 6, name: 'Delay Root Causes', type: 'bar', description: 'Primary causes of turnaround delays' },
      { id: 'stand_conflicts', sliceId: 19, name: 'Stand Conflicts', type: 'table', description: 'Overlapping stand assignments' },
    ]
  },
  assets: {
    name: 'Asset Management',
    icon: <Package className="w-4 h-4" />,
    description: 'Asset utilization, maintenance, and activity',
    color: 'purple',
    supersetDashboard: 'tam_assets',
    charts: [
      { id: 'asset_utilization', sliceId: 10, name: 'Asset Utilization Status', type: 'table', description: 'Current asset status counts' },
      { id: 'maintenance_downtime', sliceId: 11, name: 'Maintenance Downtime', type: 'table', description: 'Assets under maintenance' },
      { id: 'dwell_proxy_hourly', sliceId: 12, name: 'Dwell by Zone Hourly', type: 'table', description: 'Asset dwell time patterns' },
      { id: 'activity_heatmap', sliceId: 20, name: 'Activity Heatmap', type: 'map', description: 'Geographic activity density' },
    ]
  },
  predictive: {
    name: 'Predictive Analytics',
    icon: <Brain className="w-4 h-4" />,
    description: 'ML-powered forecasts and risk predictions',
    color: 'amber',
    supersetDashboard: 'tam_predictive',
    charts: [
      { id: 'turnaround_risk', sliceId: 24, name: 'Turnaround Risk', type: 'table', description: 'Predicted delay risk scores' },
      { id: 'congestion_forecast', sliceId: 25, name: 'Congestion Forecast', type: 'table', description: 'Expected zone congestion' },
      { id: 'zone_breach_probability', sliceId: 26, name: 'Zone Breach Probability', type: 'table', description: 'Likelihood of zone violations' },
      { id: 'asset_violation_risk', sliceId: 27, name: 'Asset Violation Risk', type: 'table', description: 'Per-asset risk assessment' },
      { id: 'violations_forecast', sliceId: 28, name: 'Violations Forecast (Hourly)', type: 'line', description: 'Predicted violation counts' },
    ]
  },
  pipeline: {
    name: 'Data Pipeline',
    icon: <Zap className="w-4 h-4" />,
    description: 'Pipeline health and event throughput',
    color: 'cyan',
    supersetDashboard: 'tam_pipeline',
    charts: [
      { id: 'pipeline_events', sliceId: 16, name: 'Pipeline Events/min', type: 'line', description: 'Data ingestion rate' },
    ]
  },
  heatmaps: {
    name: 'Spatial Analytics',
    icon: <Map className="w-4 h-4" />,
    description: 'Geographic heatmaps and spatial patterns',
    color: 'orange',
    supersetDashboard: 'tam_ops_full',
    charts: [
      { id: 'activity_heatmap_table', sliceId: 17, name: 'Activity Heatmap (Data)', type: 'table', description: 'Raw heatmap grid data' },
      { id: 'violation_heatmap_table', sliceId: 18, name: 'Violation Heatmap (Data)', type: 'table', description: 'Violation location data' },
    ]
  }
};

// Grafana dashboards configuration
const grafanaDashboards = {
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
    icon: <Truck className="w-4 h-4" />,
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
  }
};

type AnalyticsSource = 'grafana' | 'superset';
type CategoryKey = keyof typeof chartCategories;
type GrafanaKey = keyof typeof grafanaDashboards;

const AnalyticsHubPage = () => {
  const { user } = useAuth();
  const tenantCode = user?.icaoCode?.toUpperCase();
  const [source, setSource] = useState<AnalyticsSource>('superset');
  const [activeCategory, setActiveCategory] = useState<CategoryKey>('operations');
  const [activeGrafana, setActiveGrafana] = useState<GrafanaKey>('executive');
  const [expandedCategories, setExpandedCategories] = useState<Set<CategoryKey>>(new Set(['operations']));
  const [selectedChart, setSelectedChart] = useState<string | null>(null);

  // Calculate total charts
  const totalCharts = useMemo(() => {
    return Object.values(chartCategories).reduce((sum, cat) => sum + cat.charts.length, 0);
  }, []);

  const withTenantScope = (url: string, provider: 'grafana' | 'superset') => {
    if (!tenantCode) return url;
    const scopedUrl = new URL(url);
    if (provider === 'grafana') {
      // Support common dashboard variable names; unknown vars are ignored by Grafana.
      scopedUrl.searchParams.set('var-tenantCode', tenantCode);
      scopedUrl.searchParams.set('var-tenant_code', tenantCode);
      scopedUrl.searchParams.set('var-icao', tenantCode);
      scopedUrl.searchParams.set('var-airport', tenantCode);
    } else {
      scopedUrl.searchParams.set('tenant_code', tenantCode);
      scopedUrl.searchParams.set('icao', tenantCode);
    }
    return scopedUrl.toString();
  };

  // Superset dashboards are shared; tenant scope is passed as URL params.
  const getSupersetDashboardUrl = (dashboardSlug: string) => {
    const url = `http://localhost:8089/superset/dashboard/${dashboardSlug}/?standalone=2&show_filters=0`;
    return withTenantScope(url, 'superset');
  };

  // Chart-specific embeds are most stable via direct slice routes.
  const getSupersetChartUrl = (chartId: string) => {
    for (const category of Object.values(chartCategories)) {
      const chart = category.charts.find((c) => c.id === chartId);
      if (chart) {
        // Keep tenant in form_data as well; Superset may convert URL params to form_data_key during load.
        const formData: Record<string, unknown> = { slice_id: Number(chart.sliceId) };
        if (tenantCode) {
          formData.tenant_code = tenantCode;
          formData.icao = tenantCode;
          formData.url_params = { tenant_code: tenantCode, icao: tenantCode };
        }
        const encodedFormData = encodeURIComponent(JSON.stringify(formData));
        const url = `http://localhost:8089/explore/?form_data=${encodedFormData}&standalone=true`;
        return withTenantScope(url, 'superset');
      }
    }
    return getSupersetDashboardUrl(chartCategories[activeCategory].supersetDashboard);
  };

  const toggleCategory = (category: CategoryKey) => {
    const newExpanded = new Set(expandedCategories);
    if (newExpanded.has(category)) {
      newExpanded.delete(category);
    } else {
      newExpanded.add(category);
    }
    setExpandedCategories(newExpanded);
    setActiveCategory(category);
    setSelectedChart(null); // Reset chart selection when changing category
  };

  const selectChart = (chartId: string) => {
    setSelectedChart(selectedChart === chartId ? null : chartId);
  };

  const colorClasses: Record<string, { bg: string; border: string; text: string; badge: string }> = {
    blue: { bg: 'bg-blue-900/30', border: 'border-blue-500/50', text: 'text-blue-400', badge: 'bg-blue-900/50 text-blue-300' },
    red: { bg: 'bg-red-900/30', border: 'border-red-500/50', text: 'text-red-400', badge: 'bg-red-900/50 text-red-300' },
    green: { bg: 'bg-green-900/30', border: 'border-green-500/50', text: 'text-green-400', badge: 'bg-green-900/50 text-green-300' },
    purple: { bg: 'bg-purple-900/30', border: 'border-purple-500/50', text: 'text-purple-400', badge: 'bg-purple-900/50 text-purple-300' },
    amber: { bg: 'bg-amber-900/30', border: 'border-amber-500/50', text: 'text-amber-400', badge: 'bg-amber-900/50 text-amber-300' },
    cyan: { bg: 'bg-cyan-900/30', border: 'border-cyan-500/50', text: 'text-cyan-400', badge: 'bg-cyan-900/50 text-cyan-300' },
    orange: { bg: 'bg-orange-900/30', border: 'border-orange-500/50', text: 'text-orange-400', badge: 'bg-orange-900/50 text-orange-300' },
  };

  const getChartTypeBadge = (type: ChartType) => {
    const styles: Record<ChartType, string> = {
      table: 'bg-gray-700 text-gray-300',
      line: 'bg-blue-900/50 text-blue-300',
      bar: 'bg-green-900/50 text-green-300',
      kpi: 'bg-purple-900/50 text-purple-300',
      map: 'bg-orange-900/50 text-orange-300',
    };
    return styles[type];
  };

  // Get current iframe URL based on source and selection
  const getCurrentUrl = () => {
    if (source === 'grafana') {
      if (activeGrafana === 'infrastructure') {
        return grafanaDashboards[activeGrafana].url;
      }
      return withTenantScope(grafanaDashboards[activeGrafana].url, 'grafana');
    }
    // If a specific chart is selected, show that chart
    if (selectedChart) {
      return getSupersetChartUrl(selectedChart);
    }
    // Otherwise show the first chart of the active category
    const firstChart = chartCategories[activeCategory].charts[0];
    if (firstChart) {
      return getSupersetChartUrl(firstChart.id);
    }
    return getSupersetDashboardUrl(chartCategories[activeCategory].supersetDashboard);
  };

  const getCurrentTitle = () => {
    if (source === 'grafana') {
      return grafanaDashboards[activeGrafana].name;
    }
    if (selectedChart) {
      // Search across all categories to find the chart
      for (const category of Object.values(chartCategories)) {
        const chart = category.charts.find(c => c.id === selectedChart);
        if (chart) return chart.name;
      }
      return 'Chart';
    }
    // Show first chart name of active category
    const firstChart = chartCategories[activeCategory].charts[0];
    return firstChart?.name || chartCategories[activeCategory].name;
  };

  return (
    <div className="flex flex-col h-screen bg-gray-900 text-white">
      {/* Header */}
      <div className="bg-gray-800 border-b border-gray-700 px-6 py-4 flex justify-between items-center shadow-lg z-10">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2 text-white">
            <BarChart3 className="w-6 h-6 text-blue-500" />
            Analytics Hub
          </h1>
          <p className="text-sm text-gray-400 mt-1">
            Unified analytics • {source === 'grafana' ? 'Real-time Grafana dashboards' : `${totalCharts} Superset BI charts across ${Object.keys(chartCategories).length} categories`}
          </p>
        </div>
        
        <div className="flex items-center gap-4">
          {/* Source Toggle */}
          <div className="flex bg-gray-700 rounded-lg p-1">
            <button
              onClick={() => setSource('grafana')}
              className={`px-4 py-2 rounded-md text-sm font-medium transition-all flex items-center gap-2 ${
                source === 'grafana' 
                  ? 'bg-blue-600 shadow-sm text-white' 
                  : 'text-gray-300 hover:text-white hover:bg-gray-600'
              }`}
            >
              <Gauge className="w-4 h-4" />
              Real-time
            </button>
            <button
              onClick={() => setSource('superset')}
              className={`px-4 py-2 rounded-md text-sm font-medium transition-all flex items-center gap-2 ${
                source === 'superset' 
                  ? 'bg-blue-600 shadow-sm text-white' 
                  : 'text-gray-300 hover:text-white hover:bg-gray-600'
              }`}
            >
              <BarChart3 className="w-4 h-4" />
              BI Reports
            </button>
          </div>
          
          {/* Open External */}
          <a
            href={getCurrentUrl()}
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
          >
            <ExternalLink size={16} />
            Open Full Screen
          </a>
        </div>
      </div>

      {/* Content */}
      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar - Category Navigation */}
        <div className="w-72 bg-gray-800 border-r border-gray-700 overflow-y-auto flex flex-col">
          <div className="p-4 flex-1">
            <div className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">
              {source === 'superset' ? 'BI Report Categories' : 'Grafana Dashboards'}
            </div>

            {/* Superset Categories */}
            {source === 'superset' && (
              <div className="space-y-1">
                {(Object.keys(chartCategories) as CategoryKey[]).map((key) => {
                  const cat = chartCategories[key];
                  const isExpanded = expandedCategories.has(key);
                  const isActive = activeCategory === key;
                  const colors = colorClasses[cat.color];

                  return (
                    <div key={key}>
                      <button
                        onClick={() => toggleCategory(key)}
                        className={`w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-sm transition-all ${
                          isActive 
                            ? `${colors.bg} ${colors.border} border ${colors.text}` 
                            : 'hover:bg-gray-700 text-gray-300'
                        }`}
                      >
                        <div className="flex items-center gap-2.5">
                          <span className={isActive ? colors.text : 'text-gray-400'}>{cat.icon}</span>
                          <div className="text-left">
                            <div className="font-medium">{cat.name}</div>
                            <div className="text-[10px] text-gray-500">{cat.description}</div>
                          </div>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={`text-[10px] px-1.5 py-0.5 rounded-full ${colors.badge}`}>
                            {cat.charts.length}
                          </span>
                          {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                        </div>
                      </button>

                      {/* Chart List (Expanded) */}
                      {isExpanded && (
                        <div className="ml-4 mt-1 mb-2 space-y-0.5 border-l-2 border-gray-600 pl-3">
                          {cat.charts.map((chart) => (
                            <button
                              key={chart.id}
                              onClick={() => selectChart(chart.id)}
                              className={`w-full text-left px-2 py-1.5 rounded text-xs transition-all group ${
                                selectedChart === chart.id
                                  ? 'bg-blue-600/30 text-blue-300'
                                  : 'hover:bg-gray-700 text-gray-400'
                              }`}
                              title={chart.description}
                            >
                              <div className="flex items-center justify-between">
                                <span className="truncate">{chart.name}</span>
                                <span className={`text-[9px] px-1 py-0.5 rounded uppercase ${getChartTypeBadge(chart.type)}`}>
                                  {chart.type}
                                </span>
                              </div>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}

            {/* Grafana Dashboards */}
            {source === 'grafana' && (
              <div className="space-y-1">
                {(Object.keys(grafanaDashboards) as GrafanaKey[]).map((key) => {
                  const dash = grafanaDashboards[key];
                  const isActive = activeGrafana === key;

                  return (
                    <button
                      key={key}
                      onClick={() => setActiveGrafana(key)}
                      className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-all ${
                        isActive 
                          ? 'bg-blue-600/30 border border-blue-500/50 text-blue-400' 
                          : 'hover:bg-gray-700 text-gray-300'
                      }`}
                    >
                      <span className={isActive ? 'text-blue-400' : 'text-gray-400'}>{dash.icon}</span>
                      <div className="text-left flex-1">
                        <div className="font-medium">{dash.name}</div>
                        <div className="text-[10px] text-gray-500">{dash.description}</div>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Sidebar Footer - Stats */}
          <div className="p-4 border-t border-gray-700 bg-gray-900">
            <div className="grid grid-cols-2 gap-2">
              <div className="bg-gray-800 p-2.5 rounded-lg border border-gray-700 text-center">
                <div className="text-lg font-bold text-blue-400">{totalCharts}</div>
                <div className="text-[10px] text-gray-500 uppercase">BI Charts</div>
              </div>
              <div className="bg-gray-800 p-2.5 rounded-lg border border-gray-700 text-center">
                <div className="text-lg font-bold text-green-400">{Object.keys(grafanaDashboards).length}</div>
                <div className="text-[10px] text-gray-500 uppercase">Grafana</div>
              </div>
            </div>
            
            {source === 'grafana' && (
              <div className="mt-3 bg-green-900/30 border border-green-500/30 rounded-lg p-2.5">
                <div className="flex items-center gap-1.5 text-xs font-medium text-green-400">
                  <RefreshCw className="w-3 h-3 animate-spin" />
                  Live Data
                </div>
                <p className="text-[10px] text-green-500 mt-1">
                  Auto-refreshing every 10-30 seconds
                </p>
              </div>
            )}

            {user?.icaoCode && source === 'superset' && (
              <div className="mt-3 bg-blue-900/30 border border-blue-500/30 rounded-lg p-2.5">
                <div className="flex items-center gap-1.5 text-xs font-medium text-blue-400">
                  <Users className="w-3 h-3" />
                  Tenant: {user.icaoCode}
                </div>
                <p className="text-[10px] text-blue-500 mt-1">
                  Showing filtered data for your airport
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Main Content - Dashboard/Chart View */}
        <div className="flex-1 bg-gray-900 relative">
          {/* Breadcrumb */}
          <div className="absolute top-2 left-4 z-10 bg-gray-800/90 backdrop-blur-sm rounded-lg px-3 py-1.5 shadow-lg border border-gray-700">
            <div className="flex items-center gap-2 text-xs text-gray-400">
              <span>{source === 'grafana' ? 'Grafana' : 'Superset'}</span>
              <ChevronRight size={12} />
              <span className="font-medium text-white">{getCurrentTitle()}</span>
            </div>
          </div>

          {/* Iframe */}
          <iframe
            src={getCurrentUrl()}
            title={getCurrentTitle()}
            className="w-full h-full border-none"
            sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
          />

          {/* Loading fallback */}
          <div className="absolute inset-0 flex items-center justify-center -z-10 text-gray-500">
            <div className="text-center">
              <RefreshCw className="w-8 h-8 animate-spin mx-auto mb-2" />
              <p>Loading {source === 'grafana' ? 'Dashboard' : 'Report'}...</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AnalyticsHubPage;
