import apiClient from '../axios';
import { 
  Cart, 
  AppliedCouponResponse, 
  CouponRequest, 
  AddToCartRequest, 
  MergeCartRequest, 
  MergeCartResponse, 
} from '../../../types/api';

// Error type for API responses
interface ApiError {
  response?: {
    status?: number;
    statusText?: string;
    data?: {
      message?: string;
    };
    headers?: Record<string, string>;
  };
  config?: {
    url?: string;
    method?: string;
    headers?: Record<string, string>;
    withCredentials?: boolean;
    data?: string;
  };
  message?: string;
}

// Helper function to check if user is authenticated
const isAuthenticated = (): boolean => {
  if (typeof window === 'undefined') return false;
  return !!localStorage.getItem('token');
};

// Helper function to get appropriate endpoint based on authentication
const getCartEndpoint = (): string => {
  return isAuthenticated() ? '/buyer/v1/cart' : '/api/cart';
};

// Enhanced logging function for cart operations
const logCartOperation = (operation: string, details: Record<string, unknown>) => {
  console.log(`🛒 [CART ${operation.toUpperCase()}]`, {
    timestamp: new Date().toISOString(),
    operation,
    ...details,
    // Log cookie state if available
    cookieState: typeof window !== 'undefined' ? {
      hasCookies: document.cookie.length > 0,
      cookieCount: document.cookie.split(';').length,
      hasCartSessionCookie: document.cookie.includes('cart_session_id'),
      allCookies: document.cookie.split(';').map(c => c.trim().split('=')[0])
    } : 'SSR - No cookies available'
  });
};

export const getCart = async (page: number = 0, size: number = 10): Promise<Cart> => {
  logCartOperation('GET_CART_START', { page, size, isAuthenticated: isAuthenticated() });
  
  try {
    const endpoint = getCartEndpoint();
    logCartOperation('GET_CART_ENDPOINT', { endpoint });
    
    const response = await apiClient.get<Cart>(endpoint, {
      params: { page, size }
    });
    
    logCartOperation('GET_CART_SUCCESS', { 
      status: response.status,
      hasData: !!response.data,
      sessionId: response.data?.sessionId,
      itemCount: response.data?.items?.length || 0
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('GET_CART_ERROR', {
      status: apiError.response?.status,
      statusText: apiError.response?.statusText,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method,
      headers: apiError.config?.headers,
      // Log response headers for debugging
      responseHeaders: apiError.response?.headers,
      setCookieHeader: apiError.response?.headers?.['set-cookie'],
      // Log request details
      requestHeaders: apiError.config?.headers,
      withCredentials: apiError.config?.withCredentials
    });
    
    // Handle different error scenarios based on authentication status
    if (isAuthenticated()) {
      // For authenticated users, 404 means they don't have a cart yet
      if (apiError.response?.status === 404) {
        logCartOperation('GET_CART_404_AUTHENTICATED', { 
          message: 'Authenticated user has no cart - returning empty cart',
          expectedBehavior: 'Normal for new authenticated users',
          userId: 'User needs to add first item to create cart'
        });
        return {
          id: 0,
          sessionId: undefined,
          user: undefined,
          items: [],
          couponResponse: undefined,
          subtotalPrice: 0,
          totalShippingCost: 0,
          totalDiscount: 0,
          totalAmount: 0
        };
      }
      
      // For authenticated users, 401 means token is invalid
      if (apiError.response?.status === 401) {
        logCartOperation('GET_CART_401_AUTHENTICATED', { 
          message: 'Authentication failed - token may be invalid',
          expectedBehavior: 'Should redirect to login'
        });
        throw apiError; // Let the error propagate for auth handling
      }
    } else {
      // For anonymous users, 400 means no cart session exists yet
      if (apiError.response?.status === 400) {
        logCartOperation('GET_CART_400_ANONYMOUS', { 
          message: 'No cart session exists - returning empty cart',
          expectedBehavior: 'Normal for new anonymous users'
        });
        return {
          id: 0,
          sessionId: undefined,
          user: undefined,
          items: [],
          couponResponse: undefined,
          subtotalPrice: 0,
          totalShippingCost: 0,
          totalDiscount: 0,
          totalAmount: 0
        };
      }
      
      // For anonymous users, 404 means cart not found
      if (apiError.response?.status === 404) {
        logCartOperation('GET_CART_404_ANONYMOUS', { 
          message: 'Cart not found - returning empty cart',
          expectedBehavior: 'User has no cart yet'
        });
        return {
          id: 0,
          sessionId: undefined,
          user: undefined,
          items: [],
          couponResponse: undefined,
          subtotalPrice: 0,
          totalShippingCost: 0,
          totalDiscount: 0,
          totalAmount: 0
        };
      }
    }
    
    // For any other errors, throw them
    throw apiError;
  }
};

// Get cart subtotal (for anonymous users)
export const getCartSubtotal = async (): Promise<number> => {
  logCartOperation('GET_CART_SUBTOTAL_START', { isAuthenticated: isAuthenticated() });
  
  try {
    const endpoint = getCartEndpoint();
    const response = await apiClient.get<number>(`${endpoint}/subtotal`);
    
    logCartOperation('GET_CART_SUBTOTAL_SUCCESS', { 
      status: response.status,
      subtotal: response.data
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('GET_CART_SUBTOTAL_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message
    });
    
    if (apiError.response?.status === 400 || apiError.response?.status === 404) {
      return 0;
    }
    throw apiError;
  }
};

// Get cart shipping cost (for anonymous users)
export const getCartShipping = async (): Promise<number> => {
  logCartOperation('GET_CART_SHIPPING_START', { isAuthenticated: isAuthenticated() });
  
  try {
    const endpoint = getCartEndpoint();
    const response = await apiClient.get<number>(`${endpoint}/shipping`);
    
    logCartOperation('GET_CART_SHIPPING_SUCCESS', { 
      status: response.status,
      shipping: response.data
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('GET_CART_SHIPPING_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message
    });
    
    if (apiError.response?.status === 400 || apiError.response?.status === 404) {
      return 0;
    }
    throw apiError;
  }
};

// Get cart discount (for anonymous users)
export const getCartDiscount = async (): Promise<number> => {
  logCartOperation('GET_CART_DISCOUNT_START', { isAuthenticated: isAuthenticated() });
  
  try {
    const endpoint = getCartEndpoint();
    const response = await apiClient.get<number>(`${endpoint}/discount`);
    
    logCartOperation('GET_CART_DISCOUNT_SUCCESS', { 
      status: response.status,
      discount: response.data
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('GET_CART_DISCOUNT_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message
    });
    
    if (apiError.response?.status === 400 || apiError.response?.status === 404) {
      return 0;
    }
    throw apiError;
  }
};

// Get cart total (for anonymous users)
export const getCartTotal = async (): Promise<number> => {
  logCartOperation('GET_CART_TOTAL_START', { isAuthenticated: isAuthenticated() });
  
  try {
    const endpoint = getCartEndpoint();
    const response = await apiClient.get<number>(`${endpoint}/total`);
    
    logCartOperation('GET_CART_TOTAL_SUCCESS', { 
      status: response.status,
      total: response.data
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('GET_CART_TOTAL_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message
    });
    
    if (apiError.response?.status === 400 || apiError.response?.status === 404) {
      return 0;
    }
    throw apiError;
  }
};

// Get cart subtotal (for authenticated users)
export const getBuyerCartSubtotal = async (): Promise<number> => {
  logCartOperation('GET_BUYER_CART_SUBTOTAL_START', { isAuthenticated: isAuthenticated() });
  
  try {
    const response = await apiClient.get<number>('/buyer/v1/cart/subtotal');
    
    logCartOperation('GET_BUYER_CART_SUBTOTAL_SUCCESS', { 
      status: response.status,
      subtotal: response.data
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('GET_BUYER_CART_SUBTOTAL_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message
    });
    
    if (apiError.response?.status === 401 || apiError.response?.status === 404) {
      return 0;
    }
    throw apiError;
  }
};

// Get cart shipping cost (for authenticated users)
export const getBuyerCartShipping = async (): Promise<number> => {
  logCartOperation('GET_BUYER_CART_SHIPPING_START', { isAuthenticated: isAuthenticated() });
  
  try {
    const response = await apiClient.get<number>('/buyer/v1/cart/shipping');
    
    logCartOperation('GET_BUYER_CART_SHIPPING_SUCCESS', { 
      status: response.status,
      shipping: response.data
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('GET_BUYER_CART_SHIPPING_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message
    });
    
    if (apiError.response?.status === 401 || apiError.response?.status === 404) {
      return 0;
    }
    throw apiError;
  }
};

// Get cart discount (for authenticated users)
export const getBuyerCartDiscount = async (): Promise<number> => {
  logCartOperation('GET_BUYER_CART_DISCOUNT_START', { isAuthenticated: isAuthenticated() });
  
  try {
    const response = await apiClient.get<number>('/buyer/v1/cart/discount');
    
    logCartOperation('GET_BUYER_CART_DISCOUNT_SUCCESS', { 
      status: response.status,
      discount: response.data
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('GET_BUYER_CART_DISCOUNT_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message
    });
    
    if (apiError.response?.status === 401 || apiError.response?.status === 404) {
      return 0;
    }
    throw apiError;
  }
};

// Get cart total (for authenticated users) - matches backend /buyer/v1/cart/total endpoint
export const getBuyerCartTotal = async (): Promise<number> => {
  logCartOperation('GET_BUYER_CART_TOTAL_START', { isAuthenticated: isAuthenticated() });
  
  try {
    const response = await apiClient.get<number>('/buyer/v1/cart/total');
    
    logCartOperation('GET_BUYER_CART_TOTAL_SUCCESS', { 
      status: response.status,
      total: response.data
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('GET_BUYER_CART_TOTAL_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message
    });
    
    // If we get a 401 error, user is not authenticated
    // If we get a 404 error, user or cart not found
    if (apiError.response?.status === 401 || apiError.response?.status === 404) {
      return 0;
    }
    throw apiError;
  }
};

export const addToCart = async (productId: number, quantity: number): Promise<Cart> => {
  const request: AddToCartRequest = { productId, quantity };
  
  logCartOperation('ADD_TO_CART_START', { productId, quantity, isAuthenticated: isAuthenticated() });
  
  try {
    const endpoint = getCartEndpoint();
    logCartOperation('ADD_TO_CART_ENDPOINT', { endpoint });
    
    const response = await apiClient.post<Cart>(`${endpoint}/add`, request);
    
    logCartOperation('ADD_TO_CART_SUCCESS', { 
      status: response.status,
      hasData: !!response.data,
      sessionId: response.data?.sessionId,
      itemCount: response.data?.items?.length || 0,
      // Log response headers for cookie debugging
      responseHeaders: response.headers,
      setCookieHeader: response.headers?.['set-cookie']
    });
    
    // Backend automatically handles cookie creation and session management
    // No need to manually store session ID
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('ADD_TO_CART_ERROR', {
      status: apiError.response?.status,
      statusText: apiError.response?.statusText,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method,
      requestData: apiError.config?.data,
      // Log response headers for debugging
      responseHeaders: apiError.response?.headers,
      setCookieHeader: apiError.response?.headers?.['set-cookie'],
      // Log request details
      requestHeaders: apiError.config?.headers,
      withCredentials: apiError.config?.withCredentials
    });
    
    console.error('Error adding to cart:', apiError);
    throw apiError;
  }
};

export const updateCartItem = async (itemId: number, quantity: number): Promise<Cart> => {
  logCartOperation('UPDATE_CART_ITEM_START', { itemId, quantity });
  
  try {
    const endpoint = getCartEndpoint();
    logCartOperation('UPDATE_CART_ITEM_ENDPOINT', { endpoint });
    
    // Both endpoints expect quantity as query parameter
    const response = await apiClient.put<Cart>(`${endpoint}/update/${itemId}`, null, {
      params: { quantity }
    });
    
    logCartOperation('UPDATE_CART_ITEM_SUCCESS', { 
      status: response.status,
      hasData: !!response.data,
      itemCount: response.data?.items?.length || 0
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('UPDATE_CART_ITEM_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error updating cart item:', apiError);
    throw apiError;
  }
};

export const removeFromCart = async (itemId: number): Promise<Cart> => {
  logCartOperation('REMOVE_FROM_CART_START', { itemId });
  
  try {
    const endpoint = getCartEndpoint();
    logCartOperation('REMOVE_FROM_CART_ENDPOINT', { endpoint });
    
    const response = await apiClient.delete<Cart>(`${endpoint}/remove/${itemId}`);
    
    logCartOperation('REMOVE_FROM_CART_SUCCESS', { 
      status: response.status,
      hasData: !!response.data,
      itemCount: response.data?.items?.length || 0
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('REMOVE_FROM_CART_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error removing from cart:', apiError);
    throw apiError;
  }
};

export const clearCart = async (): Promise<void> => {
  logCartOperation('CLEAR_CART_START', {});
  
  try {
    const endpoint = getCartEndpoint();
    logCartOperation('CLEAR_CART_ENDPOINT', { endpoint });
    
    await apiClient.delete(`${endpoint}/clear`);
    
    logCartOperation('CLEAR_CART_SUCCESS', {});
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('CLEAR_CART_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error clearing cart:', apiError);
    throw apiError;
  }
};

// Apply coupon to cart - Updated to handle backend response structure
export const applyCoupon = async (couponCode: string): Promise<AppliedCouponResponse> => {
  logCartOperation('APPLY_COUPON_START', { couponCode, isAuthenticated: isAuthenticated() });
  
  try {
    const endpoint = getCartEndpoint();
    logCartOperation('APPLY_COUPON_ENDPOINT', { endpoint });
    
    if (isAuthenticated()) {
      // Buyer endpoint uses query parameter
      const response = await apiClient.post<AppliedCouponResponse>(`${endpoint}/apply-coupon`, null, {
        params: { couponIdentifier: couponCode }
      });
      
      logCartOperation('APPLY_COUPON_SUCCESS', { 
        status: response.status,
        discount: response.data?.discount
      });
      
      return response.data;
    } else {
      // Anonymous endpoint uses request body
      const request: CouponRequest = { code: couponCode };
      const response = await apiClient.post<AppliedCouponResponse>(`${endpoint}/coupon/apply`, request);
      
      logCartOperation('APPLY_COUPON_SUCCESS', { 
        status: response.status,
        discount: response.data?.discount
      });
      
      return response.data;
    }
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('APPLY_COUPON_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error applying coupon:', apiError);
    
    // Enhanced error handling for coupon-specific errors
    if (apiError.response?.status === 422) {
      throw new Error(apiError.response.data?.message || 'Coupon is invalid, expired, or usage limit exceeded');
    } else if (apiError.response?.status === 404) {
      throw new Error('Coupon not found');
    } else if (apiError.response?.data?.message) {
      throw new Error(apiError.response.data.message);
    }
    throw new Error('Failed to apply coupon');
  }
};

// Remove coupon from cart
export const removeCoupon = async (): Promise<Cart> => {
  logCartOperation('REMOVE_COUPON_START', { isAuthenticated: isAuthenticated() });
  
  try {
    const endpoint = getCartEndpoint();
    logCartOperation('REMOVE_COUPON_ENDPOINT', { endpoint });
    
    if (isAuthenticated()) {
      // Buyer endpoint
      const response = await apiClient.delete<Cart>(`${endpoint}/remove-coupon`);
      
      logCartOperation('REMOVE_COUPON_SUCCESS', { 
        status: response.status,
        hasData: !!response.data
      });
      
      return response.data;
    } else {
      // Anonymous endpoint
      const response = await apiClient.delete<Cart>(`${endpoint}/coupon/remove`);
      
      logCartOperation('REMOVE_COUPON_SUCCESS', { 
        status: response.status,
        hasData: !!response.data
      });
      
      return response.data;
    }
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('REMOVE_COUPON_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error removing coupon:', apiError);
    throw apiError;
  }
};

// Merge anonymous cart with user cart on login
export const mergeCartOnLogin = async (data: MergeCartRequest): Promise<MergeCartResponse> => {
  logCartOperation('MERGE_CART_START', { email: data.emailAddress });
  
  try {
    const response = await apiClient.post<MergeCartResponse>('/api/cart/merge', data);
    
    logCartOperation('MERGE_CART_SUCCESS', { 
      status: response.status,
      hasData: !!response.data,
      hasAuthResponse: !!response.data?.authResponse,
      hasCartResponse: !!response.data?.cartResponse
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logCartOperation('MERGE_CART_ERROR', {
      status: apiError.response?.status,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error merging cart on login:', apiError);
    throw apiError;
  }
};

// Utility function to get cart item count
export const getCartItemCount = async (): Promise<number> => {
  try {
    const cart = await getCart();
    return cart.items.reduce((total, item) => total + item.quantity, 0);
  } catch {
    return 0;
  }
};

// Utility function to check if product is in cart
export const isProductInCart = async (productId: number): Promise<boolean> => {
  try {
    const cart = await getCart();
    return cart.items.some(item => item.productId === productId);
  } catch {
    return false;
  }
};

// Utility function to get product quantity in cart
export const getProductQuantityInCart = async (productId: number): Promise<number> => {
  try {
    const cart = await getCart();
    const item = cart.items.find(item => item.productId === productId);
    return item ? item.quantity : 0;
  } catch {
    return 0;
  }
}; 