import apiClient from '../axios';

// Enhanced Admin User Management
export interface AdminUser {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string;
  location?: string;
  profilePictureUrl?: string;
  roles: string[];
  isActivated: boolean;
  isBlocked: boolean;
  registrationDate: string;
  lastLoginDate?: string;
  emailVerified: boolean;
  lastActivity?: string;
}

// Admin Dashboard Statistics
export interface AdminDashboardStats {
  totalUsers: number;
  totalProducts: number;
  totalOrders: number;
  totalRevenue: number;
  recentOrders: number;
  pendingOrders: number;
  lowStockProducts: number;
  activeSellers: number;
  totalCategories: number;
  systemHealth: 'good' | 'warning' | 'critical';
}

export const getAdminDashboardStats = async (): Promise<AdminDashboardStats> => {
  const response = await apiClient.get<AdminDashboardStats>('/admin/dashboard/stats');
  return response.data;
};

export interface UserAuditLog {
  id: number;
  userId: number;
  action: string;
  details: string;
  performedBy: string;
  performedAt: string;
  ipAddress?: string;
  userAgent?: string;
}

export interface AdminUserFilters {
  search?: string;
  role?: string;
  status?: 'active' | 'inactive' | 'blocked';
  isEmailVerified?: boolean;
  registrationDateFrom?: string;
  registrationDateTo?: string;
}

export interface UserUpdateRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  location?: string;
  roles?: string[];
}

// User Management
export const getAllUsers = async (
  page = 0, 
  size = 10, 
  filters?: AdminUserFilters
): Promise<{
  content: AdminUser[];
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}> => {
  const response = await apiClient.get('/admin/user/all', {
    params: {
      page,
      size,
      ...filters
    }
  });
  return response.data;
};

export const getUserById = async (id: number): Promise<AdminUser> => {
  const response = await apiClient.get<AdminUser>(`/admin/user/${id}`);
  return response.data;
};

export const getUserByEmail = async (email: string): Promise<AdminUser> => {
  const response = await apiClient.get<AdminUser>(`/admin/user/email/${email}`);
  return response.data;
};

export const updateUser = async (id: number, userData: UserUpdateRequest): Promise<AdminUser> => {
  const response = await apiClient.put<AdminUser>('/admin/user/update', {
    id,
    ...userData
  });
  return response.data;
};

export const deleteUser = async (id: number): Promise<{ message: string }> => {
  const response = await apiClient.delete<{ message: string }>(`/admin/user/${id}`);
  return response.data;
};

export const blockUser = async (id: number, reason?: string): Promise<{ message: string }> => {
  const response = await apiClient.post<{ message: string }>('/admin/user/block-user', {
    userId: id,
    reason
  });
  return response.data;
};

export const unblockUser = async (id: number): Promise<{ message: string }> => {
  const response = await apiClient.post<{ message: string }>('/admin/user/unblock-user', {
    userId: id
  });
  return response.data;
};

export const restoreUser = async (id: number): Promise<{ message: string }> => {
  const response = await apiClient.post<{ message: string }>('/admin/user/restore-user', {
    userId: id
  });
  return response.data;
};

// Audit Logs
export const getUserAuditLogs = async (
  page = 0,
  size = 10,
  userId?: number,
  action?: string,
  dateFrom?: string,
  dateTo?: string
): Promise<{
  content: UserAuditLog[];
  totalElements: number;
  totalPages: number;
}> => {
  const response = await apiClient.get('/admin/user/audit-logs', {
    params: {
      page,
      size,
      userId,
      action,
      dateFrom,
      dateTo
    }
  });
  return response.data;
};

// User Role Management (Admin functions)
export const getUserRoles = async (userId: number): Promise<string[]> => {
  const response = await apiClient.get<{ roles: string[] }>(`/admin/user/role/${userId}/roles`);
  return response.data.roles;
};

export const addUserRole = async (userId: number, role: string): Promise<{ message: string }> => {
  const response = await apiClient.post<{ message: string }>(`/admin/user/role/${userId}/add`, {
    role
  });
  return response.data;
};

export const removeUserRole = async (userId: number, role: string): Promise<{ message: string }> => {
  const response = await apiClient.delete<{ message: string }>(`/admin/user/role/${userId}/remove`, {
    data: { role }
  });
  return response.data;
};

export const assignSellerRole = async (userId: number): Promise<{ message: string }> => {
  const response = await apiClient.post<{ message: string }>('/admin/user/role/assign-seller-role', {
    userId
  });
  return response.data;
};

export const removeSellerRole = async (userId: number): Promise<{ message: string }> => {
  const response = await apiClient.post<{ message: string }>('/admin/user/role/remove-seller-role', {
    userId
  });
  return response.data;
};

export const assignAdminRole = async (userId: number): Promise<{ message: string }> => {
  const response = await apiClient.post<{ message: string }>('/admin/user/role/admin/assign-role', {
    userId
  });
  return response.data;
};

export const removeAdminRole = async (userId: number): Promise<{ message: string }> => {
  const response = await apiClient.post<{ message: string }>('/admin/user/role/admin/remove-role', {
    userId
  });
  return response.data;
};

// Statistics and Analytics
export interface UserStatistics {
  totalUsers: number;
  activeUsers: number;
  blockedUsers: number;
  deletedUsers: number;
  newUsersThisMonth: number;
  totalBuyers: number;
  totalSellers: number;
  totalAdmins: number;
  userGrowthData: Array<{
    date: string;
    count: number;
  }>;
  roleDistribution: Array<{
    role: string;
    count: number;
    percentage: number;
  }>;
  activityData: Array<{
    date: string;
    activeUsers: number;
    newRegistrations: number;
  }>;
}

export const getUserStatistics = async (
  period: 'week' | 'month' | 'quarter' | 'year' = 'month'
): Promise<UserStatistics> => {
  const response = await apiClient.get<UserStatistics>('/admin/user/statistics', {
    params: { period }
  });
  return response.data;
};

// Bulk Operations
export interface BulkUserAction {
  userIds: number[];
  action: 'block' | 'unblock' | 'delete' | 'restore' | 'add_role' | 'remove_role';
  role?: string;
  reason?: string;
}

export const performBulkUserAction = async (bulkAction: BulkUserAction): Promise<{
  message: string;
  successCount: number;
  failureCount: number;
  errors?: string[];
}> => {
  const response = await apiClient.post('/admin/user/bulk-action', bulkAction);
  return response.data;
};

// Export user data
export const exportUserData = async (
  filters?: AdminUserFilters,
  format: 'csv' | 'json' | 'xlsx' = 'csv'
): Promise<{
  downloadUrl: string;
  expiresAt: string;
}> => {
  const response = await apiClient.post('/admin/user/export', {
    filters,
    format
  });
  return response.data;
};

// User Communication
export interface BulkEmailRequest {
  userIds?: number[];
  filters?: AdminUserFilters;
  subject: string;
  content: string;
  isHtml?: boolean;
  scheduleAt?: string;
}

export const sendBulkEmail = async (emailRequest: BulkEmailRequest): Promise<{
  message: string;
  recipientCount: number;
  emailId: string;
}> => {
  const response = await apiClient.post('/admin/user/bulk-email', emailRequest);
  return response.data;
};

// System Notifications
export interface SystemNotification {
  id: number;
  title: string;
  message: string;
  type: 'info' | 'warning' | 'error' | 'success';
  targetUsers?: number[];
  targetRoles?: string[];
  isActive: boolean;
  createdAt: string;
  expiresAt?: string;
  createdBy: string;
}

export const createSystemNotification = async (
  notification: Omit<SystemNotification, 'id' | 'createdAt' | 'createdBy'>
): Promise<SystemNotification> => {
  const response = await apiClient.post<SystemNotification>('/admin/notifications', notification);
  return response.data;
};

export const getSystemNotifications = async (
  page = 0,
  size = 10,
  active?: boolean
): Promise<{
  content: SystemNotification[];
  totalElements: number;
  totalPages: number;
}> => {
  const response = await apiClient.get('/admin/notifications', {
    params: { page, size, active }
  });
  return response.data;
};

export const updateSystemNotification = async (
  id: number,
  updates: Partial<SystemNotification>
): Promise<SystemNotification> => {
  const response = await apiClient.put<SystemNotification>(`/admin/notifications/${id}`, updates);
  return response.data;
};

export const deleteSystemNotification = async (id: number): Promise<{ message: string }> => {
  const response = await apiClient.delete<{ message: string }>(`/admin/notifications/${id}`);
  return response.data;
};

// ============================================================================
// ADMIN PRODUCT MANAGEMENT
// ============================================================================

export interface AdminProductResponse {
  id: number;
  name: string;
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
  EAN?: string;
  manufacturingPlace?: string;
  authorId: number;
  companyId?: number;
  categoryId: number;
  productStatus: 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK';
  productSellType: 'DIRECT' | 'OFFER' | 'AUCTION' | 'PREORDER' | 'SUBSCRIPTION' | 'RENTAL' | 'BUNDLE' | 'DIGITAL';
  productCondition: 'NEW' | 'USED' | 'OPEN_NEVER_USED' | 'REFURBISHED';
  productConditionComment?: string;
  createdAt: string;
  updatedAt: string;
  images: AdminProductImage[];
  variants: AdminProductVariant[];
}

export interface AdminProductImage {
  id: number;
  fileName: string;
  fileUrl: string;
  contentType: string;
  fileSize: number;
  isPrimary: boolean;
  displayOrder: number;
}

export interface AdminProductVariant {
  id: number;
  name: string;
  priceAdjustment: number;
  quantity: number;
}

// Get all products (paginated)
export const getAdminProducts = async (page = 0, size = 10): Promise<{
  content: AdminProductResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}> => {
  const response = await apiClient.get('/admin/product', {
    params: { page, size }
  });
  return response.data;
};

// Get all products (non-paginated)
export const getAllAdminProducts = async (): Promise<AdminProductResponse[]> => {
  const response = await apiClient.get('/admin/product/all');
  return response.data;
};

// Search products by name (paginated)
export const searchAdminProducts = async (productName: string, page = 0, size = 10): Promise<{
  content: AdminProductResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}> => {
  const response = await apiClient.get('/admin/product/search/pages', {
    params: { productName, page, size }
  });
  return response.data;
};

// Get product by ID
export const getAdminProductById = async (id: number): Promise<AdminProductResponse> => {
  const response = await apiClient.get(`/admin/product/${id}`);
  return response.data;
};

// Delete product
export const deleteAdminProduct = async (productId: number): Promise<void> => {
  await apiClient.delete(`/admin/product/${productId}`);
};

// ============================================================================
// ADMIN CATEGORY MANAGEMENT
// ============================================================================

export interface AdminCategoryRequest {
  name: string;
  description?: string;
  parentId?: number;
  imageContent?: string;
  imageContentType?: string;
}

export interface AdminCategoryResponse {
  id: number;
  name: string;
  description?: string;
  imageUrl?: string;
  imageContent?: string;
  imageContentType?: string;
  createdDate: string;
  parentId?: number;
  subcategories: AdminCategoryResponse[];
}

// Create category
export const createAdminCategory = async (categoryData: AdminCategoryRequest): Promise<AdminCategoryResponse> => {
  const response = await apiClient.post('/admin/category', categoryData);
  return response.data;
};

// Update category
export const updateAdminCategory = async (id: number, categoryData: AdminCategoryRequest): Promise<AdminCategoryResponse> => {
  const response = await apiClient.put(`/admin/category/${id}`, categoryData);
  return response.data;
};

// Upload category image
export const uploadAdminCategoryImage = async (id: number, image: File): Promise<string> => {
  const formData = new FormData();
  formData.append('image', image);
  
  const response = await apiClient.post(`/admin/category/${id}/image`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

// Upload category image with request data
export const uploadAdminCategoryImageWithData = async (
  id: number, 
  image?: File, 
  request?: AdminCategoryRequest
): Promise<string> => {
  const formData = new FormData();
  
  if (image) {
    formData.append('image', image);
  }
  
  if (request) {
    formData.append('request', JSON.stringify(request));
  }
  
  const response = await apiClient.post(`/admin/category/${id}/image`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

// Delete category
export const deleteAdminCategory = async (id: number): Promise<void> => {
  await apiClient.delete(`/admin/category/${id}`);
};