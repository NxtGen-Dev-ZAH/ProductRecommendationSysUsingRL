"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import { FaFilter, FaChevronDown } from "react-icons/fa";
import {
  getCategoryWithProducts,
  Category,
  getCategoryById,
} from "../../api/services/category";
import { Product, getProductsByCategory } from "../../api/services/product";
import ProductCard from "../../../components/ProductCard";

type SortOption = "newest" | "priceAsc" | "priceDesc" | "nameAsc" | "nameDesc";

export default function CategoryDetailPage() {
  const params = useParams();
  const categoryId = parseInt(params.id as string, 10);

  const [category, setCategory] = useState<Category | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [sortOption, setSortOption] = useState<SortOption>("newest");
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 1000]);
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  useEffect(() => {
    const fetchCategoryData = async () => {
      try {
        // First try to get category with products, then fallback to separate calls
        try {
          const data = await getCategoryWithProducts(categoryId);
          if (data) {
            setCategory(data);
            setProducts(data.products || []);
            return;
          }
        } catch (error) {
          console.warn(
            "getCategoryWithProducts failed, trying separate calls:",
            error
          );
        }

        // Fallback: get category and products separately
        const [categoryData, productsData] = await Promise.all([
          getCategoryById(categoryId),
          getProductsByCategory(categoryId),
        ]);

        setCategory(categoryData);
        setProducts(productsData || []);
      } catch (error) {
        console.error("Error fetching category data:", error);
      } finally {
        setLoading(false);
      }
    };

    if (categoryId) {
      fetchCategoryData();
    }
  }, [categoryId]);

  // Apply filters and sorting
  const filteredAndSortedProducts = products
    .filter(
      (product) =>
        // Apply price filter
        product.price >= priceRange[0] && product.price <= priceRange[1]
    )
    .sort((a, b) => {
      // Apply sorting
      switch (sortOption) {
        case "priceAsc":
          return a.price - b.price;
        case "priceDesc":
          return b.price - a.price;
        case "nameAsc":
          return a.name.localeCompare(b.name);
        case "nameDesc":
          return b.name.localeCompare(a.name);
        case "newest":
        default:
          // For demo, we'll just use the id as proxy for newness
          return b.id - a.id;
      }
    });

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-12">
        <div className="animate-pulse h-16 bg-gray-200 rounded-lg mb-8"></div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[...Array(8)].map((_, index) => (
            <div
              key={index}
              className="bg-gray-200 animate-pulse h-64 rounded-lg"
            ></div>
          ))}
        </div>
      </div>
    );
  }

  if (!category) {
    return (
      <div className="container mx-auto px-4 py-12 text-center">
        <h1 className="text-2xl font-bold mb-4">Category Not Found</h1>
        <p>The category you are looking for does not exist.</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-12">
      <div className="mb-12">
        <h1 className="text-3xl md:text-4xl font-bold mb-4">{category.name}</h1>
        <p className="text-gray-600">{category.description}</p>
      </div>

      {/* Mobile Filter Toggle */}
      <div className="md:hidden mb-4">
        <button
          onClick={() => setIsFilterOpen(!isFilterOpen)}
          className="w-full flex items-center justify-between p-4 bg-white shadow rounded-md"
        >
          <span className="flex items-center gap-2">
            <FaFilter /> Filters
          </span>
          <FaChevronDown
            className={`transform transition-transform ${
              isFilterOpen ? "rotate-180" : ""
            }`}
          />
        </button>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        {/* Filters - Desktop always visible, mobile toggleable */}
        <div
          className={`md:w-1/4 bg-white p-6 rounded-lg shadow-sm ${
            isFilterOpen ? "block" : "hidden md:block"
          }`}
        >
          <div className="mb-6">
            <h3 className="font-bold text-lg mb-3">Price Range</h3>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span>${priceRange[0]}</span>
                <span>-</span>
                <span>${priceRange[1]}</span>
              </div>
              <input
                type="range"
                min="0"
                max="1000"
                value={priceRange[0]}
                onChange={(e) =>
                  setPriceRange([parseInt(e.target.value), priceRange[1]])
                }
                className="w-full"
              />
              <input
                type="range"
                min="0"
                max="1000"
                value={priceRange[1]}
                onChange={(e) =>
                  setPriceRange([priceRange[0], parseInt(e.target.value)])
                }
                className="w-full"
              />
            </div>
          </div>

          <div className="mb-6">
            <h3 className="font-bold text-lg mb-3">Availability</h3>
            <div className="space-y-2">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  className="rounded text-[#3b82f6] focus:ring-[#3b82f6]"
                />
                <span>In Stock</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  className="rounded text-[#3b82f6] focus:ring-[#3b82f6]"
                />
                <span>On Sale</span>
              </label>
            </div>
          </div>

          <div>
            <h3 className="font-bold text-lg mb-3">Rating</h3>
            <div className="space-y-2">
              {[5, 4, 3, 2, 1].map((rating) => (
                <label
                  key={rating}
                  className="flex items-center gap-2 cursor-pointer"
                >
                  <input
                    type="checkbox"
                    className="rounded text-[#3b82f6] focus:ring-[#3b82f6]"
                  />
                  <div className="flex text-yellow-400">
                    {[...Array(5)].map((_, i) => (
                      <svg
                        key={i}
                        xmlns="http://www.w3.org/2000/svg"
                        className={`h-4 w-4 ${
                          i < rating ? "fill-current" : "text-gray-300"
                        }`}
                        viewBox="0 0 20 20"
                      >
                        <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                      </svg>
                    ))}
                  </div>
                  <span>& Up</span>
                </label>
              ))}
            </div>
          </div>
        </div>

        {/* Product listing */}
        <div className="flex-1">
          {/* Sort controls */}
          <div className="flex justify-between items-center mb-6">
            <p className="text-gray-600">
              Showing{" "}
              <span className="font-medium">
                {filteredAndSortedProducts.length}
              </span>{" "}
              products
            </p>
            <div className="flex items-center gap-2">
              <label htmlFor="sort" className="text-sm text-gray-600">
                Sort by:
              </label>
              <select
                id="sort"
                value={sortOption}
                onChange={(e) => setSortOption(e.target.value as SortOption)}
                className="rounded-md border-gray-300 shadow-sm focus:border-[#3b82f6] focus:ring-[#3b82f6]"
              >
                <option value="newest">Newest</option>
                <option value="priceAsc">Price: Low to High</option>
                <option value="priceDesc">Price: High to Low</option>
                <option value="nameAsc">Name: A to Z</option>
                <option value="nameDesc">Name: Z to A</option>
              </select>
            </div>
          </div>

          {/* Products grid */}
          {filteredAndSortedProducts.length > 0 ? (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {filteredAndSortedProducts.map((product) => (
                <ProductCard key={product.id} product={product} />
              ))}
            </div>
          ) : (
            <div className="text-center py-12 bg-gray-50 rounded-lg">
              <h3 className="text-lg font-medium text-gray-900">
                No products found
              </h3>
              <p className="mt-2 text-gray-500">
                Try adjusting your filters to find what you&apos;re looking for.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
