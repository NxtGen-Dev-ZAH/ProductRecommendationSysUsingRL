// Cookie utility functions for token management

/**
 * Set a cookie with the given name, value, and options
 */
export function setCookie(
  name: string,
  value: string,
  options: {
    maxAge?: number;
    path?: string;
    sameSite?: 'Strict' | 'Lax' | 'None';
    secure?: boolean;
  } = {}
) {
  if (typeof document === 'undefined') return;

  const {
    maxAge = 7 * 24 * 60 * 60, // 7 days default
    path = '/',
    sameSite = 'Lax',
    secure = window.location.protocol === 'https:'
  } = options;

  let cookieString = `${name}=${value}; path=${path}; max-age=${maxAge}; SameSite=${sameSite}`;
  
  if (secure) {
    cookieString += '; Secure';
  }

  document.cookie = cookieString;
}

/**
 * Get a cookie value by name
 */
export function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;

  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  
  if (parts.length === 2) {
    return parts.pop()?.split(';').shift() || null;
  }
  
  return null;
}

/**
 * Delete a cookie by name
 */
export function deleteCookie(name: string, path: string = '/') {
  if (typeof document === 'undefined') return;

  document.cookie = `${name}=; path=${path}; expires=Thu, 01 Jan 1970 00:00:00 GMT`;
}

/**
 * Set authentication token in both localStorage and cookies
 */
export function setAuthToken(token: string) {
  // Store in localStorage for client-side access
  localStorage.setItem('token', token);
  
  // Store in cookies for server-side middleware access
  setCookie('token', token, {
    maxAge: 7 * 24 * 60 * 60, // 7 days
    path: '/',
    sameSite: 'Lax',
    secure: window.location.protocol === 'https:'
  });
}

/**
 * Clear authentication token from both localStorage and cookies
 */
export function clearAuthToken() {
  // Clear from localStorage
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
  
  // Clear from cookies
  deleteCookie('token');
}

/**
 * Get authentication token from localStorage or cookies
 */
export function getAuthToken(): string | null {
  // Try localStorage first (client-side)
  const localToken = localStorage.getItem('token');
  if (localToken) return localToken;
  
  // Fallback to cookies (server-side)
  return getCookie('token');
}

