import React from 'react';
import { Skeleton } from '../ui/Skeleton';

const TurnaroundDetailSkeleton: React.FC = () => {
    return (
        <div className="p-6 bg-gray-100 min-h-screen">
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <Skeleton className="h-4 w-32 mb-2" />
                    <Skeleton className="h-8 w-64 mb-2" />
                    <Skeleton className="h-4 w-48" />
                </div>
                <div className="text-right space-y-1">
                    <Skeleton className="h-4 w-24 ml-auto" />
                    <Skeleton className="h-4 w-24 ml-auto" />
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                <div className="bg-white rounded-lg shadow p-4">
                    <Skeleton className="h-6 w-32 mb-4" />
                    <Skeleton className="w-full aspect-video rounded" />
                </div>
                <div className="bg-white rounded-lg shadow p-4">
                    <Skeleton className="h-6 w-32 mb-4" />
                    <div className="grid grid-cols-2 gap-4">
                        {[...Array(4)].map((_, i) => (
                            <div key={i}>
                                <Skeleton className="h-4 w-16 mb-1" />
                                <Skeleton className="h-6 w-24" />
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            <div className="w-full bg-white rounded-lg shadow p-4">
                <Skeleton className="h-6 w-48 mb-4" />
                <Skeleton className="w-full h-[300px]" />
            </div>
        </div>
    );
};

export default TurnaroundDetailSkeleton;
