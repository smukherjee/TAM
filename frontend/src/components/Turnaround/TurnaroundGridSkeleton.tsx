import React from 'react';
import { Skeleton } from '../ui/Skeleton';

const TurnaroundGridSkeleton: React.FC = () => {
    return (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 p-4">
            {[...Array(8)].map((_, i) => (
                <div key={i} className="bg-gray-800 rounded-lg overflow-hidden shadow-lg h-64 relative">
                    <Skeleton className="absolute inset-0 w-full h-full opacity-20" />
                    <div className="absolute inset-0 p-4 flex flex-col justify-between">
                        <div className="flex justify-between items-start">
                            <div className="space-y-2">
                                <Skeleton className="h-6 w-20" />
                                <Skeleton className="h-4 w-16" />
                            </div>
                            <Skeleton className="h-6 w-24" />
                        </div>
                        <div className="flex gap-1">
                            {[...Array(4)].map((_, j) => (
                                <Skeleton key={j} className="w-3 h-3 rounded-full" />
                            ))}
                        </div>
                    </div>
                </div>
            ))}
        </div>
    );
};

export default TurnaroundGridSkeleton;
