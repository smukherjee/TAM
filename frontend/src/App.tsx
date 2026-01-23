import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import MainLayout from './components/Layout/MainLayout';
import MapPage from './pages/MapPage';
import TurnaroundPage from './pages/TurnaroundPage';
import TurnaroundDetailPage from './pages/TurnaroundDetailPage';
import AnalyticsPage from './pages/AnalyticsPage';
import PipelinePage from './pages/PipelinePage';
import LoginPage from './pages/LoginPage';
import PlatformAdminPage from './pages/PlatformAdminPage';
import AssetManagementPage from './pages/AssetManagementPage';
import AssetsListPage from './pages/AssetsListPage';
import KitsListPage from './pages/KitsListPage';
import CategoriesListPage from './pages/CategoriesListPage';
import TagsListPage from './pages/TagsListPage';
import LocationsListPage from './pages/LocationsListPage';
import { AuthProvider, useAuth } from './context/AuthContext';

const ProtectedRoute = ({ children }: { children: React.ReactElement }) => {
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
      <Route path="/turnaround/:id" element={
        <ProtectedRoute>
          <MainLayout>
            <TurnaroundDetailPage />
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
      <Route path="/assets" element={
        <ProtectedRoute>
          <MainLayout>
            <AssetManagementPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="/assets/list" element={
        <ProtectedRoute>
          <MainLayout>
            <AssetsListPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="/assets/kits" element={
        <ProtectedRoute>
          <MainLayout>
            <KitsListPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="/assets/categories" element={
        <ProtectedRoute>
          <MainLayout>
            <CategoriesListPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="/assets/tags" element={
        <ProtectedRoute>
          <MainLayout>
            <TagsListPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="/assets/locations" element={
        <ProtectedRoute>
          <MainLayout>
            <LocationsListPage />
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
