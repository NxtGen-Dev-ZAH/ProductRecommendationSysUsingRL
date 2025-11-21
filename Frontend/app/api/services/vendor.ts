import apiClient from '../axios';

export interface Vendor {
  id: number;
  name: string;
  description: string;
  logoUrl: string;
  email: string;
  phone: string;
  address: string;
  rating?: number;
  joinedDate: string;
  featured?: boolean;
}

export interface VendorProduct {
  id: number;
  name: string;
  description: string;
  price: number;
  quantity: number;
  active: boolean;
  imageUrl: string;
  categoryId: number;
  discount: number;
}

export interface VendorWithProducts {
  vendor: Vendor;
  products: VendorProduct[];
}

export interface UserFollowInfo {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  profilePictureUrl?: string;
  isActive: boolean;
  followDate: string;
}

// Get user profile (vendor information)
export const getVendorProfile = async (email: string): Promise<Vendor | null> => {
  try {
    const encodedEmail = encodeURIComponent(email);
    const response = await apiClient.get(`/profile/visit/${encodedEmail}`);
    return response.data;
  } catch (error) {
    console.error('Error fetching vendor profile:', error);
    return null;
  }
};

// Get vendor's favorite products
export const getVendorFavorites = async (email: string): Promise<VendorProduct[]> => {
  try {
    const encodedEmail = encodeURIComponent(email);
    const response = await apiClient.get(`/profile/visit/${encodedEmail}/favorites`);
    return response.data;
  } catch (error) {
    console.error('Error fetching vendor favorites:', error);
    return [];
  }
};

// Get vendor followers
export const getVendorFollowers = async (email: string): Promise<UserFollowInfo[]> => {
  try {
    const encodedEmail = encodeURIComponent(email);
    const response = await apiClient.get(`/profile/visit/${encodedEmail}/followers`);
    return response.data;
  } catch (error) {
    console.error('Error fetching vendor followers:', error);
    return [];
  }
};

// Get vendor following
export const getVendorFollowing = async (email: string): Promise<UserFollowInfo[]> => {
  try {
    const encodedEmail = encodeURIComponent(email);
    const response = await apiClient.get(`/profile/visit/${encodedEmail}/following`);
    return response.data;
  } catch (error) {
    console.error('Error fetching vendor following:', error);
    return [];
  }
};

// Get follower count
export const getVendorFollowerCount = async (email: string): Promise<number> => {
  try {
    const encodedEmail = encodeURIComponent(email);
    const response = await apiClient.get(`/profile/visit/${encodedEmail}/follower-count`);
    return response.data.count || 0;
  } catch (error) {
    console.error('Error fetching follower count:', error);
    return 0;
  }
};

// Get following count
export const getVendorFollowingCount = async (email: string): Promise<number> => {
  try {
    const encodedEmail = encodeURIComponent(email);
    const response = await apiClient.get(`/profile/visit/${encodedEmail}/following-count`);
    return response.data.count || 0;
  } catch (error) {
    console.error('Error fetching following count:', error);
    return 0;
  }
};

// For backward compatibility - mock data fallback
export const getAllVendors = async (): Promise<Vendor[]> => {
  // In a real app, this would fetch from an actual API
  // For demo purposes, we'll return mock data
  console.warn('getAllVendors is using mock data. Use API endpoints for real vendor data.');
  return [
    {
      id: 1,
      name: 'Tech Galaxy',
      description: 'Leading provider of high-quality electronics and gadgets',
      logoUrl: '/images/vendors/tech-galaxy.png',
      email: 'contact@techgalaxy.com',
      phone: '+1 (555) 123-4567',
      address: '123 Tech Street, San Francisco, CA 94105',
      rating: 4.8,
      joinedDate: '2020-05-15',
      featured: true
    },
    {
      id: 2,
      name: 'Fashion Forward',
      description: 'Trendy clothing and accessories for all seasons',
      logoUrl: '/images/vendors/fashion-forward.png',
      email: 'support@fashionforward.com',
      phone: '+1 (555) 987-6543',
      address: '456 Style Avenue, New York, NY 10018',
      rating: 4.5,
      joinedDate: '2019-11-20',
      featured: true
    },
    {
      id: 3,
      name: 'Home Essentials',
      description: 'Everything you need to make your house a home',
      logoUrl: '/images/vendors/home-essentials.png',
      email: 'info@homeessentials.com',
      phone: '+1 (555) 789-0123',
      address: '789 Comfort Lane, Chicago, IL 60607',
      rating: 4.6,
      joinedDate: '2021-02-10',
      featured: false
    },
    {
      id: 4,
      name: 'Sport Champions',
      description: 'Premium sporting goods and athletic wear',
      logoUrl: '/images/vendors/sport-champions.png',
      email: 'help@sportchampions.com',
      phone: '+1 (555) 456-7890',
      address: '321 Fitness Road, Boston, MA 02115',
      rating: 4.7,
      joinedDate: '2020-08-25',
      featured: true
    },
    {
      id: 5,
      name: 'Kids Kingdom',
      description: 'Safe and fun products for children of all ages',
      logoUrl: '/images/vendors/kids-kingdom.png',
      email: 'care@kidskingdom.com',
      phone: '+1 (555) 234-5678',
      address: '555 Playful Drive, Orlando, FL 32801',
      rating: 4.9,
      joinedDate: '2021-06-01',
      featured: false
    }
  ];
};

export const getFeaturedVendors = async (): Promise<Vendor[]> => {
  const vendors = await getAllVendors();
  return vendors.filter(vendor => vendor.featured);
};

export const getVendorById = async (id: number): Promise<Vendor | undefined> => {
  const vendors = await getAllVendors();
  return vendors.find(vendor => vendor.id === id);
};

export const getVendorWithProducts = async (id: number): Promise<VendorWithProducts | undefined> => {
  const vendor = await getVendorById(id);
  
  if (!vendor) return undefined;
  
  // In a real app, you would fetch the vendor's products from the API
  // For demo purposes, let's simulate it with some mock products
  const mockProducts = [
    {
      id: 101,
      name: 'Product from ' + vendor.name,
      description: 'This is a sample product description.',
      price: 99.99,
      quantity: 50,
      active: true,
      imageUrl: '/images/products/sample-1.jpg',
      categoryId: 1,
      discount: 10
    },
    {
      id: 102,
      name: 'Another Product from ' + vendor.name,
      description: 'Another sample product description.',
      price: 149.99,
      quantity: 30,
      active: true,
      imageUrl: '/images/products/sample-2.jpg',
      categoryId: 2,
      discount: 0
    },
    {
      id: 103,
      name: 'Premium Item from ' + vendor.name,
      description: 'Premium quality product for discerning customers.',
      price: 299.99,
      quantity: 15,
      active: true,
      imageUrl: '/images/products/sample-3.jpg',
      categoryId: 3,
      discount: 15
    }
  ];
  
  return {
    vendor,
    products: mockProducts
  };
}; 