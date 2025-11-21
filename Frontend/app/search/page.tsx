"use client";

import { useState, useEffect, useCallback, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import { FaSearch, FaFilter } from "react-icons/fa";
import { Product } from "../api/services/product";
import { Category } from "../api/services/category";
import { unifiedSearch, getSearchSuggestions } from "../api/services/search";
import ProductCard from "../../components/ProductCard";
import { motion } from "framer-motion";

type SortOption =
  | "relevance"
  | "price_asc"
  | "price_desc"
  | "newest"
  | "name_asc"
  | "name_desc";

function SearchPageContent() {
  const searchParams = useSearchParams();
  const initialQuery = searchParams.get("q") || "";

  const [query, setQuery] = useState(initialQuery);
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(false);
  const [sortOption, setSortOption] = useState<SortOption>("relevance");
  const [selectedCategory, setSelectedCategory] = useState<string>("");
  const [priceRange, setPriceRange] = useState<[number, number]>([0, 1000]);
  const [showFilters, setShowFilters] = useState(false);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // Search function
  const performSearch = useCallback(
    async (searchQuery: string, filters: Record<string, string> = {}) => {
      if (!searchQuery.trim()) {
        setProducts([]);
        setCategories([]);
        return;
      }

      setLoading(true);
      try {
        const searchFilters = {
          category: selectedCategory,
          minPrice: priceRange[0].toString(),
          maxPrice: priceRange[1].toString(),
          sort: sortOption,
          ...filters,
        };

        const results = await unifiedSearch({
          query: searchQuery,
          filters: searchFilters,
          searchType: "all",
        });

        setProducts(results.products);
        setCategories(results.categories);
      } catch (error) {
        console.error("Search error:", error);
        setProducts([]);
        setCategories([]);
      } finally {
        setLoading(false);
      }
    },
    [selectedCategory, priceRange, sortOption]
  );

  // Get search suggestions
  const getSuggestions = async (searchQuery: string) => {
    if (searchQuery.length < 2) {
      setSuggestions([]);
      return;
    }

    try {
      const newSuggestions = await getSearchSuggestions(searchQuery, 5);
      setSuggestions(newSuggestions);
    } catch (error) {
      console.error("Error getting suggestions:", error);
      setSuggestions([]);
    }
  };

  // Handle search input change
  const handleSearchChange = (value: string) => {
    setQuery(value);
    getSuggestions(value);
    setShowSuggestions(true);
  };

  // Handle search submit
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setShowSuggestions(false);
    performSearch(query);
  };

  // Handle suggestion click
  const handleSuggestionClick = (suggestion: string) => {
    setQuery(suggestion);
    setShowSuggestions(false);
    performSearch(suggestion);
  };

  // Initial search on mount
  useEffect(() => {
    if (initialQuery) {
      performSearch(initialQuery);
    }
  }, [initialQuery, performSearch]);

  // Search when filters change
  useEffect(() => {
    if (query) {
      performSearch(query);
    }
  }, [query, sortOption, selectedCategory, priceRange, performSearch]);

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Search Header */}
      <div className="max-w-4xl mx-auto mb-8">
        <h1 className="text-3xl font-bold mb-6 text-center">Search Products</h1>

        {/* Search Bar */}
        <form onSubmit={handleSearchSubmit} className="relative mb-6">
          <div className="relative">
            <FaSearch className="absolute left-4 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
            <input
              type="text"
              value={query}
              onChange={(e) => handleSearchChange(e.target.value)}
              onFocus={() => setShowSuggestions(true)}
              onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
              placeholder="Search for products, categories, brands..."
              className="w-full pl-12 pr-4 py-4 text-lg border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <button
              type="submit"
              className="absolute right-2 top-1/2 transform -translate-y-1/2 bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700 transition-colors"
            >
              Search
            </button>
          </div>

          {/* Search Suggestions */}
          {showSuggestions && suggestions.length > 0 && (
            <div className="absolute top-full left-0 right-0 bg-white border border-gray-300 rounded-b-lg shadow-lg z-10">
              {suggestions.map((suggestion, index) => (
                <button
                  key={index}
                  onClick={() => handleSuggestionClick(suggestion)}
                  className="w-full text-left px-4 py-2 hover:bg-gray-100 transition-colors"
                >
                  <FaSearch className="inline w-3 h-3 mr-2 text-gray-400" />
                  {suggestion}
                </button>
              ))}
            </div>
          )}
        </form>

        {/* Mobile Filter Toggle */}
        <div className="md:hidden mb-4">
          <button
            onClick={() => setShowFilters(!showFilters)}
            className="w-full flex items-center justify-between p-4 bg-white border border-gray-300 rounded-lg"
          >
            <span className="flex items-center gap-2">
              <FaFilter className="w-4 h-4" />
              Filters & Sort
            </span>
            <span>{showFilters ? "▲" : "▼"}</span>
          </button>
        </div>

        {/* Filters */}
        <div
          className={`${
            showFilters ? "block" : "hidden md:block"
          } bg-white p-6 rounded-lg border border-gray-300 mb-6`}
        >
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            {/* Sort */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Sort by
              </label>
              <select
                value={sortOption}
                onChange={(e) => setSortOption(e.target.value as SortOption)}
                className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
              >
                <option value="relevance">Most Relevant</option>
                <option value="newest">Newest First</option>
                <option value="price_asc">Price: Low to High</option>
                <option value="price_desc">Price: High to Low</option>
                <option value="name_asc">Name: A to Z</option>
                <option value="name_desc">Name: Z to A</option>
              </select>
            </div>

            {/* Category Filter */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Category
              </label>
              <select
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="w-full p-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500"
              >
                <option value="">All Categories</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id.toString()}>
                    {category.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Price Range */}
            <div className="md:col-span-2">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Price Range: ${priceRange[0]} - ${priceRange[1]}
              </label>
              <div className="flex gap-2">
                <input
                  type="range"
                  min="0"
                  max="1000"
                  value={priceRange[0]}
                  onChange={(e) =>
                    setPriceRange([parseInt(e.target.value), priceRange[1]])
                  }
                  className="flex-1"
                />
                <input
                  type="range"
                  min="0"
                  max="1000"
                  value={priceRange[1]}
                  onChange={(e) =>
                    setPriceRange([priceRange[0], parseInt(e.target.value)])
                  }
                  className="flex-1"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Search Results */}
      <div className="max-w-6xl mx-auto">
        {query && (
          <div className="mb-6">
            <h2 className="text-xl font-semibold mb-2">
              Search results for &quot;{query}&quot;
            </h2>
            <p className="text-gray-600">
              Found {products.length} product{products.length !== 1 ? "s" : ""}
              {categories.length > 0 &&
                ` and ${categories.length} categor${
                  categories.length !== 1 ? "ies" : "y"
                }`}
            </p>
          </div>
        )}

        {loading ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {[...Array(8)].map((_, index) => (
              <div
                key={index}
                className="bg-gray-200 animate-pulse h-64 rounded-lg"
              ></div>
            ))}
          </div>
        ) : (
          <>
            {/* Categories Results */}
            {categories.length > 0 && (
              <div className="mb-8">
                <h3 className="text-lg font-semibold mb-4">Categories</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                  {categories.map((category) => (
                    <motion.div
                      key={category.id}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      className="bg-white p-4 rounded-lg border border-gray-200 hover:shadow-md transition-shadow cursor-pointer"
                      onClick={() =>
                        setSelectedCategory(category.id.toString())
                      }
                    >
                      <h4 className="font-medium text-gray-900">
                        {category.name}
                      </h4>
                      <p className="text-sm text-gray-600 mt-1">
                        {category.description}
                      </p>
                    </motion.div>
                  ))}
                </div>
              </div>
            )}

            {/* Products Results */}
            {products.length > 0 ? (
              <div>
                <h3 className="text-lg font-semibold mb-4">Products</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                  {products.map((product) => (
                    <motion.div
                      key={product.id}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.1 }}
                    >
                      <ProductCard product={product} />
                    </motion.div>
                  ))}
                </div>
              </div>
            ) : query && !loading ? (
              <div className="text-center py-12">
                <div className="text-gray-400 mb-4">
                  <FaSearch className="w-16 h-16 mx-auto" />
                </div>
                <h3 className="text-xl font-medium text-gray-900 mb-2">
                  No results found
                </h3>
                <p className="text-gray-600 mb-4">
                  We couldn&apos;t find any products matching &quot;{query}
                  &quot;.
                </p>
                <div className="text-sm text-gray-500">
                  <p>Try:</p>
                  <ul className="mt-2 space-y-1">
                    <li>• Checking your spelling</li>
                    <li>• Using different keywords</li>
                    <li>• Searching for a more general term</li>
                    <li>• Adjusting your filters</li>
                  </ul>
                </div>
              </div>
            ) : !query ? (
              <div className="text-center py-12">
                <div className="text-gray-400 mb-4">
                  <FaSearch className="w-16 h-16 mx-auto" />
                </div>
                <h3 className="text-xl font-medium text-gray-900 mb-2">
                  Start your search
                </h3>
                <p className="text-gray-600">
                  Enter a product name, category, or brand to find what
                  you&apos;re looking for.
                </p>
              </div>
            ) : null}
          </>
        )}
      </div>
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-gray-50 flex items-center justify-center">
          <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
        </div>
      }
    >
      <SearchPageContent />
    </Suspense>
  );
}
