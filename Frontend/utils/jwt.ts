// JWT utility functions for decoding tokens and extracting user information

export interface JWTPayload {
  sub: string; // subject (user email) - this is what backend provides
  iat: number; // issued at
  exp: number; // expiration
  roles: string[]; // array of role strings
  [key: string]: any;
}

/**
 * Decode a JWT token without verification (client-side only)
 * @param token The JWT token to decode
 * @returns The decoded payload or null if invalid
 */
export function decodeJWT(token: string): JWTPayload | null {
  try {
    // JWT tokens have 3 parts separated by dots
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    // Decode the payload (second part)
    const payload = parts[1];
    // Add padding if needed for base64 decoding
    const paddedPayload = payload + '='.repeat((4 - payload.length % 4) % 4);
    const decodedPayload = atob(paddedPayload.replace(/-/g, '+').replace(/_/g, '/'));
    
    return JSON.parse(decodedPayload);
  } catch (error) {
    console.error('Error decoding JWT token:', error);
    return null;
  }
}

/**
 * Extract user email from JWT token
 * @param token The JWT token
 * @returns The user email or null if not found
 */
export function getEmailFromToken(token: string): string | null {
  const payload = decodeJWT(token);
  return payload?.sub || null;
}

/**
 * Extract user roles from JWT token
 * @param token The JWT token
 * @returns Array of user roles or empty array if not found
 */
export function getRolesFromToken(token: string): string[] {
  const payload = decodeJWT(token);
  return payload?.roles || [];
}

/**
 * Check if JWT token is expired
 * @param token The JWT token
 * @returns True if expired, false otherwise
 */
export function isTokenExpired(token: string): boolean {
  const payload = decodeJWT(token);
  if (!payload?.exp) {
    return true;
  }
  
  // exp is in seconds, Date.now() is in milliseconds
  return Date.now() >= payload.exp * 1000;
}

/**
 * Get token expiration time
 * @param token The JWT token
 * @returns Date object of expiration or null if invalid
 */
export function getTokenExpiration(token: string): Date | null {
  const payload = decodeJWT(token);
  if (!payload?.exp) {
    return null;
  }
  
  return new Date(payload.exp * 1000);
}
