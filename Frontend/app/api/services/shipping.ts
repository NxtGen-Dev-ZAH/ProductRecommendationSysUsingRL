import apiClient from '../axios';

// Enums matching backend
export enum ShippingStatus {
  PENDING = 'PENDING',
  SHIPPED = 'SHIPPED',
  IN_TRANSIT = 'IN_TRANSIT',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  RETURNED = 'RETURNED',
  FAILED = 'FAILED'
}

export enum ReturnStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
  COMPLETED = 'COMPLETED'
}

// Order Shipping Details Interfaces
export interface OrderShippingRequest {
  shippingCarrier: string;
  shippingMethod: string;
  shippingMethodCurrency: string;
  shippingPrice: string;
  trackingUrl?: string;
  trackingNumber?: string;
  labelUrl?: string;
  label?: string;
  shippingQuantity: number;
  shippingWeight: string;
  shippingDimensionRegularOrNot?: boolean;
  shippingDimensionHeight?: string;
  shippingDimensionWidth?: string;
  shippingDimensionDepth?: string;
}

export interface OrderShippingResponse {
  id: number;
  shippingCarrier: string;
  shippingMethod: string;
  shippingMethodCurrency: string;
  shippingPrice: string;
  trackingUrl?: string;
  trackingNumber?: string;
  labelUrl?: string;
  label?: string;
  shippingQuantity: number;
  shippingWeight: string;
  shippingDimensionRegularOrNot?: boolean;
  shippingDimensionHeight?: string;
  shippingDimensionWidth?: string;
  shippingDimensionDepth?: string;
  shippedAt?: string;
  deliveredAt?: string;
  status: ShippingStatus;
  orderId: number;
}

// Shipping Credential Interfaces
export interface OrderShippingCredentialRequest {
  recipientName: string;
  recipientEmail: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  phoneNumber?: string;
  reference?: string;
}

export interface OrderShippingCredentialResponse {
  id: number;
  recipientName: string;
  recipientEmail: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
  phoneNumber?: string;
  reference?: string;
  orderId: number;
}

// Billing Credential Interfaces
export interface OrderBillingCredentialRequest {
  billingClientName: string;
  billingClientEmail: string;
  billingAddressLine1: string;
  billingAddressLine2?: string;
  billingCity: string;
  billingPostalCode: string;
  billingCountry: string;
  billingPhoneNumber?: string;
}

export interface OrderBillingCredentialResponse {
  id: number;
  billingClientName: string;
  billingClientEmail: string;
  billingAddressLine1: string;
  billingAddressLine2?: string;
  billingCity: string;
  billingPostalCode: string;
  billingCountry: string;
  billingPhoneNumber?: string;
  orderId: number;
}

// Shipping Tracking Interfaces
export interface ShippingTrackingRequest {
  trackingNumber: string;
  carrierStatus: string;
  estimatedDeliveryDate?: string;
}

export interface ShippingTrackingResponse {
  id: number;
  trackingNumber: string;
  carrierStatus: string;
  estimatedDeliveryDate?: string;
  lastUpdated?: string;
  orderId: number;
}

// Return Request Interfaces
export interface ReturnRequestRequest {
  reason: string;
  orderItemIds: number[];
  orderId?: number;
  refundPercentage?: number;
}

export interface ReturnRequestResponse {
  id: number;
  reason: string;
  status: ReturnStatus;
  requestDate: string;
  refundAmount?: number;
  refundPercentage?: number;
  orderId: number;
  orderItemIds: number[];
}

// Refund Response Interface
export interface RefundResponse {
  id: number;
  refundDate: string;
  amount: number;
  transactionId: string;
  reason: string;
  paymentId: number;
  returnRequestId: number;
}

// =================
// ORDER SHIPPING DETAILS API CALLS
// =================

// Create shipping details for an order
export const createOrderShipping = async (orderId: number, shippingData: OrderShippingRequest): Promise<OrderShippingResponse> => {
  const response = await apiClient.post<OrderShippingResponse>(`/buyer/orders/${orderId}/details/shipping`, shippingData);
  return response.data;
};

// Get shipping details for an order
export const getOrderShipping = async (orderId: number): Promise<OrderShippingResponse> => {
  const response = await apiClient.get<OrderShippingResponse>(`/buyer/orders/${orderId}/details/shipping`);
  return response.data;
};

// Update shipping details for an order
export const updateOrderShipping = async (orderId: number, shippingData: OrderShippingRequest): Promise<OrderShippingResponse> => {
  const response = await apiClient.put<OrderShippingResponse>(`/buyer/orders/${orderId}/details/shipping`, shippingData);
  return response.data;
};

// Create shipping address for an order
export const createOrderShippingCredential = async (orderId: number, credentialData: OrderShippingCredentialRequest): Promise<OrderShippingCredentialResponse> => {
  const response = await apiClient.post<OrderShippingCredentialResponse>(`/buyer/orders/${orderId}/details/shipping-credential`, credentialData);
  return response.data;
};

// Get shipping address for an order
export const getOrderShippingCredential = async (orderId: number): Promise<OrderShippingCredentialResponse> => {
  const response = await apiClient.get<OrderShippingCredentialResponse>(`/buyer/orders/${orderId}/details/shipping-credential`);
  return response.data;
};

// Update shipping address for an order
export const updateOrderShippingCredential = async (orderId: number, credentialData: OrderShippingCredentialRequest): Promise<OrderShippingCredentialResponse> => {
  const response = await apiClient.put<OrderShippingCredentialResponse>(`/buyer/orders/${orderId}/details/shipping-credential`, credentialData);
  return response.data;
};

// Create billing address for an order
export const createOrderBillingCredential = async (orderId: number, billingData: OrderBillingCredentialRequest): Promise<OrderBillingCredentialResponse> => {
  const response = await apiClient.post<OrderBillingCredentialResponse>(`/buyer/orders/${orderId}/details/billing-credential`, billingData);
  return response.data;
};

// Get billing address for an order
export const getOrderBillingCredential = async (orderId: number): Promise<OrderBillingCredentialResponse> => {
  const response = await apiClient.get<OrderBillingCredentialResponse>(`/buyer/orders/${orderId}/details/billing-credential`);
  return response.data;
};

// Update billing address for an order
export const updateOrderBillingCredential = async (orderId: number, billingData: OrderBillingCredentialRequest): Promise<OrderBillingCredentialResponse> => {
  const response = await apiClient.put<OrderBillingCredentialResponse>(`/buyer/orders/${orderId}/details/billing-credential`, billingData);
  return response.data;
};

// =================
// POST ORDER MANAGEMENT API CALLS
// =================

// Update shipping tracking information (Admin only)
export const updateShippingTracking = async (orderId: number, trackingData: ShippingTrackingRequest): Promise<ShippingTrackingResponse> => {
  const response = await apiClient.post<ShippingTrackingResponse>(`/buyer/post-order/tracking/${orderId}`, trackingData);
  return response.data;
};

// Get shipping tracking information
export const getShippingTracking = async (orderId: number): Promise<ShippingTrackingResponse> => {
  const response = await apiClient.get<ShippingTrackingResponse>(`/buyer/post-order/tracking/${orderId}`);
  return response.data;
};

// Create a return request
export const createReturnRequest = async (orderId: number, returnData: ReturnRequestRequest): Promise<ReturnRequestResponse> => {
  const response = await apiClient.post<ReturnRequestResponse>(`/buyer/post-order/return/${orderId}`, returnData);
  return response.data;
};

// Get return requests for an order
export const getReturnRequests = async (orderId: number): Promise<ReturnRequestResponse[]> => {
  const response = await apiClient.get<ReturnRequestResponse[]>(`/buyer/post-order/return/${orderId}`);
  return response.data;
};

// Approve a return request (Admin only)
export const approveReturnRequest = async (returnRequestId: number): Promise<ReturnRequestResponse> => {
  const response = await apiClient.post<ReturnRequestResponse>(`/buyer/post-order/return/approve/${returnRequestId}`);
  return response.data;
};

// Reject a return request (Admin only)
export const rejectReturnRequest = async (returnRequestId: number, rejectionReason: string): Promise<ReturnRequestResponse> => {
  const response = await apiClient.post<ReturnRequestResponse>(`/buyer/post-order/return/reject/${returnRequestId}`, null, {
    params: { rejectionReason }
  });
  return response.data;
};

// Process a partial refund (Admin only)
export const processPartialRefund = async (returnRequestId: number): Promise<{ message: string; refundId: number; returnRequestId: number }> => {
  const response = await apiClient.post(`/buyer/post-order/refund/${returnRequestId}`);
  return response.data;
};

// =================
// HELPER FUNCTIONS
// =================

// Format shipping status for display
export const formatShippingStatus = (status: ShippingStatus): string => {
  switch (status) {
    case ShippingStatus.PENDING:
      return 'Pending';
    case ShippingStatus.SHIPPED:
      return 'Shipped';
    case ShippingStatus.IN_TRANSIT:
      return 'In Transit';
    case ShippingStatus.DELIVERED:
      return 'Delivered';
    case ShippingStatus.CANCELLED:
      return 'Cancelled';
    case ShippingStatus.RETURNED:
      return 'Returned';
    case ShippingStatus.FAILED:
      return 'Failed';
    default:
      return status;
  }
};

// Format return status for display
export const formatReturnStatus = (status: ReturnStatus): string => {
  switch (status) {
    case ReturnStatus.PENDING:
      return 'Pending';
    case ReturnStatus.APPROVED:
      return 'Approved';
    case ReturnStatus.REJECTED:
      return 'Rejected';
    case ReturnStatus.COMPLETED:
      return 'Completed';
    default:
      return status;
  }
};

// Get status color for UI styling
export const getShippingStatusColor = (status: ShippingStatus): string => {
  switch (status) {
    case ShippingStatus.PENDING:
      return 'yellow';
    case ShippingStatus.SHIPPED:
      return 'blue';
    case ShippingStatus.IN_TRANSIT:
      return 'indigo';
    case ShippingStatus.DELIVERED:
      return 'green';
    case ShippingStatus.CANCELLED:
    case ShippingStatus.FAILED:
      return 'red';
    case ShippingStatus.RETURNED:
      return 'orange';
    default:
      return 'gray';
  }
};

// Get return status color for UI styling
export const getReturnStatusColor = (status: ReturnStatus): string => {
  switch (status) {
    case ReturnStatus.PENDING:
      return 'yellow';
    case ReturnStatus.APPROVED:
      return 'green';
    case ReturnStatus.REJECTED:
      return 'red';
    case ReturnStatus.COMPLETED:
      return 'blue';
    default:
      return 'gray';
  }
};


