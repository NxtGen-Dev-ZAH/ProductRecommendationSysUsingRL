import apiClient from '../axios';
import { Product, getProductsByCategory } from './product';

export interface Category {
  id: number;
  name: string;
  description: string;
  imageUrl: string;
  imageContent?: string;
  imageContentType?: string;
  createdDate?: string;
  parentId?: number;
  subcategories?: Category[];
  
  // Frontend-specific fields for compatibility
  featured?: boolean;
  productCount?: number;
  sortOrder?: number;
}

export interface CategoryWithProducts extends Category {
  products: Product[];
}

export interface CategorySearchParams {
  name?: string;
  parentId?: number;
  page?: number;
  size?: number;
  sort?: string;
}

export interface PaginatedCategoriesResponse {
  categories: Category[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  size: number;
}

export const getAllCategories = async (): Promise<Category[]> => {
  try {
    const response = await apiClient.get<Category[]>('/api/category');
    return response.data;
  } catch (error) {
    console.error('Error fetching categories:', error);
    // Return empty array if API fails
    return [];
  }
};

// Get paginated categories
export const getPaginatedCategories = async (page: number = 0, size: number = 20): Promise<PaginatedCategoriesResponse> => {
  const response = await apiClient.get<PaginatedCategoriesResponse>('/api/category/pages', {
    params: { page, size }
  });
  return response.data;
};

// Search categories
export const searchCategories = async (params: CategorySearchParams): Promise<Category[]> => {
  const response = await apiClient.get<Category[]>('/api/category/search', {
    params
  });
  return response.data;
};

// Get sorted categories
export const getSortedCategories = async (sortBy: string = 'name'): Promise<Category[]> => {
  const response = await apiClient.get<Category[]>('/api/category/sort', {
    params: { sortBy }
  });
  return response.data;
};

// Get parent categories
export const getParentCategories = async (): Promise<Category[]> => {
  const response = await apiClient.get<Category[]>('/api/category/parents');
  return response.data;
};

// Get product selection for category
export const getCategoryProductSelection = async (categoryId?: number): Promise<Product[]> => {
  const response = await apiClient.get<Product[]>('/api/category/product-selection', {
    params: categoryId ? { categoryId } : {}
  });
  return response.data;
};

export const getFeaturedCategories = async (): Promise<Category[]> => {
  // In a real API, this would be a specific endpoint
  // For now, we'll simulate by marking some categories as featured
  const categories = await getAllCategories();
  
  // Mark some categories as featured for demonstration
  return categories.map((category, index) => ({
    ...category,
    featured: index % 2 === 0, // Every other category is featured
  })).filter(category => category.featured);
};

export const getCategoryById = async (id: number): Promise<Category> => {
  const response = await apiClient.get<Category>(`/api/category/${id}`);
  return response.data;
};

// Get subcategories
export const getSubcategories = async (parentId: number): Promise<Category[]> => {
  const response = await apiClient.get<Category[]>(`/api/category/${parentId}/subcategories`);
  return response.data;
};

// Note: This endpoint may not exist in the actual API based on documentation
// The fallback uses the correct products endpoint from ProductController
export const getCategoryWithProducts = async (id: number): Promise<CategoryWithProducts> => {
  // Use the documented products-by-category endpoint directly
  const [category, products] = await Promise.all([
    getCategoryById(id),
    getProductsByCategory(id)
  ]);
  return {
    ...category,
    products
  };
};

// Note: These endpoints are for admins only and require authentication
// They are in the admin namespace
export const createCategory = async (category: Omit<Category, 'id'>): Promise<Category> => {
  const response = await apiClient.post<Category>('/admin/category', category);
  return response.data;
};

export const updateCategory = async (id: number, category: Omit<Category, 'id'>): Promise<Category> => {
  const response = await apiClient.put<Category>(`/admin/category/${id}`, category);
  return response.data;
};

export const deleteCategory = async (id: number): Promise<void> => {
  await apiClient.delete(`/admin/category/${id}`);
};

// Upload category image (admin only)
export const uploadCategoryImage = async (id: number, imageFile: FormData): Promise<void> => {
  // Don't set Content-Type header - axios will automatically set it with boundary for FormData
  await apiClient.post(`/admin/category/${id}/image`, imageFile);
}; 