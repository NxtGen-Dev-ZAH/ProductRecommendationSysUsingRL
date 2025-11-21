"use client";

import { useState } from "react";
import { FaHeart, FaRegHeart } from "react-icons/fa";
import { toggleFavoriteProduct } from "../../app/api/services/user";
import { useAuth } from "../../app/context/AuthContext";
import { useRouter } from "next/navigation";

interface FavoriteButtonProps {
  productId: number;
  isFavorite: boolean;
  onFavoriteChange?: (isFavorite: boolean) => void;
  size?: "sm" | "md" | "lg";
  className?: string;
}

export function FavoriteButton({
  productId,
  isFavorite,
  onFavoriteChange,
  size = "md",
  className = "",
}: FavoriteButtonProps) {
  const [loading, setLoading] = useState(false);
  const [favoriteState, setFavoriteState] = useState(isFavorite);
  const { isAuthenticated } = useAuth();
  const router = useRouter();

  const handleFavoriteToggle = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    try {
      setLoading(true);
      const result = await toggleFavoriteProduct(productId);

      setFavoriteState(result.isFavorite);
      onFavoriteChange?.(result.isFavorite);
    } catch (error) {
      console.error("Error toggling favorite:", error);
      // Revert the optimistic update on error
      setFavoriteState(!favoriteState);
    } finally {
      setLoading(false);
    }
  };

  const sizeClasses = {
    sm: "w-4 h-4",
    md: "w-5 h-5",
    lg: "w-6 h-6",
  };

  const buttonSizeClasses = {
    sm: "p-1.5",
    md: "p-2",
    lg: "p-2.5",
  };

  return (
    <button
      onClick={handleFavoriteToggle}
      disabled={loading}
      className={`
        ${buttonSizeClasses[size]}
        rounded-full
        transition-all
        duration-200
        flex
        items-center
        justify-center
        hover:scale-110
        active:scale-95
        disabled:opacity-50
        disabled:cursor-not-allowed
        ${
          favoriteState
            ? "bg-red-50 text-red-500 hover:bg-red-100"
            : "bg-gray-100 text-gray-400 hover:bg-gray-200 hover:text-red-400"
        }
        ${className}
      `}
      title={favoriteState ? "Remove from favorites" : "Add to favorites"}
    >
      {loading ? (
        <div
          className={`${sizeClasses[size]} border-2 border-gray-300 border-t-transparent rounded-full animate-spin`}
        />
      ) : favoriteState ? (
        <FaHeart className={sizeClasses[size]} />
      ) : (
        <FaRegHeart className={sizeClasses[size]} />
      )}
    </button>
  );
}
