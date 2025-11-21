import apiClient from '../axios';
import { Product } from './product';

export interface WishlistItem {
  id: number;
  productId: number;
  userId: number;
  addedAt: string;
  product?: Product;
}

// In a real app, this would be tied to the authenticated user
// For demo purposes, we'll use a fixed userId and simulate the API
// const DEMO_USER_ID = 1; // Commented out as it's not currently used

// Get wishlist items for the authenticated user
export const getWishlistItems = async (): Promise<WishlistItem[]> => {
  try {
    const response = await apiClient.get<WishlistItem[]>('/buyer/profile/me/favorites');
    return response.data;
  } catch (error) {
    console.error('Error fetching wishlist items:', error);
    return [];
  }
};

// Export alias for backwards compatibility
export const getWishlist = getWishlistItems;

// Toggle favorite product (add/remove from wishlist)
export const toggleFavoriteProduct = async (productId: number): Promise<{ isFavorited: boolean }> => {
  try {
    const response = await apiClient.post(`/buyer/profile/toggle-favorite/${productId}`);
    return response.data;
  } catch (error) {
    console.error('Error toggling favorite product:', error);
    throw error;
  }
};

// Add a product to the wishlist
export const addToWishlist = async (productId: number): Promise<WishlistItem> => {
  try {
    const result = await toggleFavoriteProduct(productId);
    if (result.isFavorited) {
      // Return a mock wishlist item since the API doesn't return the full item
      return {
        id: Date.now(), // Use timestamp as ID
        productId,
        userId: 0, // Will be filled by backend
        addedAt: new Date().toISOString()
      };
    } else {
      throw new Error('Product was removed from favorites instead of added');
    }
  } catch (error) {
    console.error('Error adding to wishlist:', error);
    throw error;
  }
};

// Remove a product from the wishlist
export const removeFromWishlist = async (productId: number): Promise<void> => {
  try {
    const result = await toggleFavoriteProduct(productId);
    if (result.isFavorited) {
      throw new Error('Product was added to favorites instead of removed');
    }
  } catch (error) {
    console.error('Error removing from wishlist:', error);
    throw error;
  }
};

// Check if a product is in the wishlist
export const isInWishlist = async (productId: number): Promise<boolean> => {
  try {
    const wishlistItems = await getWishlistItems();
    return wishlistItems.some(item => item.productId === productId);
  } catch (error) {
    console.error('Error checking wishlist status:', error);
    return false;
  }
}; 