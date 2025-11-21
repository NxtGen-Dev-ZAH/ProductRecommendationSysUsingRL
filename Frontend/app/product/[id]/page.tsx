"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import {
  FaStar,
  FaRegStar,
  FaStarHalfAlt,
  FaHeart,
  FaRegHeart,
  FaShoppingCart,
  FaMinus,
  FaPlus,
} from "react-icons/fa";
import { 
  getProductById, 
  getProductImageUrl, 
  getRelatedProducts, 
  getRecommendedProducts,
  Product 
} from "../../api/services/product";
import { getProductPlaceholder } from "../../../utils/placeholder";
import { useCart } from "../../context/CartContext";
import { useAuth } from "../../context/AuthContext";
import RelatedProductsSection from "../../../components/product/RelatedProductsSection";
import RecommendationsSection from "../../../components/home/RecommendationsSection";

export default function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { isAuthenticated } = useAuth();
  const [product, setProduct] = useState<Product | null>(null);
  const [relatedProducts, setRelatedProducts] = useState<Product[]>([]);
  const [recommendations, setRecommendations] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [relatedLoading, setRelatedLoading] = useState(false);
  const [recommendationsLoading, setRecommendationsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [activeImage, setActiveImage] = useState<string | null>(null);
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [activeTab, setActiveTab] = useState<
    "description" | "reviews" | "shipping"
  >("description");

  const { addItem, loading: cartLoading } = useCart();

  useEffect(() => {
    const fetchProduct = async () => {
      try {
        setLoading(true);
        const productData = await getProductById(Number(id));
        console.log("Product data received:", {
          id: productData.id,
          name: productData.name,
          imageUrl: productData.imageUrl,
          imageAttaches: productData.imageAttaches,
          hasImageAttaches: Boolean(productData.imageAttaches?.length),
          imageAttachesLength: productData.imageAttaches?.length || 0,
        });
        setProduct(productData);
        const resolvedImageUrl = getProductImageUrl(productData);
        setActiveImage(resolvedImageUrl || getProductPlaceholder());
      } catch (err) {
        setError("Failed to load product details");
        console.error("Error fetching product:", err);
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      fetchProduct();
    }
  }, [id]);

  // Fetch related products when product is loaded
  useEffect(() => {
    const fetchRelatedProducts = async () => {
      if (!product?.id) return;

      try {
        setRelatedLoading(true);
        const related = await getRelatedProducts(product.id, 4);
        setRelatedProducts(related);
      } catch (err) {
        console.error("Error fetching related products:", err);
        setRelatedProducts([]);
      } finally {
        setRelatedLoading(false);
      }
    };

    fetchRelatedProducts();
  }, [product?.id]);

  // Fetch recommendations if authenticated
  useEffect(() => {
    const fetchRecommendations = async () => {
      if (!isAuthenticated) {
        setRecommendations([]);
        return;
      }

      try {
        setRecommendationsLoading(true);
        const recs = await getRecommendedProducts(6);
        setRecommendations(recs);
      } catch (err) {
        console.error("Error fetching recommendations:", err);
        setRecommendations([]);
      } finally {
        setRecommendationsLoading(false);
      }
    };

    fetchRecommendations();
  }, [isAuthenticated]);

  const incrementQuantity = () => {
    if (product && quantity < product.quantity) {
      setQuantity((prev) => prev + 1);
    }
  };

  const decrementQuantity = () => {
    if (quantity > 1) {
      setQuantity((prev) => prev - 1);
    }
  };

  const handleAddToCart = async () => {
    if (!product) return;

    try {
      await addItem(product.id, quantity);
    } catch (error) {
      console.error("Failed to add to cart:", error);
    }
  };

  const toggleWishlist = () => {
    setIsWishlisted(!isWishlisted);
    // In a real app, this would call an API to add/remove from wishlist
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-12">
        <div className="flex flex-col md:flex-row gap-8">
          <div className="md:w-1/2 bg-gray-200 animate-pulse h-96 rounded-lg"></div>
          <div className="md:w-1/2 space-y-4">
            <div className="h-8 bg-gray-200 animate-pulse rounded w-3/4"></div>
            <div className="h-6 bg-gray-200 animate-pulse rounded w-1/2"></div>
            <div className="h-24 bg-gray-200 animate-pulse rounded"></div>
            <div className="h-10 bg-gray-200 animate-pulse rounded w-36"></div>
            <div className="h-12 bg-gray-200 animate-pulse rounded"></div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="container mx-auto px-4 py-12 text-center">
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
          {error || "Product not found"}
        </div>
        <Link
          href="/product"
          className="mt-4 inline-block text-[#3b82f6] hover:underline"
        >
          Return to all products
        </Link>
      </div>
    );
  }

  // Mock reviews for demo purposes
  const reviews = [
    {
      id: 1,
      user: "John D.",
      rating: 5,
      comment:
        "Excellent quality and fast shipping. Very happy with my purchase!",
      date: "2023-09-15",
    },
    {
      id: 2,
      user: "Sarah M.",
      rating: 4,
      comment: "Good product for the price. Would buy again.",
      date: "2023-09-10",
    },
    {
      id: 3,
      user: "Alex K.",
      rating: 5,
      comment: "Exactly as described. Perfect fit for what I needed.",
      date: "2023-09-05",
    },
  ];

  const avgRating =
    reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length;

  return (
    <div className="container mx-auto px-4 py-12">
      <div className="flex flex-col md:flex-row gap-8">
        {/* Product Images */}
        <div className="md:w-1/2">
          <div className="relative aspect-square overflow-hidden rounded-lg mb-4">
            {activeImage && (
              <>
                {activeImage.startsWith("data:") ? (
                  <img
                    src={activeImage}
                    alt={product.name}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <Image
                    src={activeImage}
                    alt={product.name}
                    fill
                    className="object-cover"
                  />
                )}
                {/* Debug info */}
                <div className="absolute top-2 left-2 bg-black/50 text-white text-xs p-1 rounded">
                  {activeImage.startsWith("data:") ? "Data URL" : "Regular URL"}
                </div>
              </>
            )}
          </div>
          <div className="grid grid-cols-5 gap-2">
            {/* Display images from imageAttaches array */}
            {product.imageAttaches && product.imageAttaches.length > 0 ? (
              product.imageAttaches
                .filter((img) => Boolean(img?.contentType && img?.fileContent))
                .map((img, index) => {
                  const imageUrl = `data:${img.contentType};base64,${img.fileContent}`;
                  return (
                    <div
                      key={img.id}
                      className={`relative aspect-square overflow-hidden rounded-md cursor-pointer border-2 ${
                        activeImage === imageUrl
                          ? "border-primary"
                          : "border-transparent"
                      }`}
                      onClick={() => setActiveImage(imageUrl)}
                    >
                      {imageUrl.startsWith("data:") ? (
                        <img
                          src={imageUrl}
                          alt={`Product thumbnail ${index + 1}`}
                          className="w-full h-full object-cover"
                        />
                      ) : (
                        <Image
                          src={imageUrl}
                          alt={`Product thumbnail ${index + 1}`}
                          fill
                          className="object-cover"
                        />
                      )}
                    </div>
                  );
                })
            ) : (
              // Fallback to single image
              <div
                className={`relative aspect-square overflow-hidden rounded-md cursor-pointer border-2 border-primary`}
              >
                {(() => {
                  const fallbackImageUrl =
                    getProductImageUrl(product) || getProductPlaceholder();
                  return fallbackImageUrl.startsWith("data:") ? (
                    <img
                      src={fallbackImageUrl}
                      alt="Product thumbnail"
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <Image
                      src={fallbackImageUrl}
                      alt="Product thumbnail"
                      fill
                      className="object-cover"
                    />
                  );
                })()}
              </div>
            )}
          </div>
        </div>

        {/* Product Details */}
        <div className="md:w-1/2">
          <div className="mb-2">
            <Link
              href={`/category/${product.categoryId}`}
              className="text-sm text-[#3b82f6] hover:underline"
            >
              {product.categoryName || "Category"}
            </Link>
          </div>

          <h1 className="text-3xl font-bold mb-2">{product.name}</h1>

          <div className="flex items-center mb-4">
            <div className="flex text-yellow-400 mr-2">
              {[...Array(5)].map((_, i) => {
                const rating = i + 1;
                if (rating <= avgRating) {
                  return <FaStar key={i} />;
                } else if (rating <= avgRating + 0.5) {
                  return <FaStarHalfAlt key={i} />;
                } else {
                  return <FaRegStar key={i} />;
                }
              })}
            </div>
            <span className="text-sm text-gray-600">
              {reviews.length} reviews
            </span>
          </div>

          <div className="text-2xl font-bold text-[#3b82f6] mb-4">
            <div className="flex items-center gap-2">
              <span>
                €
                {product.offerPrice != null
                  ? product.offerPrice.toFixed(2)
                  : (product.price ?? 0).toFixed(2)}
              </span>
              {product.offerPrice != null && 
               product.price != null && 
               product.offerPrice !== product.price && (
                <span className="text-lg text-gray-500 line-through">
                  €{(product.price ?? 0).toFixed(2)}
                </span>
              )}
            </div>
          </div>

          <div className="mb-6">
            <p className="text-gray-700">{product.description}</p>
          </div>

          {/* Availability */}
          <div className="mb-6">
            <div className="flex items-center">
              <span className="mr-2 text-gray-700">Availability:</span>
              {product.quantity > 0 ? (
                <span className="text-green-600">
                  In Stock ({product.quantity} available)
                </span>
              ) : (
                <span className="text-red-600">Out of Stock</span>
              )}
            </div>
          </div>

          {/* Quantity selector */}
          {product.quantity > 0 && (
            <div className="mb-6">
              <div className="flex items-center">
                <span className="mr-4 text-gray-700">Quantity:</span>
                <div className="flex items-center border rounded-md">
                  <button
                    onClick={decrementQuantity}
                    disabled={quantity <= 1}
                    className="p-2 text-gray-600 hover:text-[#3b82f6] disabled:opacity-50"
                  >
                    <FaMinus size={12} />
                  </button>
                  <span className="px-4 py-1">{quantity}</span>
                  <button
                    onClick={incrementQuantity}
                    disabled={quantity >= product.quantity}
                    className="p-2 text-gray-600 hover:text-[#3b82f6] disabled:opacity-50"
                  >
                    <FaPlus size={12} />
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Action buttons */}
          <div className="flex flex-col sm:flex-row gap-3 mb-8">
            <button
              onClick={handleAddToCart}
              disabled={cartLoading || product.quantity < 1}
              className="flex-1 bg-[#60a5fa] text-white py-3 px-6 rounded-md font-medium flex items-center justify-center gap-2 hover:bg-primary-dark transition-colors disabled:opacity-50"
            >
              <FaShoppingCart />
              {cartLoading
                ? "Adding..."
                : product.quantity < 1
                ? "Out of Stock"
                : "Add to Cart"}
            </button>

            <button
              onClick={toggleWishlist}
              className="flex-1 sm:flex-none border border-gray-300 text-gray-700 py-3 px-6 rounded-md font-medium flex items-center justify-center gap-2 hover:bg-gray-50 transition-colors"
            >
              {isWishlisted ? (
                <FaHeart className="text-red-500" />
              ) : (
                <FaRegHeart />
              )}
              Wishlist
            </button>
          </div>

          {/* Product metadata */}
          <div className="border-t border-gray-200 pt-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
              <div>
                <span className="font-medium text-gray-700">SKU:</span>{" "}
                {product.productCode || product.id.toString().padStart(8, "0")}
              </div>
              <div>
                <span className="font-medium text-gray-700">Category:</span>{" "}
                {product.categoryName || "Category"}
              </div>
              {product.brand && (
                <div>
                  <span className="font-medium text-gray-700">Brand:</span>{" "}
                  {product.brand}
                </div>
              )}
              {product.manufacturingPlace && (
                <div>
                  <span className="font-medium text-gray-700">
                    Manufactured in:
                  </span>{" "}
                  {product.manufacturingPlace}
                </div>
              )}
              {product.productCondition && (
                <div>
                  <span className="font-medium text-gray-700">Condition:</span>{" "}
                  {product.productCondition}
                </div>
              )}
              {product.inventoryLocation && (
                <div>
                  <span className="font-medium text-gray-700">Location:</span>{" "}
                  {product.inventoryLocation}
                </div>
              )}
              {product.ean && (
                <div>
                  <span className="font-medium text-gray-700">EAN:</span>{" "}
                  {product.ean}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Tabs for more details */}
      <div className="mt-12">
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8">
            {["description", "reviews", "shipping"].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab as typeof activeTab)}
                className={`py-4 px-1 text-center border-b-2 font-medium text-sm ${
                  activeTab === tab
                    ? "border-[#60a5fa] text-[#3b82f6]"
                    : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              >
                {tab.charAt(0).toUpperCase() + tab.slice(1)}
              </button>
            ))}
          </nav>
        </div>

        <div className="py-6">
          {activeTab === "description" && (
            <div>
              <h2 className="text-lg font-medium mb-4">Product Description</h2>
              <div className="prose max-w-none">
                <p>{product.description}</p>
                {/* Extended description would go here */}
                <p className="mt-4">
                  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed
                  do eiusmod tempor incididunt ut labore et dolore magna aliqua.
                  Ut enim ad minim veniam, quis nostrud exercitation ullamco
                  laboris nisi ut aliquip ex ea commodo consequat.
                </p>
                <ul className="mt-4 list-disc pl-5 space-y-2">
                  <li>Feature 1: Lorem ipsum dolor sit amet</li>
                  <li>Feature 2: Consectetur adipiscing elit</li>
                  <li>Feature 3: Sed do eiusmod tempor incididunt</li>
                  <li>Feature 4: Ut labore et dolore magna aliqua</li>
                </ul>
              </div>
            </div>
          )}

          {activeTab === "reviews" && (
            <div>
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-lg font-medium">Customer Reviews</h2>
                <button className="bg-[#60a5fa] text-white py-2 px-4 rounded-md text-sm font-medium hover:bg-primary-dark transition-colors">
                  Write a Review
                </button>
              </div>

              <div className="mb-8">
                <div className="flex items-center mb-2">
                  <div className="flex text-yellow-400 mr-2">
                    {[...Array(5)].map((_, i) => {
                      const rating = i + 1;
                      if (rating <= avgRating) {
                        return <FaStar key={i} />;
                      } else if (rating <= avgRating + 0.5) {
                        return <FaStarHalfAlt key={i} />;
                      } else {
                        return <FaRegStar key={i} />;
                      }
                    })}
                  </div>
                  <span className="text-sm text-gray-600">
                    Based on {reviews.length} reviews
                  </span>
                </div>

                <div className="space-y-1">
                  {[5, 4, 3, 2, 1].map((rating) => {
                    const count = reviews.filter(
                      (r) => r.rating === rating
                    ).length;
                    const percentage = reviews.length
                      ? (count / reviews.length) * 100
                      : 0;

                    return (
                      <div key={rating} className="flex items-center text-sm">
                        <span className="w-8">{rating} star</span>
                        <div className="mx-2 flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
                          <div
                            className="h-full bg-yellow-400 rounded-full"
                            style={{ width: `${percentage}%` }}
                          ></div>
                        </div>
                        <span className="w-9 text-right text-gray-600">
                          {count}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="space-y-6">
                {reviews.map((review) => (
                  <div
                    key={review.id}
                    className="border-b border-gray-200 pb-6"
                  >
                    <div className="flex justify-between mb-2">
                      <h3 className="font-medium">{review.user}</h3>
                      <span className="text-sm text-gray-500">
                        {review.date}
                      </span>
                    </div>
                    <div className="flex text-yellow-400 mb-2">
                      {[...Array(5)].map((_, i) => (
                        <span key={i}>
                          {i < review.rating ? <FaStar /> : <FaRegStar />}
                        </span>
                      ))}
                    </div>
                    <p className="text-gray-700">{review.comment}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {activeTab === "shipping" && (
            <div>
              <h2 className="text-lg font-medium mb-4">Shipping & Returns</h2>
              <div className="prose max-w-none">
                <h3 className="text-base font-medium mt-6 mb-2">
                  Shipping Policy
                </h3>
                <p>
                  We offer free standard shipping on all orders over €50. Orders
                  typically ship within 1-2 business days and arrive within 3-5
                  business days depending on your location.
                </p>

                <h3 className="text-base font-medium mt-6 mb-2">
                  Return Policy
                </h3>
                <p>
                  We accept returns within 30 days of purchase. Items must be in
                  original condition with tags attached. Please note that
                  shipping costs for returns are the responsibility of the
                  customer unless the return is due to our error.
                </p>

                <h3 className="text-base font-medium mt-6 mb-2">Warranty</h3>
                <p>
                  This product comes with a 1-year manufacturer&apos;s warranty
                  covering defects in materials and workmanship under normal
                  use.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Related Products Section */}
      {product && (
        <RelatedProductsSection
          products={relatedProducts}
          loading={relatedLoading}
          categoryId={product.categoryId}
        />
      )}

      {/* Recommendations Section (only for authenticated users) */}
      {isAuthenticated && recommendations.length > 0 && (
        <RecommendationsSection
          products={recommendations}
          loading={recommendationsLoading}
        />
      )}
    </div>
  );
}
