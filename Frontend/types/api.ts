// =============================================
// Core E-commerce Types and Interfaces
// Matching Backend DTOs and Entities
// =============================================

// =============================================================================
// User & Authentication Types
// =============================================================================

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  roles: UserRole[];
  isActive?: boolean;
  isEmailVerified?: boolean;
  profilePicture?: string;
  dateJoined?: string;
  lastLogin?: string;
}

export interface UserRole {
  id: number;
  name: 'ROLE_BUYER' | 'ROLE_SELLER' | 'ROLE_APP_ADMIN';
  description?: string;
}

export interface UserCustomField {
  id: number;
  fieldKey: string;
  fieldValue: string;
  userId: number;
  fieldType?: 'STRING' | 'NUMBER' | 'BOOLEAN' | 'DATE';
}

export interface UserPrivacySettings {
  id: number;
  userId: number;
  showEmail: boolean;
  showPhone: boolean;
  showAddress: boolean;
  allowFollowing: boolean;
  showFavorites: boolean;
}

// =============================================================================
// Product Types
// =============================================================================

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  quantity: number;
  active: boolean;
  imageUrl: string;
  categoryId: number;
  categoryName?: string;
  sellerId?: number;
  companyId?: number;
  featured?: boolean;
  trending?: boolean;
  discount?: number;
  discountedPrice?: number;
  rating?: number;
  reviewCount?: number;
  sku?: string;
  weight?: number;
  dimensions?: ProductDimensions;
  tags?: string[];
  created?: string;
  updated?: string;
  images?: ProductImage[];
  variants?: ProductVariant[];
  reviews?: ProductReview[];
  statistics?: ProductStatistics;
}

export interface ProductImage {
  id: number;
  productId: number;
  imageUrl: string;
  imageName?: string;
  isPrimary?: boolean;
  thumbnailUrl?: string;
  sortOrder?: number;
  altText?: string;
}

export interface ProductVariant {
  id: number;
  productId: number;
  variantName: string;
  variantValue: string;
  price?: number;
  quantity?: number;
  sku?: string;
  isDefault?: boolean;
}

export interface ProductReview {
  id: number;
  productId: number;
  userId: number;
  userName?: string;
  userAvatar?: string;
  rating: number;
  comment: string;
  reviewDate: string;
  verified?: boolean;
  helpful?: number;
}

export interface ProductStatistics {
  id: number;
  productId: number;
  viewCount: number;
  purchaseCount: number;
  addToCartCount: number;
  wishlistCount: number;
  averageRating: number;
  reviewCount: number;
}

export interface ProductDimensions {
  length: number;
  width: number;
  height: number;
  unit: 'cm' | 'inch';
}

// =============================================================================
// Category Types
// =============================================================================

export interface Category {
  id: number;
  name: string;
  description: string;
  imageUrl: string;
  parentId?: number;
  featured?: boolean;
  active?: boolean;
  sortOrder?: number;
  subcategories?: Category[];
  productCount?: number;
  path?: string;
  level?: number;
}

export interface CategoryWithProducts extends Category {
  products: Product[];
}

// =============================================================================
// Address Management Types
// =============================================================================

export type AddressType = 'BILLING' | 'SHIPPING' | 'EXPEDITION' | 'CONTACT';

export interface Address {
  id: number;
  name: string;
  email?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  phoneNumber?: string;
  reference?: string;
  addressType: AddressType;
  userId?: number;
  companyId?: number;
  isDefault: boolean;
  version?: number;
}

export interface UserAddressRequest {
  name: string;
  email?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  phoneNumber?: string;
  reference?: string;
  addressType: AddressType;
  isDefault: boolean;
}

export interface UserAddressResponse extends Address {
  // Same as Address interface
}

export interface CompanyAddressRequest {
  name: string;
  email?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  phoneNumber?: string;
  reference?: string;
  addressType: AddressType;
  isDefault: boolean;
}

export interface CompanyAddressResponse extends Address {
  // Same as Address interface
}

// Order-specific address types removed - addresses are now managed separately
// and linked to orders via shippingAddressId and billingAddressId in OrderCheckoutRequest

// =============================================================================
// Cart & Order Types
// =============================================================================

export interface Cart {
  id: number;
  sessionId?: string;
  user?: User; // Updated from userId to full User object
  items: CartItem[];
  couponResponse?: CouponResponse; // Updated from coupon to couponResponse to match backend changes
  // Updated to match backend CartResponse structure from NewChanges.md
  subtotalPrice: number; // Matches backend CartResponse.subtotalPrice
  totalShippingCost: number; // Matches backend CartResponse.totalShippingCost
  totalDiscount: number; // Matches backend CartResponse.totalDiscount
  totalAmount: number; // Matches backend CartResponse.totalAmount
  created?: string;
  updated?: string;
  // Legacy fields for backward compatibility (deprecated - use new fields above)
  subtotal?: number; // Deprecated - use subtotalPrice
  discountAmount?: number; // Deprecated - use totalDiscount
}

export interface CartItem {
  id: number;
  productId: number; // Matches backend CartItemResponse.productId
  productName: string; // Matches backend CartItemResponse.productName
  price: number; // Matches backend CartItemResponse.price
  quantity: number; // Matches backend CartItemResponse.quantity
}

// Backend response structure for applied coupon
export interface AppliedCouponResponse {
  code: string; // NEW FIELD - matches backend AppliedCouponResponse.code
  discount: number;
  cartResponse: Cart;
}

export interface CouponRequest {
  code: string; // Matches backend CouponRequest.code
}

export interface AddToCartRequest {
  productId: number;
  quantity: number;
}

export interface UpdateCartItemRequest {
  quantity: number;
}

export interface MergeCartRequest {
  emailAddress: string;
  password: string;
}

export interface MergeCartResponse {
  authResponse: {
    token: string;
    refreshToken?: string;
    user: {
      id: number;
      email: string;
      firstName: string;
      lastName: string;
      roles: string[];
      isActive?: boolean;
      isEmailVerified?: boolean;
    };
  };
  cartResponse: Cart;
}

// Cart total response for authenticated users
export interface CartTotalResponse {
  total: number;
  discount: number;
}

export interface Coupon {
  id: number;
  code: string;
  description: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  minimumOrderAmount?: number;
  maxDiscountAmount?: number;
  startDate: string;
  endDate: string;
  usageLimit?: number;
  usedCount?: number;
  active: boolean;
  applicableCategories?: number[];
  applicableProducts?: number[];
  sellerId?: number; // Added for seller-specific coupons
  state?: 'ACTIVE' | 'INACTIVE' | 'DELETED'; // Added for coupon state management
}

// NEW: CouponResponse interface to match backend CartResponse.couponResponse structure
export interface CouponResponse {
  id: number;
  code: string;
  description: string;
  state: 'ACTIVE' | 'INACTIVE' | 'DELETED';
  category?: string;
  couponScope: 'GLOBAL' | 'CATEGORY' | 'PRODUCT' | 'SELLER';
  couponType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  minimumOrderAmount?: number;
  maxUses?: number;
  maxUsesPerUser?: number;
  authorId?: number;
  startFrom?: string;
  endAt?: string;
  discountPercentage?: number;
  discountFixedAmount?: number;
  couponTrackings?: any[]; // Array of coupon tracking objects
}

// New seller coupon management types
export interface SellerCouponRequest {
  code: string;
  description: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  discountValue: number;
  minimumOrderAmount?: number;
  maxDiscountAmount?: number;
  startDate: string;
  endDate: string;
  usageLimit?: number;
  applicableCategories?: number[];
  applicableProducts?: number[];
}

export interface SellerCouponResponse extends Coupon {
  // Same as Coupon interface
}

export interface SellerCouponListResponse {
  coupons: SellerCouponResponse[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface Order {
  id: number;
  userId: number;
  orderNumber?: string;
  orderDate: string;
  status: OrderStatus;
  items: OrderItem[];
  subtotal: number;
  tax?: number;
  taxRate?: number;
  shipping: number;
  total: number;
  discountAmount?: number;
  couponIdentifier?: string;
  paymentMethod: string;
  paymentStatus?: PaymentStatus;
  paymentId?: string;
  shippingAddress: Address; // Updated to use new Address type
  billingAddress?: Address; // Updated to use new Address type
  notes?: string;
  trackingNumber?: string;
  estimatedDelivery?: string;
  deliveredAt?: string;
  cancelledAt?: string;
  refundAmount?: number;
  refundReason?: string;
}

export interface OrderItem {
  id: number;
  orderId: number;
  productId: number;
  productName: string;
  productImage?: string;
  productSku?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  discountAmount?: number;
  variantInfo?: string;
}

export type OrderStatus = 
  | 'PENDING' 
  | 'CONFIRMED' 
  | 'PROCESSING' 
  | 'SHIPPED' 
  | 'OUT_FOR_DELIVERY'
  | 'DELIVERED' 
  | 'CANCELLED' 
  | 'REFUNDED'
  | 'RETURNED';

export type PaymentStatus = 
  | 'PENDING' 
  | 'PROCESSING' 
  | 'COMPLETED' 
  | 'FAILED' 
  | 'CANCELLED' 
  | 'REFUNDED';

// Legacy types - kept for backward compatibility
export interface ShippingAddress {
  id?: number;
  firstName: string;
  lastName: string;
  company?: string;
  email?: string;
  phone: string;
  address: string;
  address2?: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  isDefault?: boolean;
}

export interface BillingAddress extends ShippingAddress {
  // Same structure as shipping address
}

export interface OrderShippingDetails {
  id: number;
  orderId: number;
  carrier: string;
  trackingNumber: string;
  shippingMethod: string;
  estimatedDelivery: string;
  actualDelivery?: string;
  shippingCost: number;
  status: 'PENDING' | 'SHIPPED' | 'IN_TRANSIT' | 'DELIVERED';
}

// =============================================================================
// Payment Types
// =============================================================================

export interface Payment {
  id: number;
  orderId: number;
  amount: number;
  currency: string;
  method: PaymentMethod;
  status: PaymentStatus;
  paymentDate: string;
  transactionId?: string;
  gatewayResponse?: any;
  refundAmount?: number;
  refundDate?: string;
}

export type PaymentMethod = 
  | 'CREDIT_CARD' 
  | 'DEBIT_CARD' 
  | 'PAYPAL' 
  | 'STRIPE' 
  | 'BANK_TRANSFER' 
  | 'CASH_ON_DELIVERY';

export interface PaymentRequest {
  orderId: number;
  amount: number;
  currency: string;
  method: PaymentMethod;
  paymentDetails?: any;
}

export interface StripePaymentRequest {
  amount: number;
  currency: string;
  description: string;
  orderId?: number;
  customerId?: string;
}

export interface StripePaymentResponse {
  clientSecret: string;
  sessionId?: string;
  paymentIntentId?: string;
}

// =============================================================================
// Company & Seller Types
// =============================================================================

export interface Company {
  id: number;
  name: string;
  description: string;
  email: string;
  phone: string;
  address: string;
  website?: string;
  logoUrl?: string;
  taxId?: string;
  registrationNumber?: string;
  isActive: boolean;
  created: string;
  ownerId: number;
  employees?: CompanyEmployee[];
}

export interface CompanyEmployee {
  id: number;
  companyId: number;
  userId: number;
  role: CompanyRole;
  joinedDate: string;
  isActive: boolean;
  permissions?: string[];
}

export type CompanyRole = 'OWNER' | 'ADMIN' | 'MANAGER' | 'EMPLOYEE';

// =============================================================================
// Audit & System Types
// =============================================================================

export interface AuditLog {
  id: number;
  userId: number;
  action: string;
  entityType: string;
  entityId: number;
  oldValue?: string;
  newValue?: string;
  timestamp: string;
  ipAddress?: string;
  userAgent?: string;
}

export interface ApprovalToken {
  id: number;
  userId: number;
  tokenType: 'EMAIL_VERIFICATION' | 'PASSWORD_RESET' | 'ACCOUNT_ACTIVATION';
  token: string;
  expiresAt: string;
  used: boolean;
  createdAt: string;
}

// =============================================================================
// Search & Filter Types
// =============================================================================

export interface SearchParams {
  query?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  rating?: number;
  sortBy?: SortOption;
  sortOrder?: 'ASC' | 'DESC';
  page?: number;
  size?: number;
  filters?: SearchFilter[];
}

export type SortOption = 
  | 'RELEVANCE' 
  | 'PRICE' 
  | 'NAME' 
  | 'RATING' 
  | 'NEWEST' 
  | 'POPULARITY';

export interface SearchFilter {
  field: string;
  operator: 'EQUALS' | 'CONTAINS' | 'GREATER_THAN' | 'LESS_THAN' | 'BETWEEN';
  value: any;
}

export interface SearchResult<T> {
  items: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

// =============================================================================
// API Response Types
// =============================================================================

export interface ApiResponse<T> {
  data: T;
  message: string;
  status: number;
  timestamp: string;
}

export interface ApiError {
  error: string;
  message: string;
  status: number;
  timestamp: string;
  path?: string;
  details?: any;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

// =============================================================================
// Request/Response DTOs
// =============================================================================

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  confirmPassword?: string;
  acceptTerms?: boolean;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  expiresIn?: number;
  user: User;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  email?: string;
  phone?: string;
  profilePicture?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

// =============================================================================
// Utility Types
// =============================================================================

export type ApiEndpoint = string;
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export interface RequestConfig {
  method: HttpMethod;
  url: ApiEndpoint;
  data?: any;
  params?: any;
  headers?: Record<string, string>;
}

// Export all types as a namespace as well
export namespace ApiTypes {
  export type TUser = User;
  export type TProduct = Product;
  export type TCategory = Category;
  export type TCart = Cart;
  export type TOrder = Order;
  export type TPayment = Payment;
  export type TCompany = Company;
}
