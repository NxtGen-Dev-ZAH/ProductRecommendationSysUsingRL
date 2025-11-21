// =============================================
// Address Management API Services
// Handles all address-related API calls
// =============================================

import apiClient from '../axios';
import type {
  Address,
  UserAddressRequest,
  UserAddressResponse,
  CompanyAddressRequest,
  CompanyAddressResponse,
  ApiResponse
} from '../../../types/api';

// =============================================================================
// User Address Management
// =============================================================================

/**
 * Get all addresses for a user
 */
export const getUserAddresses = async (): Promise<UserAddressResponse[]> => {
  try {
    console.log(`🏠 [ADDRESS API] Getting addresses for current user`);
    const response = await apiClient.get<ApiResponse<UserAddressResponse[]>>(
      `/buyer/user/addresses`
    );
    console.log(`✅ [ADDRESS API] Retrieved ${response.data.data?.length || 0} addresses`);
    return response.data.data || [];
  } catch (error) {
    const apiError = error as { response?: { status?: number; data?: { message?: string } } };
    const status = apiError.response?.status;
    const message = apiError.response?.data?.message;
    console.error(`❌ [ADDRESS API] Failed to get user addresses:`, status, message);
    if (status === 404) {
      throw new Error('Addresses not found');
    }
    if (status === 422) {
      throw new Error(message || 'Invalid address request');
    }
    throw new Error(message || 'Failed to load addresses. Please try again.');
  }
};

/**
 * Add a new address for the current user
 */
export const addUserAddress = async (
  addressData: UserAddressRequest
): Promise<UserAddressResponse> => {
  try {
    const response = await apiClient.post<ApiResponse<UserAddressResponse>>(
      `/buyer/user/addresses`,
      addressData
    );
    return response.data.data as UserAddressResponse;
  } catch (error) {
    const apiError = error as { response?: { status?: number; data?: { message?: string } } };
    const status = apiError.response?.status;
    const message = apiError.response?.data?.message;
    if (status === 422) {
      throw new Error(message || 'Please check address fields and try again.');
    }
    throw new Error(message || 'Failed to save address. Please try again.');
  }
};

export const updateUserAddress = async (
  addressId: number,
  addressData: UserAddressRequest
): Promise<UserAddressResponse> => {
  try {
    const response = await apiClient.put<ApiResponse<UserAddressResponse>>(
      `/buyer/user/addresses/${addressId}`,
      addressData
    );
    return response.data.data as UserAddressResponse;
  } catch (error) {
    const apiError = error as { response?: { status?: number; data?: { message?: string } } };
    const status = apiError.response?.status;
    const message = apiError.response?.data?.message;
    if (status === 404) {
      throw new Error('Address not found');
    }
    if (status === 422) {
      throw new Error(message || 'Please check address fields and try again.');
    }
    throw new Error(message || 'Failed to update address.');
  }
};

export const deleteUserAddress = async (
  addressId: number
): Promise<void> => {
  try {
    await apiClient.delete(`/buyer/user/addresses/${addressId}`);
  } catch (error) {
    const apiError = error as { response?: { status?: number; data?: { message?: string } } };
    const status = apiError.response?.status;
    const message = apiError.response?.data?.message;
    if (status === 404) {
      throw new Error('Address not found');
    }
    throw new Error(message || 'Failed to delete address.');
  }
};

// =============================================================================
// Admin: Manage addresses for any user (separate from buyer self-service)
// =============================================================================

export const getAdminUserAddresses = async (
  userId: number
): Promise<UserAddressResponse[]> => {
  try {
    const response = await apiClient.get<ApiResponse<UserAddressResponse[]>>(
      `/admin/v1/users/${userId}/addresses`
    );
    return response.data.data || [];
  } catch (error) {
    const apiError = error as { response?: { status?: number; data?: { message?: string } } };
    const status = apiError.response?.status;
    const message = apiError.response?.data?.message;
    if (status === 404) {
      throw new Error('User or addresses not found');
    }
    throw new Error(message || 'Failed to load user addresses');
  }
};

export const addAdminUserAddress = async (
  userId: number,
  addressData: UserAddressRequest
): Promise<UserAddressResponse> => {
  try {
    const response = await apiClient.post<ApiResponse<UserAddressResponse>>(
      `/admin/v1/users/${userId}/addresses`,
      addressData
    );
    return response.data.data as UserAddressResponse;
  } catch (error) {
    const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
    throw new Error(message || 'Failed to save user address');
  }
};

export const updateAdminUserAddress = async (
  userId: number,
  addressId: number,
  addressData: UserAddressRequest
): Promise<UserAddressResponse> => {
  try {
    const response = await apiClient.put<ApiResponse<UserAddressResponse>>(
      `/admin/v1/users/${userId}/addresses/${addressId}`,
      addressData
    );
    return response.data.data as UserAddressResponse;
  } catch (error) {
    const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
    throw new Error(message || 'Failed to update user address');
  }
};

export const deleteAdminUserAddress = async (
  userId: number,
  addressId: number
): Promise<void> => {
  try {
    await apiClient.delete(`/admin/v1/users/${userId}/addresses/${addressId}`);
  } catch (error) {
    const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message;
    throw new Error(message || 'Failed to delete user address');
  }
};

/**
 * Get a specific address for the current user
 */
export const getUserAddress = async (
  addressId: number
): Promise<UserAddressResponse> => {
  try {
    const response = await apiClient.get<ApiResponse<UserAddressResponse>>(
      `/buyer/user/addresses/${addressId}`
    );
    return response.data.data as UserAddressResponse;
  } catch (error) {
    const apiError = error as { response?: { status?: number; data?: { message?: string } } };
    const status = apiError.response?.status;
    const message = apiError.response?.data?.message;
    if (status === 404) {
      throw new Error('Address not found');
    }
    throw new Error(message || 'Failed to load address');
  }
};

// =============================================================================
// Company Address Management
// =============================================================================

/**
 * Get all addresses for a company
 */
export const getCompanyAddresses = async (companyId: number): Promise<CompanyAddressResponse[]> => {
  try {
    console.log(`🏢 [ADDRESS API] Getting addresses for company ${companyId}`);
    const response = await apiClient.get<ApiResponse<CompanyAddressResponse[]>>(
      `/seller/company/${companyId}/addresses`
    );
    console.log(`✅ [ADDRESS API] Retrieved ${response.data.data?.length || 0} company addresses`);
    return response.data.data || [];
  } catch (error) {
    console.error(`❌ [ADDRESS API] Failed to get company addresses:`, error);
    throw error;
  }
};

/**
 * Add a new address for a company
 */
export const addCompanyAddress = async (
  companyId: number, 
  addressData: CompanyAddressRequest
): Promise<CompanyAddressResponse> => {
  try {
    console.log(`🏢 [ADDRESS API] Adding address for company ${companyId}:`, addressData);
    const response = await apiClient.post<ApiResponse<CompanyAddressResponse>>(
      `/seller/company/${companyId}/addresses`,
      addressData
    );
    console.log(`✅ [ADDRESS API] Company address added successfully:`, response.data.data);
    return response.data.data!;
  } catch (error) {
    console.error(`❌ [ADDRESS API] Failed to add company address:`, error);
    throw error;
  }
};

/**
 * Update an existing company address
 */
export const updateCompanyAddress = async (
  companyId: number,
  addressId: number,
  addressData: CompanyAddressRequest
): Promise<CompanyAddressResponse> => {
  try {
    console.log(`🏢 [ADDRESS API] Updating address ${addressId} for company ${companyId}:`, addressData);
    const response = await apiClient.put<ApiResponse<CompanyAddressResponse>>(
      `/seller/company/${companyId}/addresses/${addressId}`,
      addressData
    );
    console.log(`✅ [ADDRESS API] Company address updated successfully:`, response.data.data);
    return response.data.data!;
  } catch (error) {
    console.error(`❌ [ADDRESS API] Failed to update company address:`, error);
    throw error;
  }
};

/**
 * Delete a company address
 */
export const deleteCompanyAddress = async (companyId: number, addressId: number): Promise<void> => {
  try {
    console.log(`🏢 [ADDRESS API] Deleting address ${addressId} for company ${companyId}`);
    await apiClient.delete(`/seller/company/${companyId}/addresses/${addressId}`);
    console.log(`✅ [ADDRESS API] Company address deleted successfully`);
  } catch (error) {
    console.error(`❌ [ADDRESS API] Failed to delete company address:`, error);
    throw error;
  }
};

/**
 * Get a specific company address
 */
export const getCompanyAddress = async (
  companyId: number, 
  addressId: number
): Promise<CompanyAddressResponse> => {
  try {
    console.log(`🏢 [ADDRESS API] Getting address ${addressId} for company ${companyId}`);
    const response = await apiClient.get<ApiResponse<CompanyAddressResponse>>(
      `/seller/company/${companyId}/addresses/${addressId}`
    );
    console.log(`✅ [ADDRESS API] Company address retrieved successfully:`, response.data.data);
    return response.data.data!;
  } catch (error) {
    console.error(`❌ [ADDRESS API] Failed to get company address:`, error);
    throw error;
  }
};

// =============================================================================
// Order Address Management
// =============================================================================
// NOTE: Order-specific address endpoints have been removed in the backend refactoring.
// Addresses are now managed separately and linked to orders via shippingAddressId 
// and billingAddressId in the OrderCheckoutRequest during order creation.
// Use getUserAddresses() to manage addresses before creating an order.

// =============================================================================
// Utility Functions
// =============================================================================

/**
 * Get default address for a user by type
 */
export const getDefaultUserAddress = async (
  addressType: 'BILLING' | 'SHIPPING' | 'EXPEDITION' | 'CONTACT'
): Promise<UserAddressResponse | null> => {
  try {
    const addresses = await getUserAddresses();
    return addresses.find(addr => addr.addressType === addressType && addr.isDefault) || null;
  } catch (error) {
    console.error(`❌ [ADDRESS API] Failed to get default user address:`, error);
    return null;
  }
};

/**
 * Get default address for a company by type
 */
export const getDefaultCompanyAddress = async (
  companyId: number,
  addressType: 'BILLING' | 'SHIPPING' | 'EXPEDITION' | 'CONTACT'
): Promise<CompanyAddressResponse | null> => {
  try {
    const addresses = await getCompanyAddresses(companyId);
    return addresses.find(addr => addr.addressType === addressType && addr.isDefault) || null;
  } catch (error) {
    console.error(`❌ [ADDRESS API] Failed to get default company address:`, error);
    return null;
  }
};

/**
 * Format address for display
 */
export const formatAddress = (address: Address): string => {
  const parts = [
    address.addressLine1,
    address.addressLine2,
    address.city,
    address.state,
    address.postalCode,
    address.country
  ].filter(Boolean);
  
  return parts.join(', ');
};

/**
 * Enhanced address validation with improved field constraints
 */
export const validateAddress = (address: UserAddressRequest | CompanyAddressRequest): { isValid: boolean; errors: string[] } => {
  const errors: string[] = [];
  
  // Name validation - enhanced with length constraints
  if (!address.name?.trim()) {
    errors.push('Name is required');
  } else if (address.name.trim().length < 2) {
    errors.push('Name must be at least 2 characters long');
  } else if (address.name.trim().length > 100) {
    errors.push('Name cannot exceed 100 characters');
  }
  
  // Email validation - enhanced with better format checking
  if (address.email) {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(address.email)) {
      errors.push('Invalid email format');
    } else if (address.email.length > 255) {
      errors.push('Email cannot exceed 255 characters');
    }
  }
  
  // Address line 1 validation - enhanced with length constraints
  if (!address.addressLine1?.trim()) {
    errors.push('Address line 1 is required');
  } else if (address.addressLine1.trim().length < 5) {
    errors.push('Address line 1 must be at least 5 characters long');
  } else if (address.addressLine1.trim().length > 255) {
    errors.push('Address line 1 cannot exceed 255 characters');
  }
  
  // Address line 2 validation - optional but with length constraints if provided
  if (address.addressLine2 && address.addressLine2.trim().length > 255) {
    errors.push('Address line 2 cannot exceed 255 characters');
  }
  
  // City validation - enhanced with length constraints
  if (!address.city?.trim()) {
    errors.push('City is required');
  } else if (address.city.trim().length < 2) {
    errors.push('City must be at least 2 characters long');
  } else if (address.city.trim().length > 100) {
    errors.push('City cannot exceed 100 characters');
  }
  
  // State validation - optional but with length constraints if provided
  if (address.state && address.state.trim().length > 100) {
    errors.push('State cannot exceed 100 characters');
  }
  
  // Postal code validation - enhanced with format checking
  if (!address.postalCode?.trim()) {
    errors.push('Postal code is required');
  } else if (address.postalCode.trim().length < 3) {
    errors.push('Postal code must be at least 3 characters long');
  } else if (address.postalCode.trim().length > 20) {
    errors.push('Postal code cannot exceed 20 characters');
  } else if (!/^[A-Za-z0-9\s-]+$/.test(address.postalCode.trim())) {
    errors.push('Postal code contains invalid characters');
  }
  
  // Country validation - enhanced with length constraints
  if (!address.country?.trim()) {
    errors.push('Country is required');
  } else if (address.country.trim().length < 2) {
    errors.push('Country must be at least 2 characters long');
  } else if (address.country.trim().length > 100) {
    errors.push('Country cannot exceed 100 characters');
  }
  
  // Phone number validation - optional but with format checking if provided
  if (address.phoneNumber) {
    const phoneRegex = /^[\+]?[1-9][\d]{0,15}$/;
    if (!phoneRegex.test(address.phoneNumber.replace(/[\s\-\(\)]/g, ''))) {
      errors.push('Invalid phone number format. Use country code like +33 1 23 45 67 89');
    } else if (address.phoneNumber.length > 20) {
      errors.push('Phone number cannot exceed 20 characters');
    }
  }
  
  // Reference validation - optional but with length constraints if provided
  if (address.reference && address.reference.trim().length > 255) {
    errors.push('Reference cannot exceed 255 characters');
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
};

/**
 * Enhanced address validation for specific address types
 */
export const validateAddressByType = (
  address: UserAddressRequest | CompanyAddressRequest, 
  addressType: 'BILLING' | 'SHIPPING' | 'EXPEDITION' | 'CONTACT'
): { isValid: boolean; errors: string[] } => {
  const baseValidation = validateAddress(address);
  const errors = [...baseValidation.errors];
  
  // Type-specific validations
  if (addressType === 'BILLING') {
    // Billing addresses might require additional validation
    if (!address.email) {
      errors.push('Email is required for billing addresses');
    }
  }
  
  if (addressType === 'EXPEDITION') {
    // Expedition addresses might have different requirements
    if (!address.phoneNumber) {
      errors.push('Phone number is required for expedition addresses');
    }
  }
  
  return {
    isValid: errors.length === 0,
    errors
  };
};

/**
 * Sanitize address data before sending to API
 */
export const sanitizeAddressData = (address: UserAddressRequest | CompanyAddressRequest): UserAddressRequest | CompanyAddressRequest => {
  return {
    ...address,
    name: address.name?.trim(),
    email: address.email?.trim().toLowerCase(),
    addressLine1: address.addressLine1?.trim(),
    addressLine2: address.addressLine2?.trim(),
    city: address.city?.trim(),
    state: address.state?.trim(),
    postalCode: address.postalCode?.trim().toUpperCase(),
    country: address.country?.trim(),
    phoneNumber: address.phoneNumber?.trim(),
    reference: address.reference?.trim()
  };
};

