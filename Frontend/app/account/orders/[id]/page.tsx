"use client";

import { useAuth } from "@/app/context/AuthContext";
import { useRouter, useParams } from "next/navigation";
import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  FaArrowLeft,
  FaMapMarkerAlt,
  FaCreditCard,
  FaDownload,
  FaPhone,
  FaEnvelope,
  FaTruck,
} from "react-icons/fa";
import AccountNav from "../../../../components/account/AccountNav";
import { getOrder, Order } from "../../../api/services/order";

export default function OrderDetailPage() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const params = useParams();
  const orderId = params.id as string;

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Wait for auth to finish loading before checking authentication
    if (authLoading) {
      return;
    }

    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    const fetchOrder = async () => {
      try {
        const data = await getOrder(orderId);
        if (!data) {
          setError("Order not found");
          return;
        }
        setOrder(data);
      } catch (error) {
        console.error("Error fetching order:", error);
        setError("Failed to load order details");
      } finally {
        setLoading(false);
      }
    };

    if (orderId) {
      fetchOrder();
    }
  }, [isAuthenticated, authLoading, router, orderId]);

  // Show loading state while auth is being checked
  if (authLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  // Show loading state if not authenticated
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="bg-gray-50 min-h-screen">
        <div className="container mx-auto px-4 py-8">
          <div className="max-w-6xl mx-auto">
            <div className="bg-white rounded-lg shadow-sm border p-8 text-center">
              <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
              <p className="text-gray-600">Loading order details...</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="bg-gray-50 min-h-screen">
        <div className="container mx-auto px-4 py-8">
          <div className="max-w-6xl mx-auto">
            <div className="bg-white rounded-lg shadow-sm border p-8 text-center">
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                Order not found
              </h3>
              <p className="text-gray-600 mb-4">
                {error || "The order you're looking for doesn't exist."}
              </p>
              <Link
                href="/account/orders"
                className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
              >
                <FaArrowLeft className="w-4 h-4 mr-2" />
                Back to Orders
              </Link>
            </div>
          </div>
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
        return "bg-purple-100 text-purple-800";
      case "DELIVERED":
        return "bg-green-100 text-green-800";
      case "CANCELLED":
        return "bg-red-100 text-red-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  const formatDate = (date: string) => {
    return new Date(date).toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="bg-gray-50 min-h-screen">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-6xl mx-auto">
          {/* Header */}
          <div className="mb-6">
            <Link
              href="/account/orders"
              className="inline-flex items-center text-blue-600 hover:text-blue-700 mb-4"
            >
              <FaArrowLeft className="w-4 h-4 mr-2" />
              Back to Orders
            </Link>
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h1 className="text-2xl font-bold text-gray-900">
                  Order #{order.id}
                </h1>
                <p className="text-gray-600">
                  Placed on {formatDate(order.orderDate)}
                </p>
              </div>
              <div className="mt-4 sm:mt-0">
                <span
                  className={`px-3 py-1 text-sm font-medium rounded-full ${getStatusColor(
                    order.status
                  )}`}
                >
                  {order.status}
                </span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="orders" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3 space-y-6">
              {/* Order Status Timeline */}
              <div className="bg-white rounded-lg shadow-sm border p-6">
                <h3 className="text-lg font-medium text-gray-900 mb-4">
                  Order Progress
                </h3>
                <OrderTimeline status={order.status} />
              </div>

              {/* Order Items */}
              <div className="bg-white rounded-lg shadow-sm border p-6">
                <h3 className="text-lg font-medium text-gray-900 mb-4">
                  Items Ordered
                </h3>
                <div className="space-y-4">
                  {order.items.map((item, index) => (
                    <div
                      key={index}
                      className="flex items-center gap-4 p-4 border rounded-lg"
                    >
                      <div className="w-16 h-16 bg-gray-200 rounded-md flex-shrink-0">
                        <Image
                          src={item.productImage || "/placeholder-product.png"}
                          alt={item.productName || "Product Image"}
                          width={64}
                          height={64}
                          className="w-full h-full object-cover rounded-md"
                        />
                      </div>
                      <div className="flex-1">
                        <h4 className="font-medium text-gray-900">
                          {item.productName}
                        </h4>
                        <p className="text-sm text-gray-600">
                          Quantity: {item.quantity}
                        </p>
                      </div>
                      <div className="text-right">
                        <p className="font-medium text-gray-900">
                          €{item.price.toFixed(2)}
                        </p>
                        <p className="text-sm text-gray-600">each</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Order Summary & Payment */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Order Summary */}
                <div className="bg-white rounded-lg shadow-sm border p-6">
                  <h3 className="text-lg font-medium text-gray-900 mb-4">
                    Order Summary
                  </h3>
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Subtotal</span>
                      <span className="text-gray-900">
                        €{order.subtotal?.toFixed(2)}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Shipping</span>
                      <span className="text-gray-900">
                        €{order.shippingCost?.toFixed(2)}
                      </span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Tax</span>
                      <span className="text-gray-900">
                        €{order.tax?.toFixed(2)}
                      </span>
                    </div>
                    {order.discount && order.discount > 0 && (
                      <div className="flex justify-between text-green-600">
                        <span>Discount</span>
                        <span>-€{order.discount.toFixed(2)}</span>
                      </div>
                    )}
                    <div className="border-t pt-2 mt-2">
                      <div className="flex justify-between font-medium text-lg">
                        <span className="text-gray-900">Total</span>
                        <span className="text-gray-900">
                          €{(order.totalAmount || order.total).toFixed(2)}
                        </span>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Payment Information */}
                <div className="bg-white rounded-lg shadow-sm border p-6">
                  <h3 className="text-lg font-medium text-gray-900 mb-4">
                    Payment Information
                  </h3>
                  <div className="space-y-3">
                    <div className="flex items-center gap-3">
                      <FaCreditCard className="w-5 h-5 text-gray-400" />
                      <div>
                        <p className="font-medium text-gray-900">
                          {order.paymentMethod || "Credit Card"}
                        </p>
                        <p className="text-sm text-gray-600">
                          {order.paymentId
                            ? `Payment ID: ${order.paymentId}`
                            : "Payment details not available"}
                        </p>
                      </div>
                    </div>
                    <div className="pt-3 border-t">
                      <div className="flex flex-wrap gap-2">
                        <Link
                          href={`/buyer/orders/${order.id}/shipping`}
                          className="inline-flex items-center gap-2 px-3 py-1 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm"
                        >
                          <FaTruck className="w-4 h-4" />
                          Shipping Details
                        </Link>
                        <Link
                          href={`/buyer/orders/${order.id}/tracking`}
                          className="inline-flex items-center gap-2 px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm"
                        >
                          <FaMapMarkerAlt className="w-4 h-4" />
                          Track & Returns
                        </Link>
                        <button className="inline-flex items-center gap-2 text-blue-600 hover:text-blue-700 text-sm">
                          <FaDownload className="w-4 h-4" />
                          Download Invoice
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Shipping & Contact */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Shipping Address */}
                <div className="bg-white rounded-lg shadow-sm border p-6">
                  <h3 className="text-lg font-medium text-gray-900 mb-4">
                    Shipping Address
                  </h3>
                  <div className="flex items-start gap-3">
                    <FaMapMarkerAlt className="w-5 h-5 text-gray-400 mt-1" />
                    <div>
                      <p className="font-medium text-gray-900">
                        {order.shippingAddress
                          ? `${order.shippingAddress.firstName} ${order.shippingAddress.lastName}`
                          : "John Doe"}
                      </p>
                      <p className="text-gray-600">
                        {order.shippingAddress?.address || "123 Main St"}
                      </p>
                      <p className="text-gray-600">
                        {order.shippingAddress?.city || "City"},{" "}
                        {order.shippingAddress?.state || "State"}{" "}
                        {order.shippingAddress?.zipCode || "12345"}
                      </p>
                      <p className="text-gray-600">
                        {order.shippingAddress?.country || "United States"}
                      </p>
                    </div>
                  </div>
                </div>

                {/* Contact & Support */}
                <div className="bg-white rounded-lg shadow-sm border p-6">
                  <h3 className="text-lg font-medium text-gray-900 mb-4">
                    Need Help?
                  </h3>
                  <div className="space-y-3">
                    <div className="flex items-center gap-3">
                      <FaPhone className="w-5 h-5 text-gray-400" />
                      <div>
                        <p className="font-medium text-gray-900">
                          Customer Support
                        </p>
                        <p className="text-sm text-gray-600">1-800-123-4567</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <FaEnvelope className="w-5 h-5 text-gray-400" />
                      <div>
                        <p className="font-medium text-gray-900">
                          Email Support
                        </p>
                        <p className="text-sm text-gray-600">
                          support@example.com
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

interface OrderTimelineProps {
  status: string;
}

function OrderTimeline({ status }: OrderTimelineProps) {
  // If order is cancelled, show special timeline
  if (status === "CANCELLED") {
    return (
      <div className="p-4 bg-red-50 border border-red-200 rounded-lg text-center">
        <p className="text-red-600 font-medium">
          This order has been cancelled
        </p>
      </div>
    );
  }

  const statuses = [
    { key: "PENDING", label: "Order Placed", icon: "📝" },
    { key: "PROCESSING", label: "Processing", icon: "⚙️" },
    { key: "SHIPPED", label: "Shipped", icon: "🚚" },
    { key: "DELIVERED", label: "Delivered", icon: "✅" },
  ];

  // Default to -1 if status not found, which will show all steps as incomplete
  const currentStatusIndex = statuses.findIndex((s) => s.key === status);

  return (
    <div className="flex items-center justify-between">
      {statuses.map((step, index) => (
        <div key={step.key} className="flex flex-col items-center flex-1">
          <div
            className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-medium ${
              index <= currentStatusIndex
                ? "bg-blue-600 text-white"
                : "bg-gray-200 text-gray-600"
            }`}
          >
            {step.icon}
          </div>
          <p
            className={`mt-2 text-sm text-center ${
              index <= currentStatusIndex
                ? "text-gray-900 font-medium"
                : "text-gray-500"
            }`}
          >
            {step.label}
          </p>
          {index < statuses.length - 1 && (
            <div
              className={`h-0.5 w-full mt-4 ${
                index < currentStatusIndex ? "bg-blue-600" : "bg-gray-200"
              }`}
            />
          )}
        </div>
      ))}
    </div>
  );
}
