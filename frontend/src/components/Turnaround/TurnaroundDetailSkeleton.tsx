import React from 'react';
import { Skeleton } from '../ui/Skeleton';

const TurnaroundDetailSkeleton: React.FC = () => {
    return (
        <div className="p-6 bg-slate-900 min-h-screen">
            <div className="mb-6 flex items-center justify-between">
                <div>
                    <Skeleton className="h-4 w-32 mb-2 bg-slate-700" />
                    <Skeleton className="h-8 w-64 mb-2 bg-slate-700" />
                    <Skeleton className="h-4 w-48 bg-slate-700" />
                </div>
                <div className="text-right space-y-1">
                    <Skeleton className="h-4 w-24 ml-auto bg-slate-700" />
                    <Skeleton className="h-4 w-24 ml-auto bg-slate-700" />
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
                <div className="bg-slate-800 border border-slate-700 rounded-lg shadow-lg p-4">
                    <Skeleton className="h-6 w-32 mb-4 bg-slate-700" />
                    <Skeleton className="w-full aspect-video rounded bg-slate-700" />
                </div>
                <div className="bg-slate-800 border border-slate-700 rounded-lg shadow-lg p-4">
                    <Skeleton className="h-6 w-32 mb-4 bg-slate-700" />
                    <div className="grid grid-cols-2 gap-4">
                        {[...Array(4)].map((_, i) => (
                            <div key={i}>
                                <Skeleton className="h-4 w-16 mb-1 bg-slate-700" />
                                <Skeleton className="h-6 w-24 bg-slate-700" />
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            <div className="w-full bg-slate-800 border border-slate-700 rounded-lg shadow-lg p-4">
                <Skeleton className="h-6 w-48 mb-4 bg-slate-700" />
                <Skeleton className="w-full h-[300px] bg-slate-700" />
            </div>
        </div>
    );
};

export default TurnaroundDetailSkeleton;
