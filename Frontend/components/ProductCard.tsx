"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import {
  FaRegHeart,
  FaHeart,
  FaCartPlus,
  FaEye,
  FaStar,
  FaStarHalfAlt,
} from "react-icons/fa";
import { Product, getProductImageUrl } from "../app/api/services/product";
import { getProductPlaceholder } from "../utils/placeholder";
import { useCart } from "../app/context/CartContext";
import { cn } from "../utils/cn";
import { motion } from "framer-motion";
import { Card } from "./ui/card";
import { Button } from "./ui/button";
import { useToast } from "./ui/toast";

// Extended product interface to include optional discount
interface ExtendedProduct extends Omit<Product, "id"> {
  id: string | number; // Allow both string and number ids
  discountPercentage?: number;
  rating?: number;
  reviewCount?: number;
  vendor?: {
    id: number;
    name: string;
  };
}

interface ProductCardProps {
  product: ExtendedProduct;
  className?: string;
  variant?: "default" | "compact" | "featured";
  showQuickView?: boolean;
}

const ProductCard = ({
  product,
  className,
  variant = "default",
  showQuickView = true,
}: ProductCardProps) => {
  const [isWishlisted, setIsWishlisted] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const [imageLoading, setImageLoading] = useState(true);
  const [imageError, setImageError] = useState(false);
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const [currentQuantity, setCurrentQuantity] = useState(0);
  const { cart, addItem, updateItem } = useCart();
  const { addToast } = useToast();

  // Convert product ID to number for comparison
  const productId =
    typeof product.id === "string" ? parseInt(product.id, 10) : product.id;

  // Check if product is already in cart and get current quantity
  useEffect(() => {
    if (cart?.items) {
      const cartItem = cart.items.find((item) => item.productId === productId);
      setCurrentQuantity(cartItem ? cartItem.quantity : 0);
    } else {
      setCurrentQuantity(0);
    }
  }, [cart, productId]);

  const handleAddToCart = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    // Prevent multiple clicks
    if (isAddingToCart) return;

    setIsAddingToCart(true);
    try {
      if (currentQuantity > 0) {
        // Product is already in cart, update quantity
        const cartItem = cart?.items?.find(
          (item) => item.productId === productId
        );
        if (cartItem) {
          await updateItem(cartItem.id, currentQuantity + 1);
          addToast({
            message: `Quantité de ${product.name} mise à jour !`,
            type: "success",
          });
        }
      } else {
        // Product is not in cart, add it
        await addItem(productId, 1);
        addToast({
          message: `${product.name} ajouté au panier !`,
          type: "success",
        });
      }
    } catch (error) {
      console.error("Failed to add to cart:", error);
      addToast({
        message: "Échec de l'ajout au panier. Veuillez réessayer.",
        type: "error",
      });
    } finally {
      setIsAddingToCart(false);
    }
  };

  const toggleWishlist = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsWishlisted(!isWishlisted);
    addToast({
      message: isWishlisted
        ? `${product.name} retiré de la liste de souhaits`
        : `${product.name} ajouté à la liste de souhaits !`,
      type: "success",
    });
  };

  const handleQuickView = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    // TODO: Implement quick view modal
    console.log("Quick view for product:", product.id);
  };

  // Check if discount exists and is greater than 0
  const hasDiscount =
    product.discountPercentage && product.discountPercentage > 0;
  const hasOfferPrice =
    product.offerPrice != null && 
    product.price != null && 
    product.offerPrice !== product.price;
  
  // Calculate the display price - prioritize offerPrice, then apply discount, then use base price
  // This matches the logic in the product detail page
  const displayPrice = product.offerPrice != null
    ? product.offerPrice
    : hasDiscount && product.price != null && product.price > 0
    ? product.price * (1 - (product.discountPercentage || 0) / 100)
    : (product.price ?? 0);

  // Rating component
  const renderRating = () => {
    if (!product.rating) return null;

    const fullStars = Math.floor(product.rating);
    const hasHalfStar = product.rating % 1 !== 0;
    const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

    return (
      <div className="flex items-center gap-1 text-yellow-400">
        {[...Array(fullStars)].map((_, i) => (
          <FaStar key={`full-${i}`} size={12} />
        ))}
        {hasHalfStar && <FaStarHalfAlt size={12} />}
        {[...Array(emptyStars)].map((_, i) => (
          <FaStar key={`empty-${i}`} size={12} className="text-gray-300" />
        ))}
        {product.reviewCount && (
          <span className="text-xs text-muted-foreground ml-1">
            ({product.reviewCount})
          </span>
        )}
      </div>
    );
  };

  const cardContent = (
    <Card
      className={cn(
        "group relative overflow-hidden border-gray-100/50 bg-white/80 backdrop-blur-sm transition-all duration-300",
        "hover:shadow-lg hover:shadow-primary/5 hover:-translate-y-1",
        variant === "compact" && "max-w-sm",
        variant === "featured" && "border-primary/20 shadow-md",
        className
      )}
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
    >
      {/* Product Image */}
      <div className="relative aspect-square overflow-hidden bg-gray-50">
        {/* Discount Badge */}
        {hasDiscount && (
          <div className="absolute top-3 left-3 z-10">
            <span className="bg-red-500 text-white text-xs font-bold px-2.5 py-1.5 rounded-full shadow-sm">
              -{Math.round(product.discountPercentage!)}%
            </span>
          </div>
        )}

        {/* Cart Badge - Show if product is in cart */}
        {currentQuantity > 0 && (
          <div className="absolute top-3 left-3 z-10">
            <span className="bg-green-500 text-white text-xs font-bold px-2.5 py-1.5 rounded-full shadow-sm">
              {currentQuantity} dans le panier
            </span>
          </div>
        )}

        {/* Wishlist Button */}
        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          onClick={toggleWishlist}
          className={cn(
            "absolute top-3 right-3 z-10 p-2.5 rounded-full backdrop-blur-sm transition-all duration-200",
            "hover:bg-white/90 hover:shadow-md",
            isWishlisted
              ? "bg-red-50 text-red-500"
              : "bg-white/70 text-gray-600 hover:text-red-500"
          )}
          aria-label={
            isWishlisted
              ? "Retirer de la liste de souhaits"
              : "Ajouter à la liste de souhaits"
          }
        >
          {isWishlisted ? <FaHeart size={16} /> : <FaRegHeart size={16} />}
        </motion.button>

        {/* Product Image */}
        {!imageError ? (
          <Image
            src={getProductImageUrl(product) || getProductPlaceholder()}
            alt={product.name}
            fill
            className={cn(
              "object-cover transition-all duration-500 group-hover:scale-105",
              imageLoading && "scale-110 blur-sm"
            )}
            onLoad={() => setImageLoading(false)}
            onError={() => {
              setImageError(true);
              setImageLoading(false);
            }}
            sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
            unoptimized={(
              getProductImageUrl(product) || getProductPlaceholder()
            ).startsWith("data:")}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-gray-100 text-gray-400">
            <div className="text-center">
              <div className="w-16 h-16 mx-auto mb-2 opacity-50">
                <svg fill="currentColor" viewBox="0 0 20 20">
                  <path
                    fillRule="evenodd"
                    d="M4 3a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V5a2 2 0 00-2-2H4zm12 12H4l4-8 3 6 2-4 3 6z"
                    clipRule="evenodd"
                  />
                </svg>
              </div>
              <span className="text-sm">Aucune Image</span>
            </div>
          </div>
        )}

        {/* Quick Actions Overlay */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{
            opacity: isHovered ? 1 : 0,
            y: isHovered ? 0 : 20,
          }}
          transition={{ duration: 0.2 }}
          className="absolute inset-0 bg-black/20 backdrop-blur-[1px] flex items-center justify-center gap-2"
        >
          {showQuickView && (
            <Button
              size="sm"
              variant="secondary"
              onClick={handleQuickView}
              className="bg-white/90 backdrop-blur-sm hover:bg-white text-gray-800 shadow-sm"
            >
              <FaEye className="mr-1.5" size={14} />
              Aperçu Rapide
            </Button>
          )}
        </motion.div>
      </div>

      {/* Product Info */}
      <div className="p-4 space-y-3">
        {/* Vendor */}
        {product.vendor && (
          <p className="text-xs text-muted-foreground font-medium">
            {product.vendor.name}
          </p>
        )}

        {/* Product Name */}
        <h3 className="font-semibold text-gray-900 line-clamp-2 leading-tight group-hover:text-primary transition-colors">
          {product.name}
        </h3>

        {/* Rating */}
        {renderRating()}

        {/* Description - Only for featured variant */}
        {variant === "featured" && (
          <p className="text-sm text-muted-foreground line-clamp-2">
            {product.description}
          </p>
        )}

        {/* Price */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-lg font-bold text-gray-900">
              €{displayPrice.toFixed(2)}
            </span>
            {(hasDiscount || hasOfferPrice) && product.price != null && product.price > 0 && (
              <span className="text-sm text-muted-foreground line-through">
                €{product.price.toFixed(2)}
              </span>
            )}
          </div>

          {/* Stock Status */}
          <div className="flex items-center">
            {product.quantity > 0 ? (
              <span className="text-xs text-green-600 font-medium">
                En Stock
              </span>
            ) : (
              <span className="text-xs text-red-600 font-medium">
                Rupture de Stock
              </span>
            )}
          </div>
        </div>

        {/* Add to Cart Button */}
        <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
          <Button
            onClick={handleAddToCart}
            disabled={isAddingToCart || product.quantity === 0}
            className={cn(
              "w-full flex items-center justify-center gap-2 font-medium",
              currentQuantity > 0 && "bg-green-600 hover:bg-green-700"
            )}
            hover="scale"
          >
            <FaCartPlus size={16} />
            {isAddingToCart
              ? "Ajout en cours..."
              : currentQuantity > 0
              ? `Ajouter un autre (${currentQuantity})`
              : "Ajouter au Panier"}
          </Button>
        </motion.div>
      </div>
    </Card>
  );

  return (
    <Link href={`/product/${product.id}`} className="block">
      {cardContent}
    </Link>
  );
};

export default ProductCard;
