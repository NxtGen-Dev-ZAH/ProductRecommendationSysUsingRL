/**
 * Interaction tracking service for RL recommendation system
 */

import { axiosInstance } from "../axios";

export type InteractionType = 
  | "VIEW"
  | "CLICK"
  | "CART_ADD"
  | "PURCHASE"
  | "WISHLIST"
  | "REMOVE_CART"
  | "REMOVE_WISHLIST";

export interface InteractionContext {
  page?: string;
  position?: number;
  categoryId?: number;
  priceRange?: {
    min: number;
    max: number;
  };
  deviceType?: string;
  referrer?: string;
  searchQuery?: string;
}

export interface LogInteractionRequest {
  userId: number;
  productId: number;
  interactionType: InteractionType;
  sessionId?: string;
  context?: InteractionContext;
}

export interface InteractionStats {
  userId: number;
  totalInteractions: number;
  interactionsByType: Record<string, number>;
  period: string;
}

/**
 * Log a user-product interaction
 */
export const logInteraction = async (
  request: LogInteractionRequest
): Promise<void> => {
  try {
    await axiosInstance.post("/interactions/log", request);
  } catch (error) {
    // Fail silently - don't disrupt user experience
    console.error("Failed to log interaction:", error);
  }
};

/**
 * Get user interaction history
 */
export const getUserInteractionHistory = async (
  userId: number,
  page: number = 0,
  size: number = 20
): Promise<any> => {
  try {
    const response = await axiosInstance.get(`/interactions/user/${userId}`, {
      params: { page, size }
    });
    return response.data;
  } catch (error) {
    console.error("Failed to fetch interaction history:", error);
    throw error;
  }
};

/**
 * Get user interaction statistics
 */
export const getUserInteractionStats = async (
  userId: number,
  days: number = 30
): Promise<InteractionStats> => {
  try {
    const response = await axiosInstance.get(`/interactions/stats`, {
      params: { userId, days }
    });
    return response.data;
  } catch (error) {
    console.error("Failed to fetch interaction stats:", error);
    throw error;
  }
};

/**
 * Generate session ID for tracking
 */
export const generateSessionId = (): string => {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2);
  return `session_${timestamp}_${random}`;
};

/**
 * Get or create session ID from storage
 */
export const getSessionId = (): string => {
  if (typeof window === "undefined") return "";
  
  let sessionId = sessionStorage.getItem("rl_session_id");
  
  if (!sessionId) {
    sessionId = generateSessionId();
    sessionStorage.setItem("rl_session_id", sessionId);
  }
  
  return sessionId;
};

/**
 * Get device type
 */
export const getDeviceType = (): string => {
  if (typeof window === "undefined") return "unknown";
  
  const userAgent = navigator.userAgent.toLowerCase();
  
  if (/mobile|android|iphone|ipad|tablet/.test(userAgent)) {
    if (/tablet|ipad/.test(userAgent)) {
      return "tablet";
    }
    return "mobile";
  }
  
  return "desktop";
};

/**
 * Build interaction context
 */
export const buildInteractionContext = (
  additionalContext?: Partial<InteractionContext>
): InteractionContext => {
  return {
    page: typeof window !== "undefined" ? window.location.pathname : undefined,
    deviceType: getDeviceType(),
    referrer: typeof document !== "undefined" ? document.referrer : undefined,
    ...additionalContext
  };
};

