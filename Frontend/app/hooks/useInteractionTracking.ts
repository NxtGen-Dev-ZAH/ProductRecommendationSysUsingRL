/**
 * Custom hook for tracking user-product interactions
 */

import { useCallback } from "react";
import { useAuth } from "../context/AuthContext";
import {
  logInteraction,
  getSessionId,
  buildInteractionContext,
  InteractionContext,
  InteractionType
} from "../api/services/interaction";

export interface UseInteractionTrackingReturn {
  trackProductView: (productId: number, context?: Partial<InteractionContext>) => Promise<void>;
  trackProductClick: (productId: number, context?: Partial<InteractionContext>) => Promise<void>;
  trackCartAdd: (productId: number, context?: Partial<InteractionContext>) => Promise<void>;
  trackPurchase: (productId: number, context?: Partial<InteractionContext>) => Promise<void>;
  trackWishlistAdd: (productId: number, context?: Partial<InteractionContext>) => Promise<void>;
  trackCartRemove: (productId: number, context?: Partial<InteractionContext>) => Promise<void>;
  trackWishlistRemove: (productId: number, context?: Partial<InteractionContext>) => Promise<void>;
}

/**
 * Hook for tracking user interactions with products
 */
export const useInteractionTracking = (): UseInteractionTrackingReturn => {
  const { user } = useAuth();

  /**
   * Generic track function
   */
  const track = useCallback(
    async (
      productId: number,
      interactionType: InteractionType,
      additionalContext?: Partial<InteractionContext>
    ) => {
      // Only track if user is logged in
      if (!user?.id) {
        return;
      }

      try {
        const sessionId = getSessionId();
        const context = buildInteractionContext(additionalContext);

        await logInteraction({
          userId: user.id,
          productId,
          interactionType,
          sessionId,
          context
        });
      } catch (error) {
        // Fail silently - don't disrupt user experience
        console.error(`Failed to track ${interactionType}:`, error);
      }
    },
    [user]
  );

  /**
   * Track product view
   */
  const trackProductView = useCallback(
    async (productId: number, context?: Partial<InteractionContext>) => {
      await track(productId, "VIEW", context);
    },
    [track]
  );

  /**
   * Track product click
   */
  const trackProductClick = useCallback(
    async (productId: number, context?: Partial<InteractionContext>) => {
      await track(productId, "CLICK", context);
    },
    [track]
  );

  /**
   * Track add to cart
   */
  const trackCartAdd = useCallback(
    async (productId: number, context?: Partial<InteractionContext>) => {
      await track(productId, "CART_ADD", context);
    },
    [track]
  );

  /**
   * Track purchase
   */
  const trackPurchase = useCallback(
    async (productId: number, context?: Partial<InteractionContext>) => {
      await track(productId, "PURCHASE", context);
    },
    [track]
  );

  /**
   * Track wishlist add
   */
  const trackWishlistAdd = useCallback(
    async (productId: number, context?: Partial<InteractionContext>) => {
      await track(productId, "WISHLIST", context);
    },
    [track]
  );

  /**
   * Track cart remove
   */
  const trackCartRemove = useCallback(
    async (productId: number, context?: Partial<InteractionContext>) => {
      await track(productId, "REMOVE_CART", context);
    },
    [track]
  );

  /**
   * Track wishlist remove
   */
  const trackWishlistRemove = useCallback(
    async (productId: number, context?: Partial<InteractionContext>) => {
      await track(productId, "REMOVE_WISHLIST", context);
    },
    [track]
  );

  return {
    trackProductView,
    trackProductClick,
    trackCartAdd,
    trackPurchase,
    trackWishlistAdd,
    trackCartRemove,
    trackWishlistRemove
  };
};

/**
 * Hook specifically for tracking page views
 */
export const usePageViewTracking = (productId?: number) => {
  const { trackProductView } = useInteractionTracking();

  // Track view when component mounts
  useCallback(() => {
    if (productId) {
      trackProductView(productId);
    }
  }, [productId, trackProductView]);
};

