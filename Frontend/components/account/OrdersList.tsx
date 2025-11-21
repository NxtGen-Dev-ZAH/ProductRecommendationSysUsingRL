"use client";

import Link from "next/link";
import { format } from "date-fns";
import { FaEye } from "react-icons/fa";
import { Order } from "../../app/api/services/order";

// Extended Order interface for the OrdersList component
interface ExtendedOrder extends Order {
  createdAt?: string;
}

interface OrdersListProps {
  orders: (Order | ExtendedOrder)[];
  loading?: boolean;
}

const OrdersList = ({ orders, loading = false }: OrdersListProps) => {
  if (loading) {
    return (
      <div className="space-y-4">
        {[...Array(3)].map((_, i) => (
          <div key={i} className="border rounded-lg p-4 animate-pulse">
            <div className="flex justify-between items-center mb-2">
              <div className="h-5 bg-gray-200 rounded w-1/4"></div>
              <div className="h-5 bg-gray-200 rounded w-1/6"></div>
            </div>
            <div className="h-4 bg-gray-200 rounded w-1/3 mb-2"></div>
            <div className="h-4 bg-gray-200 rounded w-1/5"></div>
          </div>
        ))}
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="border rounded-lg p-6 text-center">
        <p className="text-gray-500 mb-4">
          You haven&apos;t placed any orders yet.
        </p>
        <Link href="/product" className="text-[#3b82f6] hover:underline">
          Browse products
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {orders.map((order) => (
        <div
          key={order.id}
          className="border rounded-lg p-4 hover:border-[#60a5fa] transition-colors"
        >
          <div className="flex justify-between items-center mb-2">
            <h3 className="font-medium">Order #{order.id}</h3>
            <span
              className={`px-2 py-1 text-xs rounded-full ${getStatusClass(
                order.status
              )}`}
            >
              {formatStatus(order.status)}
            </span>
          </div>

          <p className="text-sm text-gray-600 mb-2">
            Placed on{" "}
            {format(
              new Date(
                "createdAt" in order
                  ? order.createdAt || order.orderDate
                  : order.orderDate
              ),
              "MMM d, yyyy"
            )}
          </p>

          <div className="flex justify-between items-center">
            <span className="font-medium">€{order.total.toFixed(2)}</span>
            <Link
              href={`/account/orders/${order.id}`}
              className="flex items-center gap-1 text-[#3b82f6] hover:underline"
            >
              <FaEye size={14} />
              <span>View Order</span>
            </Link>
          </div>
        </div>
      ))}
    </div>
  );
};

// Helper functions for order status display
function getStatusClass(status: string) {
  // Convert status to lowercase for case-insensitive comparison
  const statusLower = status.toLowerCase();
  switch (statusLower) {
    case "DELIVERED":
      return "bg-green-100 text-green-800";
    case "SHIPPED":
      return "bg-blue-100 text-blue-800";
    case "processing":
      return "bg-yellow-100 text-yellow-800";
    case "cancelled":
      return "bg-red-100 text-red-800";
    case "pending":
      return "bg-gray-100 text-gray-800";
    default:
      return "bg-gray-100 text-gray-800";
  }
}

function formatStatus(status: string) {
  // Convert to lowercase first, then capitalize first letter
  const statusLower = status.toLowerCase();
  return statusLower.charAt(0).toUpperCase() + statusLower.slice(1);
}

export default OrdersList;
