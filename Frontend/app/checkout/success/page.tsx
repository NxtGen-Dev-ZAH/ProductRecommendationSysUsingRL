"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import {
  FaCheckCircle,
  FaShoppingBag,
  FaRegCreditCard,
  FaTruck,
} from "react-icons/fa";
import { getOrderById, Order, OrderItem } from "../../api/services/order";
import { Button } from "../../../components/ui/button";

// Wrapper component that uses searchParams
function CheckoutSuccess() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const orderId = searchParams.get("orderId");
  const [orderDetails, setOrderDetails] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchOrderDetails = async () => {
      if (!orderId) {
        router.push("/");
        return;
      }

      try {
        setLoading(true);
        const order = await getOrderById(Number(orderId));
        setOrderDetails(order);
      } catch (err) {
        setError("Failed to load order details");
        console.error("Error fetching order:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchOrderDetails();
  }, [orderId, router]);

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-12">
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
        </div>
      </div>
    );
  }

  if (error || !orderDetails) {
    return (
      <div className="container mx-auto px-4 py-12 text-center">
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error || "Order not found"}
        </div>
        <Link
          href="/"
          className="mt-4 inline-block text-[#3b82f6] hover:underline"
        >
          Return to homepage
        </Link>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-12">
      <div className="max-w-3xl mx-auto bg-white rounded-lg shadow-md overflow-hidden">
        <div className="bg-[#60a5fa] py-6 px-8 text-white text-center">
          <FaCheckCircle className="mx-auto h-16 w-16 mb-4" />
          <h1 className="text-3xl font-bold">Thank You For Your Order!</h1>
          <p className="mt-2">Your order has been placed successfully.</p>
        </div>

        <div className="p-8">
          <div className="border-b pb-6 mb-6">
            <div className="flex justify-between mb-2">
              <h2 className="text-lg font-semibold">Order #</h2>
              <span>{orderId}</span>
            </div>
            <div className="flex justify-between mb-2">
              <h2 className="text-lg font-semibold">Date</h2>
              <span>{new Date().toLocaleDateString()}</span>
            </div>
            <div className="flex justify-between">
              <h2 className="text-lg font-semibold">Total</h2>
              <span className="font-bold text-[#3b82f6]">
                €{orderDetails.total.toFixed(2)}
              </span>
            </div>
          </div>

          <h2 className="text-xl font-bold mb-4">Order Details</h2>

          <div className="space-y-4 mb-6">
            {orderDetails.items?.map((item: OrderItem, index: number) => (
              <div
                key={index}
                className="flex justify-between items-center border-b pb-4"
              >
                <div>
                  <p className="font-medium">{item.productName}</p>
                  <p className="text-sm text-gray-500">Qty: {item.quantity}</p>
                </div>
                <p className="font-medium">
                  €{(item.price * item.quantity).toFixed(2)}
                </p>
              </div>
            ))}

            <div className="flex justify-between pt-2">
              <span>Subtotal</span>
              <span>€{orderDetails.subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between">
              <span>Shipping</span>
              <span>€{orderDetails.shipping.toFixed(2)}</span>
            </div>
            <div className="flex justify-between font-bold border-t pt-4">
              <span>Total</span>
              <span>€{orderDetails.total.toFixed(2)}</span>
            </div>
          </div>

          <div className="bg-gray-50 p-6 rounded-lg mb-6">
            <h2 className="text-lg font-semibold mb-4">Shipping Information</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <h3 className="font-medium text-gray-600">
                  Contact Information
                </h3>
                <p>
                  {orderDetails.shippingAddress.firstName}{" "}
                  {orderDetails.shippingAddress.lastName}
                </p>
                <p>{orderDetails.shippingAddress.email}</p>
                <p>{orderDetails.shippingAddress.phone}</p>
              </div>
              <div>
                <h3 className="font-medium text-gray-600">Shipping Address</h3>
                <p>{orderDetails.shippingAddress.address}</p>
                <p>
                  {orderDetails.shippingAddress.city},{" "}
                  {orderDetails.shippingAddress.state}{" "}
                  {orderDetails.shippingAddress.zipCode}
                </p>
                <p>{orderDetails.shippingAddress.country}</p>
              </div>
            </div>
          </div>

          <div className="border-t pt-6">
            <h2 className="text-xl font-bold mb-4">What&apos;s Next?</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="text-center">
                <div className="bg-primary/10 rounded-full p-4 mb-2 mx-auto w-16 h-16 flex items-center justify-center">
                  <FaRegCreditCard className="text-[#3b82f6] h-8 w-8" />
                </div>
                <h3 className="font-semibold mb-1">Payment Confirmed</h3>
                <p className="text-sm text-gray-600">
                  Your payment has been successfully processed.
                </p>
              </div>
              <div className="text-center">
                <div className="bg-primary/10 rounded-full p-4 mb-2 mx-auto w-16 h-16 flex items-center justify-center">
                  <FaShoppingBag className="text-[#3b82f6] h-8 w-8" />
                </div>
                <h3 className="font-semibold mb-1">Order Processing</h3>
                <p className="text-sm text-gray-600">
                  We&apos;re preparing your order for shipping.
                </p>
              </div>
              <div className="text-center">
                <div className="bg-primary/10 rounded-full p-4 mb-2 mx-auto w-16 h-16 flex items-center justify-center">
                  <FaTruck className="text-[#3b82f6] h-8 w-8" />
                </div>
                <h3 className="font-semibold mb-1">Shipping Soon</h3>
                <p className="text-sm text-gray-600">
                  You&apos;ll receive a shipping confirmation email soon.
                </p>
              </div>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row justify-between items-center gap-4 mt-8">
            <Link href="/account/orders">
              <Button variant="outline" className="w-full sm:w-auto">
                View My Orders
              </Button>
            </Link>
            <Link href="/">
              <Button className="w-full sm:w-auto">Continue Shopping</Button>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

// Loading fallback component
function CheckoutSuccessLoading() {
  return (
    <div className="container mx-auto px-4 py-12">
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
      </div>
    </div>
  );
}

// Main export with Suspense boundary
export default function CheckoutSuccessPage() {
  return (
    <Suspense fallback={<CheckoutSuccessLoading />}>
      <CheckoutSuccess />
    </Suspense>
  );
}
