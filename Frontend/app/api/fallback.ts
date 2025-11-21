// Fallback API client for when main API is unavailable
//  import axios from 'axios';

// Mock data for development/fallback scenarios
export const mockData = {
  products: [
    {
      id: 1,
      name: 'Sample Product 1',
      description: 'This is a sample product for development',
      price: 29.99,
      quantity: 10,
      active: true,
      imageUrl: '/placeholder-product.png',
      categoryId: 1,
      categoryName: 'Electronics',
      featured: true,
      discount: 0
    },
    {
      id: 2,
      name: 'Sample Product 2',
      description: 'Another sample product for development',
      price: 49.99,
      quantity: 5,
      active: true,
      imageUrl: '/placeholder-product.png',
      categoryId: 2,
      categoryName: 'Fashion',
      featured: false,
      discount: 10
    }
  ],
  categories: [
    {
      id: 1,
      name: 'Electronics',
      description: 'Electronic devices and gadgets',
      imageUrl: '/placeholder-category.png',
      featured: true
    },
    {
      id: 2,
      name: 'Fashion',
      description: 'Clothing and fashion accessories',
      imageUrl: '/placeholder-category.png',
      featured: true
    }
  ],
  cart: {
    id: 1,
    userId: 1,
    items: [],
    total: 0,
    subtotal: 0,
    itemCount: 0
  }
};

// Fallback API functions
export const fallbackApi = {
  // Products
  getAllProducts: async () => {
    console.warn('Using fallback API for products');
    return new Promise(resolve => {
      setTimeout(() => resolve(mockData.products), 500);
    });
  },

  getProductById: async (id: number) => {
    console.warn('Using fallback API for product details');
    return new Promise(resolve => {
      setTimeout(() => {
        const product = mockData.products.find(p => p.id === id);
        resolve(product || mockData.products[0]);
      }, 500);
    });
  },

  // Categories
  getAllCategories: async () => {
    console.warn('Using fallback API for categories');
    return new Promise(resolve => {
      setTimeout(() => resolve(mockData.categories), 500);
    });
  },

  getCategoryById: async (id: number) => {
    console.warn('Using fallback API for category details');
    return new Promise(resolve => {
      setTimeout(() => {
        const category = mockData.categories.find(c => c.id === id);
        resolve(category || mockData.categories[0]);
      }, 500);
    });
  },

  // Cart
  getCart: async () => {
    console.warn('Using fallback API for cart');
    return new Promise(resolve => {
      setTimeout(() => resolve(mockData.cart), 500);
    });
  },

  // Search
  searchProducts: async (query: string) => {
    console.warn('Using fallback API for search');
    return new Promise(resolve => {
      setTimeout(() => {
        const filtered = mockData.products.filter(p => 
          p.name.toLowerCase().includes(query.toLowerCase()) ||
          p.description.toLowerCase().includes(query.toLowerCase())
        );
        resolve(filtered);
      }, 500);
    });
  }
};

// Helper function to determine if we should use fallback
export const shouldUseFallback = (error: unknown): boolean => {
  // Use fallback for network errors, CORS errors, or 500+ errors
  const errorObj = error as { 
    isNetworkError?: boolean; 
    code?: string; 
    message?: string; 
    response?: { status?: number } 
  };
  
  return (
    errorObj?.isNetworkError ||
    errorObj?.code === 'ERR_NETWORK' ||
    errorObj?.message?.includes('CORS') ||
    (errorObj?.response?.status && errorObj.response.status >= 500) ||
    !navigator.onLine // Offline
  );
};

// Helper function to show appropriate error messages
export const getErrorMessage = (error: unknown): string => {
  if (!navigator.onLine) {
    return 'You appear to be offline. Please check your internet connection.';
  }
  
  const errorObj = error as { 
    isNetworkError?: boolean; 
    code?: string; 
    message?: string; 
    response?: { status?: number; data?: { message?: string } } 
  };
  
  if (errorObj?.isNetworkError || errorObj?.code === 'ERR_NETWORK') {
    return 'Unable to connect to the server. Please try again later.';
  }
  
  if (errorObj?.message?.includes('CORS')) {
    return 'Connection issue detected. Using offline mode.';
  }
  
  if (errorObj?.response?.status && errorObj.response.status >= 500) {
    return 'Server is temporarily unavailable. Please try again later.';
  }
  
  return errorObj?.response?.data?.message || 
         errorObj?.message || 
         'An unexpected error occurred.';
};
