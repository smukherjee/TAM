import React from 'react';
import { ExternalLink } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const ReportsPage: React.FC = () => {
    const { user } = useAuth();

    const tenantCode = user?.icaoCode?.toUpperCase();
    const tenantSuffix = tenantCode ? `_${tenantCode.toLowerCase()}` : '';
    const dashboardSlug = `tam_ops${tenantSuffix}`;
    const supersetUrl = new URL(`http://localhost:8089/superset/dashboard/${dashboardSlug}/?standalone=2&show_filters=0`);
    if (tenantCode) {
        supersetUrl.searchParams.set('tenant_code', tenantCode);
        supersetUrl.searchParams.set('icao', tenantCode);
    }

    return (
        <div className="w-full h-full flex flex-col bg-gray-50">
            <div className="bg-white p-4 border-b border-gray-200 flex justify-between items-center shadow-sm z-10">
                <div>
                    <h2 className="text-xl font-bold text-gray-800">Analytics & Reports</h2>
                    <p className="text-sm text-gray-500">Powered by Apache Superset</p>
                    <p className="text-xs text-blue-500 mt-1">Dashboard: {dashboardSlug}</p>
                </div>
                <a
                    href={supersetUrl.toString()}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
                >
                    <span>Open Full Screen</span>
                    <ExternalLink size={16} />
                </a>
            </div>

            <div className="flex-1 relative bg-gray-100">
                <iframe
                    src={supersetUrl.toString()}
                    title="Superset Analytics"
                    className="w-full h-full border-none"
                    sandbox="allow-same-origin allow-scripts allow-forms allow-popups"
                />

                {/* Fallback info layer (behind iframe, visible if iframe connection refused or blocked) */}
                <div className="absolute inset-0 flex items-center justify-center -z-10 text-gray-400">
                    <p>Loading Analytics...</p>
                </div>
            </div>
        </div>
    );
};

export default ReportsPage;
