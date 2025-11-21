"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
} from "react";
import {
  loginUser,
  registerUser,
  getCurrentUser,
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  redirectToOAuth2,
  logoutUser as apiLogoutUser,
} from "../api/services/auth";
import {
  getCurrentUserProfile,
  becomeSellerRequest,
} from "../api/services/user";
import { mergeCartOnLogin } from "../api/services/cart";
import { MergeCartRequest } from "../../types/api";
import { setAuthToken, clearAuthToken } from "../../utils/cookies";

interface User {
  id: number;
  email: string; // Frontend uses email for compatibility
  emailAddress?: string; // Backend response field
  firstName: string;
  lastName: string;
  roles: string[]; // Frontend uses roles for compatibility
  userRoles?: string[]; // Backend response field
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  error: string | null;
  login: (data: LoginRequest) => Promise<void>;
  register: (
    data: RegisterRequest
  ) => Promise<AuthResponse | { message: string }>;
  loginWithGoogle: () => Promise<void>;
  handleOAuth2Completion: (
    token: string,
    refreshToken?: string,
    email?: string
  ) => Promise<void>;
  logout: () => void;
  isAuthenticated: boolean;
  isBuyer: boolean;
  isSeller: boolean;
  isAdmin: boolean;
  isSuperAdmin: boolean;
  isCompanyAdmin: boolean;
  hasRole: (role: string) => boolean;
  becomeSellerRequest: () => Promise<{ message: string; sellerId?: number }>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const isAuthenticated = !!user;
  const isBuyer = user?.roles?.includes("BUYER") || false;
  const isSeller = user?.roles?.includes("SELLER") || false;
  const isAdmin = user?.roles?.includes("ADMIN") || false;

  // Specific role checks for proper access control
  const isSuperAdmin =
    user?.roles?.includes("ROLE_APP_ADMIN") ||
    user?.roles?.includes("APP_ADMIN") ||
    false;
  const isCompanyAdmin =
    user?.roles?.includes("ROLE_COMPANY_ADMIN_SELLER") ||
    user?.roles?.includes("COMPANY_ADMIN_SELLER") ||
    false;

  // Helper function to check for specific roles
  const hasRole = (role: string): boolean => {
    if (!user?.roles) return false;
    return user.roles.includes(role) || user.roles.includes(`ROLE_${role}`);
  };

  // Check for existing auth on mount
  useEffect(() => {
    const initAuth = async () => {
      try {
        const token = localStorage.getItem("token");
        if (token) {
          try {
            const userData = await getCurrentUserProfile();
            if (userData) {
              setUser(userData);
            } else {
              // If getCurrentUserProfile returns null but we have a token,
              // we'll keep the user logged in but without profile data
              setUser({
                id: 0,
                email: "",
                firstName: "",
                lastName: "",
                roles: [],
              });
              console.warn(
                "User profile data could not be fetched, but token exists"
              );
            }
          } catch (err) {
            console.error("Error fetching user profile:", err);
            // Try fallback to getCurrentUser
            try {
              const fallbackData = await getCurrentUser();
              if (fallbackData) {
                setUser(fallbackData);
              } else {
                setUser({
                  id: 0,
                  email: "",
                  firstName: "",
                  lastName: "",
                  roles: [],
                });
              }
            } catch (fallbackErr) {
              console.error("Fallback auth method also failed:", fallbackErr);
              setUser({
                id: 0,
                email: "",
                firstName: "",
                lastName: "",
                roles: [],
              });
            }
          }
        }
      } catch (err) {
        console.error("Auth initialization error:", err);
      } finally {
        setLoading(false);
      }
    };

    initAuth();
  }, []);

  const handleAuthResponse = async (authResponse: AuthResponse) => {
    // Store token in both localStorage and cookies
    setAuthToken(authResponse.token);

    // Backend doesn't return user object, so we need to fetch it
    try {
      const userData = await getCurrentUserProfile();
      if (userData) {
        setUser(userData);
      } else {
        // Fallback: create user object from token data
        const token = authResponse.token;
        const decoded = JSON.parse(atob(token.split(".")[1]));
        setUser({
          id: 0, // We don't have ID from token
          email: decoded.sub,
          firstName: "",
          lastName: "",
          roles: decoded.roles || [],
        });
      }
    } catch (error) {
      console.error("Error fetching user profile after login:", error);
      // Fallback: create user object from token data
      const token = authResponse.token;
      const decoded = JSON.parse(atob(token.split(".")[1]));
      setUser({
        id: 0,
        email: decoded.sub,
        firstName: "",
        lastName: "",
        roles: decoded.roles || [],
      });
    }

    setError(null);
  };

  const login = async (data: LoginRequest) => {
    setLoading(true);
    try {
      // Try to merge cart first if user has items in anonymous cart
      try {
        const mergeData: MergeCartRequest = {
          emailAddress: data.email,
          password: data.password,
        };
        const mergeResponse = await mergeCartOnLogin(mergeData);

        // Handle successful cart merge and login
        setAuthToken(mergeResponse.authResponse.token);
        if (mergeResponse.authResponse.refreshToken) {
          localStorage.setItem(
            "refreshToken",
            mergeResponse.authResponse.refreshToken
          );
        }

        // Handle user data from cart merge response
        // Cart merge response has different structure, so we handle it directly
        setAuthToken(mergeResponse.authResponse.token);

        // Fetch user profile after cart merge
        try {
          const userData = await getCurrentUserProfile();
          if (userData) {
            setUser(userData);
          } else {
            // Fallback: create user object from token data
            const token = mergeResponse.authResponse.token;
            const decoded = JSON.parse(atob(token.split(".")[1]));
            setUser({
              id: 0,
              email: decoded.sub,
              firstName: "",
              lastName: "",
              roles: decoded.roles || [],
            });
          }
        } catch (error) {
          console.error("Error fetching user profile after cart merge:", error);
          // Fallback: create user object from token data
          const token = mergeResponse.authResponse.token;
          const decoded = JSON.parse(atob(token.split(".")[1]));
          setUser({
            id: 0,
            email: decoded.sub,
            firstName: "",
            lastName: "",
            roles: decoded.roles || [],
          });
        }
        setError(null);

        // Update cart context with merged cart data
        // This will be handled by the CartContext when it detects the user is logged in
        return;
      } catch (mergeError) {
        // If cart merge fails, fall back to regular login
        console.warn(
          "Cart merge failed, falling back to regular login:",
          mergeError
        );
      }

      // Regular login fallback
      const authResponse = await loginUser(data);
      await handleAuthResponse(authResponse);
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Login failed";
      setError(errorMessage || "Login failed");
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const register = async (data: RegisterRequest) => {
    setLoading(true);
    try {
      const result = await registerUser(data);
      console.log("Registration result:", result);

      // Check if registration requires activation
      // Backend returns AuthResponse with only email and message (no token) for activation-required cases
      if (
        "message" in result &&
        (!("token" in result) || result.token === null)
      ) {
        console.log("Registration requires activation, skipping auto-login");
        // Registration successful but requires activation
        // Don't attempt auto-login for accounts that need activation
        return result;
      }

      // Only auto login if registration returned a valid token (immediate activation)
      if (
        "token" in result &&
        result.token !== null &&
        result.token !== undefined
      ) {
        console.log(
          "Registration successful with immediate activation, proceeding with auto-login"
        );
        // Auto login after successful registration
        const loginData = { email: data.email, password: data.password };
        await login(loginData);
      }

      return result;
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Registration failed";
      setError(errorMessage || "Registration failed");
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const loginWithGoogle = async () => {
    setLoading(true);
    try {
      // Use the centralized OAuth2 redirect function
      redirectToOAuth2("google");
    } catch (err: unknown) {
      setError("Google login failed");
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const handleOAuth2Completion = async (
    token: string,
    refreshToken?: string,
    email?: string
  ) => {
    setLoading(true);
    try {
      // Store tokens and user profile
      setAuthToken(token);
      if (refreshToken) {
        localStorage.setItem("refreshToken", refreshToken);
      }
      if (email) {
        localStorage.setItem("userEmail", email);
      }

      // Fetch user profile
      const userData = await getCurrentUserProfile();
      if (userData) {
        setUser(userData);
      } else {
        // Fallback to getCurrentUser if profile fetch fails
        const fallbackData = await getCurrentUser();
        if (fallbackData) {
          setUser(fallbackData);
        } else {
          // Create user object from token if available
          const token = localStorage.getItem("token");
          if (token) {
            try {
              const decoded = JSON.parse(atob(token.split(".")[1]));
              setUser({
                id: 0,
                email: decoded.sub || email || "",
                firstName: "",
                lastName: "",
                roles: decoded.roles || ["BUYER"],
              });
            } catch {
              setUser({
                id: 0,
                email: email || "",
                firstName: "",
                lastName: "",
                roles: ["BUYER"], // Default role for OAuth users
              });
            }
          } else {
            setUser({
              id: 0,
              email: email || "",
              firstName: "",
              lastName: "",
              roles: ["BUYER"], // Default role for OAuth users
            });
          }
        }
      }
      setError(null);
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "OAuth2 completion failed";
      setError(errorMessage || "OAuth2 completion failed");
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const handleBecomeSellerRequest = async () => {
    setLoading(true);
    try {
      const result = await becomeSellerRequest();

      // Update user roles locally after successful API call
      if (user) {
        const updatedUser = {
          ...user,
          roles: [...user.roles, "SELLER"],
        };
        setUser(updatedUser);
      }

      return result;
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to become a seller";
      setError(errorMessage || "Failed to become a seller");
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = async () => {
    try {
      // Call the backend logout endpoint to invalidate server-side session
      await apiLogoutUser();
    } catch (error) {
      console.error("Error during server logout:", error);
      // Continue with local logout even if server logout fails
    }

    // Clear authentication data
    clearAuthToken();

    setUser(null);
    setError(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        error,
        login,
        register,
        loginWithGoogle,
        handleOAuth2Completion,
        logout,
        isAuthenticated,
        isBuyer,
        isSeller,
        isAdmin,
        isSuperAdmin,
        isCompanyAdmin,
        hasRole,
        becomeSellerRequest: handleBecomeSellerRequest,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
