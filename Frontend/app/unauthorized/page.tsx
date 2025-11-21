"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Button } from "../../components/ui/button";
import { useAuth } from "../context/AuthContext";
import { FaShieldAlt, FaExclamationTriangle, FaSync } from "react-icons/fa";
import { refreshAuthToken, getStoredRefreshToken } from "../api/services/auth";

export default function UnauthorizedPage() {
  const router = useRouter();
  const { isAuthenticated, user, isSuperAdmin, isCompanyAdmin, isSeller, logout } =
    useAuth();
  const [isRefreshing, setIsRefreshing] = useState(false);

  useEffect(() => {
    // Log the unauthorized access attempt (in a real app, you might want to track this)
    console.log("Unauthorized access attempt", {
      user: user?.email,
      roles: user?.roles,
      isAuthenticated,
      isSuperAdmin,
      isCompanyAdmin,
      isSeller,
    });
  }, [user, isAuthenticated, isSuperAdmin, isCompanyAdmin, isSeller]);

  const handleRefreshToken = async () => {
    setIsRefreshing(true);
    try {
      const refreshToken = getStoredRefreshToken();
      if (!refreshToken) {
        alert("No refresh token found. Please log out and log back in.");
        setIsRefreshing(false);
        return;
      }

      // Refresh the token (this will get updated roles from the database)
      const authResponse = await refreshAuthToken(refreshToken);
      
      // Store the new token
      localStorage.setItem("token", authResponse.token);
      if (authResponse.refreshToken) {
        localStorage.setItem("refreshToken", authResponse.refreshToken);
      }

      // Reload the page to update the auth context
      window.location.reload();
    } catch (error) {
      console.error("Error refreshing token:", error);
      alert("Failed to refresh token. Please log out and log back in to get updated permissions.");
      setIsRefreshing(false);
    }
  };

  const getAccessMessage = () => {
    if (!isAuthenticated) {
      return "You need to be logged in to access this page.";
    }

    if (isSuperAdmin) {
      return "Your token may be outdated. If you were recently granted admin access, please refresh your session or log out and log back in.";
    }

    if (isCompanyAdmin) {
      return "You have Company Admin access but this page requires Super Admin permissions.";
    }

    if (isSeller) {
      return "You have Seller access but this page requires Admin permissions.";
    }

    return "You do not have the required permissions to access this page.";
  };

  const getSuggestedActions = () => {
    if (!isAuthenticated) {
      return (
        <Link href="/auth/login" passHref>
          <Button className="w-full">Sign In</Button>
        </Link>
      );
    }

    if (isSuperAdmin) {
      return (
        <Link href="/admin" passHref>
          <Button className="w-full">Go to Admin Dashboard</Button>
        </Link>
      );
    }

    if (isCompanyAdmin) {
      return (
        <Link href="/seller/company" passHref>
          <Button className="w-full">Go to Company Dashboard</Button>
        </Link>
      );
    }

    if (isSeller) {
      return (
        <Link href="/seller" passHref>
          <Button className="w-full">Go to Seller Dashboard</Button>
        </Link>
      );
    }

    return (
      <Link href="/" passHref>
        <Button className="w-full">Return to Home</Button>
      </Link>
    );
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 text-center">
        <div>
          <div className="flex justify-center mb-4">
            <FaExclamationTriangle className="w-16 h-16 text-red-500" />
          </div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Access Denied
          </h2>
          <p className="mt-2 text-center text-sm text-gray-600">
            {getAccessMessage()}
          </p>
        </div>

        {/* Role Information */}
        {isAuthenticated && user && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <FaShieldAlt className="w-4 h-4 text-blue-600" />
              <span className="text-sm font-medium text-blue-900">
                Your Current Access Level:
              </span>
            </div>
            <div className="text-sm text-blue-800">
              {isSuperAdmin && "Super Admin (Platform-wide access)"}
              {isCompanyAdmin &&
                !isSuperAdmin &&
                "Company Admin (Company-scoped access)"}
              {isSeller &&
                !isCompanyAdmin &&
                !isSuperAdmin &&
                "Seller (Individual seller access)"}
              {!isSeller &&
                !isCompanyAdmin &&
                !isSuperAdmin &&
                "Buyer (Customer access)"}
            </div>
            <div className="text-xs text-blue-600 mt-1">
              Roles: {user.roles?.join(", ") || "None"}
            </div>
          </div>
        )}

        <div className="flex flex-col space-y-4">
          {getSuggestedActions()}
          
          {/* Refresh Token Button - Show if user is authenticated but doesn't have required role */}
          {isAuthenticated && !isSuperAdmin && (
            <Button
              onClick={handleRefreshToken}
              disabled={isRefreshing}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white"
            >
              <FaSync className={`w-4 h-4 mr-2 ${isRefreshing ? 'animate-spin' : ''}`} />
              {isRefreshing ? "Refreshing..." : "Refresh Session (Get Updated Permissions)"}
            </Button>
          )}
          
          <Button
            onClick={() => router.back()}
            variant="outline"
            className="w-full"
          >
            Go Back
          </Button>
          <Link href="/" passHref>
            <Button variant="outline" className="w-full">
              Return to Home
            </Button>
          </Link>
          {isAuthenticated && (
            <>
              <Button
                onClick={() => {
                  logout();
                  router.push("/auth/login");
                }}
                variant="outline"
                className="w-full"
              >
                Log Out and Sign In Again
              </Button>
              <Link href="/auth/login" passHref>
                <Button variant="link" className="w-full">
                  Sign in with a different account
                </Button>
              </Link>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
