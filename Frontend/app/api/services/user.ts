import apiClient from '../axios';

export interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string; // Frontend compatibility
  emailAddress: string; // Backend uses emailAddress
  phoneNumber?: string;
  location?: string;
  profilePictureUrl?: string;
  roles: string[]; // Frontend compatibility
  userRoles: string[]; // Backend uses userRoles
  isActivated?: boolean;
  isBlocked?: boolean;
  registrationDate?: string;
  lastLoginDate?: string;
  // Additional fields from UserProfileResponse
  dateOfBirth?: string;
  privacySettings?: PrivacySettings;
  favoriteProducts?: number[];
  followers?: number[];
  followersCount?: number;
  following?: number[];
  followingCount?: number;
  companyId?: number;
}

export interface PublicProfile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  profilePictureUrl?: string;
  location?: string;
  isFollowing?: boolean;
  followerCount: number;
  followingCount: number;
  favoriteProductsCount: number;
  joinDate?: string;
}

export interface PrivacySettings {
  profileVisibility: 'public' | 'private' | 'friends_only';
  showEmail: boolean;
  showPhone: boolean;
  showLocation: boolean;
  allowFollowing: boolean;
  allowMessaging: boolean;
  emailNotifications: boolean;
  smsNotifications: boolean;
}

export interface UserCustomField {
  id?: number;
  fieldKey: string;
  fieldValue: string;
  fieldType: 'text' | 'number' | 'boolean' | 'date';
  isPublic?: boolean;
}

export interface FollowResponse {
  userId: number;
  followedUserId: number;
  followDate: string;
}

export interface UserFollowInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  profilePictureUrl?: string;
  followDate?: string;
}

export interface UserProfileRequest {
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  location?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export interface Address {
  id?: number;
  fullName: string;
  streetAddress: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  isDefault?: boolean;
}

export interface PaymentMethod {
  id?: number;
  type: 'credit_card' | 'debit_card' | 'paypal';
  cardNumber?: string; // masked
  expiryMonth?: number;
  expiryYear?: number;
  cardholderName?: string;
  isDefault?: boolean;
}

// User Profile Operations
export const getCurrentUserProfile = async (): Promise<User> => {
  const token = localStorage.getItem('token');
  if (!token) {
    throw new Error('No authentication token found');
  }

  // Extract email from JWT token
  const { getEmailFromToken } = await import('../../../utils/jwt');
  const email = getEmailFromToken(token);
  // console.log('email in getCurrentUserProfile', email);
  if (!email) {
    throw new Error('Could not extract email from token');
  }

  // Use the profile visit endpoint to get user profile
  const response = await apiClient.get(`/profile/visit/${email}`);
  const profileData = response.data;
  
  // Transform backend response to match User interface
  return {
    id: profileData.id,
    firstName: profileData.firstName,
    lastName: profileData.lastName,
    email: profileData.emailAddress, // Map emailAddress to email for frontend
    emailAddress: profileData.emailAddress,
    phoneNumber: profileData.phoneNumber,
    location: profileData.location,
    profilePictureUrl: profileData.profilePictureUrl,
    roles: profileData.userRoles || [], // Map userRoles to roles for frontend
    userRoles: profileData.userRoles || [],
    isActivated: profileData.isActivated,
    isBlocked: profileData.isBlocked,
    registrationDate: profileData.registrationDate,
    lastLoginDate: profileData.lastLoginDate,
    dateOfBirth: profileData.dateOfBirth,
    privacySettings: profileData.privacySettings,
    favoriteProducts: profileData.favoriteProducts,
    followers: profileData.followers,
    followersCount: profileData.followersCount,
    following: profileData.following,
    followingCount: profileData.followingCount,
    companyId: profileData.companyId,
  };
};

export const updateUserProfile = async (data: UserProfileRequest): Promise<User> => {
  const response = await apiClient.put<User>('/buyer/profile/me/update-profile', data);
  return response.data;
};

export const uploadProfilePicture = async (file: File): Promise<{ message: string; profilePictureUrl?: string }> => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await apiClient.post<{ message: string; profilePictureUrl?: string }>('/buyer/profile/me/profile-picture', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

export const updateUserPassword = async (data: ChangePasswordRequest): Promise<{ message: string }> => {
  const response = await apiClient.put<{ message: string }>('/buyer/user/update-password', data);
  return response.data;
};

export const changeUserEmail = async (newEmail: string, password: string): Promise<{ message: string }> => {
  const response = await apiClient.put<{ message: string }>('/buyer/user/change-email', {
    newEmail,
    password
  });
  return response.data;
};

export const logoutUser = async (): Promise<{ message: string }> => {
  const token = localStorage.getItem('token');
  if (!token) {
    return { message: 'No token to logout' };
  }

  // Extract refresh token if available
  const refreshToken = localStorage.getItem('refreshToken');
  
  try {
    const response = await apiClient.post<{ message: string }>('/buyer/user/logout', {
      refreshToken: refreshToken || ''
    });
    return response.data;
  } catch (error) {
    console.error('Logout error:', error);
    // Return success even if server logout fails, as we'll clear local storage anyway
    return { message: 'Logged out locally' };
  }
};

// Address Management
// Address Management (moved to app/api/services/address.ts). Kept empty here to avoid duplicates.

// Payment Methods
export const getPaymentMethods = async (): Promise<PaymentMethod[]> => {
  const response = await apiClient.get<PaymentMethod[]>('/buyer/profile/payment-methods');
  return response.data;
};

export const addPaymentMethod = async (paymentMethod: Omit<PaymentMethod, 'id'>): Promise<PaymentMethod> => {
  const response = await apiClient.post<PaymentMethod>('/buyer/profile/payment-methods', paymentMethod);
  return response.data;
};

export const updatePaymentMethod = async (id: number, paymentMethod: Partial<PaymentMethod>): Promise<PaymentMethod> => {
  const response = await apiClient.put<PaymentMethod>(`/buyer/profile/payment-methods/${id}`, paymentMethod);
  return response.data;
};

export const deletePaymentMethod = async (id: number): Promise<void> => {
  await apiClient.delete(`/buyer/profile/payment-methods/${id}`);
};

export const setDefaultPaymentMethod = async (id: number): Promise<void> => {
  await apiClient.post(`/buyer/profile/payment-methods/${id}/set-default`);
};

// User Role Management
export const becomeSellerRequest = async (): Promise<{ message: string; sellerId?: number }> => {
  const response = await apiClient.post<{ message: string; sellerId?: number }>('/buyer/user/role/become-seller');
  return response.data;
};

// Privacy Settings Management
export const getMyPrivacySettings = async (): Promise<PrivacySettings> => {
  const response = await apiClient.get<PrivacySettings>('/profile/visit/me/privacy-settings');
  return response.data;
};

export const updatePrivacySettings = async (settings: Partial<PrivacySettings>): Promise<PrivacySettings> => {
  const response = await apiClient.put<PrivacySettings>('/buyer/profile/me/privacy-settings', settings);
  return response.data;
};

// Follow/Unfollow functionality - Using User Follow Controller endpoints
export const toggleFollowUser = async (targetEmail: string): Promise<{ isFollowing: boolean; message: string }> => {
  const response = await apiClient.post<{ isFollowing: boolean; message: string }>(`/buyer/profile/${targetEmail}/toggle-follow`);
  return response.data;
};

export const followUser = async (followedUserId: number): Promise<FollowResponse> => {
  const response = await apiClient.post<FollowResponse>(`/buyer/user/follow/${followedUserId}`);
  return response.data;
};

export const unfollowUser = async (followedUserId: number): Promise<{ message: string }> => {
  const response = await apiClient.delete<{ message: string }>(`/buyer/user/follow/${followedUserId}`);
  return response.data;
};

export const getUserFollowers = async (userId: number, page = 0, size = 10): Promise<{ content: UserFollowInfo[]; totalElements: number; totalPages: number }> => {
  const response = await apiClient.get(`/buyer/user/follow/${userId}/followers`, {
    params: { page, size }
  });
  return response.data;
};

export const getUserFollowing = async (userId: number, page = 0, size = 10): Promise<{ content: UserFollowInfo[]; totalElements: number; totalPages: number }> => {
  const response = await apiClient.get(`/buyer/user/follow/${userId}/following`, {
    params: { page, size }
  });
  return response.data;
};

export const getFollowersCount = async (userId: number): Promise<{ count: number }> => {
  const response = await apiClient.get<{ count: number }>(`/buyer/user/follow/${userId}/followers/count`);
  return response.data;
};

export const getFollowingCount = async (userId: number): Promise<{ count: number }> => {
  const response = await apiClient.get<{ count: number }>(`/buyer/user/follow/${userId}/following/count`);
  return response.data;
};

// Account Deletion
export const deleteUserAccount = async (password: string): Promise<{ message: string; deletionToken?: string }> => {
  const response = await apiClient.delete<{ message: string; deletionToken?: string }>('/buyer/user/delete-account', {
    data: { password }
  });
  return response.data;
};

export const confirmAccountDeletion = async (confirmationToken: string): Promise<{ message: string }> => {
  const response = await apiClient.delete<{ message: string }>('/buyer/user/confirm-delete-account', {
    data: { confirmationToken }
  });
  return response.data;
};

// User Custom Fields Management
export const getAllCustomFields = async (): Promise<UserCustomField[]> => {
  const response = await apiClient.get<UserCustomField[]>('/buyer/user/custom-fields');
  return response.data;
};

export const getCustomFieldByKey = async (fieldKey: string): Promise<UserCustomField> => {
  const response = await apiClient.get<UserCustomField>(`/buyer/user/custom-fields/${fieldKey}`);
  return response.data;
};

export const createCustomField = async (field: Omit<UserCustomField, 'id'>): Promise<UserCustomField> => {
  const response = await apiClient.post<UserCustomField>('/buyer/user/custom-fields', field);
  return response.data;
};

export const updateCustomField = async (fieldId: number, field: Partial<UserCustomField>): Promise<UserCustomField> => {
  const response = await apiClient.put<UserCustomField>(`/buyer/user/custom-fields/${fieldId}`, field);
  return response.data;
};

export const deleteCustomField = async (fieldId: number): Promise<{ message: string }> => {
  const response = await apiClient.delete<{ message: string }>(`/buyer/user/custom-fields/${fieldId}`);
  return response.data;
};

// Public Profile Visit APIs
export const getPublicProfile = async (email: string): Promise<PublicProfile> => {    
  const response = await apiClient.get<PublicProfile>(`/profile/visit/${email}`);
  return response.data;
};

export const getPublicProfileFollowing = async (email: string, page = 0, size = 10): Promise<{ content: UserFollowInfo[]; totalElements: number }> => {
  const response = await apiClient.get(`/profile/visit/${email}/following`, {
    params: { page, size }
  });
  return response.data;
};

export const getPublicProfileFollowingCount = async (email: string): Promise<{ count: number }> => {
      const response = await apiClient.get<{ count: number }>(`/profile/visit/${email}/following-count`);
  return response.data;
};

export const getPublicProfileFollowers = async (email: string, page = 0, size = 10): Promise<{ content: UserFollowInfo[]; totalElements: number }> => {
  const response = await apiClient.get(`/profile/visit/${email}/followers`, {
    params: { page, size }
  });
  return response.data;
};

export const getPublicProfileFollowerCount = async (email: string): Promise<{ count: number }> => {
  const response = await apiClient.get<{ count: number }>(`/profile/visit/${email}/follower-count`);
  return response.data;
};

export interface Product {
  id: number;
  name: string;
  price: number;
  imageUrl?: string;
  categoryName?: string;
  sellerId: number;
  sellerName?: string;
}

export const getPublicProfileFavorites = async (email: string, page = 0, size = 10): Promise<{ content: Product[]; totalElements: number }> => {
  const response = await apiClient.get(`/profile/visit/${email}/favorites`, {
    params: { page, size }
  });
  return response.data;
};

// Authenticated user favorite products management
export const getMyFavoriteProducts = async (page = 0, size = 10): Promise<{ content: Product[]; totalElements: number }> => {
  const response = await apiClient.get('/buyer/profile/me/favorites', {
    params: { page, size }
  });
  return response.data;
};

export const toggleFavoriteProduct = async (productId: number): Promise<{ isFavorite: boolean; message: string }> => {
  const response = await apiClient.post<{ isFavorite: boolean; message: string }>(`/buyer/profile/toggle-favorite/${productId}`);
  return response.data;
}; 