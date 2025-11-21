/**
 * RL-powered product recommendations section
 */

"use client";

import React, { useEffect, useState } from "react";
import { Product } from "@/types/api";
import ProductCard from "../ProductCard";
import { useAuth } from "@/app/context/AuthContext";

interface RLRecommendation extends Product {
  confidenceScore?: number;
  rank?: number;
  reason?: string;
}

interface RLRecommendationsProps {
  userId?: number;
  categoryId?: number;
  limit?: number;
  excludeProducts?: number[];
  title?: string;
  subtitle?: string;
}

/**
 * RL Recommendations Section Component
 * 
 * Displays personalized product recommendations using the RL service
 */
export default function RLRecommendationsSection({
  userId,
  categoryId,
  limit = 6,
  excludeProducts = [],
  title = "Recommended For You",
  subtitle = "Personalized picks based on your preferences"
}: RLRecommendationsProps) {
  const { user } = useAuth();
  const [recommendations, setRecommendations] = useState<RLRecommendation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [algorithmUsed, setAlgorithmUsed] = useState<string>("");

  useEffect(() => {
    const fetchRecommendations = async () => {
      const targetUserId = userId || user?.id;

      if (!targetUserId) {
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);

        // Call RL recommendation endpoint through backend
        // Backend will call Python RL service internally
        const response = await fetch("/api/v1/recommendations/rl", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${localStorage.getItem("token") || ""}`
          },
          body: JSON.stringify({
            userId: targetUserId,
            limit,
            categoryId,
            excludeProducts
          })
        });

        if (!response.ok) {
          throw new Error("Failed to fetch recommendations");
        }

        const data = await response.json();

        // Convert RL items to product format
        const products = data.recommendations?.map((item: any) => ({
          id: item.productId,
          name: item.productName,
          price: item.price,
          offerPrice: item.discountPrice,
          imageUrl: item.imageUrl,
          categoryId: item.categoryId,
          categoryName: item.categoryName,
          brand: item.brand,
          discount: item.discountPercentage,
          quantity: 1,
          confidenceScore: item.confidenceScore,
          rank: item.rank,
          reason: item.reason
        })) || [];

        setRecommendations(products);
        setAlgorithmUsed(data.algorithmUsed || "");
      } catch (err) {
        console.error("Error fetching RL recommendations:", err);
        setError("Unable to load personalized recommendations");
        
        // Fallback to regular recommendations
        try {
          const fallbackResponse = await fetch(
            `/api/products/recommendations?limit=${limit}`
          );
          const fallbackData = await fallbackResponse.json();
          setRecommendations(fallbackData || []);
          setAlgorithmUsed("FALLBACK");
        } catch (fallbackErr) {
          console.error("Fallback also failed:", fallbackErr);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchRecommendations();
  }, [userId, user?.id, categoryId, limit, excludeProducts]);

  if (loading) {
    return (
      <section className="py-12 bg-gray-50">
        <div className="container mx-auto px-4">
          <div className="text-center mb-8">
            <h2 className="text-3xl font-bold text-gray-900">{title}</h2>
            <p className="text-gray-600 mt-2">{subtitle}</p>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-6">
            {[...Array(limit)].map((_, i) => (
              <div key={i} className="animate-pulse">
                <div className="bg-gray-200 h-64 rounded-lg mb-4"></div>
                <div className="bg-gray-200 h-4 rounded mb-2"></div>
                <div className="bg-gray-200 h-4 rounded w-3/4"></div>
              </div>
            ))}
          </div>
        </div>
      </section>
    );
  }

  if (error && recommendations.length === 0) {
    return null; // Don't show section if there's an error and no fallback
  }

  if (recommendations.length === 0) {
    return null; // Don't show empty section
  }

  return (
    <section className="py-12 bg-gray-50">
      <div className="container mx-auto px-4">
        {/* Section Header */}
        <div className="text-center mb-8">
          <h2 className="text-3xl font-bold text-gray-900">{title}</h2>
          <p className="text-gray-600 mt-2">{subtitle}</p>
          
          {/* Algorithm Badge (for debugging/transparency) */}
          {algorithmUsed && (
            <div className="mt-2">
              <span className="inline-block px-3 py-1 text-xs font-semibold rounded-full bg-blue-100 text-blue-800">
                Powered by {algorithmUsed === "FALLBACK" ? "Smart Picks" : "AI"}
              </span>
            </div>
          )}
        </div>

        {/* Recommendations Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-6">
          {recommendations.map((product, index) => (
            <div key={product.id} className="relative">
              <ProductCard
                product={product}
                context={{
                  page: "rl-recommendations",
                  position: index + 1,
                  algorithm: algorithmUsed
                }}
              />
              
              {/* Confidence Indicator (optional) */}
              {product.confidenceScore && product.confidenceScore > 0.8 && (
                <div className="absolute top-2 right-2 bg-green-500 text-white text-xs font-bold px-2 py-1 rounded">
                  Top Pick
                </div>
              )}
              
              {/* Reason Tooltip (optional) */}
              {product.reason && (
                <div className="mt-2 text-xs text-gray-500 text-center italic">
                  {product.reason}
                </div>
              )}
            </div>
          ))}
        </div>

        {/* View More Link */}
        {recommendations.length >= limit && (
          <div className="text-center mt-8">
            <button
              onClick={() => {
                // Navigate to full recommendations page
                window.location.href = "/recommendations";
              }}
              className="inline-block px-6 py-3 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition"
            >
              View More Recommendations
            </button>
          </div>
        )}
      </div>
    </section>
  );
}

/**
 * Compact version for sidebars
 */
export function RLRecommendationsSidebar({
  userId,
  limit = 4,
  title = "You Might Like"
}: RLRecommendationsProps) {
  const { user } = useAuth();
  const [recommendations, setRecommendations] = useState<RLRecommendation[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchRecommendations = async () => {
      const targetUserId = userId || user?.id;

      if (!targetUserId) {
        setLoading(false);
        return;
      }

      try {
        const response = await fetch("/api/v1/recommendations/rl", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${localStorage.getItem("token") || ""}`
          },
          body: JSON.stringify({
            userId: targetUserId,
            limit
          })
        });

        if (response.ok) {
          const data = await response.json();
          
          // Convert RL items to product format
          const products = data.recommendations?.map((item: any) => ({
            id: item.productId,
            name: item.productName,
            price: item.price,
            offerPrice: item.discountPrice,
            imageUrl: item.imageUrl,
            categoryId: item.categoryId,
            categoryName: item.categoryName,
            brand: item.brand,
            discount: item.discountPercentage,
            quantity: 1,
            confidenceScore: item.confidenceScore,
            rank: item.rank,
            reason: item.reason
          })) || [];
          
          setRecommendations(products);
        }
      } catch (err) {
        console.error("Error fetching sidebar recommendations:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchRecommendations();
  }, [userId, user?.id, limit]);

  if (loading || recommendations.length === 0) {
    return null;
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-xl font-bold text-gray-900 mb-4">{title}</h3>
      <div className="space-y-4">
        {recommendations.map((product) => (
          <a
            key={product.id}
            href={`/product/${product.id}`}
            className="flex items-center space-x-4 hover:bg-gray-50 p-2 rounded transition"
          >
            <img
              src={product.imageUrl || "/placeholder.png"}
              alt={product.name}
              className="w-16 h-16 object-cover rounded"
            />
            <div className="flex-1">
              <h4 className="text-sm font-semibold text-gray-900 line-clamp-2">
                {product.name}
              </h4>
              <p className="text-sm font-bold text-blue-600 mt-1">
                ${product.price.toFixed(2)}
              </p>
            </div>
          </a>
        ))}
      </div>
    </div>
  );
}

