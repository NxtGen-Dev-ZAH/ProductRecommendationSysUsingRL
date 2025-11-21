import apiClient from '../axios';
import { getProductPlaceholder } from '../../../utils/placeholder';

export interface Product {
  id: number;
  name: string;
  description?: string;
  price: number;
  offerPrice?: number;
  quantity: number;
  inventoryLocation?: string;
  warranty?: string;
  brand?: string;
  productCode?: string;
  manufacturingPieceNumber?: string;
  manufacturingDate?: string;
  expirationDate?: string;
  manufacturingPlace?: string;
  authorId?: number;
  companyId?: number;
  categoryId: number;
  productStatus?: string;
  productSellType?: string;
  productCondition?: string;
  productConditionComment?: string;
  createdAt?: string;
  updatedAt?: string;
  active?: boolean;
  imageUrl?: string;
  categoryName?: string;
  featured?: boolean;
  trending?: boolean;
  discount?: number;
  images?: ProductImage[];
  imageAttaches?: ProductImageAttach[];
  variants?: ProductVariant[];
  sellerId?: number;
  ean?: string;
}

export interface ProductImage {
  id: number;
  productId: number;
  imageUrl: string;
  imageName?: string;
  isPrimary?: boolean;
  thumbnailUrl?: string;
}

export interface ProductImageAttach {
  id: number;
  fileName: string;
  fileContent: string; // Base64 encoded image data
  contentType: string;
  fileSize: number;
  fileExtension: string;
  createdAt: string;
  displayOrder: number;
  primary: boolean;
}

export interface ProductVariant {
  id: number;
  productId: number;
  variantName: string;
  variantValue: string;
  price?: number;
  quantity?: number;
}

export interface ProductSearchParams {
  name?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  quantity: number;
  active: boolean;
  imageUrl: string;
  categoryId: number;
  featured?: boolean;
  trending?: boolean;
  discount?: number;
}

// Define the pagination response structure
export interface PageResponse<T> {
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

// Helper function to get the primary image URL from a product
// Helper function to validate base64 data
const isValidBase64 = (str: string): boolean => {
  try {
    return btoa(atob(str)) === str;
  } catch {
    return false;
  }
};

export const getProductImageUrl = (product: Product | { imageAttaches?: ProductImageAttach[]; imageUrl?: string }): string => {

  // Prefer base64 image attaches only when they actually contain data
  if (product?.imageAttaches && Array.isArray(product.imageAttaches) && product.imageAttaches.length > 0) {
    const primaryImage =
      product.imageAttaches.find((img: ProductImageAttach) => img?.primary) || product.imageAttaches[0];


    if (primaryImage?.contentType && primaryImage?.fileContent && isValidBase64(primaryImage.fileContent)) {
      const dataUrl = `data:${primaryImage.contentType};base64,${primaryImage.fileContent}`;
      return dataUrl;
    } else {
      console.log('Base64 validation failed or missing data');
    }
  }

  // Fallback to regular imageUrl if present
  if (product?.imageUrl) {
    console.log('Falling back to imageUrl:', product.imageUrl);
    return product.imageUrl;
  }

  // As a last resort, show a placeholder
  console.log('Using placeholder image');
  return getProductPlaceholder();
};

export const getAllProducts = async (): Promise<Product[]> => {
  try {
    const response = await apiClient.get<PageResponse<Product>>('/api/v1/products', {
      params: {
        page: 0,
        size: 50 // Get more products for homepage
      }
    });
    return response.data.content; // Extract the content array from the pagination response
  } catch (error) {
    console.error('Error fetching products:', error);
    // Return empty array if API fails
    return [];
  }
};

/**
 * Get featured products
 * @param limit - Maximum number of products to return (default: 6, max: 20)
 */
export const getFeaturedProducts = async (limit: number = 6): Promise<Product[]> => {
  try {
    const response = await apiClient.get<Product[]>(`/api/v1/products/featured`, {
      params: { limit: Math.min(limit, 20) }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching featured products:', error);
    return [];
  }
};

/**
 * Get new arrival products (recently added)
 * @param limit - Maximum number of products to return (default: 8, max: 20)
 */
export const getNewArrivals = async (limit: number = 8): Promise<Product[]> => {
  try {
    const response = await apiClient.get<Product[]>(`/api/v1/products/new-arrivals`, {
      params: { limit: Math.min(limit, 20) }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching new arrivals:', error);
    return [];
  }
};

/**
 * Get best selling products
 * @param limit - Maximum number of products to return (default: 8, max: 20)
 */
export const getBestSellers = async (limit: number = 8): Promise<Product[]> => {
  try {
    const response = await apiClient.get<Product[]>(`/api/v1/products/best-sellers`, {
      params: { limit: Math.min(limit, 20) }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching best sellers:', error);
    return [];
  }
};

/**
 * Get products on sale (paginated)
 * @param page - Page number (default: 0)
 * @param size - Page size (default: 10)
 */
export const getOnSaleProducts = async (page: number = 0, size: number = 10): Promise<PageResponse<Product>> => {
  try {
    const response = await apiClient.get<PageResponse<Product>>(`/api/v1/products/on-sale`, {
      params: { page, size }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching on-sale products:', error);
    return {
      content: [],
      totalElements: 0,
      totalPages: 0,
      size: 0,
      number: 0,
      first: true,
      last: true,
      numberOfElements: 0,
      empty: true
    };
  }
};

/**
 * Get related products for a specific product
 * @param productId - The product ID to get related products for
 * @param limit - Maximum number of products to return (default: 4, max: 10)
 */
export const getRelatedProducts = async (productId: number, limit: number = 4): Promise<Product[]> => {
  try {
    const response = await apiClient.get<Product[]>(`/api/v1/products/${productId}/related`, {
      params: { limit: Math.min(limit, 10) }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching related products:', error);
    return [];
  }
};

/**
 * Get personalized product recommendations
 * @param limit - Maximum number of products to return (default: 6, max: 20)
 * @returns Empty array for unauthenticated users
 */
export const getRecommendedProducts = async (limit: number = 6): Promise<Product[]> => {
  try {
    const response = await apiClient.get<Product[]>(`/api/v1/products/recommendations`, {
      params: { limit: Math.min(limit, 20) }
    });
    return response.data || [];
  } catch (error) {
    // Returns empty array for unauthenticated users or on error
    console.error('Error fetching recommendations:', error);
    return [];
  }
};

/**
 * Get trending products (legacy - kept for backward compatibility)
 * @deprecated Use getBestSellers() or getNewArrivals() instead
 */
export const getTrendingProducts = async (): Promise<Product[]> => {
  // For backward compatibility, return best sellers
  return getBestSellers(8);
};

export const getProductById = async (id: number): Promise<Product> => {
  const response = await apiClient.get<Product>(`/api/v1/products/${id}`);
  return response.data;
};

// Get product variants
export const getProductVariants = async (productId: number): Promise<ProductVariant[]> => {
  const response = await apiClient.get<ProductVariant[]>(`/api/v1/products/${productId}/variants`);
  return response.data;
};

// Get all product images
export const getProductImages = async (productId: number): Promise<ProductImage[]> => {
  const response = await apiClient.get<ProductImage[]>(`/api/v1/products/${productId}/images`);
  return response.data;
};

// Get product primary image
export const getProductPrimaryImage = async (productId: number): Promise<ProductImage> => {
  const response = await apiClient.get<ProductImage>(`/api/v1/products/${productId}/primary-image`);
  return response.data;
};

// Get specific product image
export const getProductImage = async (productId: number, imageId: number): Promise<ProductImage> => {
  const response = await apiClient.get<ProductImage>(`/api/v1/products/${productId}/images/${imageId}`);
  return response.data;
};

// Get product thumbnail
export const getProductThumbnail = async (productId: number): Promise<string> => {
  const response = await apiClient.get<string>(`/api/v1/products/${productId}/images/thumbnail`);
  return response.data;
};

// Get image by ID
export const getImageById = async (imageId: number): Promise<ProductImage> => {
  const response = await apiClient.get<ProductImage>(`/api/v1/products/images/${imageId}`);
  return response.data;
};

// Get image thumbnail by ID
export const getImageThumbnail = async (imageId: number): Promise<string> => {
  const response = await apiClient.get<string>(`/api/v1/products/images/${imageId}/thumbnail`);
  return response.data;
};

// Search products by name
export const searchProductsByName = async (name: string): Promise<Product[]> => {
  const response = await apiClient.get<PageResponse<Product>>(`/api/v1/products/search`, {
    params: { name }
  });
  return response.data.content; // Extract the content array from the pagination response
};

// Get products by category
export const getProductsByCategory = async (categoryId: number): Promise<Product[]> => {
  try {
    const response = await apiClient.get<PageResponse<Product>>(`/api/v1/products/category/${categoryId}`, {
      params: {
        page: 0,
        size: 50 // Request more products and ensure proper data loading
      }
    });
    return response.data.content; // Extract the content array from the pagination response
  } catch (error) {
    console.error('Error fetching products by category:', error);
    return [];
  }
};

// Advanced product search with filters
export const searchProducts = async (params: ProductSearchParams): Promise<Product[]> => {
  const response = await apiClient.get<PageResponse<Product>>('/api/v1/products/search', {
    params
  });
  return response.data.content; // Extract the content array from the pagination response
};

// Note: These endpoints are for sellers/admins and require authentication
// Keeping them here for completeness but they won't work for public access
export const createProduct = async (product: ProductRequest): Promise<Product> => {
  const response = await apiClient.post<Product>('/seller/v1/products', product);
  return response.data;
};

export const updateProduct = async (id: number, product: ProductRequest): Promise<Product> => {
  const response = await apiClient.put<Product>(`/seller/v1/products/${id}`, product);
  return response.data;
};

export const deleteProduct = async (id: number): Promise<void> => {
  await apiClient.delete(`/seller/v1/products/${id}`);
};

export const updateProductQuantity = async (id: number, quantity: number): Promise<Product> => {
  const response = await apiClient.patch<Product>(`/seller/v1/products/${id}/quantity`, { quantity });
  return response.data;
}; 