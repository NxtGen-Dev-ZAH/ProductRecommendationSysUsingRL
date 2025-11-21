"use client";

import { useEffect, Suspense } from "react";
import { useRouter } from "next/navigation";

function GoogleOAuthCallback() {
  const router = useRouter();

  useEffect(() => {
    // This route is called by the backend after successful OAuth2 authentication
    // The backend should redirect here with token data
    // For now, we'll redirect to the main OAuth callback page
    const redirectToMainCallback = () => {
      // Check if we have token data in localStorage (set by backend redirect)
      const token = localStorage.getItem("token");
      if (token) {
        // Token is already set, redirect to main callback to handle auth context
        router.push("/auth/oauth-callback");
      } else {
        // No token, redirect to login with error
        router.push("/auth/login?error=OAuth authentication failed");
      }
    };

    // Small delay to ensure any backend redirects are processed
    setTimeout(redirectToMainCallback, 500);
  }, [router]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="p-8 bg-white shadow-md rounded-lg max-w-md w-full">
        <div className="text-center">
          <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-blue-100 mb-4">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Processing Google Authentication...
          </h1>
          <p className="text-gray-600">
            Please wait while we complete your Google login.
          </p>
        </div>
      </div>
    </div>
  );
}

// Loading component for Suspense fallback
function GoogleOAuthLoading() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="p-8 bg-white shadow-md rounded-lg max-w-md w-full">
        <div className="text-center">
          <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-blue-100 mb-4">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Processing Google Authentication...
          </h1>
          <p className="text-gray-600">
            Please wait while we complete your Google login.
          </p>
        </div>
      </div>
    </div>
  );
}

// Main export with Suspense boundary
export default function GoogleOAuthPage() {
  return (
    <Suspense fallback={<GoogleOAuthLoading />}>
      <GoogleOAuthCallback />
    </Suspense>
  );
}
