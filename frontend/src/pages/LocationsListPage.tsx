import React, { useState, useEffect } from 'react';
import { MapPin, Search, Filter } from 'lucide-react';
import api from '../services/api';

interface Location {
    id: string;
    locationId: string;
    name: string;
    type?: string;
    description?: string;
    tenantCode: string;
    zone?: string;
    capacity?: number;
    itemCount?: number;
    createdAt: string;
    updatedAt: string;
}

const LocationsListPage: React.FC = () => {
    const [locations, setLocations] = useState<Location[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');
    const [typeFilter, setTypeFilter] = useState('All Types');

    useEffect(() => {
        fetchLocations();
    }, []);

    const fetchLocations = async () => {
        try {
            setLoading(true);
            // TODO: Replace with actual locations endpoint when backend is ready
            const response = await api.get('/locations');
            setLocations(response.data.data || []);
        } catch (error) {
            console.error('Error fetching locations:', error);
            setLocations([]);
        } finally {
            setLoading(false);
        }
    };

    const filteredLocations = locations.filter(location => {
        const matchesSearch = location.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
            location.locationId.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesType = typeFilter === 'All Types' || location.type === typeFilter;
        return matchesSearch && matchesType;
    });

    return (
        <div className="p-6 bg-gray-900 min-h-screen">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center space-x-3">
                        <MapPin className="text-blue-500" size={32} />
                        <div>
                            <h1 className="text-2xl font-bold text-white">Locations</h1>
                            <p className="text-gray-400 text-sm">{filteredLocations.length} locations</p>
                        </div>
                    </div>
                    <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center space-x-2 transition-colors">
                        <span className="text-xl">+</span>
                        <span>New location</span>
                    </button>
                </div>

                {/* Search and Filter */}
                <div className="bg-gray-800 rounded-lg p-4 mb-6 flex items-center space-x-4">
                    <div className="flex-1 relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                        <input
                            type="text"
                            placeholder="Search locations"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full bg-gray-700 text-white pl-10 pr-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <div className="flex items-center space-x-2">
                        <Filter className="text-gray-400" size={20} />
                        <select
                            value={typeFilter}
                            onChange={(e) => setTypeFilter(e.target.value)}
                            className="bg-gray-700 text-white px-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                            <option>All Types</option>
                            <option>Terminal</option>
                            <option>Warehouse</option>
                            <option>Apron</option>
                            <option>Service Area</option>
                        </select>
                    </div>
                </div>

                {/* Locations Table */}
                <div className="bg-gray-800 rounded-lg overflow-hidden">
                    {loading ? (
                        <div className="p-8 text-center text-gray-400">Loading locations...</div>
                    ) : filteredLocations.length === 0 ? (
                        <div className="p-8 text-center text-gray-400">No locations found</div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-gray-700">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Name</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Location ID</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Type</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Zone</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Items</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Capacity</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Description</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-700">
                                    {filteredLocations.map((location) => (
                                        <tr key={location.id} className="hover:bg-gray-700 transition-colors">
                                            <td className="px-6 py-4 text-sm font-medium text-white">{location.name}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{location.locationId}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{location.type || '-'}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{location.zone || '-'}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{location.itemCount || 0}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{location.capacity || '-'}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{location.description || '-'}</td>
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

export default LocationsListPage;
