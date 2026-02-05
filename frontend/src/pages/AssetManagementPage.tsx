import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Package, Box, Layers, Tag, MapPin, BarChart } from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

interface Asset {
    id: string;
    assetId: string;
    name: string;
    qrId: string;
    status: string;
    description: string;
    category: string;
    tenantCode: string;
}

const AssetManagementPage: React.FC = () => {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [stats, setStats] = useState({ total: 0, available: 0, inUse: 0, maintenance: 0 });

    useEffect(() => {
        fetchAssets();
    }, []);

    const fetchAssets = async () => {
        try {
            const response = await api.get('/assets');
            const data = response.data.data || [];
            calculateStats(data);
        } catch (error) {
            console.error('Error fetching assets:', error);
            calculateStats([]);
        }
    };

    const calculateStats = (data: Asset[]) => {
        setStats({
            total: data.length,
            available: data.filter(a => a.status === 'Available').length,
            inUse: data.filter(a => a.status === 'In Use').length,
            maintenance: data.filter(a => a.status === 'Maintenance').length,
        });
    };

    const menuItems = [
        { icon: BarChart, label: 'Dashboard', path: '/assets/dashboard', color: 'blue' },
        { icon: Package, label: 'Assets', path: '/assets/list', color: 'green' },
        { icon: Box, label: 'Kits', path: '/assets/kits', color: 'purple' },
        { icon: Layers, label: 'Categories', path: '/assets/categories', color: 'orange' },
        { icon: Tag, label: 'Tags', path: '/assets/tags', color: 'pink' },
        { icon: MapPin, label: 'Locations', path: '/assets/locations', color: 'cyan' },
        { icon: BarChart, label: 'Telematics Integrations', path: '/admin/telematics-integrations', color: 'indigo' },
    ];

    return (
        <div className="h-full bg-gray-50 overflow-auto">
            {/* Header */}
            <div className="bg-white border-b border-gray-200 px-6 py-4">
                <h1 className="text-2xl font-bold text-gray-900">Asset Management</h1>
                <p className="text-sm text-gray-600 mt-1">Manage assets, kits, and inventory for {user?.icaoCode}</p>
            </div>

            {/* Stats */}
            <div className="px-6 py-6">
                <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
                    <div className="bg-white p-6 rounded-lg shadow">
                        <div className="text-sm text-gray-600">Total Assets</div>
                        <div className="text-3xl font-bold text-gray-900 mt-2">{stats.total}</div>
                    </div>
                    <div className="bg-green-50 p-6 rounded-lg shadow border border-green-200">
                        <div className="text-sm text-green-600">Available</div>
                        <div className="text-3xl font-bold text-green-700 mt-2">{stats.available}</div>
                    </div>
                    <div className="bg-blue-50 p-6 rounded-lg shadow border border-blue-200">
                        <div className="text-sm text-blue-600">In Use</div>
                        <div className="text-3xl font-bold text-blue-700 mt-2">{stats.inUse}</div>
                    </div>
                    <div className="bg-orange-50 p-6 rounded-lg shadow border border-orange-200">
                        <div className="text-sm text-orange-600">Maintenance</div>
                        <div className="text-3xl font-bold text-orange-700 mt-2">{stats.maintenance}</div>
                    </div>
                </div>

                {/* Menu Grid */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    {menuItems.map((item, index) => {
                        const Icon = item.icon;
                        return (
                            <button
                                key={index}
                                onClick={() => navigate(item.path)}
                                className={`bg-white p-8 rounded-lg shadow hover:shadow-lg transition-all duration-200 border-2 border-transparent hover:border-${item.color}-500 group`}
                            >
                                <div className={`flex items-center space-x-4`}>
                                    <div className={`p-4 bg-${item.color}-50 rounded-lg group-hover:bg-${item.color}-100 transition-colors`}>
                                        <Icon className={`text-${item.color}-600`} size={32} />
                                    </div>
                                    <div className="text-left">
                                        <h3 className="text-lg font-semibold text-gray-900">{item.label}</h3>
                                        <p className="text-sm text-gray-500 mt-1">Manage {item.label.toLowerCase()}</p>
                                    </div>
                                </div>
                            </button>
                        );
                    })}
                </div>
            </div>
        </div>
    );
};

export default AssetManagementPage;
