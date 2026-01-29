import React, { useState, Fragment } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import { ZoneViolation, SEVERITY_COLORS } from '../../types/tracking';
import { formatDistanceToNow, format } from 'date-fns';
import { 
    X, 
    AlertOctagon, 
    MapPin, 
    Clock,
    CheckCircle,
    Loader2
} from 'lucide-react';

interface AcknowledgeViolationModalProps {
    violation: ZoneViolation;
    isOpen: boolean;
    onClose: () => void;
    onAcknowledge: (notes: string) => void;
    isSubmitting: boolean;
}

/**
 * AcknowledgeViolationModal - Modal for acknowledging zone violations
 * Feature: 005-asset-tracking-security
 * Phase 9: Zone Violations Report
 */
const AcknowledgeViolationModal: React.FC<AcknowledgeViolationModalProps> = ({
    violation,
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

    // Format duration
    const formatDuration = (seconds?: number) => {
        if (!seconds) return 'Ongoing';
        if (seconds < 60) return `${seconds} seconds`;
        if (seconds < 3600) return `${Math.floor(seconds / 60)} minutes`;
        const hours = Math.floor(seconds / 3600);
        const mins = Math.floor((seconds % 3600) / 60);
        return `${hours}h ${mins}m`;
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
                                        <AlertOctagon className="h-5 w-5 text-red-500 mr-2" />
                                        Acknowledge Violation
                                    </Dialog.Title>
                                    <button
                                        onClick={onClose}
                                        className="text-gray-400 hover:text-gray-300"
                                    >
                                        <X className="h-5 w-5" />
                                    </button>
                                </div>

                                {/* Violation Details */}
                                <div className="bg-gray-700 rounded-lg p-4 mb-4">
                                    <div className="space-y-3">
                                        {/* Asset */}
                                        <div className="flex justify-between">
                                            <span className="text-sm text-gray-400">Asset</span>
                                            <span className="text-sm font-medium text-white">
                                                {violation.assetIdentifier}
                                                {violation.assetName && (
                                                    <span className="text-gray-400 ml-1">
                                                        ({violation.assetName})
                                                    </span>
                                                )}
                                            </span>
                                        </div>

                                        {/* Zone */}
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-gray-400 flex items-center">
                                                <MapPin className="h-4 w-4 mr-1" />
                                                Zone
                                            </span>
                                            <div className="text-right">
                                                <span className="text-sm font-medium text-white">
                                                    {violation.zoneName}
                                                </span>
                                                <span 
                                                    className="ml-2 inline-flex items-center px-2 py-0.5 rounded text-xs font-medium"
                                                    style={{
                                                        backgroundColor: violation.zoneType === 'PROHIBITED' ? 'rgba(127, 29, 29, 0.5)' : 
                                                                        violation.zoneType === 'RESTRICTED' ? 'rgba(124, 45, 18, 0.5)' :
                                                                        violation.zoneType === 'CONTROLLED' ? 'rgba(113, 63, 18, 0.5)' : 'rgba(30, 58, 138, 0.5)',
                                                        color: violation.zoneType === 'PROHIBITED' ? '#FCA5A5' : 
                                                               violation.zoneType === 'RESTRICTED' ? '#FDBA74' :
                                                               violation.zoneType === 'CONTROLLED' ? '#FCD34D' : '#93C5FD'
                                                    }}
                                                >
                                                    {violation.zoneType}
                                                </span>
                                            </div>
                                        </div>

                                        {/* Severity */}
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-gray-500">Severity</span>
                                            <span 
                                                className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium"
                                                style={{ 
                                                    backgroundColor: `${SEVERITY_COLORS[violation.severity]}20`, 
                                                    color: SEVERITY_COLORS[violation.severity],
                                                    border: `1px solid ${SEVERITY_COLORS[violation.severity]}40`
                                                }}
                                            >
                                                {violation.severity}
                                            </span>
                                        </div>

                                        {/* Timestamp */}
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-gray-400 flex items-center">
                                                <Clock className="h-4 w-4 mr-1" />
                                                Entry Time
                                            </span>
                                            <div className="text-right">
                                                <span className="text-sm font-medium text-white">
                                                    {format(new Date(violation.timestamp), 'MMM d, yyyy HH:mm')}
                                                </span>
                                                <span className="text-xs text-gray-500 block">
                                                    {formatDistanceToNow(new Date(violation.timestamp), { addSuffix: true })}
                                                </span>
                                            </div>
                                        </div>

                                        {/* Duration */}
                                        <div className="flex justify-between">
                                            <span className="text-sm text-gray-400">Duration</span>
                                            <span className="text-sm font-medium text-white">
                                                {formatDuration(violation.durationSeconds)}
                                            </span>
                                        </div>

                                        {/* Location */}
                                        {violation.entryLatitude && violation.entryLongitude && (
                                            <div className="flex justify-between">
                                                <span className="text-sm text-gray-400">Entry Location</span>
                                                <span className="text-sm text-gray-300 font-mono">
                                                    {violation.entryLatitude.toFixed(6)}, {violation.entryLongitude.toFixed(6)}
                                                </span>
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
                                            Resolution Notes (Optional)
                                        </label>
                                        <textarea
                                            id="notes"
                                            rows={3}
                                            value={notes}
                                            onChange={(e) => setNotes(e.target.value)}
                                            placeholder="Describe the resolution or any relevant notes..."
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

export default AcknowledgeViolationModal;
