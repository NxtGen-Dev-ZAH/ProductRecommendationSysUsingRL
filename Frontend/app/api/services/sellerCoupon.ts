import apiClient from '../axios';
import { 
  SellerCouponRequest, 
  SellerCouponResponse, 
  SellerCouponListResponse 
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

// Enhanced logging function for seller coupon operations
const logSellerCouponOperation = (operation: string, details: Record<string, unknown>) => {
  console.log(`🎫 [SELLER COUPON ${operation.toUpperCase()}]`, {
    timestamp: new Date().toISOString(),
    operation,
    ...details
  });
};

// Create a new coupon for the seller
export const createSellerCoupon = async (couponData: SellerCouponRequest): Promise<SellerCouponResponse> => {
  logSellerCouponOperation('CREATE_COUPON_START', { 
    code: couponData.code,
    discountType: couponData.discountType,
    discountValue: couponData.discountValue
  });
  
  try {
    const response = await apiClient.post<SellerCouponResponse>('/seller/v1/coupons', couponData);
    
    logSellerCouponOperation('CREATE_COUPON_SUCCESS', { 
      status: response.status,
      couponId: response.data?.id,
      code: response.data?.code
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logSellerCouponOperation('CREATE_COUPON_ERROR', {
      status: apiError.response?.status,
      statusText: apiError.response?.statusText,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error creating seller coupon:', apiError);
    throw apiError;
  }
};

// Update an existing coupon
export const updateSellerCoupon = async (couponId: number, couponData: SellerCouponRequest): Promise<SellerCouponResponse> => {
  logSellerCouponOperation('UPDATE_COUPON_START', { 
    couponId,
    code: couponData.code,
    discountType: couponData.discountType,
    discountValue: couponData.discountValue
  });
  
  try {
    const response = await apiClient.put<SellerCouponResponse>(`/seller/v1/coupons/${couponId}`, couponData);
    
    logSellerCouponOperation('UPDATE_COUPON_SUCCESS', { 
      status: response.status,
      couponId: response.data?.id,
      code: response.data?.code
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logSellerCouponOperation('UPDATE_COUPON_ERROR', {
      status: apiError.response?.status,
      statusText: apiError.response?.statusText,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error updating seller coupon:', apiError);
    throw apiError;
  }
};

// Delete a coupon (soft delete - sets state to DELETED)
export const deleteSellerCoupon = async (couponId: number): Promise<void> => {
  logSellerCouponOperation('DELETE_COUPON_START', { couponId });
  
  try {
    await apiClient.delete(`/seller/v1/coupons/${couponId}`);
    
    logSellerCouponOperation('DELETE_COUPON_SUCCESS', { 
      couponId
    });
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logSellerCouponOperation('DELETE_COUPON_ERROR', {
      status: apiError.response?.status,
      statusText: apiError.response?.statusText,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error deleting seller coupon:', apiError);
    throw apiError;
  }
};

// Get all coupons for the seller
export const getSellerCoupons = async (page: number = 0, size: number = 10): Promise<SellerCouponListResponse> => {
  logSellerCouponOperation('GET_SELLER_COUPONS_START', { page, size });
  
  try {
    const response = await apiClient.get<SellerCouponListResponse>('/seller/v1/coupons', {
      params: { page, size }
    });
    
    logSellerCouponOperation('GET_SELLER_COUPONS_SUCCESS', { 
      status: response.status,
      totalElements: response.data?.totalElements,
      currentPage: response.data?.currentPage,
      couponCount: response.data?.coupons?.length || 0
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logSellerCouponOperation('GET_SELLER_COUPONS_ERROR', {
      status: apiError.response?.status,
      statusText: apiError.response?.statusText,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error fetching seller coupons:', apiError);
    throw apiError;
  }
};

// Get a specific coupon by ID
export const getSellerCouponById = async (couponId: number): Promise<SellerCouponResponse> => {
  logSellerCouponOperation('GET_COUPON_BY_ID_START', { couponId });
  
  try {
    const response = await apiClient.get<SellerCouponResponse>(`/seller/v1/coupons/${couponId}`);
    
    logSellerCouponOperation('GET_COUPON_BY_ID_SUCCESS', { 
      status: response.status,
      couponId: response.data?.id,
      code: response.data?.code
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logSellerCouponOperation('GET_COUPON_BY_ID_ERROR', {
      status: apiError.response?.status,
      statusText: apiError.response?.statusText,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error fetching seller coupon by ID:', apiError);
    throw apiError;
  }
};

// Toggle coupon active status
export const toggleCouponStatus = async (couponId: number, active: boolean): Promise<SellerCouponResponse> => {
  logSellerCouponOperation('TOGGLE_COUPON_STATUS_START', { couponId, active });
  
  try {
    const response = await apiClient.patch<SellerCouponResponse>(`/seller/v1/coupons/${couponId}/status`, {
      active
    });
    
    logSellerCouponOperation('TOGGLE_COUPON_STATUS_SUCCESS', { 
      status: response.status,
      couponId: response.data?.id,
      active: response.data?.active
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logSellerCouponOperation('TOGGLE_COUPON_STATUS_ERROR', {
      status: apiError.response?.status,
      statusText: apiError.response?.statusText,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error toggling coupon status:', apiError);
    throw apiError;
  }
};

// Get coupon usage statistics
export const getCouponUsageStats = async (couponId: number): Promise<{
  couponId: number;
  totalUses: number;
  remainingUses?: number;
  usagePercentage: number;
  lastUsed?: string;
}> => {
  logSellerCouponOperation('GET_COUPON_USAGE_STATS_START', { couponId });
  
  try {
    const response = await apiClient.get(`/seller/v1/coupons/${couponId}/usage`);
    
    logSellerCouponOperation('GET_COUPON_USAGE_STATS_SUCCESS', { 
      status: response.status,
      couponId: response.data?.couponId,
      totalUses: response.data?.totalUses
    });
    
    return response.data;
  } catch (error: unknown) {
    const apiError = error as ApiError;
    logSellerCouponOperation('GET_COUPON_USAGE_STATS_ERROR', {
      status: apiError.response?.status,
      statusText: apiError.response?.statusText,
      message: apiError.response?.data?.message || apiError.message,
      url: apiError.config?.url,
      method: apiError.config?.method
    });
    
    console.error('Error fetching coupon usage stats:', apiError);
    throw apiError;
  }
};

// Validate coupon data before submission
export const validateCouponData = (couponData: SellerCouponRequest): { isValid: boolean; errors: string[] } => {
  const errors: string[] = [];
  
  // Required field validation
  if (!couponData.code || couponData.code.trim().length === 0) {
    errors.push('Coupon code is required');
  } else if (couponData.code.length < 3) {
    errors.push('Coupon code must be at least 3 characters long');
  }
  
  if (!couponData.description || couponData.description.trim().length === 0) {
    errors.push('Description is required');
  }
  
  if (!couponData.discountType) {
    errors.push('Discount type is required');
  }
  
  if (!couponData.discountValue || couponData.discountValue <= 0) {
    errors.push('Discount value must be greater than 0');
  }
  
  // Date validation
  const startDate = new Date(couponData.startDate);
  const endDate = new Date(couponData.endDate);
  const now = new Date();
  
  if (startDate < now) {
    errors.push('Start date cannot be in the past');
  }
  
  if (endDate <= startDate) {
    errors.push('End date must be after start date');
  }
  
  // Discount value validation
  if (couponData.discountType === 'PERCENTAGE' && couponData.discountValue > 100) {
    errors.push('Percentage discount cannot exceed 100%');
  }
  
  // Usage limit validation
  if (couponData.usageLimit && couponData.usageLimit <= 0) {
    errors.push('Usage limit must be greater than 0');
  }
  
  // Minimum order amount validation
  if (couponData.minimumOrderAmount && couponData.minimumOrderAmount < 0) {
    errors.push('Minimum order amount cannot be negative');
  }
  
  // Max discount amount validation
  if (couponData.maxDiscountAmount && couponData.maxDiscountAmount < 0) {
    errors.push('Maximum discount amount cannot be negative');
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
};

// Format coupon for display
export const formatCouponDisplay = (coupon: SellerCouponResponse): string => {
  if (coupon.discountType === 'PERCENTAGE') {
    return `${coupon.discountValue}% OFF`;
  } else {
    return `$${coupon.discountValue} OFF`;
  }
};

// Check if coupon is active and valid
export const isCouponValid = (coupon: SellerCouponResponse): boolean => {
  const now = new Date();
  const startDate = new Date(coupon.startDate);
  const endDate = new Date(coupon.endDate);
  
  return (
    coupon.active &&
    coupon.state === 'ACTIVE' &&
    startDate <= now &&
    endDate >= now &&
    (!coupon.usageLimit || (coupon.usedCount || 0) < coupon.usageLimit)
  );
};

// Get coupon status text
export const getCouponStatusText = (coupon: SellerCouponResponse): string => {
  if (!coupon.active) return 'Inactive';
  if (coupon.state === 'DELETED') return 'Deleted';
  
  const now = new Date();
  const startDate = new Date(coupon.startDate);
  const endDate = new Date(coupon.endDate);
  
  if (startDate > now) return 'Scheduled';
  if (endDate < now) return 'Expired';
  if (coupon.usageLimit && (coupon.usedCount || 0) >= coupon.usageLimit) return 'Usage Limit Reached';
  
  return 'Active';
};

// Get coupon status color for UI
export const getCouponStatusColor = (coupon: SellerCouponResponse): string => {
  const status = getCouponStatusText(coupon);
  
  switch (status) {
    case 'Active':
      return 'text-green-600 bg-green-100';
    case 'Scheduled':
      return 'text-blue-600 bg-blue-100';
    case 'Expired':
    case 'Usage Limit Reached':
      return 'text-red-600 bg-red-100';
    case 'Inactive':
      return 'text-gray-600 bg-gray-100';
    case 'Deleted':
      return 'text-red-800 bg-red-200';
    default:
      return 'text-gray-600 bg-gray-100';
  }
};

const sellerCouponService = {
  createSellerCoupon,
  updateSellerCoupon,
  deleteSellerCoupon,
  getSellerCoupons,
  getSellerCouponById,
  toggleCouponStatus,
  getCouponUsageStats,
  validateCouponData,
  formatCouponDisplay,
  isCouponValid,
  getCouponStatusText,
  getCouponStatusColor
};

export default sellerCouponService;
