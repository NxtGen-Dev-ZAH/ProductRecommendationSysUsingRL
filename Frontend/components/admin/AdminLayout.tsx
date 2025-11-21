"use client";

import { useAuth } from "../../app/context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import AdminNav from "./AdminNav";
import { useTokenRefresh } from "../../app/hooks/useTokenRefresh";
import { Button } from "../ui/button";
import { FaSync, FaExclamationTriangle } from "react-icons/fa";

interface AdminLayoutProps {
  children: React.ReactNode;
  activeNav?: string;
  title?: string;
  description?: string;
}

export default function AdminLayout({
  children,
  activeNav,
  title,
  description,
}: AdminLayoutProps) {
  const { isAuthenticated, isSuperAdmin, loading, user } = useAuth();
  const router = useRouter();
  const { refreshToken, isRefreshing } = useTokenRefresh();
  const [showRefreshPrompt, setShowRefreshPrompt] = useState(false);

  useEffect(() => {
    // Don't redirect while still loading user data
    if (loading) return;
    
    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    // Only Super Admins (ROLE_APP_ADMIN) can access admin panel
    if (!isSuperAdmin) {
      // If user is authenticated but not super admin, they might have outdated token
      // Show refresh prompt instead of immediately redirecting
      if (isAuthenticated && user) {
        setShowRefreshPrompt(true);
        // Still redirect after a delay to give them a chance to refresh
        const timer = setTimeout(() => {
          router.push("/unauthorized");
        }, 5000);
        return () => clearTimeout(timer);
      } else {
        router.push("/unauthorized");
        return;
      }
    } else {
      setShowRefreshPrompt(false);
    }
  }, [isAuthenticated, isSuperAdmin, loading, router, user]);

  const handleRefresh = async () => {
    const success = await refreshToken();
    if (!success) {
      alert("Failed to refresh token. Please log out and log back in.");
    }
  };

  // Show loading while checking authentication
  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  // Show refresh prompt if authenticated but not super admin (likely outdated token)
  if (showRefreshPrompt && isAuthenticated && !isSuperAdmin) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-6 text-center">
          <FaExclamationTriangle className="w-12 h-12 text-yellow-500 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-gray-900 mb-2">
            Token May Be Outdated
          </h2>
          <p className="text-gray-600 mb-6">
            Your session token may not include your latest permissions. 
            Click below to refresh your session and get updated access.
          </p>
          <div className="flex flex-col gap-3">
            <Button
              onClick={handleRefresh}
              disabled={isRefreshing}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white"
            >
              <FaSync className={`w-4 h-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
              {isRefreshing ? "Refreshing..." : "Refresh Session"}
            </Button>
            <Button
              onClick={() => router.push("/unauthorized")}
              variant="outline"
              className="w-full"
            >
              Go to Unauthorized Page
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // Show loading if not authenticated or not super admin (after refresh prompt timeout)
  if (!isAuthenticated || !isSuperAdmin) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-red-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-7xl mx-auto">
          {/* Header */}
          {(title || description) && (
            <div className="mb-6">
              {title && (
                <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
              )}
              {description && (
                <p className="text-gray-600 mt-1">{description}</p>
              )}
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AdminNav activeItem={activeNav} />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">{children}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
