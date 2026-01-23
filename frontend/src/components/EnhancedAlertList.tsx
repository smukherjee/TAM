import React, { useState } from 'react';
import { AlertTriangle, X, ChevronDown, ChevronUp } from 'lucide-react';
import './MapIcons.css';

interface Alert {
  id: string;
  vehicleId?: string;
  flightId?: string;
  message: string;
  severity: 'critical' | 'warning' | 'info';
  timestamp: Date;
}

interface EnhancedAlertListProps {
  alerts: Alert[];
  onDismiss?: (alertId: string) => void;
}

const EnhancedAlertList: React.FC<EnhancedAlertListProps> = ({ alerts, onDismiss }) => {
  const [collapsed, setCollapsed] = useState(false);
  const [filter, setFilter] = useState<'all' | 'critical' | 'warning' | 'info'>('all');

  const groupedAlerts = {
    critical: alerts.filter(a => a.severity === 'critical'),
    warning: alerts.filter(a => a.severity === 'warning'),
    info: alerts.filter(a => a.severity === 'info'),
  };

  const filteredAlerts = filter === 'all' ? alerts : groupedAlerts[filter];

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'critical': return 'text-red-400 bg-red-900/20 border-red-500/30';
      case 'warning': return 'text-amber-400 bg-amber-900/20 border-amber-500/30';
      case 'info': return 'text-blue-400 bg-blue-900/20 border-blue-500/30';
      default: return 'text-gray-400 bg-gray-900/20 border-gray-500/30';
    }
  };

  const getSeverityIcon = () => {
    return <AlertTriangle className="w-4 h-4" />;
  };

  if (alerts.length === 0) {
    return null;
  }

  return (
    <div className="enhanced-alert-list glass-panel" style={{ 
      position: 'absolute', 
      top: '20px', 
      right: '20px', 
      width: '350px',
      maxHeight: collapsed ? '60px' : '500px',
      overflow: 'hidden',
      zIndex: 1000,
      padding: '16px',
    }}>
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <AlertTriangle className="w-5 h-5 text-amber-400" />
          <h3 className="text-white font-semibold">
            Active Alerts ({alerts.length})
          </h3>
        </div>
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="text-gray-400 hover:text-white transition-colors"
        >
          {collapsed ? <ChevronDown className="w-5 h-5" /> : <ChevronUp className="w-5 h-5" />}
        </button>
      </div>

      {!collapsed && (
        <>
          {/* Severity Filter */}
          <div className="flex gap-2 mb-3">
            {(['all', 'critical', 'warning', 'info'] as const).map(sev => (
              <button
                key={sev}
                onClick={() => setFilter(sev)}
                className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
                  filter === sev
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                }`}
              >
                {sev === 'all' ? `All (${alerts.length})` : 
                 `${sev.charAt(0).toUpperCase() + sev.slice(1)} (${groupedAlerts[sev].length})`}
              </button>
            ))}
          </div>

          {/* Alert List */}
          <div className="space-y-2 max-h-80 overflow-y-auto pr-2" style={{
            scrollbarWidth: 'thin',
            scrollbarColor: '#4b5563 #1f2937'
          }}>
            {filteredAlerts.map((alert, index) => (
              <div
                key={alert.id}
                className={`p-3 rounded-lg border ${getSeverityColor(alert.severity)} alert-item-new`}
                style={{ animationDelay: `${index * 0.05}s` }}
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-start gap-2 flex-1">
                    {getSeverityIcon()}
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium text-white mb-1">
                        {alert.vehicleId && `Vehicle: ${alert.vehicleId}`}
                        {alert.flightId && `Flight: ${alert.flightId}`}
                      </div>
                      <div className="text-xs text-gray-300">
                        {alert.message}
                      </div>
                      <div className="text-xs text-gray-500 mt-1">
                        {new Date(alert.timestamp).toLocaleTimeString()}
                      </div>
                    </div>
                  </div>
                  {onDismiss && (
                    <button
                      onClick={() => onDismiss(alert.id)}
                      className="text-gray-400 hover:text-white transition-colors flex-shrink-0"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </>
      )}
    </div>
  );
};

export default EnhancedAlertList;
