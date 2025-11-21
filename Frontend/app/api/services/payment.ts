import apiClient from '../axios';

export interface PaymentRequest {
  amount: number;
  method: 'credit_card' | 'paypal';
  currency?: string;
  description?: string;
  cardDetails?: {
    number?: string;
    expiry?: string;
    cvc?: string;
    name?: string;
  };
}

export interface PaymentResponse {
  id: string;
  status: 'succeeded' | 'pending' | 'failed';
  amount: number;
  paymentMethod: string;
  createdAt: string;
}

export interface StripePaymentRequest {
  amount: number;
  currency: string;
  description: string;
}

export interface StripePaymentResponse {
  clientSecret: string;
}

// Process payment for an order
export const processPayment = async (paymentRequest: PaymentRequest): Promise<PaymentResponse> => {
  const response = await apiClient.post<PaymentResponse>('/buyer/payment/process', paymentRequest);
  return response.data;
};

// Finalize payment
export const finalizePayment = async (paymentId: string): Promise<PaymentResponse> => {
  const response = await apiClient.post<PaymentResponse>('/buyer/payment/finalize', { paymentId });
  return response.data;
};

// Create Stripe payment session
export const createStripeSession = async (paymentRequest: StripePaymentRequest): Promise<StripePaymentResponse> => {
  const response = await apiClient.post<StripePaymentResponse>('/buyer/payment/stripe/create-session', paymentRequest);
  return response.data;
};

// Handle successful Stripe payment
export const handleStripeSuccess = async (sessionId: string): Promise<PaymentResponse> => {
  const response = await apiClient.post<PaymentResponse>('/buyer/payment/stripe/success', { sessionId });
  return response.data;
};

// Backward compatibility
export const createPaymentIntent = createStripeSession; 