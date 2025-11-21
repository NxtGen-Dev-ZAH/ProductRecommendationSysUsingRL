"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  FaArrowLeft,
  FaTruck,
  FaMapMarkerAlt,
  FaUndo,
  FaReceipt,
  FaShoppingCart,
  FaClock,
} from "react-icons/fa";
import { Button } from "../../../../components/ui/button";
import { useAuth } from "../../../context/AuthContext";
import { useToast } from "@/components/ui/toast";
import { getOrderById, Order } from "../../../api/services/order";

export default function OrderDetailsPage() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { addToast } = useToast();

  const orderId = parseInt(params.id as string);

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);

  const loadOrder = useCallback(async () => {
    try {
      setLoading(true);
      const orderData = await getOrderById(orderId);
      setOrder(orderData);
    } catch (error) {
      console.error("Error loading order:", error);
      addToast({
        message: "Failed to load order details",
        type: "error",
      });
    } finally {
      setLoading(false);
    }
  }, [orderId, addToast]);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    loadOrder();
  }, [isAuthenticated, orderId, router, loadOrder]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!order) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">
            Order Not Found
          </h1>
          <Button onClick={() => router.push("/buyer/orders")}>
            Back to Orders
          </Button>
        </div>
      </div>
    );
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case "PENDING":
        return "bg-yellow-100 text-yellow-800";
      case "PROCESSING":
        return "bg-blue-100 text-blue-800";
      case "SHIPPED":
        return "bg-indigo-100 text-indigo-800";
      case "DELIVERED":
        return "bg-green-100 text-green-800";
      case "CANCELLED":
        return "bg-red-100 text-red-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          {/* Header */}
          <div className="mb-8">
            <Button
              variant="ghost"
              onClick={() => router.push("/buyer/orders")}
              className="mb-4"
            >
              <FaArrowLeft className="w-4 h-4 mr-2" />
              Back to Orders
            </Button>
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-3xl font-bold text-gray-900">
                  Order #{orderId}
                </h1>
                <p className="text-gray-600 mt-2">
                  Placed on {new Date(order.orderDate).toLocaleDateString()}
                </p>
              </div>
              <span
                className={`inline-flex px-3 py-1 text-sm font-semibold rounded-full ${getStatusColor(
                  order.status
                )}`}
              >
                {order.status}
              </span>
            </div>
          </div>

          <div className="space-y-6">
            {/* Quick Actions */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Quick Actions
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <Button
                  variant="outline"
                  onClick={() =>
                    router.push(`/buyer/orders/${orderId}/shipping`)
                  }
                  className="flex items-center justify-center p-4 h-auto"
                >
                  <div className="text-center">
                    <FaTruck className="w-6 h-6 mx-auto mb-2 text-blue-600" />
                    <div className="font-medium">Shipping Details</div>
                    <div className="text-sm text-gray-600">
                      Manage shipping info
                    </div>
                  </div>
                </Button>

                <Button
                  variant="outline"
                  onClick={() =>
                    router.push(`/buyer/orders/${orderId}/tracking`)
                  }
                  className="flex items-center justify-center p-4 h-auto"
                >
                  <div className="text-center">
                    <FaMapMarkerAlt className="w-6 h-6 mx-auto mb-2 text-green-600" />
                    <div className="font-medium">Track Order</div>
                    <div className="text-sm text-gray-600">
                      View tracking & returns
                    </div>
                  </div>
                </Button>

                <Button
                  variant="outline"
                  onClick={() => window.print()}
                  className="flex items-center justify-center p-4 h-auto"
                >
                  <div className="text-center">
                    <FaReceipt className="w-6 h-6 mx-auto mb-2 text-purple-600" />
                    <div className="font-medium">Print Receipt</div>
                    <div className="text-sm text-gray-600">
                      Download invoice
                    </div>
                  </div>
                </Button>

                {order.status === "DELIVERED" && (
                  <Button
                    variant="outline"
                    onClick={() =>
                      router.push(`/buyer/orders/${orderId}/tracking`)
                    }
                    className="flex items-center justify-center p-4 h-auto"
                  >
                    <div className="text-center">
                      <FaUndo className="w-6 h-6 mx-auto mb-2 text-orange-600" />
                      <div className="font-medium">Return Items</div>
                      <div className="text-sm text-gray-600">
                        Request return
                      </div>
                    </div>
                  </Button>
                )}
              </div>
            </div>

            {/* Order Summary */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Order Summary
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div>
                  <p className="text-sm text-gray-600">Order Status</p>
                  <p className="font-medium text-lg">{order.status}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Payment Method</p>
                  <p className="font-medium text-lg">{order.paymentMethod}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Total Amount</p>
                  <p className="font-medium text-lg text-green-600">
                    €{order.total.toFixed(2)}
                  </p>
                </div>
              </div>
            </div>

            {/* Order Items */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                <FaShoppingCart className="w-5 h-5 mr-2" />
                Order Items ({order.items.length})
              </h2>
              <div className="space-y-4">
                {order.items.map((item) => (
                  <div
                    key={item.id}
                    className="flex items-center space-x-4 p-4 border rounded-lg"
                  >
                    {item.productImage && (
                      <div className="flex-shrink-0">
                        <img
                          src={item.productImage}
                          alt={item.productName || `Product ${item.productId}`}
                          className="w-16 h-16 object-cover rounded-md"
                        />
                      </div>
                    )}
                    <div className="flex-1 min-w-0">
                      <h3 className="font-medium text-gray-900">
                        {item.productName || `Product ${item.productId}`}
                      </h3>
                      <p className="text-sm text-gray-600">
                        Quantity: {item.quantity}
                      </p>
                      <p className="text-sm text-gray-600">
                        Price: €{item.price.toFixed(2)} each
                      </p>
                    </div>
                    <div className="flex-shrink-0">
                      <p className="font-medium text-gray-900">
                        €{(item.price * item.quantity).toFixed(2)}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Price Breakdown */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Price Breakdown
              </h2>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-gray-600">Subtotal</span>
                  <span className="font-medium">
                    €{order.subtotal.toFixed(2)}
                  </span>
                </div>
                {order.tax && (
                  <div className="flex justify-between">
                    <span className="text-gray-600">Tax</span>
                    <span className="font-medium">€{order.tax.toFixed(2)}</span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-gray-600">Shipping</span>
                  <span className="font-medium">
                    €{order.shipping.toFixed(2)}
                  </span>
                </div>
                {order.discount && (
                  <div className="flex justify-between">
                    <span className="text-gray-600">Discount</span>
                    <span className="font-medium text-green-600">
                      -€{order.discount.toFixed(2)}
                    </span>
                  </div>
                )}
                <div className="border-t pt-2">
                  <div className="flex justify-between text-lg font-semibold">
                    <span>Total</span>
                    <span className="text-green-600">
                      €{order.total.toFixed(2)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Shipping Address */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                <FaMapMarkerAlt className="w-5 h-5 mr-2" />
                Shipping Address
              </h2>
              <div className="text-gray-900">
                <p className="font-medium">
                  {order.shippingAddress.firstName}{" "}
                  {order.shippingAddress.lastName}
                </p>
                <p>{order.shippingAddress.address}</p>
                <p>
                  {order.shippingAddress.city}, {order.shippingAddress.state}{" "}
                  {order.shippingAddress.zipCode}
                </p>
                <p>{order.shippingAddress.country}</p>
                {order.shippingAddress.phone && (
                  <p className="mt-2 text-gray-600">
                    Phone: {order.shippingAddress.phone}
                  </p>
                )}
              </div>
            </div>

            {/* Order Timeline */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
                <FaClock className="w-5 h-5 mr-2" />
                Order Timeline
              </h2>
              <div className="space-y-4">
                <div className="flex items-start space-x-3">
                  <div className="flex-shrink-0 w-6 h-6 bg-green-100 rounded-full flex items-center justify-center">
                    <div className="w-2 h-2 bg-green-600 rounded-full"></div>
                  </div>
                  <div>
                    <p className="font-medium text-gray-900">Order Placed</p>
                    <p className="text-sm text-gray-600">
                      {new Date(order.orderDate).toLocaleString()}
                    </p>
                  </div>
                </div>

                {order.status !== "PENDING" && (
                  <div className="flex items-start space-x-3">
                    <div className="flex-shrink-0 w-6 h-6 bg-blue-100 rounded-full flex items-center justify-center">
                      <div className="w-2 h-2 bg-blue-600 rounded-full"></div>
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">
                        Order Confirmed
                      </p>
                      <p className="text-sm text-gray-600">
                        Your order has been confirmed and is being processed
                      </p>
                    </div>
                  </div>
                )}

                {(order.status === "SHIPPED" ||
                  order.status === "DELIVERED") && (
                  <div className="flex items-start space-x-3">
                    <div className="flex-shrink-0 w-6 h-6 bg-indigo-100 rounded-full flex items-center justify-center">
                      <div className="w-2 h-2 bg-indigo-600 rounded-full"></div>
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">Order Shipped</p>
                      <p className="text-sm text-gray-600">
                        Your order is on its way
                      </p>
                    </div>
                  </div>
                )}

                {order.status === "DELIVERED" && (
                  <div className="flex items-start space-x-3">
                    <div className="flex-shrink-0 w-6 h-6 bg-green-100 rounded-full flex items-center justify-center">
                      <div className="w-2 h-2 bg-green-600 rounded-full"></div>
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">
                        Order Delivered
                      </p>
                      <p className="text-sm text-gray-600">
                        Your order has been delivered successfully
                      </p>
                    </div>
                  </div>
                )}

                {order.status === "CANCELLED" && (
                  <div className="flex items-start space-x-3">
                    <div className="flex-shrink-0 w-6 h-6 bg-red-100 rounded-full flex items-center justify-center">
                      <div className="w-2 h-2 bg-red-600 rounded-full"></div>
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">
                        Order Cancelled
                      </p>
                      <p className="text-sm text-gray-600">
                        This order has been cancelled
                      </p>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
