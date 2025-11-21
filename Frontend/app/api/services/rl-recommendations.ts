/**
 * RL Recommendation Service
 * Handles communication with backend RL recommendation endpoint
 */

import apiClient from "../axios";
import { Product } from "./product";

export interface RLRecommendationRequest {
  userId: number;
  limit?: number;
  categoryId?: number;
  priceRangeMin?: number;
  priceRangeMax?: number;
  excludeProducts?: number[];
  context?: Record<string, any>;
}

export interface RLRecommendationItem {
  productId: number;
  productName: string;
  categoryId: number;
  categoryName?: string;
  brand?: string;
  price: number;
  discountPrice?: number;
  discountPercentage?: number;
  imageUrl?: string;
  rating?: number;
  reviewCount?: number;
  confidenceScore: number;
  rank: number;
  reason?: string;
}

export interface RLRecommendationResponse {
  userId: number;
  recommendations: RLRecommendationItem[];
  totalCount: number;
  algorithmUsed: string;
  modelVersion?: string;
  timestamp: string;
  metadata?: Record<string, any>;
}

/**
 * Get RL-powered recommendations from backend
 * Backend will call Python RL service internally
 */
export const getRLRecommendations = async (
  request: RLRecommendationRequest
): Promise<RLRecommendationResponse> => {
  try {
    console.log("Requesting RL recommendations:", request);

    const response = await apiClient.post<RLRecommendationResponse>(
      "/api/v1/recommendations/rl",
      request
    );

    console.log("RL recommendations received:", {
      count: response.data.recommendations.length,
      algorithm: response.data.algorithmUsed
    });

    return response.data;
  } catch (error) {
    console.error("Error fetching RL recommendations:", error);
    throw error;
  }
};

/**
 * Convert RL recommendation items to Product format for display
 */
export const convertRLItemsToProducts = (
  items: RLRecommendationItem[]
): (Product & { confidenceScore?: number; rank?: number; reason?: string })[] => {
  return items.map((item) => ({
    id: item.productId,
    name: item.productName,
    price: item.price,
    offerPrice: item.discountPrice,
    imageUrl: item.imageUrl,
    categoryId: item.categoryId,
    categoryName: item.categoryName,
    brand: item.brand,
    discount: item.discountPercentage,
    quantity: 1, // Default
    active: true,
    // RL-specific fields
    confidenceScore: item.confidenceScore,
    rank: item.rank,
    reason: item.reason
  }));
};

/**
 * Get model status
 */
export const getRLModelStatus = async (): Promise<Record<string, any>> => {
  try {
    const response = await apiClient.get("/api/v1/recommendations/rl/status");
    return response.data;
  } catch (error) {
    console.error("Error fetching RL model status:", error);
    return { status: "error", message: "Unable to fetch model status" };
  }
};

/**
 * Submit feedback for online learning
 */
export const submitRLFeedback = async (
  userId: number,
  productId: number,
  reward: number
): Promise<void> => {
  try {
    await apiClient.post("/api/v1/recommendations/rl/feedback", null, {
      params: { userId, productId, reward }
    });
  } catch (error) {
    // Fail silently - don't disrupt user experience
    console.error("Error submitting RL feedback:", error);
  }
};

