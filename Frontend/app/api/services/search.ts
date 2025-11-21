import { Product, searchProductsByName, searchProducts as searchProductsAdvanced, ProductSearchParams } from './product';
import { Category, searchCategories, CategorySearchParams } from './category';

export interface SearchFilters {
  category?: string;
  minPrice?: string;
  maxPrice?: string;
  discount?: boolean;
  sort?: string;
  page?: number;
  size?: number;
}

export interface SearchResult {
  products: Product[];
  categories: Category[];
  totalProducts: number;
  totalCategories: number;
}

export interface UnifiedSearchParams {
  query: string;
  filters?: SearchFilters;
  searchType?: 'products' | 'categories' | 'all';
}

// Search products using the API endpoint
export async function searchProducts(query: string, filters: SearchFilters = {}): Promise<Product[]> {
  try {
    if (!query.trim()) {
      // If no query, use the advanced search with filters
      const searchParams: ProductSearchParams = {
        categoryId: filters.category ? parseInt(filters.category) : undefined,
        minPrice: filters.minPrice ? parseFloat(filters.minPrice) : undefined,
        maxPrice: filters.maxPrice ? parseFloat(filters.maxPrice) : undefined,
        page: filters.page || 0,
        size: filters.size || 20,
        sort: filters.sort
      };
      return await searchProductsAdvanced(searchParams);
    }
    
    // Use the dedicated search by name endpoint
    const products = await searchProductsByName(query);
    
    // Apply additional client-side filters if needed
    let filteredProducts = products;
    
    // Filter by category
    if (filters.category) {
      const categoryId = parseInt(filters.category);
      filteredProducts = filteredProducts.filter(product => 
        product.categoryId === categoryId
      );
    }
    
    // Filter by price range
    if (filters.minPrice) {
      const minPrice = parseFloat(filters.minPrice);
      filteredProducts = filteredProducts.filter(product => 
        product.price >= minPrice
      );
    }
    
    if (filters.maxPrice) {
      const maxPrice = parseFloat(filters.maxPrice);
      filteredProducts = filteredProducts.filter(product => 
        product.price <= maxPrice
      );
    }
    
    // Filter by discount
    if (filters.discount) {
      filteredProducts = filteredProducts.filter(product => 
        (product.discount || 0) > 0
      );
    }
    
    // Sort products
    if (filters.sort) {
      switch (filters.sort) {
        case 'price_asc':
          filteredProducts.sort((a, b) => a.price - b.price);
          break;
        case 'price_desc':
          filteredProducts.sort((a, b) => b.price - a.price);
          break;
        case 'newest':
          filteredProducts.sort((a, b) => b.id - a.id);
          break;
        case 'name_asc':
          filteredProducts.sort((a, b) => a.name.localeCompare(b.name));
          break;
        case 'name_desc':
          filteredProducts.sort((a, b) => b.name.localeCompare(a.name));
          break;
        // 'relevance' is default from the API
      }
    }
    
    return filteredProducts;
  } catch (error) {
    console.error('Error searching products:', error);
    return [];
  }
}

// Search categories
export async function searchCategoriesFunc(query: string, filters: SearchFilters = {}): Promise<Category[]> {
  try {
    const searchParams: CategorySearchParams = {
      name: query,
      page: filters.page || 0,
      size: filters.size || 20,
      sort: filters.sort
    };
    
    return await searchCategories(searchParams);
  } catch (error) {
    console.error('Error searching categories:', error);
    return [];
  }
}

// Unified search function that searches both products and categories
export async function unifiedSearch(params: UnifiedSearchParams): Promise<SearchResult> {
  const { query, filters = {}, searchType = 'all' } = params;
  
  try {
    let products: Product[] = [];
    let categories: Category[] = [];
    
    if (searchType === 'all' || searchType === 'products') {
      products = await searchProducts(query, filters);
    }
    
    if (searchType === 'all' || searchType === 'categories') {
      categories = await searchCategoriesFunc(query, filters);
    }
    
    return {
      products,
      categories,
      totalProducts: products.length,
      totalCategories: categories.length
    };
  } catch (error) {
    console.error('Error in unified search:', error);
    return {
      products: [],
      categories: [],
      totalProducts: 0,
      totalCategories: 0
    };
  }
}

// Get search suggestions based on query
export async function getSearchSuggestions(query: string, limit: number = 5): Promise<string[]> {
  try {
    if (!query.trim() || query.length < 2) {
      return [];
    }
    
    // Get products that match the query
    const products = await searchProductsByName(query);
    
    // Extract unique product names and category names as suggestions
    const suggestions = new Set<string>();
    
    products.slice(0, limit).forEach(product => {
      suggestions.add(product.name);
      if (product.categoryName) {
        suggestions.add(product.categoryName);
      }
    });
    
    return Array.from(suggestions).slice(0, limit);
  } catch (error) {
    console.error('Error getting search suggestions:', error);
    return [];
  }
}

// Advanced search with multiple criteria
export async function advancedSearch(params: {
  query?: string;
  categoryId?: number;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: string;
  page?: number;
  size?: number;
}): Promise<Product[]> {
  try {
    const searchParams: ProductSearchParams = {
      name: params.query,
      categoryId: params.categoryId,
      minPrice: params.minPrice,
      maxPrice: params.maxPrice,
      sort: params.sortBy,
      page: params.page || 0,
      size: params.size || 20
    };
    
    return await searchProductsAdvanced(searchParams);
  } catch (error) {
    console.error('Error in advanced search:', error);
    return [];
  }
} 