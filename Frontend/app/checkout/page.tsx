"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { FaLock, FaCreditCard, FaPaypal } from "react-icons/fa";
import { useAuth } from "../context/AuthContext";
import { useCart } from "../context/CartContext";
import { createOrder } from "../api/services/order";
import { processPayment } from "../api/services/payment";
import {
  getUserAddresses,
  addUserAddress,
  type UserAddressRequest,
  type Address,
  type AddressType,
} from "../api/services";
import { Button } from "../../components/ui/button";
import { AddressSelector } from "../../components/address";
import Link from "next/link";

type PaymentMethod = "credit_card" | "paypal";

export default function CheckoutPage() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuth();
  const { cart, loading: cartLoading, clearCart } = useCart();
  const [paymentMethod, setPaymentMethod] =
    useState<PaymentMethod>("credit_card");
  const [orderProcessing, setOrderProcessing] = useState(false);
  const [orderError, setOrderError] = useState<string | null>(null);

  // Address management state
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedShippingAddress, setSelectedShippingAddress] =
    useState<Address | null>(null);
  const [selectedBillingAddress, setSelectedBillingAddress] =
    useState<Address | null>(null);
  const [addressesLoading, setAddressesLoading] = useState(true);
  const [useSameAddress, setUseSameAddress] = useState(true);

  // Load user addresses
  useEffect(() => {
    const loadAddresses = async () => {
      if (!user?.id) return;

      try {
        setAddressesLoading(true);
        const userAddresses = await getUserAddresses();
        setAddresses(userAddresses);

        // Set default addresses if available
        const defaultShipping = userAddresses.find(
          (addr) => addr.addressType === "SHIPPING" && addr.isDefault
        );
        const defaultBilling = userAddresses.find(
          (addr) => addr.addressType === "BILLING" && addr.isDefault
        );

        if (defaultShipping) {
          setSelectedShippingAddress(defaultShipping);
        }
        if (defaultBilling) {
          setSelectedBillingAddress(defaultBilling);
        } else if (defaultShipping) {
          setSelectedBillingAddress(defaultShipping);
        }
      } catch (error) {
        console.error("Failed to load addresses:", error);
        setOrderError("Failed to load addresses. Please try again.");
      } finally {
        setAddressesLoading(false);
      }
    };

    if (isAuthenticated && user?.id) {
      loadAddresses();
    }
  }, [isAuthenticated, user?.id]);

  // Redirect to login if not authenticated
  useEffect(() => {
    if (!isAuthenticated && !cartLoading) {
      router.push("/auth/login?redirect=/checkout");
    }
  }, [isAuthenticated, cartLoading, router]);

  // Redirect to cart if cart is empty
  useEffect(() => {
    if (!cartLoading && (!cart || !cart.items || cart.items.length === 0)) {
      router.push("/cart");
    }
  }, [cart, cartLoading, router]);

  // Update billing address when shipping address changes and useSameAddress is true
  useEffect(() => {
    if (useSameAddress && selectedShippingAddress) {
      setSelectedBillingAddress(selectedShippingAddress);
    }
  }, [selectedShippingAddress, useSameAddress]);

  // Use backend-calculated values from CartResponse (NewChanges.md)
  const calculateSubtotal = () => {
    // Use subtotalPrice from backend CartResponse
    return cart?.subtotalPrice ?? cart?.subtotal ?? 0;
  };

  const calculateShipping = () => {
    // Use totalShippingCost from backend CartResponse
    return cart?.totalShippingCost ?? 0;
  };

  const calculateDiscount = () => {
    // Use totalDiscount from backend CartResponse
    return cart?.totalDiscount ?? cart?.discountAmount ?? 0;
  };

  const calculateTotal = () => {
    // Use totalAmount from backend CartResponse (already includes subtotal + shipping - discount)
    return cart?.totalAmount ?? 0;
  };

  // Address management functions
  const handleAddNewAddress = async (addressData: UserAddressRequest) => {
    if (!user?.id) return;

    try {
      const newAddress = await addUserAddress(addressData);
      setAddresses((prev) => [...prev, newAddress]);

      // Auto-select the new address if it's the first of its type
      if (addressData.addressType === "SHIPPING" && !selectedShippingAddress) {
        setSelectedShippingAddress(newAddress);
      }
      if (addressData.addressType === "BILLING" && !selectedBillingAddress) {
        setSelectedBillingAddress(newAddress);
      }
    } catch (error) {
      console.error("Failed to add address:", error);
      setOrderError("Failed to add address. Please try again.");
    }
  };

  const handleAddressSelect = (address: Address, type: AddressType) => {
    if (type === "SHIPPING") {
      setSelectedShippingAddress(address);
    } else if (type === "BILLING") {
      setSelectedBillingAddress(address);
    }
  };

  const canProceedToPayment = () => {
    return selectedShippingAddress && selectedBillingAddress;
  };

  const handleSubmit = async () => {
    if (!cart || !selectedShippingAddress || !selectedBillingAddress) return;

    setOrderProcessing(true);
    setOrderError(null);

    try {
      // 1. Process payment
      const paymentResult = await processPayment({
        amount: calculateTotal(),
        method: paymentMethod,
        // In a real app, we would collect and send card details or redirect to PayPal
      });

      // 2. Validate payment was successful
      if (paymentResult.status !== 'succeeded' && paymentResult.status !== 'pending') {
        throw new Error(`Payment failed with status: ${paymentResult.status}. Please try again.`);
      }

      // 3. Create order with address IDs (new unified address system)
      const orderData = {
        selectedCartItemIds: cart.items.map((item) => item.id).filter((id): id is number => id !== undefined),
        shippingAddressId: selectedShippingAddress.id,
        billingAddressId: selectedBillingAddress.id,
        // Optional: Add shipping details if needed
        shippingDetails: {
          shippingCost: calculateShipping(),
        },
      };

      const order = await createOrder(orderData);

      // 4. Clear cart only after successful order creation
      await clearCart();

      // 5. Redirect to success page with order and payment info
      router.push(`/checkout/success?orderId=${order.id}&paymentId=${paymentResult.id}&paymentStatus=${paymentResult.status}`);
    } catch (err: unknown) {
      console.error("Checkout error:", err);
      const errorMessage =
        err instanceof Error
          ? err.message
          : "Failed to process your order. Please try again.";
      setOrderError(errorMessage);
    } finally {
      setOrderProcessing(false);
    }
  };

  if (cartLoading || !isAuthenticated) {
    return (
      <div className="container mx-auto px-4 py-12">
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-12">
      <h1 className="text-3xl font-bold mb-8">Paiement</h1>

      {orderError && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-6">
          {orderError}
        </div>
      )}

      <div className="flex flex-col lg:flex-row gap-8">
        {/* Checkout form */}
        <div className="lg:w-2/3">
          <div>
            {/* Shipping address selection */}
            <div className="bg-white rounded-lg shadow p-6 mb-6">
              {(!addressesLoading && addresses.filter(a => a.addressType === "SHIPPING").length === 0) && (
                <div className="mb-4 text-sm text-blue-700 bg-blue-50 border border-blue-200 rounded p-3">
                  You have no shipping addresses yet. Please add one below.
                </div>
              )}
              <AddressSelector
                addresses={addresses.filter(
                  (addr) => addr.addressType === "SHIPPING"
                )}
                addressType="SHIPPING"
                selectedAddressId={selectedShippingAddress?.id}
                onSelect={(address) => handleAddressSelect(address, "SHIPPING")}
                onAddNew={handleAddNewAddress}
                isLoading={addressesLoading}
                title="Adresse de Livraison"
                description="Sélectionnez l'adresse où vous souhaitez recevoir votre commande"
              />
            </div>

            {/* Billing address selection */}
            <div className="bg-white rounded-lg shadow p-6 mb-6">
              <div className="mb-4">
                <div className="flex items-center">
                  <input
                    id="useSameAddress"
                    type="checkbox"
                    checked={useSameAddress}
                    onChange={(e) => setUseSameAddress(e.target.checked)}
                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                  />
                  <label
                    htmlFor="useSameAddress"
                    className="ml-2 block text-sm text-gray-700"
                  >
                    Utiliser la même adresse pour la facturation
                  </label>
                </div>
              </div>

              {!useSameAddress && (
                <AddressSelector
                  addresses={addresses.filter(
                    (addr) => addr.addressType === "BILLING"
                  )}
                  addressType="BILLING"
                  selectedAddressId={selectedBillingAddress?.id}
                  onSelect={(address) =>
                    handleAddressSelect(address, "BILLING")
                  }
                  onAddNew={handleAddNewAddress}
                  isLoading={addressesLoading}
                  title="Adresse de Facturation"
                  description="Sélectionnez l'adresse de facturation pour votre commande"
                />
              )}
            </div>

            {/* Payment method */}
            <div className="bg-white rounded-lg shadow p-6 mb-6">
              <h2 className="text-xl font-semibold mb-4">
                Méthode de Paiement
              </h2>

              <div className="space-y-4">
                <label className="flex items-center p-4 border rounded-md cursor-pointer">
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="credit_card"
                    checked={paymentMethod === "credit_card"}
                    onChange={() => setPaymentMethod("credit_card")}
                    className="mr-3"
                  />
                  <FaCreditCard className="text-gray-600 mr-3" />
                  <span>Carte de Crédit</span>
                </label>

                <label className="flex items-center p-4 border rounded-md cursor-pointer">
                  <input
                    type="radio"
                    name="paymentMethod"
                    value="paypal"
                    checked={paymentMethod === "paypal"}
                    onChange={() => setPaymentMethod("paypal")}
                    className="mr-3"
                  />
                  <FaPaypal className="text-blue-500 mr-3" />
                  <span>PayPal</span>
                </label>
              </div>

              {paymentMethod === "credit_card" && (
                <div className="mt-6 space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Numéro de Carte
                    </label>
                    <input
                      type="text"
                      placeholder="4242 4242 4242 4242"
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Date d&apos;Expiration
                      </label>
                      <input
                        type="text"
                        placeholder="MM/AA"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        CVC
                      </label>
                      <input
                        type="text"
                        placeholder="123"
                        className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      />
                    </div>
                  </div>
                </div>
              )}

              <div className="mt-6 text-sm text-gray-500 flex items-center">
                <FaLock className="mr-2" />
                <span>
                  Vos informations de paiement sont sécurisées. Nous utilisons
                  le chiffrement pour protéger vos données.
                </span>
              </div>
            </div>

            <div className="flex justify-between items-center">
              <Link href="/cart" className="text-[#3b82f6] hover:underline">
                Retour au Panier
              </Link>

              <Button
                onClick={handleSubmit}
                disabled={orderProcessing || !canProceedToPayment()}
                className="py-3 px-6"
              >
                {orderProcessing ? "Traitement..." : "Finaliser la Commande"}
              </Button>
            </div>
          </div>
        </div>

        {/* Order summary */}
        <div className="lg:w-1/3">
          <div className="bg-white rounded-lg shadow p-6 sticky top-24">
            <h2 className="text-xl font-semibold mb-4">
              Résumé de la Commande
            </h2>

            <div className="divide-y">
              {cart?.items.map((item) => (
                <div key={item.id} className="py-3 flex">
                  <div className="relative h-16 w-16 flex-shrink-0">
                    {/* Note: CartItem doesn't have imageUrl, using placeholder */}
                    <div className="h-full w-full bg-gray-200 rounded"></div>
                    <span className="absolute -top-1 -right-1 bg-gray-200 text-gray-800 text-xs rounded-full w-5 h-5 flex items-center justify-center">
                      {item.quantity}
                    </span>
                  </div>
                  <div className="ml-4 flex-1">
                    <p className="text-sm font-medium">{item.productName}</p>
                    <p className="text-sm text-gray-500">
                      €{(item.price ?? 0).toFixed(2)} x {item.quantity}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-medium">
                      €{((item.price ?? 0) * item.quantity).toFixed(2)}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            <div className="border-t mt-6 pt-4 space-y-2">
              <div className="flex justify-between">
                <span>Sous-total</span>
                <span>€{calculateSubtotal().toFixed(2)}</span>
              </div>
              {calculateDiscount() > 0 && (
                <div className="flex justify-between text-green-600">
                  <span>Réduction</span>
                  <span>-€{calculateDiscount().toFixed(2)}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span>Livraison</span>
                <span>
                  {calculateShipping() === 0
                    ? "Gratuit"
                    : `€${calculateShipping().toFixed(2)}`}
                </span>
              </div>
              <div className="border-t pt-2 mt-2 flex justify-between font-bold">
                <span>Total</span>
                <span>€{calculateTotal().toFixed(2)}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
