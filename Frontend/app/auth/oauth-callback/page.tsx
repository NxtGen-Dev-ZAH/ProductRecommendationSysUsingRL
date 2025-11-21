"use client";

import { useEffect, Suspense, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "../../context/AuthContext"; // Commented out as it's not currently used
import { setAuthToken } from "../../../utils/cookies";

function OAuthCallbackContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { handleOAuth2Completion } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(true);

  useEffect(() => {
    const token = searchParams.get("token");
    const refreshToken = searchParams.get("refreshToken");
    const email = searchParams.get("email");

    console.log("OAuth callback received:", {
      token: !!token,
      refreshToken: !!refreshToken,
      email,
    });

    if (token) {
      // Store the token in localStorage
      localStorage.setItem("token", token);

      // Also set cookie so middleware can read it immediately
      try {
        setAuthToken(token);
      } catch (e) {
        console.error("Failed to set auth cookie", e);
      }

      // Store refresh token if provided
      if (refreshToken) {
        localStorage.setItem("refreshToken", refreshToken);
      }

      // Store email if provided
      if (email) {
        localStorage.setItem("userEmail", email);
      }

      // Update authentication context
      const updateAuthContext = async () => {
        try {
          // Use the new handleOAuth2Completion method
          console.log("Token stored, updating auth context...");
          await handleOAuth2Completion(
            token,
            refreshToken || undefined,
            email || undefined
          );

          // Redirect to home page after successful authentication
          setTimeout(() => {
            router.push("/");
          }, 1000);
        } catch (err) {
          console.error("Error updating auth context:", err);
          // Even if context update fails, user is authenticated via token
          // Redirect to home page
          setTimeout(() => {
            router.push("/");
          }, 1000);
        } finally {
          setIsProcessing(false);
        }
      };

      updateAuthContext();
    } else {
      // If no token is found, redirect to login page
      console.error("No token received in OAuth callback");
      setError("Authentication failed. No token received.");
      setIsProcessing(false);
      setTimeout(() => {
        router.push("/auth/login");
      }, 3000);
    }
  }, [searchParams, router, handleOAuth2Completion]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="p-8 bg-white shadow-md rounded-lg max-w-md w-full">
          <div className="text-center">
            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-red-100 mb-4">
              <svg
                className="h-6 w-6 text-red-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M6 18L18 6M6 6l12 12"
                />
              </svg>
            </div>
            <h1 className="text-2xl font-bold text-gray-900 mb-2">
              Authentication Error
            </h1>
            <p className="text-gray-600 mb-4">{error}</p>
            <p className="text-sm text-gray-500">
              Redirecting to login page...
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (isProcessing) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="p-8 bg-white shadow-md rounded-lg max-w-md w-full">
          <div className="text-center">
            <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-blue-100 mb-4">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
            </div>
            <h1 className="text-2xl font-bold text-gray-900 mb-2">
              Processing Authentication...
            </h1>
            <p className="text-gray-600">
              Please wait while we complete your login.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="p-8 bg-white shadow-md rounded-lg max-w-md w-full">
        <div className="text-center">
          <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-green-100 mb-4">
            <svg
              className="h-6 w-6 text-green-600"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M5 13l4 4L19 7"
              />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Authentication Successful!
          </h1>
          <p className="text-gray-600 mb-4">
            Welcome back! You have been successfully logged in.
          </p>
          <div className="flex justify-center">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500"></div>
          </div>
          <p className="text-sm text-gray-500 mt-4">
            Redirecting to homepage...
          </p>
        </div>
      </div>
    </div>
  );
}

// Loading component
function OAuthCallbackLoading() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="p-8 bg-white shadow-md rounded-lg max-w-md w-full">
        <div className="text-center">
          <div className="mx-auto flex items-center justify-center h-12 w-12 rounded-full bg-blue-100 mb-4">
            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">
            Processing Authentication...
          </h1>
          <p className="text-gray-600">
            Please wait while we complete your login.
          </p>
        </div>
      </div>
    </div>
  );
}

// Main export with Suspense boundary
export default function OAuthCallback() {
  return (
    <Suspense fallback={<OAuthCallbackLoading />}>
      <OAuthCallbackContent />
    </Suspense>
  );
}
