"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  FaTruck,
  FaMapMarkerAlt,
  FaEdit,
  FaSave,
  FaTimes,
  FaArrowLeft,
  FaReceipt,
} from "react-icons/fa";
import { Button } from "../../../../../components/ui/button";
import { useAuth } from "../../../../context/AuthContext";
import { useToast } from "@/components/ui/toast";
import {
  OrderShippingRequest,
  OrderShippingResponse,
  OrderShippingCredentialResponse,
  OrderBillingCredentialResponse,
  getOrderShipping,
  getOrderShippingCredential,
  getOrderBillingCredential,
  createOrderShipping,
  createOrderShippingCredential,
  createOrderBillingCredential,
  updateOrderShipping,
  updateOrderShippingCredential,
  updateOrderBillingCredential,
  formatShippingStatus,
  getShippingStatusColor,
} from "../../../../api/services/shipping";
import { getOrderById, Order } from "../../../../api/services/order";

// Validation schemas
const shippingDetailsSchema = z.object({
  shippingCarrier: z.string().min(1, "Shipping carrier is required"),
  shippingMethod: z.string().min(1, "Shipping method is required"),
  shippingMethodCurrency: z.string().min(1, "Currency is required"),
  shippingPrice: z.string().min(1, "Price is required"),
  trackingUrl: z.string().url().optional().or(z.literal("")),
  trackingNumber: z.string().optional(),
  labelUrl: z.string().url().optional().or(z.literal("")),
  label: z.string().optional(),
  shippingQuantity: z.number().min(1, "Quantity must be at least 1"),
  shippingWeight: z.string().min(1, "Weight is required"),
  shippingDimensionRegularOrNot: z.boolean().optional(),
  shippingDimensionHeight: z.string().optional(),
  shippingDimensionWidth: z.string().optional(),
  shippingDimensionDepth: z.string().optional(),
});

const shippingCredentialSchema = z.object({
  recipientName: z.string().min(1, "Recipient name is required"),
  recipientEmail: z.string().email("Valid email is required"),
  addressLine1: z.string().min(1, "Address line 1 is required"),
  addressLine2: z.string().optional(),
  city: z.string().min(1, "City is required"),
  state: z.string().optional(),
  postalCode: z.string().min(1, "Postal code is required"),
  country: z.string().min(1, "Country is required"),
  phoneNumber: z.string().optional(),
  reference: z.string().optional(),
});

const billingCredentialSchema = z.object({
  billingClientName: z.string().min(1, "Billing name is required"),
  billingClientEmail: z.string().email("Valid email is required"),
  billingAddressLine1: z.string().min(1, "Billing address is required"),
  billingAddressLine2: z.string().optional(),
  billingCity: z.string().min(1, "Billing city is required"),
  billingPostalCode: z.string().min(1, "Billing postal code is required"),
  billingCountry: z.string().min(1, "Billing country is required"),
  billingPhoneNumber: z.string().optional(),
});

type ShippingDetailsFormData = z.infer<typeof shippingDetailsSchema>;
type ShippingCredentialFormData = z.infer<typeof shippingCredentialSchema>;
type BillingCredentialFormData = z.infer<typeof billingCredentialSchema>;

export default function OrderShippingPage() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const { addToast } = useToast();

  const orderId = parseInt(params.id as string);

  const [order, setOrder] = useState<Order | null>(null);
  const [shippingDetails, setShippingDetails] =
    useState<OrderShippingResponse | null>(null);
  const [shippingCredential, setShippingCredential] =
    useState<OrderShippingCredentialResponse | null>(null);
  const [billingCredential, setBillingCredential] =
    useState<OrderBillingCredentialResponse | null>(null);

  const [loading, setLoading] = useState(true);
  const [editingSection, setEditingSection] = useState<
    "shipping" | "address" | "billing" | null
  >(null);

  // Form configurations
  const shippingForm = useForm<ShippingDetailsFormData>({
    resolver: zodResolver(shippingDetailsSchema),
  });

  const addressForm = useForm<ShippingCredentialFormData>({
    resolver: zodResolver(shippingCredentialSchema),
  });

  const billingForm = useForm<BillingCredentialFormData>({
    resolver: zodResolver(billingCredentialSchema),
  });

  const loadOrderData = useCallback(async () => {
    try {
      setLoading(true);

      // Load order details
      const orderData = await getOrderById(orderId);
      setOrder(orderData);

      // Load shipping details (may not exist yet)
      try {
        const shippingData = await getOrderShipping(orderId);
        setShippingDetails(shippingData);
        shippingForm.reset({
          ...shippingData,
          shippingQuantity: shippingData.shippingQuantity || 1,
        });
      } catch {
        console.log("No shipping details found");
      }

      // Load shipping credential (may not exist yet)
      try {
        const credentialData = await getOrderShippingCredential(orderId);
        setShippingCredential(credentialData);
        addressForm.reset(credentialData);
      } catch {
        console.log("No shipping credential found");
      }

      // Load billing credential (may not exist yet)
      try {
        const billingData = await getOrderBillingCredential(orderId);
        setBillingCredential(billingData);
        billingForm.reset(billingData);
      } catch {
        console.log("No billing credential found");
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
  }, [orderId, addToast, shippingForm, addressForm, billingForm]);

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    loadOrderData();
  }, [isAuthenticated, orderId, router, loadOrderData]);

  const handleShippingSubmit = async (data: ShippingDetailsFormData) => {
    try {
      const shippingRequest: OrderShippingRequest = {
        ...data,
        trackingUrl: data.trackingUrl || undefined,
        trackingNumber: data.trackingNumber || undefined,
        labelUrl: data.labelUrl || undefined,
        label: data.label || undefined,
      };

      let result;
      if (shippingDetails) {
        result = await updateOrderShipping(orderId, shippingRequest);
      } else {
        result = await createOrderShipping(orderId, shippingRequest);
      }

      setShippingDetails(result);
      setEditingSection(null);
      addToast({
        message: "Shipping details saved successfully",
        type: "success",
      });
    } catch (error) {
      console.error("Error saving shipping details:", error);
      addToast({
        message: "Failed to save shipping details",
        type: "error",
      });
    }
  };

  const handleAddressSubmit = async (data: ShippingCredentialFormData) => {
    try {
      let result;
      if (shippingCredential) {
        result = await updateOrderShippingCredential(orderId, data);
      } else {
        result = await createOrderShippingCredential(orderId, data);
      }

      setShippingCredential(result);
      setEditingSection(null);
      addToast({
        message: "Shipping address saved successfully",
        type: "success",
      });
    } catch (error) {
      console.error("Error saving shipping address:", error);
      addToast({
        message: "Failed to save shipping address",
        type: "error",
      });
    }
  };

  const handleBillingSubmit = async (data: BillingCredentialFormData) => {
    try {
      let result;
      if (billingCredential) {
        result = await updateOrderBillingCredential(orderId, data);
      } else {
        result = await createOrderBillingCredential(orderId, data);
      }

      setBillingCredential(result);
      setEditingSection(null);
      addToast({
        message: "Billing address saved successfully",
        type: "success",
      });
    } catch (error) {
      console.error("Error saving billing address:", error);
      addToast({
        message: "Failed to save billing address",
        type: "error",
      });
    }
  };

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
              Shipping Details - Order #{orderId}
            </h1>
            <p className="text-gray-600 mt-2">
              Manage shipping information and addresses for this order
            </p>
          </div>

          <div className="space-y-6">
            {/* Order Summary */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">
                Order Summary
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
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
                  <p className="font-medium">${order.total.toFixed(2)}</p>
                </div>
              </div>
            </div>

            {/* Shipping Details */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                  <FaTruck className="w-5 h-5 mr-2 text-blue-600" />
                  Shipping Details
                </h2>
                {editingSection !== "shipping" && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setEditingSection("shipping")}
                  >
                    <FaEdit className="w-4 h-4 mr-2" />
                    {shippingDetails ? "Edit" : "Add"}
                  </Button>
                )}
              </div>

              {editingSection === "shipping" ? (
                <form
                  onSubmit={shippingForm.handleSubmit(handleShippingSubmit)}
                  className="space-y-4"
                >
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Shipping Carrier *
                      </label>
                      <input
                        {...shippingForm.register("shippingCarrier")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="e.g., FedEx, UPS, DHL"
                      />
                      {shippingForm.formState.errors.shippingCarrier && (
                        <p className="text-red-500 text-sm mt-1">
                          {
                            shippingForm.formState.errors.shippingCarrier
                              .message
                          }
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Shipping Method *
                      </label>
                      <input
                        {...shippingForm.register("shippingMethod")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="e.g., Standard, Express, Overnight"
                      />
                      {shippingForm.formState.errors.shippingMethod && (
                        <p className="text-red-500 text-sm mt-1">
                          {shippingForm.formState.errors.shippingMethod.message}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Price *
                      </label>
                      <input
                        {...shippingForm.register("shippingPrice")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="19.99"
                      />
                      {shippingForm.formState.errors.shippingPrice && (
                        <p className="text-red-500 text-sm mt-1">
                          {shippingForm.formState.errors.shippingPrice.message}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Currency *
                      </label>
                      <select
                        {...shippingForm.register("shippingMethodCurrency")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      >
                        <option value="">Select currency</option>
                        <option value="USD">USD</option>
                        <option value="EUR">EUR</option>
                        <option value="GBP">GBP</option>
                        <option value="CAD">CAD</option>
                      </select>
                      {shippingForm.formState.errors.shippingMethodCurrency && (
                        <p className="text-red-500 text-sm mt-1">
                          {
                            shippingForm.formState.errors.shippingMethodCurrency
                              .message
                          }
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Tracking Number
                      </label>
                      <input
                        {...shippingForm.register("trackingNumber")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="1234567890"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Tracking URL
                      </label>
                      <input
                        {...shippingForm.register("trackingUrl")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="https://track.carrier.com/123456"
                      />
                      {shippingForm.formState.errors.trackingUrl && (
                        <p className="text-red-500 text-sm mt-1">
                          {shippingForm.formState.errors.trackingUrl.message}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Quantity *
                      </label>
                      <input
                        type="number"
                        min="1"
                        {...shippingForm.register("shippingQuantity", {
                          valueAsNumber: true,
                        })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="1"
                      />
                      {shippingForm.formState.errors.shippingQuantity && (
                        <p className="text-red-500 text-sm mt-1">
                          {
                            shippingForm.formState.errors.shippingQuantity
                              .message
                          }
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Weight *
                      </label>
                      <input
                        {...shippingForm.register("shippingWeight")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="2.5 kg"
                      />
                      {shippingForm.formState.errors.shippingWeight && (
                        <p className="text-red-500 text-sm mt-1">
                          {shippingForm.formState.errors.shippingWeight.message}
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="border-t pt-4">
                    <div className="flex items-center mb-4">
                      <input
                        type="checkbox"
                        {...shippingForm.register(
                          "shippingDimensionRegularOrNot"
                        )}
                        className="mr-2"
                      />
                      <label className="text-sm text-gray-700">
                        Add custom dimensions
                      </label>
                    </div>

                    {shippingForm.watch("shippingDimensionRegularOrNot") && (
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">
                            Height
                          </label>
                          <input
                            {...shippingForm.register(
                              "shippingDimensionHeight"
                            )}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder="10 cm"
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">
                            Width
                          </label>
                          <input
                            {...shippingForm.register("shippingDimensionWidth")}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder="20 cm"
                          />
                        </div>

                        <div>
                          <label className="block text-sm font-medium text-gray-700 mb-1">
                            Depth
                          </label>
                          <input
                            {...shippingForm.register("shippingDimensionDepth")}
                            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder="30 cm"
                          />
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="flex space-x-2">
                    <Button type="submit">
                      <FaSave className="w-4 h-4 mr-2" />
                      Save
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setEditingSection(null)}
                    >
                      <FaTimes className="w-4 h-4 mr-2" />
                      Cancel
                    </Button>
                  </div>
                </form>
              ) : shippingDetails ? (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <p className="text-sm text-gray-600">Carrier</p>
                    <p className="font-medium">
                      {shippingDetails.shippingCarrier}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Method</p>
                    <p className="font-medium">
                      {shippingDetails.shippingMethod}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Price</p>
                    <p className="font-medium">
                      {shippingDetails.shippingPrice}{" "}
                      {shippingDetails.shippingMethodCurrency}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Status</p>
                    <span
                      className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full bg-${getShippingStatusColor(
                        shippingDetails.status
                      )}-100 text-${getShippingStatusColor(
                        shippingDetails.status
                      )}-800`}
                    >
                      {formatShippingStatus(shippingDetails.status)}
                    </span>
                  </div>
                  {shippingDetails.trackingNumber && (
                    <div>
                      <p className="text-sm text-gray-600">Tracking Number</p>
                      <p className="font-medium">
                        {shippingDetails.trackingNumber}
                      </p>
                    </div>
                  )}
                  {shippingDetails.trackingUrl && (
                    <div>
                      <p className="text-sm text-gray-600">Track Package</p>
                      <a
                        href={shippingDetails.trackingUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-blue-600 hover:text-blue-800 font-medium"
                      >
                        Track Package →
                      </a>
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-gray-500">No shipping details added yet</p>
              )}
            </div>

            {/* Shipping Address */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                  <FaMapMarkerAlt className="w-5 h-5 mr-2 text-green-600" />
                  Shipping Address
                </h2>
                {editingSection !== "address" && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setEditingSection("address")}
                  >
                    <FaEdit className="w-4 h-4 mr-2" />
                    {shippingCredential ? "Edit" : "Add"}
                  </Button>
                )}
              </div>

              {editingSection === "address" ? (
                <form
                  onSubmit={addressForm.handleSubmit(handleAddressSubmit)}
                  className="space-y-4"
                >
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Recipient Name *
                      </label>
                      <input
                        {...addressForm.register("recipientName")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="John Doe"
                      />
                      {addressForm.formState.errors.recipientName && (
                        <p className="text-red-500 text-sm mt-1">
                          {addressForm.formState.errors.recipientName.message}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Email Address *
                      </label>
                      <input
                        type="email"
                        {...addressForm.register("recipientEmail")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="john@example.com"
                      />
                      {addressForm.formState.errors.recipientEmail && (
                        <p className="text-red-500 text-sm mt-1">
                          {addressForm.formState.errors.recipientEmail.message}
                        </p>
                      )}
                    </div>

                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Address Line 1 *
                      </label>
                      <input
                        {...addressForm.register("addressLine1")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="123 Main Street"
                      />
                      {addressForm.formState.errors.addressLine1 && (
                        <p className="text-red-500 text-sm mt-1">
                          {addressForm.formState.errors.addressLine1.message}
                        </p>
                      )}
                    </div>

                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Address Line 2
                      </label>
                      <input
                        {...addressForm.register("addressLine2")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="Apt 4B"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        City *
                      </label>
                      <input
                        {...addressForm.register("city")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="New York"
                      />
                      {addressForm.formState.errors.city && (
                        <p className="text-red-500 text-sm mt-1">
                          {addressForm.formState.errors.city.message}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        State/Province
                      </label>
                      <input
                        {...addressForm.register("state")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="NY"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Postal Code *
                      </label>
                      <input
                        {...addressForm.register("postalCode")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="10001"
                      />
                      {addressForm.formState.errors.postalCode && (
                        <p className="text-red-500 text-sm mt-1">
                          {addressForm.formState.errors.postalCode.message}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Country *
                      </label>
                      <input
                        {...addressForm.register("country")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="United States"
                      />
                      {addressForm.formState.errors.country && (
                        <p className="text-red-500 text-sm mt-1">
                          {addressForm.formState.errors.country.message}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Phone Number
                      </label>
                      <input
                        {...addressForm.register("phoneNumber")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="+1 (555) 123-4567"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Reference
                      </label>
                      <input
                        {...addressForm.register("reference")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="Delivery instructions"
                      />
                    </div>
                  </div>

                  <div className="flex space-x-2">
                    <Button type="submit">
                      <FaSave className="w-4 h-4 mr-2" />
                      Save
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setEditingSection(null)}
                    >
                      <FaTimes className="w-4 h-4 mr-2" />
                      Cancel
                    </Button>
                  </div>
                </form>
              ) : shippingCredential ? (
                <div className="space-y-3">
                  <div>
                    <p className="font-medium">
                      {shippingCredential.recipientName}
                    </p>
                    <p className="text-gray-600">
                      {shippingCredential.recipientEmail}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-900">
                      {shippingCredential.addressLine1}
                    </p>
                    {shippingCredential.addressLine2 && (
                      <p className="text-gray-900">
                        {shippingCredential.addressLine2}
                      </p>
                    )}
                    <p className="text-gray-900">
                      {shippingCredential.city}, {shippingCredential.state}{" "}
                      {shippingCredential.postalCode}
                    </p>
                    <p className="text-gray-900">
                      {shippingCredential.country}
                    </p>
                  </div>
                  {shippingCredential.phoneNumber && (
                    <div>
                      <p className="text-gray-600">
                        Phone: {shippingCredential.phoneNumber}
                      </p>
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-gray-500">No shipping address added yet</p>
              )}
            </div>

            {/* Billing Address */}
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-gray-900 flex items-center">
                  <FaReceipt className="w-5 h-5 mr-2 text-purple-600" />
                  Billing Address
                </h2>
                {editingSection !== "billing" && (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setEditingSection("billing")}
                  >
                    <FaEdit className="w-4 h-4 mr-2" />
                    {billingCredential ? "Edit" : "Add"}
                  </Button>
                )}
              </div>

              {editingSection === "billing" ? (
                <form
                  onSubmit={billingForm.handleSubmit(handleBillingSubmit)}
                  className="space-y-4"
                >
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Billing Name *
                      </label>
                      <input
                        {...billingForm.register("billingClientName")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="John Doe"
                      />
                      {billingForm.formState.errors.billingClientName && (
                        <p className="text-red-500 text-sm mt-1">
                          {
                            billingForm.formState.errors.billingClientName
                              .message
                          }
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Billing Email *
                      </label>
                      <input
                        type="email"
                        {...billingForm.register("billingClientEmail")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="john@example.com"
                      />
                      {billingForm.formState.errors.billingClientEmail && (
                        <p className="text-red-500 text-sm mt-1">
                          {
                            billingForm.formState.errors.billingClientEmail
                              .message
                          }
                        </p>
                      )}
                    </div>

                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Billing Address Line 1 *
                      </label>
                      <input
                        {...billingForm.register("billingAddressLine1")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="123 Main Street"
                      />
                      {billingForm.formState.errors.billingAddressLine1 && (
                        <p className="text-red-500 text-sm mt-1">
                          {
                            billingForm.formState.errors.billingAddressLine1
                              .message
                          }
                        </p>
                      )}
                    </div>

                    <div className="md:col-span-2">
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Billing Address Line 2
                      </label>
                      <input
                        {...billingForm.register("billingAddressLine2")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="Apt 4B"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Billing City *
                      </label>
                      <input
                        {...billingForm.register("billingCity")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="New York"
                      />
                      {billingForm.formState.errors.billingCity && (
                        <p className="text-red-500 text-sm mt-1">
                          {billingForm.formState.errors.billingCity.message}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Billing Postal Code *
                      </label>
                      <input
                        {...billingForm.register("billingPostalCode")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="10001"
                      />
                      {billingForm.formState.errors.billingPostalCode && (
                        <p className="text-red-500 text-sm mt-1">
                          {
                            billingForm.formState.errors.billingPostalCode
                              .message
                          }
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Billing Country *
                      </label>
                      <input
                        {...billingForm.register("billingCountry")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="United States"
                      />
                      {billingForm.formState.errors.billingCountry && (
                        <p className="text-red-500 text-sm mt-1">
                          {billingForm.formState.errors.billingCountry.message}
                        </p>
                      )}
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Billing Phone Number
                      </label>
                      <input
                        {...billingForm.register("billingPhoneNumber")}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="+1 (555) 123-4567"
                      />
                    </div>
                  </div>

                  <div className="flex space-x-2">
                    <Button type="submit">
                      <FaSave className="w-4 h-4 mr-2" />
                      Save
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => setEditingSection(null)}
                    >
                      <FaTimes className="w-4 h-4 mr-2" />
                      Cancel
                    </Button>
                  </div>
                </form>
              ) : billingCredential ? (
                <div className="space-y-3">
                  <div>
                    <p className="font-medium">
                      {billingCredential.billingClientName}
                    </p>
                    <p className="text-gray-600">
                      {billingCredential.billingClientEmail}
                    </p>
                  </div>
                  <div>
                    <p className="text-gray-900">
                      {billingCredential.billingAddressLine1}
                    </p>
                    {billingCredential.billingAddressLine2 && (
                      <p className="text-gray-900">
                        {billingCredential.billingAddressLine2}
                      </p>
                    )}
                    <p className="text-gray-900">
                      {billingCredential.billingCity},{" "}
                      {billingCredential.billingPostalCode}
                    </p>
                    <p className="text-gray-900">
                      {billingCredential.billingCountry}
                    </p>
                  </div>
                  {billingCredential.billingPhoneNumber && (
                    <div>
                      <p className="text-gray-600">
                        Phone: {billingCredential.billingPhoneNumber}
                      </p>
                    </div>
                  )}
                </div>
              ) : (
                <p className="text-gray-500">No billing address added yet</p>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
