"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  FaHeart,
  FaTimes,
  FaShoppingBag,
  FaShoppingCart,
} from "react-icons/fa";
import { useAuth } from "../context/AuthContext";
import {
  getMyFavoriteProducts,
  toggleFavoriteProduct,
  Product,
} from "../api/services/user";
import ProductCard from "../../components/ProductCard";
import { Button } from "../../components/ui/button";

export default function WishlistPage() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [removingProducts, setRemovingProducts] = useState<Set<number>>(
    new Set()
  );
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  useEffect(() => {
    // Wait for auth to finish loading before checking authentication
    if (authLoading) {
      return;
    }

    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    loadFavoriteProducts();
  }, [isAuthenticated, authLoading, router]);

  const loadFavoriteProducts = async () => {
    try {
      setLoading(true);
      const data = await getMyFavoriteProducts(0, 20);
      setProducts(data.content);
    } catch (error) {
      console.error("Error loading favorite products:", error);
      setMessage({ type: "error", text: "Failed to load wishlist" });
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveFromWishlist = async (productId: number) => {
    try {
      setRemovingProducts((prev) => new Set(prev).add(productId));

      const result = await toggleFavoriteProduct(productId);

      if (!result.isFavorite) {
        setProducts((prev) =>
          prev.filter((product) => product.id !== productId)
        );
        setMessage({ type: "success", text: "Product removed from wishlist" });
      }
    } catch (error) {
      console.error("Error removing from wishlist:", error);
      setMessage({
        type: "error",
        text: "Failed to remove product from wishlist",
      });
    } finally {
      setRemovingProducts((prev) => {
        const newSet = new Set(prev);
        newSet.delete(productId);
        return newSet;
      });
    }
  };

  const handleAddToCart = (productId: number) => {
    // This would be implemented with cart functionality
    console.log("Add to cart:", productId);
    setMessage({ type: "success", text: "Product added to cart" });
  };

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

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-6xl mx-auto">
          {/* Header */}
          <div className="mb-8">
            <h1 className="text-3xl font-bold text-gray-900 mb-2">
              Ma Liste de Souhaits
            </h1>
            <p className="text-gray-600">
              Gardez une trace des articles que vous aimez et que vous souhaitez
              acheter plus tard
            </p>
          </div>

          {/* Message Display */}
          {message && (
            <div
              className={`mb-6 p-4 rounded-md ${
                message.type === "success"
                  ? "bg-green-50 text-green-700 border border-green-200"
                  : "bg-red-50 text-red-700 border border-red-200"
              }`}
            >
              <div className="flex items-center justify-between">
                <span>{message.text}</span>
                <button
                  onClick={() => setMessage(null)}
                  className="text-sm hover:underline"
                >
                  Fermer
                </button>
              </div>
            </div>
          )}

          {/* Loading State */}
          {loading ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
              {[...Array(8)].map((_, index) => (
                <div
                  key={index}
                  className="bg-white rounded-lg shadow-sm border p-4 animate-pulse"
                >
                  <div className="w-full h-48 bg-gray-200 rounded-lg mb-4"></div>
                  <div className="h-4 bg-gray-200 rounded mb-2"></div>
                  <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                  <div className="h-6 bg-gray-200 rounded w-1/2"></div>
                </div>
              ))}
            </div>
          ) : products.length > 0 ? (
            <>
              {/* Wishlist Stats */}
              <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <FaHeart className="w-6 h-6 text-red-500" />
                    <div>
                      <h3 className="text-lg font-medium text-gray-900">
                        {products.length}{" "}
                        {products.length === 1 ? "Article" : "Articles"} dans la
                        Liste
                      </h3>
                      <p className="text-sm text-gray-600">
                        Valeur totale : €
                        {products
                          .reduce((sum, product) => sum + product.price, 0)
                          .toFixed(2)}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      onClick={() => router.push("/products")}
                    >
                      Continuer les Achats
                    </Button>
                    <Button
                      onClick={() => {
                        // Add all to cart functionality
                        products.forEach((product) =>
                          handleAddToCart(product.id)
                        );
                      }}
                    >
                      Tout Ajouter au Panier
                    </Button>
                  </div>
                </div>
              </div>

              {/* Products Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                {products.map((product) => (
                  <div key={product.id} className="relative group">
                    <ProductCard
                      product={{
                        ...product,
                        quantity: 0,
                        categoryId: 0,
                        discountPercentage: 0,
                        rating: 0,
                        reviewCount: 0,
                        vendor: {
                          id: product.sellerId,
                          name: product.sellerName || "Unknown Vendor",
                        },
                      }}
                    />

                    {/* Overlay Actions */}
                    <div className="absolute top-3 right-3 flex flex-col gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => handleRemoveFromWishlist(product.id)}
                        disabled={removingProducts.has(product.id)}
                        className="p-2 bg-red-500 text-white rounded-full hover:bg-red-600 transition-colors shadow-lg disabled:opacity-50"
                        title="Retirer de la liste de souhaits"
                      >
                        {removingProducts.has(product.id) ? (
                          <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        ) : (
                          <FaTimes className="w-4 h-4" />
                        )}
                      </button>
                    </div>

                    {/* Quick Add to Cart */}
                    <div className="absolute bottom-3 left-3 right-3 opacity-0 group-hover:opacity-100 transition-opacity">
                      <Button
                        onClick={() => handleAddToCart(product.id)}
                        className="w-full bg-black bg-opacity-80 text-white hover:bg-opacity-100 transition-all"
                        size="sm"
                      >
                        <FaShoppingCart className="w-4 h-4 mr-2" />
                        Ajouter au Panier
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            </>
          ) : (
            /* Empty State */
            <div className="text-center py-16">
              <div className="max-w-md mx-auto">
                <FaHeart className="w-16 h-16 text-gray-400 mx-auto mb-6" />
                <h3 className="text-xl font-medium text-gray-900 mb-4">
                  Votre Liste de Souhaits est Vide
                </h3>
                <p className="text-gray-600 mb-8">
                  Commencez à ajouter des produits à votre liste de souhaits en
                  cliquant sur l&apos;icône cœur sur n&apos;importe quel produit
                  que vous aimez.
                </p>
                <div className="flex flex-col sm:flex-row gap-4 justify-center">
                  <Button onClick={() => router.push("/products")}>
                    <FaShoppingBag className="w-4 h-4 mr-2" />
                    Parcourir les Produits
                  </Button>
                  <Button
                    variant="outline"
                    onClick={() => router.push("/category")}
                  >
                    Acheter par Catégorie
                  </Button>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
