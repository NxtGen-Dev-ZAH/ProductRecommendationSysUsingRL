"use client";

import { useEffect, useState } from "react";
import { 
  getFeaturedProducts, 
  getNewArrivals, 
  getBestSellers, 
  getRecommendedProducts,
  Product 
} from "./api/services/product";
import { getAllCategories, Category } from "./api/services/category";
import { useAuth } from "./context/AuthContext";

// Import our new components
import HeroSection from "../components/home/HeroSection";
import FeaturesSection from "../components/home/FeaturesSection";
import CategoriesSection from "../components/home/CategoriesSection";
import FeaturedProductsSection from "../components/home/FeaturedProductsSection";
import PromoBanner from "../components/home/PromoBanner";
import TestimonialsSection from "../components/home/TestimonialsSection";
import NewsletterSection from "../components/home/NewsletterSection";
import TrendingProductsSection from "@/components/home/TrendingProductsSection";
import BrandShowcase from "@/components/home/BrandShowcase";
import RecentlyViewed from "@/components/home/RecentlyViewed";
import NewArrivalsSection from "@/components/home/NewArrivalsSection";
import BestSellersSection from "@/components/home/BestSellersSection";
import RecommendationsSection from "@/components/home/RecommendationsSection";

export default function Home() {
  const { isAuthenticated } = useAuth();
  const [featuredProducts, setFeaturedProducts] = useState<Product[]>([]);
  const [newArrivals, setNewArrivals] = useState<Product[]>([]);
  const [bestSellers, setBestSellers] = useState<Product[]>([]);
  const [recommendations, setRecommendations] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [featured, newArrivalsData, bestSellersData, categoriesData] = await Promise.all([
          getFeaturedProducts(8),
          getNewArrivals(8),
          getBestSellers(8),
          getAllCategories(),
        ]);

        setFeaturedProducts(featured);
        setNewArrivals(newArrivalsData);
        setBestSellers(bestSellersData);
        setCategories(categoriesData);

        // Fetch recommendations only if authenticated
        if (isAuthenticated) {
          try {
            const recommendationsData = await getRecommendedProducts(6);
            setRecommendations(recommendationsData);
          } catch (error) {
            console.error("Error fetching recommendations:", error);
            setRecommendations([]);
          }
        }
      } catch (error) {
        console.error("Error fetching homepage data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [isAuthenticated]);

  return (
    <div className="flex flex-col gap-4">
      <HeroSection />

      <FeaturesSection />

      <RecentlyViewed products={featuredProducts} loading={loading} />

      {/* Categories Section */}
      <CategoriesSection categories={categories} loading={loading} />

      {/* Featured Products Section */}
      <FeaturedProductsSection products={featuredProducts} loading={loading} />

      {/* New Arrivals Section */}
      <NewArrivalsSection products={newArrivals} loading={loading} />

      {/* Best Sellers Section */}
      <BestSellersSection products={bestSellers} loading={loading} />

      {/* Trending Products Section (using best sellers) */}
      <TrendingProductsSection products={bestSellers} loading={loading} />

      {/* Recommendations Section (only for authenticated users) */}
      {isAuthenticated && recommendations.length > 0 && (
        <RecommendationsSection products={recommendations} loading={loading} />
      )}

      <BrandShowcase />

      {/* Promotional Banner */}
      <PromoBanner />

      {/* Testimonials Section */}
      <TestimonialsSection />

      {/* Newsletter Section */}
      <NewsletterSection />
    </div>
  );
}
