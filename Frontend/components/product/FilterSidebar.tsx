"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { FaFilter, FaChevronDown, FaChevronUp } from "react-icons/fa";
import { Category } from "../../app/api/services/category";
import { getAllCategories } from "../../app/api/services/category";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Card } from "../ui/card";
import { cn } from "../../utils/cn";

interface FilterSidebarProps {
  filters: {
    category: string;
    minPrice: string;
    maxPrice: string;
    discount: boolean;
    sort: string;
  };
  onFilterChange: (filters: Record<string, unknown>) => void;
  className?: string;
}

const FilterSidebar = ({
  filters,
  onFilterChange,
  className,
}: FilterSidebarProps) => {
  const router = useRouter();
  const [categories, setCategories] = useState<Category[]>([]);
  const [expanded, setExpanded] = useState<Record<string, boolean>>({
    categories: true,
    price: true,
    discount: true,
  });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchCategories = async () => {
      setIsLoading(true);
      try {
        const categoriesData = await getAllCategories();
        setCategories(categoriesData);
      } catch (error) {
        console.error("Error fetching categories:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchCategories();
  }, []);

  const toggleSection = (section: string) => {
    setExpanded((prev) => ({
      ...prev,
      [section]: !prev[section],
    }));
  };

  const applyFilters = () => {
    // Build query string
    const params = new URLSearchParams(window.location.search);

    if (filters.category) params.set("category", filters.category);
    else params.delete("category");

    if (filters.minPrice) params.set("minPrice", filters.minPrice);
    else params.delete("minPrice");

    if (filters.maxPrice) params.set("maxPrice", filters.maxPrice);
    else params.delete("maxPrice");

    if (filters.discount) params.set("discount", "true");
    else params.delete("discount");

    if (filters.sort !== "relevance") params.set("sort", filters.sort);
    else params.delete("sort");

    // Update URL
    const query = params.toString();
    const path = window.location.pathname + (query ? `?${query}` : "");
    router.push(path);
  };

  const clearFilters = () => {
    onFilterChange({
      category: "",
      minPrice: "",
      maxPrice: "",
      discount: false,
      sort: "relevance",
    });
  };

  return (
    <Card
      className={cn(
        "p-5 sticky top-24 border-gray-100/50 bg-white/80 backdrop-blur-sm",
        className
      )}
    >
      <div className="flex justify-between items-center mb-5">
        <h3 className="font-bold text-lg flex items-center gap-2 text-foreground">
          <FaFilter size={16} className="text-primary" />
          Filters
        </h3>
        <Button
          onClick={clearFilters}
          variant="link"
          size="sm"
          className="text-sm font-medium"
        >
          Clear All
        </Button>
      </div>

      {/* Categories section */}
      <FilterSection
        title="Categories"
        expanded={expanded.categories}
        onToggle={() => toggleSection("categories")}
        isLoading={isLoading}
      >
        <div className="space-y-2.5 mt-3">
          <div className="flex items-center">
            <input
              type="radio"
              id="all-categories"
              name="category"
              checked={!filters.category}
              onChange={() => onFilterChange({ category: "" })}
              className="mr-3 h-4 w-4 text-primary border-gray-300 focus:ring-primary"
            />
            <label htmlFor="all-categories" className="text-sm">
              All Categories
            </label>
          </div>

          {categories.map((category) => (
            <div key={category.id} className="flex items-center">
              <input
                type="radio"
                id={`category-${category.id}`}
                name="category"
                checked={filters.category === category.id.toString()}
                onChange={() => onFilterChange({ category: category.id })}
                className="mr-3 h-4 w-4 text-primary border-gray-300 focus:ring-primary"
              />
              <label htmlFor={`category-${category.id}`} className="text-sm">
                {category.name}
              </label>
            </div>
          ))}
        </div>
      </FilterSection>

      {/* Price range section */}
      <FilterSection
        title="Price Range"
        expanded={expanded.price}
        onToggle={() => toggleSection("price")}
      >
        <div className="space-y-2 mt-3">
          <div className="flex gap-3">
            <div className="flex-1">
              <label className="text-sm text-muted-foreground block mb-1.5">
                Min
              </label>
              <Input
                type="number"
                value={filters.minPrice}
                onChange={(e) => onFilterChange({ minPrice: e.target.value })}
                placeholder="0"
              />
            </div>
            <div className="flex-1">
              <label className="text-sm text-muted-foreground block mb-1.5">
                Max
              </label>
              <Input
                type="number"
                value={filters.maxPrice}
                onChange={(e) => onFilterChange({ maxPrice: e.target.value })}
                placeholder="10000"
              />
            </div>
          </div>
        </div>
      </FilterSection>

      {/* Discount section */}
      <FilterSection
        title="Discount"
        expanded={expanded.discount}
        onToggle={() => toggleSection("discount")}
      >
        <div className="space-y-2 mt-3">
          <div className="flex items-center">
            <input
              type="checkbox"
              id="discount-only"
              checked={filters.discount}
              onChange={(e) => onFilterChange({ discount: e.target.checked })}
              className="mr-3 h-4 w-4 rounded text-primary border-gray-300 focus:ring-primary"
            />
            <label htmlFor="discount-only" className="text-sm">
              On Sale Only
            </label>
          </div>
        </div>
      </FilterSection>

      <Button onClick={applyFilters} className="w-full mt-5" hover="scale">
        Apply Filters
      </Button>
    </Card>
  );
};

interface FilterSectionProps {
  title: string;
  expanded: boolean;
  onToggle: () => void;
  children: React.ReactNode;
  isLoading?: boolean;
}

const FilterSection = ({
  title,
  expanded,
  onToggle,
  children,
  isLoading = false,
}: FilterSectionProps) => {
  return (
    <div className="border-b border-gray-100/50 pb-4 mb-4">
      <button
        className="flex justify-between items-center w-full mb-1 py-1 hover:text-primary transition-colors"
        onClick={onToggle}
        type="button"
      >
        <h4 className="font-medium">{title}</h4>
        {expanded ? <FaChevronUp size={12} /> : <FaChevronDown size={12} />}
      </button>

      {expanded &&
        (isLoading ? (
          <div className="mt-3 space-y-2">
            <div className="h-4 w-2/3 bg-gray-200 rounded animate-pulse"></div>
            <div className="h-4 w-1/2 bg-gray-200 rounded animate-pulse"></div>
            <div className="h-4 w-3/4 bg-gray-200 rounded animate-pulse"></div>
          </div>
        ) : (
          children
        ))}
    </div>
  );
};

export default FilterSidebar;
