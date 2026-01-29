import React from 'react';
import './DashboardLayout.css';

interface DashboardLayoutProps {
  children: React.ReactNode;
}

const DashboardLayout: React.FC<DashboardLayoutProps> = ({ children }) => {
  return (
    <div className="dashboard-layout">
      <header className="dashboard-header">
        <h1>TAM Dashboard</h1>
      </header>
      <main className="dashboard-main">
        {children}
      </main>
    </div>
  );
};

export default DashboardLayout;
