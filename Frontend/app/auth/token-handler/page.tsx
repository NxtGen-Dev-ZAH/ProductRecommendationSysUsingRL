'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { setAuthToken } from '../../../utils/cookies';

export default function TokenHandlerPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  
  useEffect(() => {
    // Extract token from URL
    // This handles cases where the backend might return the token in different formats
    const extractToken = () => {
      // Get the full URL
      const url = window.location.href;
      
      // Check for token in various formats
      const tokenParam = new URLSearchParams(window.location.search).get('token');
      if (tokenParam) return tokenParam;
      
      // Check if token is in the URL path
      const pathMatch = url.match(/\/token\/([^\/]+)/);
      if (pathMatch && pathMatch[1]) return pathMatch[1];
      
      // Check if the entire page content might be the token
      const bodyContent = document.body.textContent?.trim();
      if (bodyContent && bodyContent.length > 20 && !bodyContent.includes('<') && !bodyContent.includes('>')) {
        return bodyContent;
      }
      
      return null;
    };
    
    try {
      const token = extractToken();
      
      if (token) {
        // Store token and set cookie so middleware can read it immediately
        try {
          setAuthToken(token);
          console.log('Token successfully captured and stored');
        } catch {
          // Fallback to localStorage if cookie helper fails (should be rare)
          localStorage.setItem('token', token);
          console.warn('setAuthToken failed, fell back to localStorage');
        }
        
        // Redirect to home page
        setTimeout(() => {
          router.push('/');
        }, 1000);
      } else {
        setError('No token found in the response');
      }
    } catch (err) {
      console.error('Error processing token:', err);
      setError('Failed to process authentication token');
    }
  }, [router]);
  
  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="p-8 bg-white rounded-lg shadow-md max-w-md w-full">
          <div className="text-red-600 mb-4">
            <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12 mx-auto" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <h2 className="text-2xl font-semibold mb-2 text-center">Authentication Error</h2>
          <p className="text-gray-600 text-center mb-6">{error}</p>
          <div className="flex justify-center">
            <button 
              onClick={() => router.push('/auth/login')}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              Return to Login
            </button>
          </div>
        </div>
      </div>
    );
  }
  
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="p-8 bg-white rounded-lg shadow-md">
        <h2 className="text-2xl font-semibold mb-4 text-center">Processing Authentication...</h2>
        <p className="text-gray-600 mb-6 text-center">Please wait while we complete your login</p>
        <div className="flex justify-center">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
        </div>
      </div>
    </div>
  );
} 