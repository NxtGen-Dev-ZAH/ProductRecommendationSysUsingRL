import apiClient from '../axios';

export interface OrderItem {
  id?: number;
  productId: number;
  productName?: string;
  productImage?: string;
  quantity: number;
  price: number;
  total?: number;
}

export interface ShippingAddress {
  firstName: string;
  lastName: string;
  email?: string;
  phone: string;
  address: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
}

export interface Order {
  id: number;
  userId: number;
  orderDate: string;
  status: 'PENDING' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
  items: OrderItem[];
  subtotal: number;
  tax?: number;
  shipping: number;
  total: number;
  paymentMethod: string;
  discount?: number;
  totalAmount?: number;
  shippingCost?: number;
  paymentId?: string;
  shippingAddress: ShippingAddress;
}

// Matches backend OrderCheckoutRequest (renamed from OrderRequest in NewChanges.md)
export interface CreateOrderRequest {
  selectedCartItemIds?: number[]; // Optional: for partial checkout
  usedCouponId?: string; // Optional: coupon code to apply
  shippingAddressId: number; // Required: shipping address ID
  billingAddressId: number; // Required: billing address ID
  shippingDetails?: {
    carrier?: string;
    trackingNumber?: string;
    shippingMethod?: string;
    estimatedDelivery?: string;
    shippingCost?: number;
  };
}

// Create a new order (buyer endpoint)
export const createOrder = async (orderData: CreateOrderRequest): Promise<Order> => {
  const response = await apiClient.post<Order>('/buyer/orders', orderData);
  return response.data;
};

// Get orders for the authenticated user
export const getUserOrders = async (userId: number): Promise<Order[]> => {
  const response = await apiClient.get<Order[]>(`/buyer/orders/user/${userId}`);
  return response.data;
};

// Get order by ID
export const getOrderById = async (id: number): Promise<Order> => {
  const response = await apiClient.get<Order>(`/buyer/orders/${id}`);
  return response.data;
};

// Update order status
export const updateOrderStatus = async (id: number, status: string): Promise<Order> => {
  const response = await apiClient.put<Order>(`/buyer/orders/${id}/status`, { status });
  return response.data;
};

// Cancel order (using status update)
export const cancelOrder = async (id: number): Promise<Order> => {
  return updateOrderStatus(id, 'CANCELLED');
};

// Backward compatibility functions
export async function getAllOrders(): Promise<Order[]> {
  console.log('Fetching all orders');
  // Note: This requires user ID, which should come from auth context
  // For now, we'll throw an error to indicate this needs proper implementation
  throw new Error('getAllOrders requires authenticated user context. Use getUserOrders(userId) instead.');
}

export async function getOrder(orderId: string | number): Promise<Order | null> {
  console.log('Fetching order:', orderId);
  
  const id = typeof orderId === 'string' ? parseInt(orderId) : orderId;
  try {
    return await getOrderById(id);
  } catch (error) {
    console.error('Error fetching order:', error);
    return null;
  }
}

// // Mock data for orders
// const mockOrders: Order[] = [
//   {
//     id: 'ORD-1234',
//     userId: 'user123',
//     items: [
//       {
//         id: 'item1',
//         productId: 'prod1',
//         name: 'Wireless Headphones',
//         price: 89.99,
//         quantity: 1,
//         image: '/images/products/headphones.jpg'
//       },
//       {
//         id: 'item2',
//         productId: 'prod2',
//         name: 'Smartphone Case',
//         price: 19.99,
//         quantity: 2,
//         image: '/images/products/case.jpg'
//       }
//     ],
//     total: 129.97,
//     status: 'delivered',
//     createdAt: '2023-10-15T14:30:00Z',
//     shippingAddress: {
//       name: 'John Doe',
//       address: '123 Main St',
//       city: 'Anytown',
//       state: 'CA',
//       postalCode: '12345',
//       country: 'USA',
//       phone: '555-123-4567'
//     },
//     paymentMethod: 'credit_card'
//   },
//   {
//     id: 'ORD-5678',
//     userId: 'user123',
//     items: [
//       {
//         id: 'item3',
//         productId: 'prod3',
//         name: 'Smart Watch',
//         price: 129.99,
//         quantity: 1,
//         image: '/images/products/watch.jpg'
//       }
//     ],
//     total: 129.99,
//     status: 'shipped',
//     createdAt: '2023-11-05T09:15:00Z',
//     shippingAddress: {
//       name: 'John Doe',
//       address: '123 Main St',
//       city: 'Anytown',
//       state: 'CA',
//       postalCode: '12345',
//       country: 'USA',
//       phone: '555-123-4567'
//     },
//     paymentMethod: 'paypal'
//   }
// ]; 