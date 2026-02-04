import React, { lazy, Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import MainLayout from './components/Layout/MainLayout';
import UnifiedMapPage from './pages/UnifiedMapPage';
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
import HotspotAnalysisPage from './pages/HotspotAnalysisPage';
import { AuthProvider, useAuth } from './context/AuthContext';

// Lazy-loaded Security Report Pages
const RestrictedZoneReportPage = lazy(() => import('./pages/RestrictedZoneReportPage'));
const MovementDiscrepancyReportPage = lazy(() => import('./pages/MovementDiscrepancyReportPage'));
const MovementTrailPage = lazy(() => import('./pages/MovementTrailPage'));

// Lazy-loaded Admin Pages
const DataGeneratorAdminPage = lazy(() => import('./components/admin/DataGeneratorAdminPage'));
const ZoneEditorPage = lazy(() => import('./pages/admin/ZoneEditorPage'));
const PathEditorPage = lazy(() => import('./pages/admin/PathEditorPage'));

// Create a QueryClient instance
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 5000,
    },
  },
});

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
      {/* Unified Map - Single map with all layers (flights, vehicles, assets, heatmap) */}
      <Route path="/" element={
        <ProtectedRoute>
          <MainLayout>
            <UnifiedMapPage />
          </MainLayout>
        </ProtectedRoute>
      } />
      {/* Redirect legacy routes to unified map */}
      <Route path="/map/turnaround" element={<Navigate to="/" replace />} />
      <Route path="/tracking/assets" element={<Navigate to="/" replace />} />
      {/* Hotspot Analysis - Heatmap view */}
      <Route path="/tracking/hotspots" element={
        <ProtectedRoute>
          <MainLayout>
            <HotspotAnalysisPage />
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
      {/* Security Reports - Zone Violations */}
      <Route path="/tracking/violations" element={
        <ProtectedRoute>
          <MainLayout>
            <Suspense fallback={<div className="h-full flex items-center justify-center">Loading...</div>}>
              <RestrictedZoneReportPage />
            </Suspense>
          </MainLayout>
        </ProtectedRoute>
      } />
      {/* Security Reports - Movement Discrepancies */}
      <Route path="/tracking/discrepancies" element={
        <ProtectedRoute>
          <MainLayout>
            <Suspense fallback={<div className="h-full flex items-center justify-center">Loading...</div>}>
              <MovementDiscrepancyReportPage />
            </Suspense>
          </MainLayout>
        </ProtectedRoute>
      } />
      {/* Security Reports - Movement Trail */}
      <Route path="/tracking/trail" element={
        <ProtectedRoute>
          <MainLayout>
            <Suspense fallback={<div className="h-full flex items-center justify-center">Loading...</div>}>
              <MovementTrailPage />
            </Suspense>
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
      {/* Admin - Data Generator Management (requires ADMIN role) */}
      <Route path="/admin/generators" element={
        <ProtectedRoute>
          <MainLayout>
            <Suspense fallback={<div className="h-full flex items-center justify-center">Loading...</div>}>
              <DataGeneratorAdminPage />
            </Suspense>
          </MainLayout>
        </ProtectedRoute>
      } />
      {/* Admin - Zone Editor (requires ADMIN role) */}
      <Route path="/admin/zones" element={
        <ProtectedRoute>
          <MainLayout>
            <Suspense fallback={<div className="h-full flex items-center justify-center">Loading...</div>}>
              <ZoneEditorPage />
            </Suspense>
          </MainLayout>
        </ProtectedRoute>
      } />
      {/* Admin - Path Editor (requires ADMIN role) */}
      <Route path="/admin/paths" element={
        <ProtectedRoute>
          <MainLayout>
            <Suspense fallback={<div className="h-full flex items-center justify-center">Loading...</div>}>
              <PathEditorPage />
            </Suspense>
          </MainLayout>
        </ProtectedRoute>
      } />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </Router>
    </QueryClientProvider>
  );
};

export default App;
