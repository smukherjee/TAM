import React from 'react';

interface ErrorMessageProps {
    message: string;
    onRetry?: () => void;
}

export const ErrorMessage: React.FC<ErrorMessageProps> = ({ message, onRetry }) => {
    return (
        <div className="bg-red-900 border-l-4 border-red-500 text-red-100 p-4 rounded shadow-md m-4" role="alert">
            <div className="flex justify-between items-center">
                <div>
                    <p className="font-bold">Error</p>
                    <p>{message}</p>
                </div>
                {onRetry && (
                    <button 
                        onClick={onRetry}
                        className="bg-red-700 hover:bg-red-600 text-white font-bold py-2 px-4 rounded"
                    >
                        Retry
                    </button>
                )}
            </div>
        </div>
    );
};
