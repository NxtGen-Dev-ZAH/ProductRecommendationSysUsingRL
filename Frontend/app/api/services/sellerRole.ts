import apiClient from '../axios';

// Company Management
export interface Company {
  id: number;
  name: string;
  description?: string;
  address?: string;
  phone?: string;
  email?: string;
  website?: string;
  logoUrl?: string;
  isActive: boolean;
  createdAt: string;
  admins: CompanyAdmin[];
  sellers: CompanySeller[];
}

export interface CompanyAdmin {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  isActive: boolean;
  joinedAt: string;
}

export interface CompanySeller {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  isActive: boolean;
  joinedAt: string;
}

export interface CompanyRequest {
  name: string;
  description?: string;
  address?: string;
  phone?: string;
  email?: string;
  website?: string;
}

export interface AdminRightsRequest {
  id: number;
  companyId: number;
  userId: number;
  companyName?: string;
  status: 'pending' | 'approved' | 'denied';
  requestedAt: string;
  processedAt?: string;
}

// Company Admin Seller Request
export const becomeCompanyAdminSeller = async (
  companyRequest: CompanyRequest,
  logoFile?: File
): Promise<{ message: string }> => {
  const formData = new FormData();
  formData.append('companyRequest', JSON.stringify(companyRequest));
  
  if (logoFile) {
    formData.append('file', logoFile);
  }

  const response = await apiClient.post('/seller/user/role/v2/become-company-admin-seller', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data;
};

// Approve Company Admin Request
export const approveCompanyAdmin = async (
  companyId: number,
  userId: number,
  token: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(
    `/seller/user/role/approve-company-admin/company/${companyId}/user/${userId}`,
    null,
    { params: { token } }
  );
  return response.data;
};

// Deny Company Admin Request
export const denyCompanyAdmin = async (
  companyId: number,
  userId: number,
  token: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(
    `/seller/user/role/deny-company-admin/company/${companyId}/user/${userId}`,
    null,
    { params: { token } }
  );
  return response.data;
};

// Approve Company Admin by Rights ID
export const approveCompanyAdminByRightsId = async (
  rightsId: number
): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/approve-company-admin/${rightsId}`);
  return response.data;
};

// Deny Company Admin by Rights ID
export const denyCompanyAdminByRightsId = async (
  rightsId: number
): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/deny-company-admin/${rightsId}`);
  return response.data;
};

// Company Management Operations
export const addSellerToCompany = async (
  companyId: number,
  sellerEmail: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(
    `/seller/user/role/company/${companyId}/add-seller/${sellerEmail}`
  );
  return response.data;
};

export const removeSellerFromCompany = async (
  companyId: number,
  sellerEmail: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(
    `/seller/user/role/company/${companyId}/remove-seller/${sellerEmail}`
  );
  return response.data;
};

export const promoteSellerToAdmin = async (
  companyId: number,
  sellerEmail: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(
    `/seller/user/role/company/${companyId}/promote-admin/${sellerEmail}`
  );
  return response.data;
};

export const demoteAdminToSeller = async (
  companyId: number,
  sellerEmail: string
): Promise<{ message: string }> => {
  const response = await apiClient.post(
    `/seller/user/role/company/${companyId}/demote-admin/${sellerEmail}`
  );
  return response.data;
};

export const updateAdminRights = async (
  companyId: number,
  sellerEmail: string,
  rights: string[]
): Promise<{ message: string }> => {
  const response = await apiClient.post(
    `/seller/user/role/company/${companyId}/update-admin-rights/${sellerEmail}`,
    { rights }
  );
  return response.data;
};

export const deleteCompany = async (companyId: number): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/company/${companyId}/delete`);
  return response.data;
};

export const revokeCompany = async (companyId: number): Promise<{ message: string }> => {
  const response = await apiClient.post(`/seller/user/role/company/${companyId}/revoke`);
  return response.data;
};

// Get company information (these would be additional endpoints that might exist)
export const getCompanyById = async (companyId: number): Promise<Company> => {
  const response = await apiClient.get<Company>(`/seller/company/${companyId}`);
  return response.data;
};

export const getUserCompanies = async (): Promise<Company[]> => {
  const response = await apiClient.get<Company[]>('/seller/user/companies');
  return response.data;
};

export const getPendingAdminRequests = async (): Promise<AdminRightsRequest[]> => {
  const response = await apiClient.get<AdminRightsRequest[]>('/seller/user/role/pending-requests');
  return response.data;
};

export const getCompanyMembers = async (companyId: number): Promise<{
  admins: CompanyAdmin[];
  sellers: CompanySeller[];
}> => {
  const response = await apiClient.get(`/seller/company/${companyId}/members`);
  return response.data;
};

// Company Statistics
export interface CompanyStats {
  totalMembers: number;
  totalAdmins: number;
  totalSellers: number;
  totalProducts: number;
  totalOrders: number;
  monthlyRevenue: number;
}

export const getCompanyStats = async (companyId: number): Promise<CompanyStats> => {
  const response = await apiClient.get<CompanyStats>(`/seller/company/${companyId}/stats`);
  return response.data;
};

// Company Profile Management
export interface CompanyProfile {
  id: number;
  name: string;
  description?: string;
  address?: string;
  phone?: string;
  email?: string;
  website?: string;
  logoUrl?: string;
  businessLicense?: string;
  taxId?: string;
  establishedYear?: number;
  industry?: string;
}

export const getCompanyProfile = async (companyId: number): Promise<CompanyProfile> => {
  const response = await apiClient.get<CompanyProfile>(`/seller/company/${companyId}/profile`);
  return response.data;
};

export const updateCompanyProfile = async (
  companyId: number,
  profile: Partial<CompanyProfile>
): Promise<CompanyProfile> => {
  const response = await apiClient.put<CompanyProfile>(`/seller/company/${companyId}/profile`, profile);
  return response.data;
};

export const uploadCompanyLogo = async (companyId: number, file: File): Promise<string> => {
  const formData = new FormData();
  formData.append('logo', file);
  
  const response = await apiClient.post<{ url: string }>(`/seller/company/${companyId}/logo`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return response.data.url;
};
