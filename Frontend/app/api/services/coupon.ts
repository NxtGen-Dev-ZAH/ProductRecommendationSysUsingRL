import apiClient from '../axios';
import { Coupon } from '../../../types/api';

export interface CouponValidationRequest {
  couponCode: string;
  totalAmount?: number;
  userId?: number;
  productIds?: number[];
}

export interface CouponValidationResponse {
  isValid: boolean;
  coupon?: Coupon;
  discountAmount?: number;
  errorMessage?: string;
  canApply?: boolean;
}

export interface CouponUsageResponse {
  couponId: number;
  usageCount: number;
  remainingUses?: number;
  isExpired: boolean;
}

// Validate a coupon code
export const validateCoupon = async (request: CouponValidationRequest): Promise<CouponValidationResponse> => {
  try {
    const response = await apiClient.post<CouponValidationResponse>('/api/coupon/validate', request);
    return response.data;
  } catch (error) {
    console.error('Error validating coupon:', error);
    return {
      isValid: false,
      errorMessage: 'Failed to validate coupon'
    };
    console.error('Error validating coupon:', error);
  }
};

// Apply coupon to cart (this is handled in cart service but included here for completeness)
export const applyCouponToCart = async (couponCode: string): Promise<{ success: boolean; message?: string }> => {
  try {
    const response = await apiClient.post('/api/cart/coupon/apply', { couponCode });
    return response.data;
  } catch (error) {
    console.error('Error applying coupon to cart:', error);
    throw error;
  }
};

// Remove coupon from cart (this is handled in cart service but included here for completeness)
export const removeCouponFromCart = async (): Promise<{ success: boolean; message?: string }> => {
  try {
    const response = await apiClient.delete('/api/cart/coupon/remove');
    return response.data;
  } catch (error) {
    console.error('Error removing coupon from cart:', error);
    throw error;
  }
};

// Get coupon usage statistics (admin function)
export const getCouponUsage = async (couponId: number): Promise<CouponUsageResponse> => {
  try {
    const response = await apiClient.get<CouponUsageResponse>(`/admin/coupon/${couponId}/usage`);
    return response.data;
  } catch (error) {
    console.error('Error fetching coupon usage:', error);
    throw error;
  }
};

// Check if coupon is still valid and can be used
export const checkCouponAvailability = async (couponCode: string): Promise<boolean> => {
  try {
    const validation = await validateCoupon({ couponCode });
    return validation.isValid && validation.canApply === true;
  } catch (error) {
    console.error('Error checking coupon availability:', error);
    return false;
  }
};

// Get coupon details by code
export const getCouponByCode = async (couponCode: string): Promise<Coupon | null> => {
  try {
    const validation = await validateCoupon({ couponCode });
    return validation.coupon || null;
  } catch (error) {
    console.error('Error fetching coupon details:', error);
    return null;
  }
};

// Calculate discount amount for a given total
export const calculateDiscount = async (couponCode: string, totalAmount: number): Promise<number> => {
  try {
    const validation = await validateCoupon({ couponCode, totalAmount });
    return validation.discountAmount || 0;
  } catch (error) {
    console.error('Error calculating discount:', error);
    return 0;
  }
};

// Format coupon for display
export const formatCouponDisplay = (coupon: Coupon): string => {
  if (coupon.discountType === 'PERCENTAGE') {
    return `${coupon.discountValue}% OFF`;
  } else {
    return `$${coupon.discountValue} OFF`;
  }
};

// Check if coupon has usage restrictions
export const hasCouponRestrictions = (coupon: Coupon): boolean => {
  return !!(
    coupon.minimumOrderAmount ||
    coupon.maxDiscountAmount ||
    coupon.applicableCategories?.length ||
    coupon.applicableProducts?.length ||
    coupon.usageLimit
  );
};

// Get coupon restrictions as human-readable text
export const getCouponRestrictions = (coupon: Coupon): string[] => {
  const restrictions: string[] = [];

  if (coupon.minimumOrderAmount) {
    restrictions.push(`Minimum order: $${coupon.minimumOrderAmount}`);
  }

  if (coupon.maxDiscountAmount && coupon.discountType === 'PERCENTAGE') {
    restrictions.push(`Maximum discount: $${coupon.maxDiscountAmount}`);
  }

  if (coupon.usageLimit) {
    const remaining = coupon.usageLimit - (coupon.usedCount || 0);
    restrictions.push(`${remaining} uses remaining`);
  }

  if (coupon.applicableCategories?.length) {
    restrictions.push('Valid for specific categories only');
  }

  if (coupon.applicableProducts?.length) {
    restrictions.push('Valid for specific products only');
  }

  const now = new Date();
  const endDate = new Date(coupon.endDate);
  if (endDate > now) {
    const daysRemaining = Math.ceil((endDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
    restrictions.push(`Expires in ${daysRemaining} day${daysRemaining === 1 ? '' : 's'}`);
  }

  return restrictions;
};

const couponService = {
  validateCoupon,
  applyCouponToCart,
  removeCouponFromCart,
  getCouponUsage,
  checkCouponAvailability,
  getCouponByCode,
  calculateDiscount,
  formatCouponDisplay,
  hasCouponRestrictions,
  getCouponRestrictions
};

export default couponService;
