import React, { useState, useEffect } from 'react';
import { Package, Search, Filter } from 'lucide-react';
import api from '../services/api';

interface Asset {
    id: string;
    assetId: string;
    name: string;
    qrId?: string;
    status: string;
    description?: string;
    category?: string;
    tenantCode: string;
    location?: string;
    createdAt: string;
    updatedAt: string;
}

const AssetsListPage: React.FC = () => {
    const [assets, setAssets] = useState<Asset[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('All Status');

    useEffect(() => {
        fetchAssets();
    }, []);

    const fetchAssets = async () => {
        try {
            setLoading(true);
            const response = await api.get('/assets');
            setAssets(response.data.data || []);
        } catch (error) {
            console.error('Error fetching assets:', error);
            setAssets([]);
        } finally {
            setLoading(false);
        }
    };

    const filteredAssets = assets.filter(asset => {
        const matchesSearch = asset.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
            asset.assetId.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesStatus = statusFilter === 'All Status' || asset.status === statusFilter;
        return matchesSearch && matchesStatus;
    });

    return (
        <div className="p-6 bg-gray-900 min-h-screen">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center space-x-3">
                        <Package className="text-blue-500" size={32} />
                        <div>
                            <h1 className="text-2xl font-bold text-white">Assets</h1>
                            <p className="text-gray-400 text-sm">{filteredAssets.length} assets</p>
                        </div>
                    </div>
                    <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center space-x-2 transition-colors">
                        <span className="text-xl">+</span>
                        <span>New asset</span>
                    </button>
                </div>

                {/* Search and Filter */}
                <div className="bg-gray-800 rounded-lg p-4 mb-6 flex items-center space-x-4">
                    <div className="flex-1 relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                        <input
                            type="text"
                            placeholder="Search assets"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full bg-gray-700 text-white pl-10 pr-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <div className="flex items-center space-x-2">
                        <Filter className="text-gray-400" size={20} />
                        <select
                            value={statusFilter}
                            onChange={(e) => setStatusFilter(e.target.value)}
                            className="bg-gray-700 text-white px-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                            <option>All Status</option>
                            <option>Available</option>
                            <option>In Use</option>
                            <option>Maintenance</option>
                        </select>
                    </div>
                </div>

                {/* Assets Table */}
                <div className="bg-gray-800 rounded-lg overflow-hidden">
                    {loading ? (
                        <div className="p-8 text-center text-gray-400">Loading assets...</div>
                    ) : filteredAssets.length === 0 ? (
                        <div className="p-8 text-center text-gray-400">No assets found</div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-gray-700">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Name</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Asset ID</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">QR ID</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Status</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Description</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Category</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Location</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-700">
                                    {filteredAssets.map((asset) => (
                                        <tr key={asset.id} className="hover:bg-gray-700 transition-colors">
                                            <td className="px-6 py-4 text-sm text-white">{asset.name}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{asset.assetId}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{asset.qrId || '-'}</td>
                                            <td className="px-6 py-4 text-sm">
                                                <span className={`px-2 py-1 rounded-full text-xs ${
                                                    asset.status === 'Available' ? 'bg-green-900 text-green-300' :
                                                    asset.status === 'In Use' ? 'bg-blue-900 text-blue-300' :
                                                    'bg-yellow-900 text-yellow-300'
                                                }`}>
                                                    {asset.status}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{asset.description || '-'}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{asset.category || '-'}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{asset.location || '-'}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default AssetsListPage;
