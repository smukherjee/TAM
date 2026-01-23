import React, { useState, useEffect } from 'react';
import { FolderTree, Search } from 'lucide-react';
import api from '../services/api';

interface Category {
    id: string;
    categoryId: string;
    name: string;
    description?: string;
    tenantCode: string;
    itemCount?: number;
    createdAt: string;
    updatedAt: string;
}

const CategoriesListPage: React.FC = () => {
    const [categories, setCategories] = useState<Category[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        fetchCategories();
    }, []);

    const fetchCategories = async () => {
        try {
            setLoading(true);
            // TODO: Replace with actual categories endpoint when backend is ready
            const response = await api.get('/categories');
            setCategories(response.data.data || []);
        } catch (error) {
            console.error('Error fetching categories:', error);
            setCategories([]);
        } finally {
            setLoading(false);
        }
    };

    const filteredCategories = categories.filter(category =>
        category.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        category.categoryId.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="p-6 bg-gray-900 min-h-screen">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center space-x-3">
                        <FolderTree className="text-blue-500" size={32} />
                        <div>
                            <h1 className="text-2xl font-bold text-white">Categories</h1>
                            <p className="text-gray-400 text-sm">{filteredCategories.length} categories</p>
                        </div>
                    </div>
                    <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center space-x-2 transition-colors">
                        <span className="text-xl">+</span>
                        <span>New category</span>
                    </button>
                </div>

                {/* Search */}
                <div className="bg-gray-800 rounded-lg p-4 mb-6">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                        <input
                            type="text"
                            placeholder="Search categories"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full bg-gray-700 text-white pl-10 pr-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                </div>

                {/* Categories Table */}
                <div className="bg-gray-800 rounded-lg overflow-hidden">
                    {loading ? (
                        <div className="p-8 text-center text-gray-400">Loading categories...</div>
                    ) : filteredCategories.length === 0 ? (
                        <div className="p-8 text-center text-gray-400">No categories found</div>
                    ) : (
                        <div className="overflow-x-auto">
                            <table className="w-full">
                                <thead className="bg-gray-700">
                                    <tr>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Name</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Category ID</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Description</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Items</th>
                                        <th className="px-6 py-3 text-left text-xs font-medium text-gray-300 uppercase tracking-wider">Created</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-700">
                                    {filteredCategories.map((category) => (
                                        <tr key={category.id} className="hover:bg-gray-700 transition-colors">
                                            <td className="px-6 py-4 text-sm font-medium text-white">{category.name}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{category.categoryId}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{category.description || '-'}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">{category.itemCount || 0}</td>
                                            <td className="px-6 py-4 text-sm text-gray-300">
                                                {new Date(category.createdAt).toLocaleDateString()}
                                            </td>
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

export default CategoriesListPage;
