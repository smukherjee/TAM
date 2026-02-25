import React, { useState, Fragment } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import { MovementDiscrepancy, SEVERITY_COLORS, DISCREPANCY_TYPE_LABELS } from '../../types/tracking';
import { formatDistanceToNow, format } from 'date-fns';
import {
    X,
    AlertTriangle,
    MapPin,
    Activity,
    CheckCircle,
    Loader2
} from 'lucide-react';

interface AcknowledgeDiscrepancyModalProps {
    discrepancy: MovementDiscrepancy;
    isOpen: boolean;
    onClose: () => void;
    onAcknowledge: (notes: string) => void;
    isSubmitting: boolean;
}

/**
 * AcknowledgeDiscrepancyModal - Modal for acknowledging movement discrepancies
 * Feature: 005-asset-tracking-security
 * Phase 10: Movement Discrepancy Report
 */
const AcknowledgeDiscrepancyModal: React.FC<AcknowledgeDiscrepancyModalProps> = ({
    discrepancy,
    isOpen,
    onClose,
    onAcknowledge,
    isSubmitting
}) => {
    const [notes, setNotes] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onAcknowledge(notes);
    };

    // Format deviation
    const formatDeviation = (meters?: number) => {
        if (!meters) return 'N/A';
        if (meters < 1000) return `${meters.toFixed(1)} meters`;
        return `${(meters / 1000).toFixed(2)} km`;
    };

    return (
        <Transition appear show={isOpen} as={Fragment}>
            <Dialog as="div" className="relative z-50" onClose={onClose}>
                <Transition.Child
                    as={Fragment}
                    enter="ease-out duration-300"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="ease-in duration-200"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                >
                    <div className="fixed inset-0 bg-black bg-opacity-25" />
                </Transition.Child>

                <div className="fixed inset-0 overflow-y-auto">
                    <div className="flex min-h-full items-center justify-center p-4 text-center">
                        <Transition.Child
                            as={Fragment}
                            enter="ease-out duration-300"
                            enterFrom="opacity-0 scale-95"
                            enterTo="opacity-100 scale-100"
                            leave="ease-in duration-200"
                            leaveFrom="opacity-100 scale-100"
                            leaveTo="opacity-0 scale-95"
                        >
                            <Dialog.Panel className="w-full max-w-md transform overflow-hidden rounded-2xl bg-gray-800 p-6 text-left align-middle shadow-xl transition-all">
                                {/* Header */}
                                <div className="flex items-center justify-between mb-4">
                                    <Dialog.Title as="h3" className="text-lg font-medium leading-6 text-white flex items-center">
                                        <AlertTriangle className="h-5 w-5 text-orange-500 mr-2" />
                                        Acknowledge Discrepancy
                                    </Dialog.Title>
                                    <button
                                        onClick={onClose}
                                        className="text-gray-400 hover:text-gray-300"
                                    >
                                        <X className="h-5 w-5" />
                                    </button>
                                </div>

                                {/* Discrepancy Details */}
                                <div className="bg-gray-700 rounded-lg p-4 mb-4">
                                    <div className="space-y-3">
                                        {/* Asset */}
                                        <div className="flex justify-between">
                                            <span className="text-sm text-gray-400">Asset</span>
                                            <span className="text-sm font-medium text-white">
                                                {discrepancy.assetIdentifier}
                                                {discrepancy.assetName && (
                                                    <span className="text-gray-400 ml-1">
                                                        ({discrepancy.assetName})
                                                    </span>
                                                )}
                                            </span>
                                        </div>

                                        {/* Type */}
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-gray-400 flex items-center">
                                                <Activity className="h-4 w-4 mr-1" />
                                                Type
                                            </span>
                                            <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-orange-900/50 text-orange-400">
                                                {DISCREPANCY_TYPE_LABELS[discrepancy.discrepancyType as keyof typeof DISCREPANCY_TYPE_LABELS] || discrepancy.discrepancyType}
                                            </span>
                                        </div>

                                        {/* Severity */}
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-gray-400">Severity</span>
                                            <span
                                                className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                                                style={{
                                                    backgroundColor: `${SEVERITY_COLORS[discrepancy.severity]}20`,
                                                    color: SEVERITY_COLORS[discrepancy.severity],
                                                    border: `1px solid ${SEVERITY_COLORS[discrepancy.severity]}40`
                                                }}
                                            >
                                                {discrepancy.severity}
                                            </span>
                                        </div>

                                        {/* Deviation */}
                                        {discrepancy.deviationMeters && (
                                            <div className="flex justify-between">
                                                <span className="text-sm text-gray-400 flex items-center">
                                                    <MapPin className="h-4 w-4 mr-1" />
                                                    Deviation
                                                </span>
                                                <span className={`text-sm font-medium ${discrepancy.deviationMeters > 100 ? 'text-red-400' : 'text-gray-300'
                                                    }`}>
                                                    {formatDeviation(discrepancy.deviationMeters)}
                                                </span>
                                            </div>
                                        )}

                                        {/* Expected Location */}
                                        {discrepancy.expectedLatitude != null && discrepancy.expectedLongitude != null && (
                                            <div className="flex justify-between">
                                                <span className="text-sm text-gray-400">Expected</span>
                                                <span className="text-sm text-green-400 font-mono">
                                                    {discrepancy.expectedLatitude.toFixed(6)}, {discrepancy.expectedLongitude.toFixed(6)}
                                                </span>
                                            </div>
                                        )}

                                        {/* Actual Location */}
                                        {discrepancy.actualLatitude != null && discrepancy.actualLongitude != null && (
                                            <div className="flex justify-between">
                                                <span className="text-sm text-gray-400">Actual</span>
                                                <span className="text-sm text-red-400 font-mono">
                                                    {discrepancy.actualLatitude.toFixed(6)}, {discrepancy.actualLongitude.toFixed(6)}
                                                </span>
                                            </div>
                                        )}

                                        {/* Timestamp */}
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-gray-400">Detected</span>
                                            <div className="text-right">
                                                <span className="text-sm font-medium text-white">
                                                    {format(new Date(discrepancy.timestamp), 'MMM d, yyyy HH:mm')}
                                                </span>
                                                <span className="text-xs text-gray-500 block">
                                                    {formatDistanceToNow(new Date(discrepancy.timestamp), { addSuffix: true })}
                                                </span>
                                            </div>
                                        </div>

                                        {/* Description */}
                                        {discrepancy.description && (
                                            <div className="pt-2 border-t border-gray-600">
                                                <span className="text-sm text-gray-400 block mb-1">Description</span>
                                                <p className="text-sm text-gray-300">{discrepancy.description}</p>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                {/* Form */}
                                <form onSubmit={handleSubmit}>
                                    <div className="mb-4">
                                        <label
                                            htmlFor="notes"
                                            className="block text-sm font-medium text-gray-300 mb-2"
                                        >
                                            Investigation Notes (Optional)
                                        </label>
                                        <textarea
                                            id="notes"
                                            rows={3}
                                            value={notes}
                                            onChange={(e) => setNotes(e.target.value)}
                                            placeholder="Describe the investigation findings or resolution..."
                                            className="block w-full px-3 py-2 border border-gray-600 rounded-md 
                                                bg-gray-700 text-white placeholder-gray-500
                                                focus:outline-none focus:ring-1 focus:ring-blue-500 
                                                focus:border-blue-500 text-sm resize-none"
                                        />
                                    </div>

                                    {/* Actions */}
                                    <div className="flex justify-end space-x-3">
                                        <button
                                            type="button"
                                            onClick={onClose}
                                            disabled={isSubmitting}
                                            className="px-4 py-2 text-sm font-medium text-gray-300 bg-gray-700 
                                                border border-gray-600 rounded-md hover:bg-gray-600 
                                                focus:outline-none focus:ring-2 focus:ring-offset-2 
                                                focus:ring-offset-gray-800 focus:ring-blue-500 disabled:opacity-50"
                                        >
                                            Cancel
                                        </button>
                                        <button
                                            type="submit"
                                            disabled={isSubmitting}
                                            className="inline-flex items-center px-4 py-2 text-sm font-medium 
                                                text-white bg-blue-600 border border-transparent rounded-md 
                                                hover:bg-blue-700 focus:outline-none focus:ring-2 
                                                focus:ring-offset-2 focus:ring-offset-gray-800 focus:ring-blue-500 disabled:opacity-50"
                                        >
                                            {isSubmitting ? (
                                                <>
                                                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                                    Acknowledging...
                                                </>
                                            ) : (
                                                <>
                                                    <CheckCircle className="h-4 w-4 mr-2" />
                                                    Acknowledge
                                                </>
                                            )}
                                        </button>
                                    </div>
                                </form>
                            </Dialog.Panel>
                        </Transition.Child>
                    </div>
                </div>
            </Dialog>
        </Transition>
    );
};

export default AcknowledgeDiscrepancyModal;
