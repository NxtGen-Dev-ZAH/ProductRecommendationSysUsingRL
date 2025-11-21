import apiClient from './axios';

export const testApiConnection = async () => {
  console.log('Testing API connection...');
  console.log('Base URL:', apiClient.defaults.baseURL);
  
  try {
    // Test categories endpoint
    console.log('Testing categories endpoint...');
    const categoriesResponse = await apiClient.get('/api/category');
    console.log('Categories response:', categoriesResponse.data);
    
    // Test products endpoint
    console.log('Testing products endpoint...');
    const productsResponse = await apiClient.get('/api/v1/products', {
      params: { page: 0, size: 5 }
    });
    console.log('Products response:', productsResponse.data);
    
    return { success: true, categories: categoriesResponse.data, products: productsResponse.data };
  } catch (error: unknown) {
    const errorObj = error as { 
      message?: string; 
      code?: string; 
      response?: { status?: number; statusText?: string }; 
      config?: { url?: string; baseURL?: string; timeout?: number } 
    };
    
    console.error('API connection test failed:', {
      message: errorObj.message,
      code: errorObj.code,
      status: errorObj.response?.status,
      statusText: errorObj.response?.statusText,
      url: errorObj.config?.url,
      baseURL: errorObj.config?.baseURL,
      timeout: errorObj.config?.timeout
    });
    
    return { 
      success: false, 
      error: {
        message: errorObj.message,
        code: errorObj.code,
        status: errorObj.response?.status,
        statusText: errorObj.response?.statusText
      }
    };
  }
};
