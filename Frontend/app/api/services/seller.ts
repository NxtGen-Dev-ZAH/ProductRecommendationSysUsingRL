import apiClient from '../axios';
import { Product } from './product';
import { Order } from './order';

// Seller Product Management
export interface SellerProductRequest {
  name: string;
  description?: string;
  price: number;
  offerPrice?: number;
  quantity: number;
  categoryId: number;
  brand?: string;
  warranty?: string;
  inventoryLocation?: string;
  productCode?: string;
  manufacturingPieceNumber?: string;
  manufacturingDate?: string;
  expirationDate?: string;
  EAN?: string;
  manufacturingPlace?: string;
  productStatus: 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK';
  productSellType: 'DIRECT' | 'OFFER' | 'AUCTION' | 'PREORDER' | 'SUBSCRIPTION' | 'RENTAL' | 'BUNDLE' | 'DIGITAL';
  productCondition: 'NEW' | 'USED' | 'OPEN_NEVER_USED' | 'REFURBISHED';
  productConditionComment?: string;
}

export interface SellerProductImage {
  id?: number;
  fileName: string;
  fileUrl: string;
  contentType: string;
  fileSize: number;
  isPrimary: boolean;
  displayOrder: number;
}

export interface SellerProduct extends Omit<Product, 'images' | 'variants'> {
  images: SellerProductImage[];
  variants: ProductVariant[];
  productId: number;
  deleted: boolean;
  author: {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
  };
}

export interface ProductVariant {
  id?: number;
  name: string;
  priceAdjustment: number;
  quantity: number;
}

// Get seller's products
export const getSellerProducts = async (page = 0, size = 10, search?: string) => {
  const response = await apiClient.get('/seller/v1/products/all-seller-products', {
    params: { page, size, search }
  });
  return response.data;
};

// Search seller products
export const searchSellerProducts = async (query: string, page = 0, size = 10) => {
  const response = await apiClient.get('/seller/v1/products/search', {
    params: { query, page, size }
  });
  return response.data;
};

// Get company author products (paginated)
export const getCompanyAuthorProducts = async (page = 0, size = 10) => {
  const response = await apiClient.get('/seller/v1/products/all-company-author-products', {
    params: { page, size }
  });
  return response.data;
};

export const getSellerProductById = async (id: number): Promise<SellerProduct> => {
  const response = await apiClient.get<SellerProduct>(`/seller/product/${id}`);
  return response.data;
};

export const createSellerProduct = async (
  product: SellerProductRequest, 
  images?: File[]
): Promise<SellerProduct> => {
  const formData = new FormData();
  
  // Create a Blob with application/json Content-Type for the product part
  // Spring Boot's @RequestPart expects the part to have Content-Type: application/json
  const productBlob = new Blob([JSON.stringify(product)], { type: 'application/json' });
  formData.append('product', productBlob);
  
  if (images && images.length > 0) {
    images.forEach((image) => {
      formData.append('images', image);
    });
  }
  
  // Don't set Content-Type header - let the browser set it automatically with boundary
  const response = await apiClient.post<SellerProduct>('/seller/v1/products', formData);
  return response.data;
};

export const updateSellerProduct = async (
  id: number,
  product: Partial<SellerProductRequest>,
  newImages?: File[],
  imagesToRemove?: number[],
  primaryImageId?: number
): Promise<SellerProduct> => {
  const formData = new FormData();
  
  // Create a Blob with application/json Content-Type for the product part
  // Spring Boot's @RequestPart expects the part to have Content-Type: application/json
  const productBlob = new Blob([JSON.stringify(product)], { type: 'application/json' });
  formData.append('product', productBlob);
  
  if (newImages && newImages.length > 0) {
    newImages.forEach((image) => {
      formData.append('images', image);
    });
  }
  
  if (imagesToRemove && imagesToRemove.length > 0) {
    formData.append('imagesToRemove', JSON.stringify(imagesToRemove));
  }
  
  if (primaryImageId) {
    formData.append('primaryImageId', primaryImageId.toString());
  }
  
  // Don't set Content-Type header - let the browser set it automatically with boundary
  const response = await apiClient.put<SellerProduct>(`/seller/v1/products/${id}`, formData);
  return response.data;
};

export const deleteSellerProduct = async (id: number): Promise<void> => {
  await apiClient.delete(`/seller/v1/products/${id}`);
};

export const updateProductQuantity = async (id: number, quantity: number): Promise<SellerProduct> => {
  const response = await apiClient.patch<SellerProduct>(`/seller/v1/products/${id}/quantity`, { quantity });
  return response.data;
};

export const updateProductPrice = async (id: number, price: number): Promise<SellerProduct> => {
  const response = await apiClient.patch<SellerProduct>(`/seller/v1/products/${id}/price`, { price });
  return response.data;
};

// Update product images
export const updateProductImages = async (
  id: number,
  newImages?: File[],
  imagesToRemove?: number[],
  primaryImageId?: number
): Promise<SellerProduct> => {
  const formData = new FormData();
  
  if (newImages && newImages.length > 0) {
    newImages.forEach((image) => {
      formData.append('images', image);
    });
  }
  
  if (imagesToRemove && imagesToRemove.length > 0) {
    formData.append('imagesToRemove', JSON.stringify(imagesToRemove));
  }
  
  if (primaryImageId) {
    formData.append('primaryImageId', primaryImageId.toString());
  }
  
  // Don't set Content-Type header - let the browser set it automatically with boundary
  const response = await apiClient.patch<SellerProduct>(`/seller/v1/products/${id}/update/images`, formData);
  return response.data;
};

// Merge products to company
export const mergeProductsToCompany = async (companyId: number, productIds: number[]): Promise<void> => {
  await apiClient.post(`/seller/v1/products/merge-to-company/${companyId}`, { productIds });
};

// Seller Order Management
export interface SellerOrderFilters {
  status?: string;
  dateFrom?: string;
  dateTo?: string;
  productId?: number;
}

export const getSellerOrders = async (page = 0, size = 10, filters?: SellerOrderFilters) => {
  const response = await apiClient.get('/seller/orders', {
    params: {
      page,
      size,
      ...filters
    }
  });
  return response.data;
};

export const getSellerOrderById = async (id: number): Promise<Order> => {
  const response = await apiClient.get<Order>(`/seller/orders/${id}`);
  return response.data;
};

export const updateSellerOrderStatus = async (id: number, status: string): Promise<void> => {
  await apiClient.put(`/seller/orders/${id}/status`, { status });
};

export const fulfillOrder = async (
  id: number, 
  fulfillmentData: {
    trackingNumber?: string;
    shippingCarrier?: string;
    shippingMethod?: string;
    estimatedDelivery?: string;
  }
): Promise<void> => {
  await apiClient.post(`/seller/orders/${id}/fulfill`, fulfillmentData);
};

// Seller Analytics
export interface SellerDashboardStats {
  totalProducts: number;
  activeProducts: number;
  totalOrders: number;
  pendingOrders: number;
  totalRevenue: number;
  monthlyRevenue: number;
  averageOrderValue: number;
  topSellingProducts: Array<{
    id: number;
    name: string;
    soldQuantity: number;
    revenue: number;
  }>;
  recentOrders: Order[];
  lowStockProducts: SellerProduct[];
}

export const getSellerDashboardStats = async (): Promise<SellerDashboardStats> => {
  const response = await apiClient.get<SellerDashboardStats>('/seller/dashboard/stats');
  return response.data;
};

export interface SellerSalesData {
  date: string;
  sales: number;
  orders: number;
  revenue: number;
}

export const getSellerSalesAnalytics = async (
  period: 'day' | 'week' | 'month' | 'year' = 'month'
): Promise<SellerSalesData[]> => {
  const response = await apiClient.get<SellerSalesData[]>('/seller/analytics/sales', {
    params: { period }
  });
  return response.data;
};

export const getSellerProductAnalytics = async (productId?: number) => {
  const response = await apiClient.get('/seller/analytics/products', {
    params: { productId }
  });
  return response.data;
};

// Inventory Management
export interface InventoryItem {
  productId: number;
  productName: string;
  currentStock: number;
  reservedStock: number;
  availableStock: number;
  lowStockThreshold: number;
  isLowStock: boolean;
  lastUpdated: string;
}

export const getInventory = async (page = 0, size = 10, search?: string): Promise<{
  content: InventoryItem[];
  totalElements: number;
  totalPages: number;
}> => {
  const response = await apiClient.get('/seller/inventory', {
    params: { page, size, search }
  });
  return response.data;
};

export const updateStock = async (productId: number, quantity: number, reason?: string): Promise<void> => {
  await apiClient.post(`/seller/inventory/${productId}/update-stock`, {
    quantity,
    reason
  });
};

export const setLowStockAlert = async (productId: number, threshold: number): Promise<void> => {
  await apiClient.post(`/seller/inventory/${productId}/low-stock-alert`, {
    threshold
  });
};

// Seller Profile & Settings
export interface SellerProfile {
  businessName?: string;
  businessDescription?: string;
  businessAddress?: string;
  businessPhone?: string;
  businessEmail?: string;
  taxId?: string;
  businessLicense?: string;
  returnPolicy?: string;
  shippingPolicy?: string;
  termsAndConditions?: string;
  storeLogo?: string;
  storeBanner?: string;
}

export const getSellerProfile = async (): Promise<SellerProfile> => {
  const response = await apiClient.get<SellerProfile>('/seller/profile');
  return response.data;
};

export const updateSellerProfile = async (profile: Partial<SellerProfile>): Promise<SellerProfile> => {
  const response = await apiClient.put<SellerProfile>('/seller/profile', profile);
  return response.data;
};

export const uploadSellerLogo = async (file: File): Promise<string> => {
  const formData = new FormData();
  formData.append('logo', file);
  
  // Don't set Content-Type header - let the browser set it automatically with boundary
  const response = await apiClient.post<{ url: string }>('/seller/profile/logo', formData);
  return response.data.url;
};

export const uploadSellerBanner = async (file: File): Promise<string> => {
  const formData = new FormData();
  formData.append('banner', file);
  
  // Don't set Content-Type header - let the browser set it automatically with boundary
  const response = await apiClient.post<{ url: string }>('/seller/profile/banner', formData);
  return response.data.url;
};

// Customer Management
export interface CustomerData {
  id: number;
  name: string;
  email: string;
  totalOrders: number;
  totalSpent: number;
  lastOrderDate?: string;
  isReturningCustomer: boolean;
}

export const getSellerCustomers = async (page = 0, size = 10): Promise<{
  content: CustomerData[];
  totalElements: number;
  totalPages: number;
}> => {
  const response = await apiClient.get('/seller/customers', {
    params: { page, size }
  });
  return response.data;
};

// Reviews and Ratings
export interface ProductReview {
  id: number;
  productId: number;
  productName: string;
  customerName: string;
  rating: number;
  comment: string;
  createdAt: string;
  isResolved: boolean;
}

export const getProductReviews = async (
  page = 0, 
  size = 10, 
  productId?: number,
  rating?: number
): Promise<{
  content: ProductReview[];
  totalElements: number;
  totalPages: number;
  averageRating: number;
}> => {
  const response = await apiClient.get('/seller/reviews', {
    params: { page, size, productId, rating }
  });
  return response.data;
};

export const respondToReview = async (reviewId: number, response: string): Promise<void> => {
  await apiClient.post(`/seller/reviews/${reviewId}/respond`, { response });
};

// Bulk Operations
export const bulkUpdateProductStatus = async (productIds: number[], status: string): Promise<void> => {
  await apiClient.post('/seller/products/bulk-update-status', {
    productIds,
    status
  });
};

export const bulkUpdatePrices = async (updates: Array<{ productId: number; price: number }>): Promise<void> => {
  await apiClient.post('/seller/products/bulk-update-prices', { updates });
};

export const bulkUpdateStock = async (updates: Array<{ productId: number; quantity: number }>): Promise<void> => {
  await apiClient.post('/seller/products/bulk-update-stock', { updates });
}; 