import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;  // Subject (email) - this is what backend provides
  roles: string[];  // Array of role strings
  exp: number;  // Expiration timestamp
  iat: number;  // Issued at timestamp
}

// Define protected routes and required roles
// Special value 'AUTHENTICATED' means any authenticated user can access (no specific role required)
const protectedRoutes = {
  // Super Admin routes - only accessible by ROLE_APP_ADMIN
  '/admin': ['ROLE_APP_ADMIN'],
  '/admin/users': ['ROLE_APP_ADMIN'],
  '/admin/products': ['ROLE_APP_ADMIN'],
  '/admin/orders': ['ROLE_APP_ADMIN'],
  '/admin/settings': ['ROLE_APP_ADMIN'],
  '/admin/categories': ['ROLE_APP_ADMIN'],
  '/admin/analytics': ['ROLE_APP_ADMIN'],
  '/admin/roles': ['ROLE_APP_ADMIN'],
  
  // Seller routes - accessible by SELLER, ROLE_SELLER, COMPANY_ADMIN_SELLER, ROLE_COMPANY_ADMIN_SELLER, and ROLE_APP_ADMIN
  '/seller': ['SELLER', 'ROLE_SELLER', 'ROLE_COMPANY_ADMIN_SELLER', 'ROLE_APP_ADMIN'],
  '/seller/products': ['SELLER', 'ROLE_SELLER', 'ROLE_COMPANY_ADMIN_SELLER', 'ROLE_APP_ADMIN'],
  '/seller/orders': ['SELLER', 'ROLE_SELLER', 'ROLE_COMPANY_ADMIN_SELLER', 'ROLE_APP_ADMIN'],
  '/seller/analytics': ['SELLER', 'ROLE_SELLER', 'ROLE_COMPANY_ADMIN_SELLER', 'ROLE_APP_ADMIN'],
  '/seller/promotions': ['SELLER', 'ROLE_SELLER', 'ROLE_COMPANY_ADMIN_SELLER', 'ROLE_APP_ADMIN'],
  '/seller/company': ['ROLE_COMPANY_ADMIN_SELLER', 'ROLE_APP_ADMIN'],
  '/seller/roles': ['ROLE_COMPANY_ADMIN_SELLER', 'ROLE_APP_ADMIN'],
  
  // Account routes - accessible by any authenticated user (no specific role required)
  '/account': ['AUTHENTICATED'],
};

export function middleware(request: NextRequest) {
  // Try to get token from both cookies and Authorization header
  const tokenFromCookie = request.cookies.get('token')?.value;
  const authHeader = request.headers.get('authorization');
  const tokenFromHeader = authHeader?.startsWith('Bearer ') ? authHeader.substring(7) : null;
  const token = tokenFromCookie || tokenFromHeader;
  
  const { pathname } = request.nextUrl;
  
  // Check if the path is protected
  const isProtectedRoute = Object.keys(protectedRoutes).some(route => 
    pathname.startsWith(route)
  );
  
  if (!isProtectedRoute) {
    return NextResponse.next();
  }
  
  // If protected but no token, allow the request to proceed
  // Client-side components (like SellerLayout) will handle authentication
  // This prevents redirect loops when cookies aren't available but localStorage is
  if (!token) {
    console.log('🔐 [MIDDLEWARE] No token found in cookies for protected route:', pathname);
    console.log('🔐 [MIDDLEWARE] Allowing request to proceed - client-side will handle auth check');
    // Don't redirect here - let client-side components handle it
    // This prevents the issue where cookies aren't available but localStorage is
    return NextResponse.next();
  }
  
  try {
    // Verify token and check roles
    const decoded = jwtDecode<JwtPayload>(token);
    
    console.log('🔐 [MIDDLEWARE] Token decoded successfully:', {
      email: decoded.sub,
      roles: decoded.roles,
      exp: new Date(decoded.exp * 1000).toISOString()
    });
    
    // Check if token is expired
    if (decoded.exp * 1000 < Date.now()) {
      console.log('🔐 [MIDDLEWARE] Token expired for route:', pathname);
      const url = new URL('/auth/login', request.url);
      url.searchParams.set('callbackUrl', pathname);
      return NextResponse.redirect(url);
    }
    
    // Check if user has required roles for the route
    const userRoles = decoded.roles || [];
    
    // Find which protected route pattern matches the current path
    const matchingRoute = Object.keys(protectedRoutes).find(route => 
      pathname.startsWith(route)
    );
    
    if (matchingRoute) {
      const requiredRoles = protectedRoutes[matchingRoute as keyof typeof protectedRoutes];
      
      // Special case: 'AUTHENTICATED' means any authenticated user can access
      if (requiredRoles.includes('AUTHENTICATED')) {
        // User is authenticated (we have a valid token), so allow access
        console.log('🔐 [MIDDLEWARE] Account route - authenticated user allowed:', pathname);
        return NextResponse.next();
      }
      
      // For role-based routes, check if user has required role
      const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role));
      
      console.log('🔐 [MIDDLEWARE] Role check:', {
        route: matchingRoute,
        requiredRoles,
        userRoles,
        hasRequiredRole
      });
      
      if (!hasRequiredRole) {
        console.log('🔐 [MIDDLEWARE] Insufficient permissions for route:', pathname);
        // Redirect to unauthorized page
        return NextResponse.redirect(new URL('/unauthorized', request.url));
      }
    }
    
    console.log('🔐 [MIDDLEWARE] Access granted for route:', pathname);
    return NextResponse.next();
  } catch (error) {
    console.error('🔐 [MIDDLEWARE] Token validation error:', error);
    // Invalid token
    const url = new URL('/auth/login', request.url);
    url.searchParams.set('callbackUrl', pathname);
    return NextResponse.redirect(url);
  }
}

export const config = {
  matcher: [
    '/seller/:path*',
    '/admin/:path*',
    '/account/:path*',
  ],
};