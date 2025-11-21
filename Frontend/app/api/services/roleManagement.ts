import apiClient from '../axios';

// Role Management Types
export interface UserRole {
  id: number;
  name: string;
  description?: string;
  permissions?: string[];
}

export interface CompanyAdminRequest {
  companyId?: number;
  userId?: number;
  rightsId?: number;
  sellerEmail?: string;
  requestReason?: string;
  adminRights?: string[];
}

export interface Company {
  id: number;
  name: string;
  description?: string;
  address?: string;
  phone?: string;
  email?: string;
  website?: string;
  isActive: boolean;
  createdAt: string;
  admins: CompanyAdmin[];
  sellers: CompanySeller[];
}

export interface CompanyAdmin {
  id: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  adminRights: string[];
  assignedAt: string;
}

export interface CompanySeller {
  id: number;
  userId: number;
  email: string;
  firstName: string;
  lastName: string;
  joinedAt: string;
  isActive: boolean;
}

export interface SellerRoleRequest {
  message: string;
  sellerId?: number;
  status: 'pending' | 'approved' | 'rejected';
}

// Basic Role Operations
export const getUserRoles = async (userId: number): Promise<UserRole[]> => {
  const response = await apiClient.get<UserRole[]>(`/admin/user/role/${userId}/roles`);
  return response.data;
};

export const addRoleToUser = async (userId: number, roleName: string): Promise<{ message: string }> => {
  const response = await apiClient.post(`/admin/user/role/${userId}/add`, { roleName });
  return response.data;
};

export const removeRoleFromUser = async (userId: number, roleName: string): Promise<{ message: string }> => {
  const response = await apiClient.delete(`/admin/user/role/${userId}/remove`, {
    data: { roleName }
  });
  return response.data;
};

// Seller Role Management
export const becomeIndividualSeller = async (): Promise<SellerRoleRequest> => {
  const response = await apiClient.post<SellerRoleRequest>('/buyer/user/role/become-seller');
  return response.data;
};

export const assignSellerRole = async (userId: number, businessInfo?: { 
  businessName?: string; 
  businessType?: string; 
  description?: string; 
  website?: string; 
  phone?: string; 
  address?: string 
}): Promise<{ message: string }> => {
  const response = await apiClient.post('/admin/user/role/assign-seller-role', {
    userId,
    businessInfo
  });
  return response.data;
};

export const removeSellerRole = async (userId: number): Promise<{ message: string }> => {
  const response = await apiClient.post('/admin/user/role/remove-seller-role', { userId });
  return response.data;
};

// Admin Role Management  
export const assignAdminRole = async (userId: number): Promise<{ message: string }> => {
  const response = await apiClient.post('/admin/user/role/admin/assign-role', { userId });
  return response.data;
};

export const removeAdminRole = async (userId: number): Promise<{ message: string }> => {
  const response = await apiClient.post('/admin/user/role/admin/remove-role', { userId });
  return response.data;
};

// Company Admin Management
export const requestCompanyAdminRole = async (companyId?: number): Promise<{ message: string; requestId?: number }> => {
  const response = await apiClient.post('/seller/user/role/v2/become-company-admin-seller', {
    companyId
  });
  return response.data;
};

export const approveCompanyAdminRequest = async (rightsId: number): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/approve-company-admin/${rightsId}`);
  return response.data;
};

export const denyCompanyAdminRequest = async (rightsId: number, reason?: string): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/deny-company-admin/${rightsId}`, {
    reason
  });
  return response.data;
};

export const approveCompanyAdminForUser = async (
  companyId: number, 
  userId: number, 
  adminRights?: string[]
): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/approve-company-admin/company/${companyId}/user/${userId}`, {
    adminRights
  });
  return response.data;
};

export const denyCompanyAdminForUser = async (
  companyId: number, 
  userId: number, 
  reason?: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/deny-company-admin/company/${companyId}/user/${userId}`, {
    reason
  });
  return response.data;
};

// Company Management
export const addSellerToCompany = async (
  companyId: number, 
  sellerEmail: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/company/${companyId}/add-seller/${sellerEmail}`);
  return response.data;
};

export const removeSellerFromCompany = async (
  companyId: number, 
  sellerEmail: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/company/${companyId}/remove-seller/${sellerEmail}`);
  return response.data;
};

export const promoteToCompanyAdmin = async (
  companyId: number, 
  sellerEmail: string,
  adminRights?: string[]
): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/company/${companyId}/promote-admin/${sellerEmail}`, {
    adminRights
  });
  return response.data;
};

export const demoteCompanyAdmin = async (
  companyId: number, 
  sellerEmail: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/company/${companyId}/demote-admin/${sellerEmail}`);
  return response.data;
};

export const updateCompanyAdminRights = async (
  companyId: number, 
  sellerEmail: string, 
  adminRights: string[]
): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/company/${companyId}/update-admin-rights/${sellerEmail}`, {
    adminRights
  });
  return response.data;
};

export const deleteCompany = async (companyId: number): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/company/${companyId}/delete`);
  return response.data;
};

export const restoreCompany = async (companyId: number): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/company/${companyId}/revoke`);
  return response.data;
};

// Get user's companies and roles
export const getUserCompanies = async (): Promise<Company[]> => {
  const response = await apiClient.get<Company[]>('/seller/user/companies');
  return response.data;
};

export const getCompanyDetails = async (companyId: number): Promise<Company> => {
  const response = await apiClient.get<Company>(`/seller/user/company/${companyId}`);
  return response.data;
};

// Pending requests management
export interface PendingAdminRequest {
  id: number;
  userId: number;
  userEmail: string;
  userName: string;
  companyId?: number;
  companyName?: string;
  requestedAt: string;
  reason?: string;
  status: 'pending' | 'approved' | 'denied';
}

export const getPendingAdminRequests = async (page = 0, size = 10): Promise<{
  content: PendingAdminRequest[];
  totalElements: number;
  totalPages: number;
}> => {
  const response = await apiClient.get('/seller/user/role/pending-admin-requests', {
    params: { page, size }
  });
  return response.data;
};

export const getMyAdminRequests = async (): Promise<PendingAdminRequest[]> => {
  const response = await apiClient.get<PendingAdminRequest[]>('/seller/user/role/my-admin-requests');
  return response.data;
};
