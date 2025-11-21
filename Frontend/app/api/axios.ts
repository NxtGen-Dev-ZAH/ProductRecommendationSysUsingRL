import axios from 'axios';

// Environment-based API configuration
const getApiBaseURL = () => {
  // Since you're using production API during development, always use production
  return process.env.NEXT_PUBLIC_PROD_API_URL || 'https://api.shopora.fr/ecommerce';
};

console.log('API Configuration:', {
  nodeEnv: process.env.NODE_ENV,
  useProxy: process.env.NEXT_PUBLIC_USE_PROXY,
  apiUrl: process.env.NEXT_PUBLIC_API_URL,
  prodApiUrl: process.env.NEXT_PUBLIC_PROD_API_URL,
  resolvedBaseURL: getApiBaseURL()
});

const apiClient = axios.create({
  baseURL: getApiBaseURL(),
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Enable cookies for cart session management
  timeout: 30000, // 30 second timeout for production API
});

// Add request interceptor for auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      console.log('Sending auth token:', token.substring(0, 20) + '...', 'to:', config.url);
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Handle FormData: remove Content-Type header to let browser set it with boundary
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type'];
      // Axios will automatically set the correct Content-Type with boundary for FormData
    }
    
    // Enhanced cart-related request logging
    if (config.url?.includes('/cart')) {
      console.log('🛒 [CART REQUEST]', {
        method: config.method?.toUpperCase(),
        url: config.url,
        fullUrl: config.baseURL + config.url,
        hasAuth: !!token,
        withCredentials: config.withCredentials,
        headers: {
          'Content-Type': config.headers['Content-Type'],
          'Authorization': config.headers.Authorization ? 'Bearer [REDACTED]' : 'None',
          'Cookie': config.headers.Cookie || 'None'
        },
        data: config.data,
        params: config.params,
        timestamp: new Date().toISOString(),
        origin: typeof window !== 'undefined' ? window.location.origin : 'SSR',
        targetOrigin: 'https://api.shopora.fr'
      });
      
      // Log cookie state for cart requests
      if (typeof window !== 'undefined') {
        console.log('🍪 [CART COOKIE STATE]', {
          documentCookie: document.cookie,
          hasCartSessionCookie: document.cookie.includes('cart_session_id'),
          cookieLength: document.cookie.length,
          allCookies: document.cookie.split(';').map(c => c.trim()),
          timestamp: new Date().toISOString()
        });
      }
    }
    
    return config;
  },
  (error) => {
    console.error('❌ Request Interceptor Error:', error);
    return Promise.reject(error);
  }
);

// Add response interceptor for handling common errors
apiClient.interceptors.response.use(
  (response) => {
    // Enhanced cart-related response logging
    if (response.config.url?.includes('/cart')) {
      console.log('✅ [CART RESPONSE]', {
        status: response.status,
        statusText: response.statusText,
        url: response.config.url,
        fullUrl: response.config.baseURL + response.config.url,
        hasData: !!response.data,
        sessionId: response.data?.sessionId,
        headers: {
          'set-cookie': response.headers['set-cookie'],
          'content-type': response.headers['content-type'],
          'access-control-allow-credentials': response.headers['access-control-allow-credentials'],
          'access-control-allow-origin': response.headers['access-control-allow-origin']
        },
        cookies: response.headers['set-cookie'],
        timestamp: new Date().toISOString()
      });
      
      // Check if cookies were set
      if (response.headers['set-cookie']) {
        console.log('🍪 [CART COOKIES SET]', {
          cookies: response.headers['set-cookie'],
          hasCartSessionCookie: response.headers['set-cookie'].some((cookie: string) => 
            cookie.includes('cart_session_id')
          ),
          timestamp: new Date().toISOString()
        });
      }
    }
    return response;
  },
  (error) => {
    const { status } = error.response || {};
    const url = error.config?.url;
    
    // Enhanced error logging for cart-related errors
    if (url?.includes('/cart')) {
      console.error('❌ [CART ERROR]', {
        status,
        statusText: error.response?.statusText,
        url: error.config?.url,
        fullUrl: error.config?.baseURL + error.config?.url,
        method: error.config?.method?.toUpperCase(),
        message: error.response?.data?.message || error.message,
        responseData: error.response?.data,
        responseHeaders: error.response?.headers,
        requestHeaders: {
          'Content-Type': error.config?.headers['Content-Type'],
          'Authorization': error.config?.headers.Authorization ? 'Bearer [REDACTED]' : 'None',
          'Cookie': error.config?.headers.Cookie || 'None'
        },
        requestData: error.config?.data,
        requestParams: error.config?.params,
        isNetworkError: !error.response,
        isCORSError: error.code === 'ERR_NETWORK' || error.message?.includes('CORS'),
        isTimeoutError: error.code === 'ECONNABORTED',
        timestamp: new Date().toISOString(),
        origin: typeof window !== 'undefined' ? window.location.origin : 'SSR',
        targetOrigin: 'https://api.shopora.fr'
      });
      
      // Log cookie state for cart errors
      if (typeof window !== 'undefined') {
        console.log('🍪 [CART ERROR COOKIE STATE]', {
          documentCookie: document.cookie,
          hasCartSessionCookie: document.cookie.includes('cart_session_id'),
          cookieLength: document.cookie.length,
          allCookies: document.cookie.split(';').map(c => c.trim()),
          timestamp: new Date().toISOString()
        });
      }
    }
    
    // Don't log expected cart errors (400/401/404 for missing cart session)
    const isExpectedCartError = (status === 400 || status === 401 || status === 404) && url?.includes('/cart') && !url?.includes('/add');
    
    if (!isExpectedCartError) {
      // Enhanced error logging
      console.error('❌ [API ERROR]', {
        status, 
        url: error.config?.url,
        method: error.config?.method,
        data: error.config?.data,
        message: error.response?.data?.message || error.message,
        responseData: error.response?.data,
        isNetworkError: !error.response,
        isCORSError: error.code === 'ERR_NETWORK' || error.message?.includes('CORS')
      });
    }
    
    // Handle different types of errors
    if (error.code === 'ERR_NETWORK' || error.message?.includes('CORS')) {
      console.warn('🌐 Network/CORS error detected. Check your proxy configuration or backend CORS settings.');
      
      // Don't redirect to login for network errors
      return Promise.reject({
        ...error,
        isNetworkError: true,
        message: 'Unable to connect to the server. Please check your internet connection or try again later.'
      });
    }
    
    // Handle timeout errors specifically
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      console.error('⏰ API request timed out. This might be due to:', {
        url: error.config?.url,
        method: error.config?.method,
        baseURL: error.config?.baseURL,
        timeout: error.config?.timeout
      });
      
      return Promise.reject({
        ...error,
        isTimeoutError: true,
        message: 'Request timed out. The server is taking too long to respond. Please try again later.'
      });
    }
    
    // Handle authentication errors - only for authenticated users
    if (status === 401) {
      const token = localStorage.getItem('token');
      if (token) {
        // Only redirect authenticated users who get 401
        console.warn('🔐 Authentication failed for authenticated user. Redirecting to login.');
        if (typeof window !== 'undefined') {
          localStorage.removeItem('token');
          localStorage.removeItem('user');
          localStorage.removeItem('refreshToken');
          
          // Only redirect if not already on auth pages
          if (!window.location.pathname.startsWith('/auth/')) {
            window.location.href = '/auth/login';
          }
        }
      } else {
        // Anonymous user got 401 - this is expected for cart operations
        console.log('🔐 Anonymous user got 401 - this is expected for cart operations');
      }
    }
    
    // Handle server errors
    if (status >= 500) {
      console.error('🔥 Server error detected:', status);
    }
    
    return Promise.reject(error);
  }
);

export default apiClient;   