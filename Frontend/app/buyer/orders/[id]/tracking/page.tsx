"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  FaTruck,
  FaMapMarkerAlt,
  FaUndo,
  FaArrowLeft,
  FaClock,
  FaCheckCircle,
  FaTimesCircle,
  FaExclamationTriangle,
  FaExternalLinkAlt,
} from "react-icons/fa";
import { Button } from "../../../../../components/ui/button";
import { useAuth } from "../../../../context/AuthContext";
import { useToast } from "@/components/ui/toast";
import {
  ShippingTrackingResponse,
  ReturnRequestRequest,
  ReturnRequestResponse,
  getShippingTracking,
  createReturnRequest,
  getReturnRequests,
  formatReturnStatus,
  getReturnStatusColor,
} from "../../../../api/services/shipping";
import { getOrderById, Order } from "../../../../api/services/order";

// Validation schema for return request
const returnRequestSchema = z.object({
  reason: z.string().min(10, "Reason must be at least 10 characters long"),
  orderItemIds: z
    .array(z.number())
    .min(1, "Please select at least one item to return"),
  refundPercentage: z.number().min(0).max(1).optional(),
});

type ReturnRequestFormData = z.infer<typeof returnRequestSchema>;

export default function OrderTrackingPage() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { addToast } = useToast();

  const orderId = parseInt(params.id as string);

  const [order, setOrder] = useState<Order | null>(null);
  const [trackingInfo, setTrackingInfo] =
    useState<ShippingTrackingResponse | null>(null);
  const [returnRequests, setReturnRequests] = useState<ReturnRequestResponse[]>(
    []
  );
  const [loading, setLoading] = useState(true);
  const [showReturnForm, setShowReturnForm] = useState(false);
  const [selectedItems, setSelectedItems] = useState<number[]>([]);

  // Return request form
  const returnForm = useForm<ReturnRequestFormData>({
    resolver: zodResolver(returnRequestSchema),
    defaultValues: {
      reason: "",
      orderItemIds: [],
      refundPercentage: 1.0, // Full refund by default
    },
  });

  const loadOrderData = useCallback(async () => {
    try {
      setLoading(true);

      // Load order details
      const orderData = await getOrderById(orderId);
      setOrder(orderData);

      // Load tracking information (may not exist yet)
      try {
        const trackingData = await getShippingTracking(orderId);
        setTrackingInfo(trackingData);
      } catch {
        console.log("No tracking information found");
      }

      // Load return requests
      try {
        const returnData = await getReturnRequests(orderId);
        setReturnRequests(returnData);
      } catch {
        console.log("No return requests found");
      }
    } catch (error) {
      console.error("Error loading order data:", error);
      addToast({
        message: "Failed to load order data",
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

    loadOrderData();
  }, [isAuthenticated, orderId, router, loadOrderData]);

  const handleReturnSubmit = async (data: ReturnRequestFormData) => {
    try {
      const returnRequest: ReturnRequestRequest = {
        reason: data.reason,
        orderItemIds: selectedItems,
        orderId: orderId,
        refundPercentage: data.refundPercentage,
      };

      const result = await createReturnRequest(orderId, returnRequest);
      setReturnRequests([...returnRequests, result]);
      setShowReturnForm(false);
      setSelectedItems([]);
      returnForm.reset();

      addToast({
        message: "Return request submitted successfully",
        type: "success",
      });
    } catch (error) {
      console.error("Error submitting return request:", error);
      addToast({
        message: "Failed to submit return request",
        type: "error",
      });
    }
  };

  const handleItemSelection = (itemId: number) => {
    setSelectedItems((prev) => {
      if (prev.includes(itemId)) {
        return prev.filter((id) => id !== itemId);
      } else {
        return [...prev, itemId];
      }
    });
  };

  useEffect(() => {
    returnForm.setValue("orderItemIds", selectedItems);
  }, [selectedItems, returnForm]);

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

  const canRequestReturn =
    order.status === "DELIVERED" &&
    !returnRequests.some(
      (req) => req.status === "PENDING" || req.status === "APPROVED"
    );

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
          {/* Header */}
          <div className="mb-8">
            <Button
              variant="ghost"
              onClick={() => router.push(`/buyer/orders/${orderId}`)}
              className="mb-4"
            >
              <FaArrowLeft className="w-4 h-4 mr-2" />
              Back to Order
            </Button>
            <h1 className="text-3xl font-bold text-gray-900">
              Order Tracking - Order #{orderId}
            </h1>
            <p className="text-gray-600 mt-2">
              Track your order and manage returns
            </p>
          </div>

          <div className="space-y-6">
            {/* Order Summary */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Order Summary
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                  <p className="text-sm text-gray-600">Order Status</p>
                  <p className="font-medium">{order.status}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Order Date</p>
                  <p className="font-medium">
                    {new Date(order.orderDate).toLocaleDateString()}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Total Amount</p>
                  <p className="font-medium">€{order.total.toFixed(2)}</p>
                </div>
                <div>
                  <p className="text-sm text-gray-600">Items</p>
                  <p className="font-medium">{order.items.length} item(s)</p>
                </div>
              </div>
            </div>

            {/* Tracking Information */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                  <FaTruck className="w-5 h-5 mr-2 text-blue-600" />
                  Tracking Information
                </h2>
              </div>

              {trackingInfo ? (
                <div className="space-y-4">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm text-gray-600">Tracking Number</p>
                      <p className="font-medium font-mono">
                        {trackingInfo.trackingNumber}
                      </p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Carrier Status</p>
                      <p className="font-medium">
                        {trackingInfo.carrierStatus}
                      </p>
                    </div>
                    {trackingInfo.estimatedDeliveryDate && (
                      <div>
                        <p className="text-sm text-gray-600">
                          Estimated Delivery
                        </p>
                        <p className="font-medium">
                          {new Date(
                            trackingInfo.estimatedDeliveryDate
                          ).toLocaleDateString()}
                        </p>
                      </div>
                    )}
                    {trackingInfo.lastUpdated && (
                      <div>
                        <p className="text-sm text-gray-600">Last Updated</p>
                        <p className="font-medium">
                          {new Date(trackingInfo.lastUpdated).toLocaleString()}
                        </p>
                      </div>
                    )}
                  </div>

                  {/* Mock tracking timeline */}
                  <div className="border-t pt-4">
                    <h3 className="font-medium text-gray-900 mb-3">
                      Tracking Timeline
                    </h3>
                    <div className="space-y-3">
                      <div className="flex items-start space-x-3">
                        <div className="flex-shrink-0 w-6 h-6 bg-green-100 rounded-full flex items-center justify-center">
                          <FaCheckCircle className="w-3 h-3 text-green-600" />
                        </div>
                        <div>
                          <p className="font-medium text-gray-900">
                            Order Confirmed
                          </p>
                          <p className="text-sm text-gray-600">
                            {new Date(order.orderDate).toLocaleString()}
                          </p>
                        </div>
                      </div>

                      {order.status !== "PENDING" && (
                        <div className="flex items-start space-x-3">
                          <div className="flex-shrink-0 w-6 h-6 bg-blue-100 rounded-full flex items-center justify-center">
                            <FaCheckCircle className="w-3 h-3 text-blue-600" />
                          </div>
                          <div>
                            <p className="font-medium text-gray-900">
                              Order Processed
                            </p>
                            <p className="text-sm text-gray-600">
                              Your order is being prepared
                            </p>
                          </div>
                        </div>
                      )}

                      {(order.status === "SHIPPED" ||
                        order.status === "DELIVERED") && (
                        <div className="flex items-start space-x-3">
                          <div className="flex-shrink-0 w-6 h-6 bg-blue-100 rounded-full flex items-center justify-center">
                            <FaTruck className="w-3 h-3 text-blue-600" />
                          </div>
                          <div>
                            <p className="font-medium text-gray-900">
                              Order Shipped
                            </p>
                            <p className="text-sm text-gray-600">
                              Tracking: {trackingInfo.trackingNumber}
                            </p>
                          </div>
                        </div>
                      )}

                      {order.status === "DELIVERED" && (
                        <div className="flex items-start space-x-3">
                          <div className="flex-shrink-0 w-6 h-6 bg-green-100 rounded-full flex items-center justify-center">
                            <FaMapMarkerAlt className="w-3 h-3 text-green-600" />
                          </div>
                          <div>
                            <p className="font-medium text-gray-900">
                              Delivered
                            </p>
                            <p className="text-sm text-gray-600">
                              Package delivered successfully
                            </p>
                          </div>
                        </div>
                      )}

                      {order.status === "PENDING" && (
                        <div className="flex items-start space-x-3">
                          <div className="flex-shrink-0 w-6 h-6 bg-yellow-100 rounded-full flex items-center justify-center">
                            <FaClock className="w-3 h-3 text-yellow-600" />
                          </div>
                          <div>
                            <p className="font-medium text-gray-900">
                              Processing
                            </p>
                            <p className="text-sm text-gray-600">
                              Your order is being processed
                            </p>
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="text-center py-8">
                  <FaTruck className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-500">
                    No tracking information available yet
                  </p>
                  <p className="text-sm text-gray-400 mt-1">
                    Tracking details will appear once your order ships
                  </p>
                </div>
              )}
            </div>

            {/* Return Requests */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                  <FaUndo className="w-5 h-5 mr-2 text-orange-600" />
                  Return Requests
                </h2>
                {canRequestReturn && !showReturnForm && (
                  <Button
                    onClick={() => setShowReturnForm(true)}
                    className="bg-orange-600 hover:bg-orange-700"
                  >
                    <FaUndo className="w-4 h-4 mr-2" />
                    Request Return
                  </Button>
                )}
              </div>

              {showReturnForm ? (
                <form
                  onSubmit={returnForm.handleSubmit(handleReturnSubmit)}
                  className="space-y-6"
                >
                  {/* Item Selection */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-3">
                      Select Items to Return *
                    </label>
                    <div className="space-y-2 border rounded-md p-4">
                      {order.items.map((item) => (
                        <div
                          key={item.id}
                          className="flex items-center space-x-3"
                        >
                          <input
                            type="checkbox"
                            checked={selectedItems.includes(item.id!)}
                            onChange={() => handleItemSelection(item.id!)}
                            className="rounded border-gray-300"
                          />
                          <div className="flex-1">
                            <p className="font-medium">
                              {item.productName || `Product ${item.productId}`}
                            </p>
                            <p className="text-sm text-gray-600">
                              Quantity: {item.quantity} × $
                              {item.price.toFixed(2)}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                    {returnForm.formState.errors.orderItemIds && (
                      <p className="text-red-500 text-sm mt-1">
                        {returnForm.formState.errors.orderItemIds.message}
                      </p>
                    )}
                  </div>

                  {/* Reason */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Reason for Return *
                    </label>
                    <textarea
                      {...returnForm.register("reason")}
                      rows={4}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      placeholder="Please describe why you want to return these items..."
                    />
                    {returnForm.formState.errors.reason && (
                      <p className="text-red-500 text-sm mt-1">
                        {returnForm.formState.errors.reason.message}
                      </p>
                    )}
                  </div>

                  {/* Refund Percentage */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Refund Amount
                    </label>
                    <select
                      {...returnForm.register("refundPercentage", {
                        valueAsNumber: true,
                      })}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                      <option value={1.0}>Full Refund (100%)</option>
                      <option value={0.75}>Partial Refund (75%)</option>
                      <option value={0.5}>Partial Refund (50%)</option>
                      <option value={0.25}>Partial Refund (25%)</option>
                    </select>
                  </div>

                  <div className="flex space-x-2">
                    <Button type="submit">Submit Return Request</Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => {
                        setShowReturnForm(false);
                        setSelectedItems([]);
                        returnForm.reset();
                      }}
                    >
                      Cancel
                    </Button>
                  </div>
                </form>
              ) : returnRequests.length > 0 ? (
                <div className="space-y-4">
                  {returnRequests.map((returnRequest) => (
                    <div
                      key={returnRequest.id}
                      className="border rounded-lg p-4"
                    >
                      <div className="flex items-center justify-between mb-3">
                        <h3 className="font-medium">
                          Return Request #{returnRequest.id}
                        </h3>
                        <span
                          className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-${getReturnStatusColor(
                            returnRequest.status
                          )}-100 text-${getReturnStatusColor(
                            returnRequest.status
                          )}-800`}
                        >
                          {formatReturnStatus(returnRequest.status)}
                        </span>
                      </div>

                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-3">
                        <div>
                          <p className="text-sm text-gray-600">Request Date</p>
                          <p className="font-medium">
                            {new Date(
                              returnRequest.requestDate
                            ).toLocaleDateString()}
                          </p>
                        </div>
                        {returnRequest.refundAmount && (
                          <div>
                            <p className="text-sm text-gray-600">
                              Refund Amount
                            </p>
                            <p className="font-medium">
                              €{returnRequest.refundAmount.toFixed(2)}
                            </p>
                          </div>
                        )}
                      </div>

                      <div className="mb-3">
                        <p className="text-sm text-gray-600">Reason</p>
                        <p className="text-gray-900">{returnRequest.reason}</p>
                      </div>

                      <div>
                        <p className="text-sm text-gray-600">Items</p>
                        <p className="text-gray-900">
                          {returnRequest.orderItemIds.length} item(s) selected
                          for return
                        </p>
                      </div>

                      {returnRequest.status === "REJECTED" && (
                        <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-md">
                          <div className="flex items-center">
                            <FaTimesCircle className="w-4 h-4 text-red-600 mr-2" />
                            <p className="text-sm text-red-800">
                              This return request has been rejected. Please
                              contact customer support for more information.
                            </p>
                          </div>
                        </div>
                      )}

                      {returnRequest.status === "APPROVED" && (
                        <div className="mt-3 p-3 bg-green-50 border border-green-200 rounded-md">
                          <div className="flex items-center">
                            <FaCheckCircle className="w-4 h-4 text-green-600 mr-2" />
                            <p className="text-sm text-green-800">
                              Your return request has been approved. Please
                              follow the return instructions sent to your email.
                            </p>
                          </div>
                        </div>
                      )}

                      {returnRequest.status === "PENDING" && (
                        <div className="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded-md">
                          <div className="flex items-center">
                            <FaClock className="w-4 h-4 text-yellow-600 mr-2" />
                            <p className="text-sm text-yellow-800">
                              Your return request is being reviewed. We&apos;ll
                              notify you once a decision is made.
                            </p>
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8">
                  <FaUndo className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                  <p className="text-gray-500">No return requests</p>
                  {!canRequestReturn && (
                    <p className="text-sm text-gray-400 mt-1">
                      Return requests are only available for delivered orders
                    </p>
                  )}
                </div>
              )}
            </div>

            {/* Help Section */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
              <div className="flex items-start space-x-3">
                <FaExclamationTriangle className="w-5 h-5 text-blue-600 mt-0.5" />
                <div>
                  <h3 className="font-medium text-blue-900">Need Help?</h3>
                  <p className="text-blue-800 text-sm mt-1">
                    If you have questions about your order tracking or return
                    requests, please contact our customer support team.
                  </p>
                  <div className="mt-3 space-x-2">
                    <Button variant="outline" size="sm">
                      Contact Support
                    </Button>
                    <Button variant="outline" size="sm">
                      <FaExternalLinkAlt className="w-3 h-3 mr-1" />
                      Help Center
                    </Button>
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
