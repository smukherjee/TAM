import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './components/Layout/MainLayout';
import MapPage from './pages/MapPage';
import TurnaroundPage from './pages/TurnaroundPage';
import AnalyticsPage from './pages/AnalyticsPage';
import PipelinePage from './pages/PipelinePage';
import LoginPage from './pages/LoginPage';
import PlatformAdminPage from './pages/PlatformAdminPage';
import { AuthProvider, useAuth } from './context/AuthContext';

const ProtectedRoute = ({ children }: { children: JSX.Element }) => {
  const { user, isLoading } = useAuth();
  if (isLoading) return <div className="h-screen flex items-center justify-center bg-gray-900 text-white">Loading...</div>;
  if (!user) return <Navigate to="/login" replace />;
  return children;
};

const AppRoutes = () => {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={
        <ProtectedRoute>
          <MainLayout>
            <MapPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="/turnaround" element={
        <ProtectedRoute>
          <MainLayout>
            <TurnaroundPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="/reports" element={
        <ProtectedRoute>
          <MainLayout>
            <AnalyticsPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="/pipeline" element={
        <ProtectedRoute>
          <MainLayout>
            <PipelinePage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="/admin" element={
        <ProtectedRoute>
          <MainLayout>
            <PlatformAdminPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

const App: React.FC = () => {
  return (
    <Router>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </Router>
  );
};

export default App;
