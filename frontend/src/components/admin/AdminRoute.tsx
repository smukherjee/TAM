import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { Shield, AlertTriangle } from 'lucide-react';

/**
 * T086: AdminRoute - Route guard component requiring ADMIN role.
 * Wraps protected routes and redirects unauthorized users.
 */

interface User {
  id: string;
  username: string;
  email: string;
  roles: string[];
  tenantCode: string;
}

interface AdminRouteProps {
  children: React.ReactNode;
  requiredRole?: string;
}

// Hook to get current user - would typically come from auth context
const useCurrentUser = (): { user: User | null; isLoading: boolean } => {
  // This would be replaced with actual auth context hook
  // For now, we'll use localStorage as a placeholder
  const [user, setUser] = React.useState<User | null>(null);
  const [isLoading, setIsLoading] = React.useState(true);

  React.useEffect(() => {
    const loadUser = async () => {
      try {
        // Check for stored auth token
        const token = localStorage.getItem('authToken');
        if (token) {
          // Fetch user profile from API
          const response = await fetch('/api/auth/me', {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          });
          if (response.ok) {
            const userData = await response.json();
            setUser(userData);
          }
        }
      } catch (error) {
        console.error('Failed to load user:', error);
      } finally {
        setIsLoading(false);
      }
    };
    loadUser();
  }, []);

  return { user, isLoading };
};

/**
 * Check if user has the required role
 */
const hasRole = (user: User | null, role: string): boolean => {
  if (!user) return false;
  return user.roles.includes(role) || user.roles.includes('SUPER_ADMIN');
};

/**
 * AdminRoute component that protects routes requiring admin access
 */
export const AdminRoute: React.FC<AdminRouteProps> = ({
  children,
  requiredRole = 'ADMIN',
}) => {
  const { user, isLoading } = useCurrentUser();
  const location = useLocation();

  // Show loading spinner while checking auth
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto" />
          <p className="mt-4 text-gray-600">Checking authorization...</p>
        </div>
      </div>
    );
  }

  // If not logged in, redirect to login
  if (!user) {
    return (
      <Navigate
        to="/login"
        state={{ from: location, message: 'Please log in to access admin features' }}
        replace
      />
    );
  }

  // If logged in but lacks required role, show access denied
  if (!hasRole(user, requiredRole)) {
    return <AccessDenied requiredRole={requiredRole} userRoles={user.roles} />;
  }

  // User is authorized, render children
  return <>{children}</>;
};

/**
 * Access Denied component shown when user lacks required permissions
 */
interface AccessDeniedProps {
  requiredRole: string;
  userRoles: string[];
}

const AccessDenied: React.FC<AccessDeniedProps> = ({ requiredRole, userRoles }) => {
  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-lg overflow-hidden">
        {/* Header */}
        <div className="bg-red-500 px-6 py-8 text-center">
          <Shield className="w-16 h-16 text-white mx-auto" />
          <h1 className="mt-4 text-2xl font-bold text-white">Access Denied</h1>
        </div>

        {/* Content */}
        <div className="px-6 py-6">
          <div className="flex items-start gap-3 mb-4">
            <AlertTriangle className="w-6 h-6 text-amber-500 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-gray-700">
                You do not have permission to access this page.
              </p>
              <p className="mt-2 text-sm text-gray-500">
                This page requires the <strong>{requiredRole}</strong> role.
              </p>
            </div>
          </div>

          {/* Current Roles */}
          <div className="mt-6 p-4 bg-gray-50 rounded-lg">
            <p className="text-sm font-medium text-gray-700 mb-2">Your current roles:</p>
            <div className="flex flex-wrap gap-2">
              {userRoles.length > 0 ? (
                userRoles.map((role) => (
                  <span
                    key={role}
                    className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-200 text-gray-700"
                  >
                    {role}
                  </span>
                ))
              ) : (
                <span className="text-sm text-gray-500">No roles assigned</span>
              )}
            </div>
          </div>

          {/* Actions */}
          <div className="mt-6 flex gap-3">
            <a
              href="/"
              className="flex-1 inline-flex justify-center items-center px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Go Home
            </a>
            <a
              href="mailto:admin@example.com?subject=Access%20Request%20-%20Admin%20Features"
              className="flex-1 inline-flex justify-center items-center px-4 py-2 border border-transparent rounded-md text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Request Access
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};

/**
 * Higher-order component version for class components or complex wrapping
 */
export const withAdminAuth = <P extends object>(
  WrappedComponent: React.ComponentType<P>,
  requiredRole: string = 'ADMIN'
): React.FC<P> => {
  const WithAdminAuth: React.FC<P> = (props) => (
    <AdminRoute requiredRole={requiredRole}>
      <WrappedComponent {...props} />
    </AdminRoute>
  );

  WithAdminAuth.displayName = `WithAdminAuth(${
    WrappedComponent.displayName || WrappedComponent.name || 'Component'
  })`;

  return WithAdminAuth;
};

export default AdminRoute;
