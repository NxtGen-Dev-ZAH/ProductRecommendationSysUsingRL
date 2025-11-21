import apiClient from '../axios';
import { getEmailFromToken, getRolesFromToken, isTokenExpired } from '../../../utils/jwt';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword?: string;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  email: string;
  userRoles?: string[];  // Backend roles (Set<Roles>)
  provider?: string;
  message?: string;
  // Note: Backend doesn't return user object, we'll need to fetch it separately
}

export interface PasswordResetRequest {
  token: string;
  newPassword: string;
  confirmPassword?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AccountActivationRequest {
  token: string;
  email?: string;
}

export interface LoginFormResponse {
  formHtml: string;
  csrfToken?: string;
}

export interface AuthErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}

export const loginUser = async (data: LoginRequest): Promise<AuthResponse> => {
  // Transform the data to match backend expectations
  const backendData = {
    emailAddress: data.email,  // ✅ Correct field name
    password: data.password    // ✅ Correct field name
  };
  const response = await apiClient.post<AuthResponse>('/auth/login/submit', backendData);
  return response.data;
};

// Submit login form (alternative endpoint)
export const loginUserForm = async (data: LoginRequest): Promise<AuthResponse> => {
  const response = await apiClient.post<AuthResponse>('/auth/login/submit-form', data);
  return response.data;
};

// Get login form HTML
export const getLoginForm = async (): Promise<LoginFormResponse> => {
  const response = await apiClient.get<LoginFormResponse>('/auth/login/form');
  return response.data;
};

// Get JSON-consuming login form
export const getLoginFormJson = async (): Promise<LoginFormResponse> => {
  const response = await apiClient.get<LoginFormResponse>('/auth/login/form-consumes-json');
  return response.data;
};

// Handle authentication errors
export const getAuthError = async (): Promise<AuthErrorResponse> => {
  const response = await apiClient.get<AuthErrorResponse>('/auth/error');
  return response.data;
};

export const registerUser = async (data: RegisterRequest): Promise<AuthResponse | { message: string }> => {
  // Transform the data to match backend expectations
  const backendData = {
    firstName: data.firstName,
    lastName: data.lastName,
    emailAddress: data.email,
    password: data.password,
    confirmPassword: data.confirmPassword || data.password // Use password as confirmPassword if not provided
  };
  
  console.log('Sending registration data:', {
    ...backendData,
    password: '[REDACTED]',
    confirmPassword: '[REDACTED]'
  });
  
  try {
    const response = await apiClient.post('/auth/register', backendData);
    console.log('Registration response:', response.data);
    return response.data;
  } catch (error: unknown) {
    console.error('Registration error:', error);
    
    // Handle backend validation errors
    if ((error as { response?: { data?: { errors?: Record<string, string[]>; message?: string }; status?: number } })?.response) {
      const errorResponse = (error as { response: { data?: { errors?: Record<string, string[]>; message?: string }; status?: number } }).response;
      const errorData = errorResponse.data;
      console.log('Error response data:', errorData);
      
      // Handle 409 Conflict (User Already Exists)
      if (errorResponse.status === 409 && errorData?.message) {
        // Clean up error message if it contains duplicate prefixes
        const cleanMessage = errorData.message.replace(/^USER_ALREADY_EXISTS:\s*/i, '').trim();
        throw new Error(cleanMessage || 'This email is already registered. Please use a different email or try logging in.');
      }
      
      // If it's a validation error with field-specific errors
      if (errorData?.errors && typeof errorData.errors === 'object') {
        const fieldErrors = Object.values(errorData.errors).join(', ');
        throw new Error(fieldErrors);
      }
      
      // If it's a general error with message
      if (errorData?.message) {
        throw new Error(errorData.message);
      }
    }
    
    // Re-throw the original error if we can't parse it
    throw error;
  }
};

// Activate user account
export const activateAccount = async (token: string, email?: string): Promise<{ message: string }> => {
  const params: AccountActivationRequest = { token };
  if (email) params.email = email;
  
  const response = await apiClient.get('/auth/activate-account', { params });
  return response.data;
};

// Refresh authentication token
export const refreshAuthToken = async (refreshToken: string): Promise<AuthResponse> => {
  // Backend expects raw string in request body, not JSON object
  const response = await apiClient.post<AuthResponse>('/auth/refresh', refreshToken, {
    headers: {
      'Content-Type': 'application/json',
    },
  });
  return response.data;
};

// OAuth2 login URLs (use direct redirects)
export const getGoogleLoginUrl = (): string => {
  return `${apiClient.defaults.baseURL}/oauth2/authorization/google`;
};

export const getFacebookLoginUrl = (): string => {
  return `${apiClient.defaults.baseURL}/oauth2/authorization/facebook`;
};

export const getAppleLoginUrl = (): string => {
  return `${apiClient.defaults.baseURL}/oauth2/authorization/apple`;
};

// Redirect to OAuth2 provider
export const redirectToOAuth2 = (provider: 'google' | 'facebook' | 'apple') => {
  const urls = {
    google: getGoogleLoginUrl(),
    facebook: getFacebookLoginUrl(),
    apple: getAppleLoginUrl()
  };
  
  window.location.href = urls[provider];
};

// This function is deprecated - use redirectToOAuth2 instead
export const googleLogin = async () => {
  redirectToOAuth2('google');
};

export const getCurrentUser = async (): Promise<{
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
} | null> => {
  try {
    const token = localStorage.getItem('token');
    if (!token) {
      return null;
    }

    // Check if token is expired
    if (isTokenExpired(token)) {
      console.warn('Token is expired, clearing auth data');
      logoutUser();
      return null;
    }

    // Extract user information from JWT token
    const email = getEmailFromToken(token);
    const roles = getRolesFromToken(token);
    
    if (!email) {
      console.error('Could not extract email from token');
      return null;
    }

    // Try to get additional user profile information from the backend
    try {
      const encodedEmail = encodeURIComponent(email);
      const profileResponse = await apiClient.get(`/profile/visit/${encodedEmail}`);
      const profileData = profileResponse.data;
      
      return {
        id: profileData.id,
        email: profileData.emailAddress,
        firstName: profileData.firstName,
        lastName: profileData.lastName,
        roles: profileData.userRoles || roles,
      };
    } catch (profileError) {
      console.warn('Could not fetch profile data, using token data only:', profileError);
      
      // Fallback to basic user info from token
      return {
        id: 0, // We don't have the ID from token, so use 0
        email: email,
        firstName: '', // We don't have this from token
        lastName: '', // We don't have this from token
        roles: roles,
      };
    }
  } catch (error) {
    console.error('Error fetching current user:', error);
    return null;
  }
};

// Check if user is authenticated
export const isAuthenticated = (): boolean => {
  const token = localStorage.getItem('token');
  return !!token;
};

// Logout user (remove tokens)
export const logoutUser = (): void => {
  localStorage.removeItem('token');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('user');
};

// Store authentication data
export const storeAuthData = (authResponse: AuthResponse): void => {
  localStorage.setItem('token', authResponse.token);
  
  if (authResponse.refreshToken) {
    localStorage.setItem('refreshToken', authResponse.refreshToken);
  }
};

// Get stored user data
export const getStoredUser = () => {
  const userStr = localStorage.getItem('user');
  return userStr ? JSON.parse(userStr) : null;
};

// Get stored token
export const getStoredToken = (): string | null => {
  return localStorage.getItem('token');
};

// Get stored refresh token
export const getStoredRefreshToken = (): string | null => {
  return localStorage.getItem('refreshToken');
};

/**
 * Request a password reset email
 * @param email The email address to send the reset link to
 */
export const requestPasswordReset = async (email: string): Promise<{ message: string }> => {
  try {
    const request: ForgotPasswordRequest = { email };
    const response = await apiClient.post('/auth/forgot-password', request);
    return response.data;
  } catch (error) {
    console.error('Error requesting password reset:', error);
    throw error;
  }
};

/**
 * Reset password with token
 * @param token The reset token from the email link
 * @param newPassword The new password
 * @param confirmPassword Confirmation of the new password
 */
export const resetPassword = async (token: string, newPassword: string, confirmPassword?: string): Promise<{ message: string }> => {
  try {
    const request: PasswordResetRequest = { 
      token, 
      newPassword
    };
    
    if (confirmPassword) {
      request.confirmPassword = confirmPassword;
    }
    
    const response = await apiClient.post('/auth/reset-password', request);
    return response.data;
  } catch (error) {
    console.error('Error resetting password:', error);
    throw error;
  }
}; 