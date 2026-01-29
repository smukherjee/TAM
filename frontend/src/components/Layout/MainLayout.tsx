import React, { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { Map, Activity, BarChart3, Plane, ChevronLeft, ChevronRight, LogOut, Network, Package, ChevronDown, ChevronUp, Flame, AlertOctagon, AlertTriangle, Route } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

const MainLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isOpen, setIsOpen] = useState(true);
    const [assetMenuOpen, setAssetMenuOpen] = useState(false);
    const [securityMenuOpen, setSecurityMenuOpen] = useState(false);
    const { user, logout } = useAuth();

    // Logic for role-based access
    const role = user?.role ? user.role.trim().toUpperCase() : '';
    const showMap = role === 'ADMIN' || role === 'GH';
    const showTurnaround = role === 'ADMIN' || role === 'GH';
    const showReports = role === 'ADMIN' || role === 'AIRPORT_USER';
    const showAssets = role === 'ADMIN' || role === 'GH';
    const showPipeline = role === 'ADMIN';
    const showAirsideOps = role === 'ADMIN' || role === 'GH' || role === 'AIRPORT_USER';
    const showSecurityReports = role === 'ADMIN' || role === 'GH';

    return (
        <div className="flex h-screen bg-gray-900 text-white overflow-hidden font-sans">
            {/* Sidebar */}
            <div className={`relative bg-gray-800 border-r border-gray-700 flex flex-col shadow-2xl z-20 transition-all duration-300 ease-in-out ${isOpen ? 'w-64' : 'w-20'}`}>

                {/* Header */}
                <div className="p-6 border-b border-gray-700 flex items-center justify-between bg-gray-900 h-20 overflow-hidden">
                    <div className="flex items-center space-x-3">
                        <Plane className="text-blue-500 animate-pulse shrink-0" size={28} />
                        <h1 className={`text-xl font-bold tracking-wider transition-opacity duration-200 whitespace-nowrap ${isOpen ? 'opacity-100' : 'opacity-0 hidden'}`}>
                            TAM <span className="text-blue-500">OS</span>
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

                    {showMap && <NavItem to="/" icon={<Map size={20} />} label="Live Map" isOpen={isOpen} />}
                    {showAssets && (
                        <>
                            <div
                                onClick={() => setAssetMenuOpen(!assetMenuOpen)}
                                className="flex items-center justify-between px-3 py-2.5 rounded-lg transition-all duration-200 cursor-pointer hover:bg-gray-700 text-gray-300 hover:text-white group mb-1"
                            >
                                <div className="flex items-center space-x-3">
                                    <Package size={20} className="shrink-0" />
                                    {isOpen && <span className="font-medium text-sm">Asset Management</span>}
                                </div>
                                {isOpen && (
                                    assetMenuOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />
                                )}
                            </div>
                            {assetMenuOpen && isOpen && (
                                <div className="ml-6 space-y-1 border-l-2 border-gray-700 pl-3 mb-2">
                                    <SubNavItem to="/assets/list" label="Assets" />
                                    <SubNavItem to="/assets/kits" label="Kits" />
                                    <SubNavItem to="/assets/categories" label="Categories" />
                                    <SubNavItem to="/assets/tags" label="Tags" />
                                    <SubNavItem to="/assets/locations" label="Locations" />
                                </div>
                            )}
                        </>
                    )}
                    {showTurnaround && <NavItem to="/turnaround" icon={<Activity size={20} />} label="Turnaround" isOpen={isOpen} />}
                    {showReports && <NavItem to="/reports" icon={<BarChart3 size={20} />} label="Analytics" isOpen={isOpen} />}
                    
                    {/* Airside Operations Section - Hotspot Analysis only (Assets integrated into Live Map) */}
                    {showAirsideOps && (
                        <div className={`mt-4 pt-4 border-t border-gray-700`}>
                            <div className={`text-xs font-semibold text-gray-500 uppercase tracking-wilder mb-2 px-2 transition-all duration-300 ${isOpen ? 'opacity-100 h-auto' : 'opacity-0 h-0 hidden'}`}>
                                Airside Operations
                            </div>
                            <NavItem to="/tracking/hotspots" icon={<Flame size={20} />} label="Hotspot Analysis" isOpen={isOpen} />
                        </div>
                    )}
                    
                    {/* Security Reports Section */}
                    {showSecurityReports && (
                        <div className={`mt-4 pt-4 border-t border-gray-700`}>
                            <div className={`text-xs font-semibold text-gray-500 uppercase tracking-wilder mb-2 px-2 transition-all duration-300 ${isOpen ? 'opacity-100 h-auto' : 'opacity-0 h-0 hidden'}`}>
                                Security Reports
                            </div>
                            <div
                                onClick={() => setSecurityMenuOpen(!securityMenuOpen)}
                                className="flex items-center justify-between px-3 py-2.5 rounded-lg transition-all duration-200 cursor-pointer hover:bg-gray-700 text-gray-300 hover:text-white group mb-1"
                            >
                                <div className="flex items-center space-x-3">
                                    <AlertOctagon size={20} className="shrink-0 text-red-400" />
                                    {isOpen && <span className="font-medium text-sm">Security</span>}
                                </div>
                                {isOpen && (
                                    securityMenuOpen ? <ChevronUp size={16} /> : <ChevronDown size={16} />
                                )}
                            </div>
                            {(securityMenuOpen || !isOpen) && isOpen && (
                                <div className="ml-6 space-y-1 border-l-2 border-gray-700 pl-3 mb-2">
                                    <SubNavItem to="/tracking/violations" label="Zone Violations" icon={<AlertOctagon size={14} className="text-red-400" />} />
                                    <SubNavItem to="/tracking/discrepancies" label="Discrepancies" icon={<AlertTriangle size={14} className="text-orange-400" />} />
                                    <SubNavItem to="/tracking/trail" label="Movement Trail" icon={<Route size={14} className="text-blue-400" />} />
                                </div>
                            )}
                            {!isOpen && (
                                <div className="space-y-1">
                                    <NavItem to="/tracking/violations" icon={<AlertOctagon size={20} className="text-red-400" />} label="Zone Violations" isOpen={isOpen} />
                                    <NavItem to="/tracking/discrepancies" icon={<AlertTriangle size={20} className="text-orange-400" />} label="Discrepancies" isOpen={isOpen} />
                                    <NavItem to="/tracking/trail" icon={<Route size={20} className="text-blue-400" />} label="Movement Trail" isOpen={isOpen} />
                                </div>
                            )}
                        </div>
                    )}
                    
                    {showPipeline && <NavItem to="/pipeline" icon={<Network size={20} />} label="Observability" isOpen={isOpen} />}
                    {role === 'ADMIN' && (
                        <div className={`mt-6 pt-6 border-t border-gray-700 ${isOpen ? 'block' : 'hidden md:block'}`}>
                            <div className={`text-xs font-semibold text-gray-500 uppercase tracking-wilder mb-2 px-2 transition-all duration-300 ${isOpen ? 'opacity-100 h-auto' : 'opacity-0 h-0 hidden'}`}>
                                Admin
                            </div>
                            <NavItem to="/admin" icon={<Network size={20} className="text-purple-400" />} label="Platform Admin" isOpen={isOpen} />
                        </div>
                    )}
                </nav>

                {/* Footer / User Info */}
                <div className="p-4 bg-gray-900 border-t border-gray-700">
                    <div className={`flex items-center ${isOpen ? 'justify-between' : 'justify-center'} mb-4 transition-all duration-200`}>
                        {isOpen ? (
                            <div className="overflow-hidden">
                                <p className="text-sm font-medium text-white truncate">{user?.username}</p>
                                <p className="text-xs text-gray-500 truncate">{user?.role} • {user?.icaoCode}</p>
                                <div className="text-[10px] text-gray-500 mt-1">
                                    Role: "{role}"<br />
                                    ShowObs: {showPipeline ? 'YES' : 'NO'}
                                </div>
                            </div>
                        ) : null}
                    </div>
                    <button onClick={logout} className={`p-2 rounded hover:bg-red-500/10 text-red-500 w-full flex items-center ${isOpen ? 'justify-start space-x-3' : 'justify-center'} transition-colors duration-200`} title="Sign Out">
                        <LogOut size={18} />
                        {isOpen && <span>Sign Out</span>}
                    </button>
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
        className={({ isActive }: { isActive: boolean }) =>
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

const SubNavItem: React.FC<{ to: string; label: string; icon?: React.ReactNode }> = ({ to, label, icon }) => {
    return (
        <NavLink
            to={to}
            className={({ isActive }) =>
                `flex items-center px-3 py-2 rounded-md transition-all duration-200 text-sm ${
                    isActive
                        ? 'bg-blue-600 text-white'
                        : 'text-gray-400 hover:bg-gray-700 hover:text-white'
                }`
            }
        >
            {icon && <span className="mr-2">{icon}</span>}
            {label}
        </NavLink>
    );
};

export default MainLayout;
