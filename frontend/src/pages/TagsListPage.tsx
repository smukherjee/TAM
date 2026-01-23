import React, { useState, useEffect } from 'react';
import { Tag, Search } from 'lucide-react';
import api from '../services/api';

interface TagItem {
    id: string;
    tagId: string;
    name: string;
    color?: string;
    description?: string;
    tenantCode: string;
    itemCount?: number;
    createdAt: string;
    updatedAt: string;
}

const TagsListPage: React.FC = () => {
    const [tags, setTags] = useState<TagItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [searchTerm, setSearchTerm] = useState('');

    useEffect(() => {
        fetchTags();
    }, []);

    const fetchTags = async () => {
        try {
            setLoading(true);
            // TODO: Replace with actual tags endpoint when backend is ready
            const response = await api.get('/tags');
            setTags(response.data.data || []);
        } catch (error) {
            console.error('Error fetching tags:', error);
            setTags([]);
        } finally {
            setLoading(false);
        }
    };

    const filteredTags = tags.filter(tag =>
        tag.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        tag.tagId.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="p-6 bg-gray-900 min-h-screen">
            <div className="max-w-7xl mx-auto">
                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <div className="flex items-center space-x-3">
                        <Tag className="text-blue-500" size={32} />
                        <div>
                            <h1 className="text-2xl font-bold text-white">Tags</h1>
                            <p className="text-gray-400 text-sm">{filteredTags.length} tags</p>
                        </div>
                    </div>
                    <button className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg flex items-center space-x-2 transition-colors">
                        <span className="text-xl">+</span>
                        <span>New tag</span>
                    </button>
                </div>

                {/* Search */}
                <div className="bg-gray-800 rounded-lg p-4 mb-6">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                        <input
                            type="text"
                            placeholder="Search tags"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full bg-gray-700 text-white pl-10 pr-4 py-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                </div>

                {/* Tags Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
                    {loading ? (
                        <div className="col-span-full p-8 text-center text-gray-400 bg-gray-800 rounded-lg">
                            Loading tags...
                        </div>
                    ) : filteredTags.length === 0 ? (
                        <div className="col-span-full p-8 text-center text-gray-400 bg-gray-800 rounded-lg">
                            No tags found
                        </div>
                    ) : (
                        filteredTags.map((tag) => (
                            <div
                                key={tag.id}
                                className="bg-gray-800 rounded-lg p-4 hover:bg-gray-700 transition-colors cursor-pointer border border-gray-700"
                            >
                                <div className="flex items-center justify-between mb-2">
                                    <div className="flex items-center space-x-2">
                                        <div
                                            className="w-3 h-3 rounded-full"
                                            style={{ backgroundColor: tag.color || '#3B82F6' }}
                                        ></div>
                                        <h3 className="text-white font-medium">{tag.name}</h3>
                                    </div>
                                    <span className="text-xs text-gray-400 bg-gray-700 px-2 py-1 rounded">
                                        {tag.itemCount || 0} items
                                    </span>
                                </div>
                                {tag.description && (
                                    <p className="text-sm text-gray-400 mb-2">{tag.description}</p>
                                )}
                                <div className="text-xs text-gray-500">
                                    ID: {tag.tagId}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};

export default TagsListPage;
