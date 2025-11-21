"use client";

import {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
} from "react";
import {
  getCart,
  addToCart,
  updateCartItem,
  removeFromCart,
  clearCart as clearCartApi,
  applyCoupon,
  removeCoupon,
} from "../api/services/cart";
import { Cart, AppliedCouponResponse } from "../../types/api";
import { useAuth } from "./AuthContext";

// Error type for API responses
interface ApiError {
  response?: {
    status?: number;
    data?: {
      message?: string;
    };
  };
  message?: string;
}

interface CartContextType {
  cart: Cart | null;
  loading: boolean;
  itemCount: number;
  addItem: (productId: number, quantity: number) => Promise<void>;
  removeItem: (itemId: number) => Promise<void>;
  updateItem: (itemId: number, quantity: number) => Promise<void>;
  clearCart: () => Promise<void>;
  applyCouponToCart: (couponCode: string) => Promise<void>;
  removeCouponFromCart: () => Promise<void>;
  refreshCart: () => Promise<void>;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export const CartProvider = ({ children }: { children: ReactNode }) => {
  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(false);
  const [initialized, setInitialized] = useState(false);
  const { isAuthenticated } = useAuth();

  const itemCount =
    cart?.items?.reduce((total, item) => total + item.quantity, 0) || 0;

  // Load cart when user is authenticated or has cart session
  useEffect(() => {
    if (!initialized) {
      fetchCart();
      setInitialized(true);
    }
  }, [initialized]);

  // Refresh cart when authentication status changes, but only once
  useEffect(() => {
    if (initialized) {
      // Add a small delay to prevent rapid re-fetching
      const timeoutId = setTimeout(() => {
        fetchCart();
      }, 100);

      return () => clearTimeout(timeoutId);
    }
  }, [isAuthenticated, initialized]);

  const fetchCart = async () => {
    setLoading(true);
    try {
      console.log("Fetching cart...");
      const cartData = await getCart();
      console.log("Cart fetched:", cartData);
      setCart(cartData);
    } catch (err: unknown) {
      const apiError = err as ApiError;
      console.error("Error fetching cart:", apiError);

      // Don't set error for cart loading failures as it's normal for new users
      // Set empty cart instead of error
      setCart({
        id: 0,
        sessionId: undefined,
        user: undefined,
        items: [],
        couponResponse: undefined,
        subtotalPrice: 0,
        totalShippingCost: 0,
        totalDiscount: 0,
        totalAmount: 0,
      });
    } finally {
      setLoading(false);
    }
  };

  const addItem = async (productId: number, quantity: number) => {
    setLoading(true);
    try {
      console.log("Adding item to cart:", { productId, quantity });
      const updatedCart = await addToCart(productId, quantity);
      console.log("Cart updated after adding item:", updatedCart);

      // Update cart state with the response from the API
      setCart(updatedCart);

      // Don't call fetchCart() here - use the response directly
    } catch (err: unknown) {
      const apiError = err as ApiError;
      console.error("Error adding item to cart:", apiError);
      const errorMessage =
        apiError.response?.data?.message || "Failed to add item to cart";
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const updateItem = async (itemId: number, quantity: number) => {
    setLoading(true);
    try {
      console.log("Updating cart item:", { itemId, quantity });
      const updatedCart = await updateCartItem(itemId, quantity);
      console.log("Cart updated after updating item:", updatedCart);

      // Update cart state with the response from the API
      setCart(updatedCart);

      // Don't call fetchCart() here - use the response directly
    } catch (err: unknown) {
      const apiError = err as ApiError;
      console.error("Error updating cart item:", apiError);
      const errorMessage =
        apiError.response?.data?.message || "Failed to update cart item";
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const removeItem = async (itemId: number) => {
    setLoading(true);
    try {
      console.log("Removing item from cart:", { itemId });
      const updatedCart = await removeFromCart(itemId);
      console.log("Cart updated after removing item:", updatedCart);

      // Update cart state with the response from the API
      setCart(updatedCart);

      // Don't call fetchCart() here - use the response directly
    } catch (err: unknown) {
      const apiError = err as ApiError;
      console.error("Error removing item from cart:", apiError);
      const errorMessage =
        apiError.response?.data?.message || "Failed to remove item from cart";
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const clearCart = async () => {
    setLoading(true);
    try {
      console.log("Clearing cart...");
      await clearCartApi();
      setCart({
        id: 0,
        sessionId: undefined,
        user: undefined,
        items: [],
        couponResponse: undefined,
        subtotalPrice: 0,
        totalShippingCost: 0,
        totalDiscount: 0,
        totalAmount: 0,
      });
    } catch (err: unknown) {
      const apiError = err as ApiError;
      console.error("Error clearing cart:", apiError);
      const errorMessage =
        apiError.response?.data?.message || "Failed to clear cart";
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const applyCouponToCart = async (couponCode: string) => {
    setLoading(true);
    try {
      console.log("Applying coupon to cart:", { couponCode });
      const appliedCouponResponse: AppliedCouponResponse = await applyCoupon(
        couponCode
      );
      console.log("Cart updated after applying coupon:", appliedCouponResponse);

      // Log the new response structure with code field
      console.log("Applied coupon code:", appliedCouponResponse.code);
      console.log("Discount amount:", appliedCouponResponse.discount);

      setCart(appliedCouponResponse.cartResponse);
    } catch (err: unknown) {
      const apiError = err as ApiError;
      console.error("Error applying coupon:", apiError);
      // Enhanced error handling for coupon-specific errors
      let errorMessage = "Failed to apply coupon";

      if (apiError.response?.status === 422) {
        errorMessage =
          apiError.response?.data?.message ||
          "Coupon is invalid, expired, or usage limit exceeded";
      } else if (apiError.response?.status === 404) {
        errorMessage = "Coupon not found";
      } else if (apiError.response?.data?.message) {
        errorMessage = apiError.response.data.message;
      }

      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const removeCouponFromCart = async () => {
    setLoading(true);
    try {
      console.log("Removing coupon from cart...");
      const updatedCart = await removeCoupon();
      console.log("Cart updated after removing coupon:", updatedCart);
      setCart(updatedCart);
    } catch (err: unknown) {
      const apiError = err as ApiError;
      console.error("Error removing coupon:", apiError);
      const errorMessage =
        apiError.response?.data?.message || "Failed to remove coupon";
      throw new Error(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const refreshCart = async () => {
    await fetchCart();
  };

  return (
    <CartContext.Provider
      value={{
        cart,
        loading,
        itemCount,
        addItem,
        updateItem,
        removeItem,
        clearCart,
        applyCouponToCart,
        removeCouponFromCart,
        refreshCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (context === undefined) {
    throw new Error("useCart must be used within a CartProvider");
  }
  return context;
};
