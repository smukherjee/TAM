import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { Map, Activity, BarChart3, Plane, ChevronLeft, ChevronRight } from 'lucide-react';

const MainLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isOpen, setIsOpen] = useState(true);

    return (
        <div className="flex h-screen bg-gray-900 text-white overflow-hidden font-sans">
            {/* Sidebar */}
            <div className={`relative bg-gray-800 border-r border-gray-700 flex flex-col shadow-2xl z-20 transition-all duration-300 ease-in-out ${isOpen ? 'w-64' : 'w-20'}`}>

                {/* Header */}
                <div className="p-6 border-b border-gray-700 flex items-center justify-between bg-gray-900 h-20 overflow-hidden">
                    <div className="flex items-center space-x-3">
                        <Plane className="text-blue-500 animate-pulse shrink-0" size={28} />
                        <h1 className={`text-xl font-bold tracking-wider transition-opacity duration-200 whitespace-nowrap ${isOpen ? 'opacity-100' : 'opacity-0 hidden'}`}>
                            UTAM <span className="text-blue-500">OS</span>
                        </h1>
                    </div>
                </div>

                {/* Toggle Button */}
                <button
                    onClick={() => setIsOpen(!isOpen)}
                    className={`absolute top-8 -right-3 z-30 bg-blue-600 text-white p-1 rounded-full shadow-lg border-2 border-gray-900 hover:bg-blue-700 transition-transform duration-200 focus:outline-none`}
                >
                    {isOpen ? <ChevronLeft size={14} /> : <ChevronRight size={14} />}
                </button>

                {/* Navigation */}
                <nav className="flex-1 p-3 space-y-2 overflow-y-auto overflow-x-hidden mt-2">
                    <div className={`text-xs font-semibold text-gray-500 uppercase tracking-wilder mb-2 px-2 transition-all duration-300 ${isOpen ? 'opacity-100 h-auto' : 'opacity-0 h-0 hidden'}`}>
                        Dashboards
                    </div>

                    <NavItem to="/" icon={<Map size={20} />} label="Live Map" isOpen={isOpen} />
                    <NavItem to="/turnaround" icon={<Activity size={20} />} label="Turnaround" isOpen={isOpen} />
                    <NavItem to="/reports" icon={<BarChart3 size={20} />} label="Analytics" isOpen={isOpen} />
                </nav>

                {/* Footer */}
                <div className={`p-4 bg-gray-900/50 text-xs text-gray-500 text-center border-t border-gray-700 whitespace-nowrap overflow-hidden transition-all duration-300 ${isOpen ? 'opacity-100' : 'opacity-0'}`}>
                    {isOpen ? 'TAM Platform v1.0' : ''}
                </div>
            </div>

            {/* Main Content */}
            <div className="flex-1 flex flex-col overflow-hidden bg-gray-100 text-black relative">
                {children}
            </div>
        </div>
    );
};

const NavItem = ({ to, icon, label, isOpen }: { to: string; icon: React.ReactNode; label: string; isOpen: boolean }) => (
    <NavLink
        to={to}
        className={({ isActive }) =>
            `flex items-center ${isOpen ? 'justify-start space-x-3 px-4' : 'justify-center px-0'} py-3 rounded-xl transition-all duration-200 ${isActive
                ? 'bg-blue-600 text-white shadow-lg shadow-blue-500/20'
                : 'text-gray-400 hover:bg-gray-700 hover:text-white'
            }`
        }
        title={!isOpen ? label : ''}
    >
        <div className="shrink-0">{icon}</div>
        <span className={`font-medium transition-all duration-200 whitespace-nowrap overflow-hidden ${isOpen ? 'w-auto opacity-100 ml-3' : 'w-0 opacity-0 ml-0'}`}>
            {label}
        </span>
    </NavLink>
);

export default MainLayout;
