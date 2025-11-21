"use client";

import { useAuth } from "../../app/context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import CompanyAdminNav from "./CompanyAdminNav";

interface CompanyAdminLayoutProps {
  children: React.ReactNode;
  title?: string;
  description?: string;
}

export default function CompanyAdminLayout({
  children,
  title,
  description,
}: CompanyAdminLayoutProps) {
  const { isAuthenticated, isCompanyAdmin, isSuperAdmin } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    // Only Company Admins and Super Admins can access company admin panel
    if (!isCompanyAdmin && !isSuperAdmin) {
      router.push("/unauthorized");
      return;
    }
  }, [isAuthenticated, isCompanyAdmin, isSuperAdmin, router]);

  if (!isAuthenticated || (!isCompanyAdmin && !isSuperAdmin)) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-green-600 mx-auto"></div>
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
              <CompanyAdminNav />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">{children}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
