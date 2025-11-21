"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  FaTrash,
  FaMinus,
  FaPlus,
  FaShoppingCart,
  FaArrowRight,
} from "react-icons/fa";
import { useCart } from "../context/CartContext";
import { Button } from "../../components/ui/button";

export default function CartPage() {
  const router = useRouter();
  const {
    cart,
    loading,
    updateItem,
    removeItem,
    clearCart,
    itemCount,
    applyCouponToCart,
    removeCouponFromCart,
    refreshCart,
  } = useCart();
  const [couponCode, setCouponCode] = useState("");
  const [couponLoading, setCouponLoading] = useState(false);
  const [couponError, setCouponError] = useState<string | null>(null);

  const handleQuantityChange = async (itemId: number, newQuantity: number) => {
    if (newQuantity < 1) return;
    try {
      await updateItem(itemId, newQuantity);
    } catch (error) {
      console.error("Error updating quantity:", error);
      // You could show a toast notification here
    }
  };

  const handleRemoveItem = async (itemId: number) => {
    try {
      console.log("Removing item with ID:", itemId);
      await removeItem(itemId);
      console.log("Item removed successfully");
    } catch (error: unknown) {
      console.error("Error removing item:", error);

      // If the error is related to session issues, try to refresh the cart
      const errorMessage =
        error instanceof Error
          ? error.message
          : (error as { message?: string })?.message || "Unknown error";

      if (errorMessage.includes("session") || errorMessage.includes("cart")) {
        console.log("Session-related error detected, refreshing cart...");
        try {
          // Try to refresh the cart to get the latest state
          await refreshCart();
        } catch (refreshError) {
          console.error("Failed to refresh cart:", refreshError);
        }
      }

      // You could show a toast notification here
      // For now, we'll just log the error
    }
  };

  const handleApplyCoupon = async () => {
    if (!couponCode.trim()) return;

    setCouponLoading(true);
    setCouponError(null);

    try {
      await applyCouponToCart(couponCode.trim());
      setCouponCode("");
    } catch (error: unknown) {
      const errorMessage =
        error instanceof Error
          ? error.message
          : (error as { message?: string })?.message ||
            "Failed to apply coupon";
      setCouponError(errorMessage);
    } finally {
      setCouponLoading(false);
    }
  };

  const handleRemoveCoupon = async () => {
    try {
      await removeCouponFromCart();
    } catch (error) {
      console.error("Error removing coupon:", error);
    }
  };

  // Use backend-calculated values from CartResponse (NewChanges.md)
  const calculateSubtotal = () => {
    // Use subtotalPrice from backend CartResponse
    return cart?.subtotalPrice ?? cart?.subtotal ?? 0;
  };

  const calculateDiscount = () => {
    // Use totalDiscount from backend CartResponse
    return cart?.totalDiscount ?? cart?.discountAmount ?? 0;
  };

  const calculateShipping = () => {
    // Use totalShippingCost from backend CartResponse
    return cart?.totalShippingCost ?? 0;
  };

  const calculateTotal = () => {
    // Use totalAmount from backend CartResponse (already includes subtotal + shipping - discount)
    return cart?.totalAmount ?? 0;
  };

  const handleCheckout = () => {
    router.push("/checkout");
  };

  // Get applied coupon code from couponResponse object
  const getAppliedCouponCode = () => {
    return cart?.couponResponse?.code || null;
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold mb-8">Votre Panier</h1>
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
        </div>
      </div>
    );
  }

  if (!cart?.items?.length) {
    return (
      <div className="container mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold mb-8">Votre Panier</h1>
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <div className="mb-6">
            <FaShoppingCart className="mx-auto h-16 w-16 text-gray-300" />
          </div>
          <h2 className="text-2xl font-semibold mb-4">Votre panier est vide</h2>
          <p className="text-gray-600 mb-6">
            Il semble que vous n&apos;ayez pas encore ajouté de produits à votre
            panier.
          </p>
          <Link href="/product">
            <Button className="px-6">Commencer les Achats</Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-12">
      <h1 className="text-3xl font-bold mb-8">Votre Panier</h1>

      <div className="flex flex-col lg:flex-row gap-8">
        {/* Cart Items */}
        <div className="lg:w-2/3">
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="hidden md:grid grid-cols-12 gap-4 p-4 bg-gray-50 text-sm font-medium text-gray-700">
              <div className="col-span-6">Produit</div>
              <div className="col-span-2 text-center">Prix</div>
              <div className="col-span-2 text-center">Quantité</div>
              <div className="col-span-2 text-right">Total</div>
            </div>

            <div className="divide-y">
              {cart.items.map((item) => (
                <div
                  key={item.id}
                  className="p-4 grid grid-cols-1 md:grid-cols-12 gap-4 items-center"
                >
                  {/* Product */}
                  <div className="md:col-span-6 flex gap-4 items-center">
                    <div className="relative h-20 w-20 flex-shrink-0">
                      <div className="h-full w-full bg-gray-200 rounded flex items-center justify-center">
                        Aucune Image
                      </div>
                    </div>
                    <div>
                      <h3 className="font-medium">
                        <Link
                          href={`/product/${item.productId}`}
                          className="hover:text-[#3b82f6]"
                        >
                          {item.productName || `Product ${item.productId}`}
                        </Link>
                      </h3>
                      <button
                        onClick={() => handleRemoveItem(item.id)}
                        className="text-red-500 text-sm hover:text-red-700 flex items-center gap-1 mt-1"
                      >
                        <FaTrash size={12} /> Supprimer
                      </button>
                    </div>
                  </div>

                  {/* Price */}
                  <div className="md:col-span-2 text-center">
                    <div className="md:hidden inline-block font-medium mr-2">
                      Prix:
                    </div>
                    €{(item.price ?? 0).toFixed(2)}
                  </div>

                  {/* Quantity */}
                  <div className="md:col-span-2 text-center">
                    <div className="md:hidden inline-block font-medium mr-2">
                      Quantité:
                    </div>
                    <div className="flex items-center justify-center">
                      <button
                        onClick={() =>
                          handleQuantityChange(item.id, item.quantity - 1)
                        }
                        disabled={item.quantity <= 1}
                        className="p-1 bg-gray-100 rounded disabled:opacity-50"
                      >
                        <FaMinus size={12} />
                      </button>
                      <span className="mx-3">{item.quantity}</span>
                      <button
                        onClick={() =>
                          handleQuantityChange(item.id, item.quantity + 1)
                        }
                        className="p-1 bg-gray-100 rounded disabled:opacity-50"
                      >
                        <FaPlus size={12} />
                      </button>
                    </div>
                  </div>

                  {/* Total */}
                  <div className="md:col-span-2 text-right font-medium">
                    <div className="md:hidden inline-block font-medium mr-2">
                      Total:
                    </div>
                    €{(item.price * item.quantity).toFixed(2)}
                  </div>
                </div>
              ))}
            </div>

            <div className="p-4 bg-gray-50 flex justify-between">
              <button
                onClick={() => clearCart()}
                className="text-sm text-gray-600 hover:text-gray-900"
              >
                Vider le Panier
              </button>
              <Link
                href="/product"
                className="text-[#3b82f6] hover:text-[#3b82f6]-dark text-sm flex items-center gap-1"
              >
                <span>Continuer les Achats</span> <FaArrowRight size={12} />
              </Link>
            </div>
          </div>
        </div>

        {/* Order Summary */}
        <div className="lg:w-1/3">
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-lg font-bold mb-6">Résumé de la Commande</h2>

            <div className="space-y-4 mb-6">
              <div className="flex justify-between">
                <span>Sous-total ({itemCount} articles)</span>
                <span>€{calculateSubtotal().toFixed(2)}</span>
              </div>

              {getAppliedCouponCode() && (
                <div className="flex justify-between text-green-600">
                  <span>Réduction ({getAppliedCouponCode()})</span>
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

              <div className="border-t pt-4 flex justify-between font-bold">
                <span>Total</span>
                <span>€{calculateTotal().toFixed(2)}</span>
              </div>
            </div>

            {/* Coupon */}
            <div className="mb-6">
              <label
                htmlFor="coupon"
                className="block text-sm font-medium text-gray-700 mb-2"
              >
                {getAppliedCouponCode()
                  ? `Coupon "${getAppliedCouponCode()}" appliqué`
                  : "Appliquer un Code Coupon"}
              </label>
              <div className="flex">
                <input
                  type="text"
                  id="coupon"
                  value={couponCode}
                  onChange={(e) => setCouponCode(e.target.value)}
                  placeholder="Entrer le code coupon"
                  className="flex-grow rounded-l-md border-gray-300 shadow-sm focus:border-[#60a5fa] focus:ring-primary"
                  disabled={!!getAppliedCouponCode() || couponLoading}
                />
                <button
                  onClick={handleApplyCoupon}
                  disabled={
                    !couponCode || !!getAppliedCouponCode() || couponLoading
                  }
                  className="bg-gray-100 px-4 rounded-r-md border border-gray-300 hover:bg-gray-200 disabled:opacity-50"
                >
                  {couponLoading ? "..." : "Appliquer"}
                </button>
              </div>
              {couponError && (
                <p className="text-red-600 text-sm mt-2">{couponError}</p>
              )}
              {getAppliedCouponCode() && (
                <button
                  onClick={handleRemoveCoupon}
                  className="text-sm text-red-600 hover:text-red-800 mt-2"
                >
                  Supprimer le coupon
                </button>
              )}
            </div>

            {/* Checkout button */}
            <Button
              onClick={handleCheckout}
              className="w-full flex items-center justify-center gap-2"
            >
              Procéder au Paiement <FaArrowRight />
            </Button>

            <div className="mt-6 text-sm text-gray-500">
              <p className="mb-2">Nous acceptons :</p>
              <div className="flex gap-2">
                <div className="h-8 w-12 bg-gray-200 rounded"></div>
                <div className="h-8 w-12 bg-gray-200 rounded"></div>
                <div className="h-8 w-12 bg-gray-200 rounded"></div>
                <div className="h-8 w-12 bg-gray-200 rounded"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
