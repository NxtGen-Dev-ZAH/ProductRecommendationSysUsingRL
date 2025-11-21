"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  FaEye,
  FaShoppingCart,
  FaTruck,
  FaMapMarkerAlt,
  FaSearch,
} from "react-icons/fa";
import { Button } from "../../../components/ui/button";
import { useAuth } from "../../context/AuthContext";
import { useToast } from "@/components/ui/toast";
import { getUserOrders, Order } from "../../api/services/order";

export default function BuyerOrdersPage() {
  const router = useRouter();
  const { user, isAuthenticated } = useAuth();
  const { addToast } = useToast();

  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState<string>("");

  const loadOrders = useCallback(async () => {
    try {
      setLoading(true);
      if (user?.id) {
        const ordersData = await getUserOrders(user.id);
        setOrders(ordersData);
      }
    } catch (error) {
      console.error("Error loading orders:", error);
      addToast({
        message: "Failed to load orders",
        type: "error",
      });
    } finally {
      setLoading(false);
    }
  }, [user?.id, addToast]);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    loadOrders();
  }, [isAuthenticated, user, router, loadOrders]);

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

  const filteredOrders = orders.filter((order) => {
    const matchesSearch =
      searchTerm === "" ||
      order.id.toString().includes(searchTerm) ||
      order.items.some((item) =>
        item.productName?.toLowerCase().includes(searchTerm.toLowerCase())
      );

    const matchesStatus = statusFilter === "" || order.status === statusFilter;

    return matchesSearch && matchesStatus;
  });

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-6xl mx-auto">
          {/* Header */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-gray-900">My Orders</h1>
            <p className="text-gray-600 mt-2">Track and manage your orders</p>
          </div>

          {/* Filters */}
          <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="flex-1">
                <div className="relative">
                  <FaSearch className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
                  <input
                    type="text"
                    placeholder="Search by order ID or product name..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>
              <div className="md:w-48">
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">All Statuses</option>
                  <option value="PENDING">Pending</option>
                  <option value="PROCESSING">Processing</option>
                  <option value="SHIPPED">Shipped</option>
                  <option value="DELIVERED">Delivered</option>
                  <option value="CANCELLED">Cancelled</option>
                </select>
              </div>
            </div>
          </div>

          {/* Orders List */}
          {filteredOrders.length > 0 ? (
            <div className="space-y-4">
              {filteredOrders.map((order) => (
                <div
                  key={order.id}
                  className="bg-white rounded-lg shadow-sm border p-6"
                >
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900">
                        Order #{order.id}
                      </h3>
                      <p className="text-sm text-gray-600">
                        Placed on{" "}
                        {new Date(order.orderDate).toLocaleDateString()}
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

                  <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
                    <div>
                      <p className="text-sm text-gray-600">Items</p>
                      <p className="font-medium">
                        {order.items.length} item(s)
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Total Amount</p>
                      <p className="font-medium text-green-600">
                        €{order.total.toFixed(2)}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Payment Method</p>
                      <p className="font-medium capitalize">
                        {order.paymentMethod.replace("_", " ")}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Shipping</p>
                      <p className="font-medium">
                        €{order.shipping.toFixed(2)}
                      </p>
                    </div>
                  </div>

                  {/* Order Items Preview */}
                  <div className="mb-4">
                    <h4 className="font-medium text-gray-900 mb-2">Items:</h4>
                    <div className="space-y-2">
                      {order.items.slice(0, 3).map((item) => (
                        <div
                          key={item.id}
                          className="flex items-center space-x-3 text-sm"
                        >
                          {item.productImage && (
                            <img
                              src={item.productImage}
                              alt={
                                item.productName || `Product ${item.productId}`
                              }
                              className="w-8 h-8 object-cover rounded"
                            />
                          )}
                          <span className="flex-1">
                            {item.productName || `Product ${item.productId}`} ×{" "}
                            {item.quantity}
                          </span>
                          <span className="font-medium">
                            €{(item.price * item.quantity).toFixed(2)}
                          </span>
                        </div>
                      ))}
                      {order.items.length > 3 && (
                        <p className="text-sm text-gray-500">
                          +{order.items.length - 3} more item(s)
                        </p>
                      )}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex flex-wrap gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => router.push(`/buyer/orders/${order.id}`)}
                    >
                      <FaEye className="w-4 h-4 mr-2" />
                      View Details
                    </Button>

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        router.push(`/buyer/orders/${order.id}/shipping`)
                      }
                    >
                      <FaTruck className="w-4 h-4 mr-2" />
                      Shipping Info
                    </Button>

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        router.push(`/buyer/orders/${order.id}/tracking`)
                      }
                    >
                      <FaMapMarkerAlt className="w-4 h-4 mr-2" />
                      Track Order
                    </Button>

                    {order.status === "DELIVERED" && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() =>
                          router.push(`/buyer/orders/${order.id}/tracking`)
                        }
                        className="text-orange-600 border-orange-200 hover:bg-orange-50"
                      >
                        Request Return
                      </Button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="bg-white rounded-lg shadow-sm border p-12 text-center">
              <FaShoppingCart className="w-16 h-16 text-gray-400 mx-auto mb-4" />
              <h3 className="text-xl font-medium text-gray-900 mb-2">
                {searchTerm || statusFilter
                  ? "No matching orders found"
                  : "No orders yet"}
              </h3>
              <p className="text-gray-600 mb-6">
                {searchTerm || statusFilter
                  ? "Try adjusting your search criteria"
                  : "When you place orders, they'll appear here"}
              </p>
              {!searchTerm && !statusFilter && (
                <Button onClick={() => router.push("/")}>Start Shopping</Button>
              )}
            </div>
          )}

          {/* Summary Stats */}
          {orders.length > 0 && (
            <div className="mt-8 grid grid-cols-1 md:grid-cols-4 gap-4">
              <div className="bg-white rounded-lg shadow-sm border p-4 text-center">
                <p className="text-2xl font-bold text-gray-900">
                  {orders.length}
                </p>
                <p className="text-sm text-gray-600">Total Orders</p>
              </div>
              <div className="bg-white rounded-lg shadow-sm border p-4 text-center">
                <p className="text-2xl font-bold text-green-600">
                  {orders.filter((o) => o.status === "DELIVERED").length}
                </p>
                <p className="text-sm text-gray-600">Delivered</p>
              </div>
              <div className="bg-white rounded-lg shadow-sm border p-4 text-center">
                <p className="text-2xl font-bold text-blue-600">
                  {orders.filter((o) => o.status === "SHIPPED").length}
                </p>
                <p className="text-sm text-gray-600">In Transit</p>
              </div>
              <div className="bg-white rounded-lg shadow-sm border p-4 text-center">
                <p className="text-2xl font-bold text-gray-900">
                  $
                  {orders
                    .reduce((sum, order) => sum + order.total, 0)
                    .toFixed(2)}
                </p>
                <p className="text-sm text-gray-600">Total Spent</p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
