// =============================================
// API Services Index
// Central export point for all API services
// =============================================

// Core services - explicit re-exports to avoid naming conflicts
export { 
  loginUser, 
  registerUser, 
  logoutUser, 
  refreshAuthToken,
  requestPasswordReset,
  resetPassword,
  activateAccount,
  getCurrentUser,
  isAuthenticated,
  storeAuthData,
  getStoredUser,
  getStoredToken,
  getStoredRefreshToken
} from './auth';

export { 
  getAllProducts, 
  getProductById, 
  searchProducts,
  getProductVariants,
  getProductImages,
  createProduct,
  updateProduct,
  deleteProduct,
  updateProductQuantity,
  getFeaturedProducts,
  getNewArrivals,
  getBestSellers,
  getOnSaleProducts,
  getRelatedProducts,
  getRecommendedProducts,
  getTrendingProducts
} from './product';

export { 
  getAllCategories, 
  getPaginatedCategories, 
  getCategoryProductSelection 
} from './category';

export { 
  getCart, 
  getCartTotal, 
  addToCart, 
  updateCartItem, 
  removeFromCart, 
  clearCart, 
  applyCoupon, 
  removeCoupon,
  mergeCartOnLogin
} from './cart';

export { 
  searchProducts as searchProductsFromSearch 
} from './search';

// User & Order services
export { 
  getCurrentUserProfile, 
  updateUserProfile, 
  updateUserPassword, 
  deleteUserAccount,
  getPaymentMethods,
  addPaymentMethod,
  updatePaymentMethod,
  deletePaymentMethod,
  getAllCustomFields,
  createCustomField,
  updateCustomField,
  deleteCustomField
} from './user';

export { 
  getOrder, 
  createOrder, 
  cancelOrder
} from './order';

// Address Management services
export {
  getUserAddresses,
  addUserAddress,
  updateUserAddress,
  deleteUserAddress,
  getUserAddress,
  getCompanyAddresses,
  addCompanyAddress,
  updateCompanyAddress,
  deleteCompanyAddress,
  getCompanyAddress,
  getDefaultUserAddress,
  getDefaultCompanyAddress,
  formatAddress,
  validateAddress
} from './address';

export { 
  processPayment, 
  createPaymentIntent
} from './payment';

export { 
  getWishlist, 
  addToWishlist, 
  removeFromWishlist, 
  toggleFavoriteProduct
} from './wishlist';

// Business services - only export functions that actually exist
export { 
  getAllVendors, 
  getVendorById
} from './vendor';

export { 
  getSellerProducts, 
  getSellerOrders
} from './seller';

export { 
  // Admin functions - export only what exists
} from './admin';

export { 
  // Coupon functions - export only what exists
} from './coupon';

// Axios client
export { default as apiClient } from '../axios';

// Re-export types for convenience
export * from '../../../types/api';

// Import all services for organized exports
import * as authServices from './auth';
import * as productServices from './product';
import * as cartServices from './cart';
import * as orderServices from './order';
import * as paymentServices from './payment';
import * as userServices from './user';
import * as addressServices from './address';
import * as searchServices from './search';
import * as categoryServices from './category';
import * as wishlistServices from './wishlist';
import * as vendorServices from './vendor';
import * as couponServices from './coupon';

// Service collections for organized imports
export const services = {
  auth: authServices,
  product: productServices,
  category: categoryServices,
  cart: cartServices,
  order: orderServices,
  payment: paymentServices,
  user: userServices,
  address: addressServices,
  search: searchServices,
  wishlist: wishlistServices,
  vendor: vendorServices,
  coupon: couponServices
};

// Default export with all services grouped
export default services;
