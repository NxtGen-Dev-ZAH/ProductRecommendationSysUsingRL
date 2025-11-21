"use client";

import { useState } from "react";
import ProductCard from "../ProductCard";
import { Product } from "../../app/api/services/product";
import LoadingProductCard from "./LoadingProductCard";
import { Button } from "../ui/button";
import { cn } from "../../utils/cn";

// Extended product interface to include optional discount
interface ExtendedProduct extends Omit<Product, "id"> {
  id: string | number; // Allow both string and number ids
  discountPercentage?: number;
}

interface ProductGridProps {
  products: ExtendedProduct[];
  loading?: boolean;
  columns?: 2 | 3 | 4;
  showPagination?: boolean;
  itemsPerPage?: number;
  className?: string;
}

const ProductGrid = ({
  products,
  loading = false,
  columns = 4,
  showPagination = false,
  itemsPerPage = 12,
  className,
}: ProductGridProps) => {
  const [currentPage, setCurrentPage] = useState(1);

  // Calculate grid column class based on prop
  const gridColsClass = {
    2: "grid-cols-1 sm:grid-cols-2",
    3: "grid-cols-1 sm:grid-cols-2 lg:grid-cols-3",
    4: "grid-cols-1 sm:grid-cols-2 lg:grid-cols-4",
  }[columns];

  // Handle pagination logic
  const totalPages = showPagination
    ? Math.ceil(products.length / itemsPerPage)
    : 1;
  const currentProducts = showPagination
    ? products.slice(
        (currentPage - 1) * itemsPerPage,
        currentPage * itemsPerPage
      )
    : products;

  const goToPage = (page: number) => {
    if (page >= 1 && page <= totalPages) {
      setCurrentPage(page);
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  if (loading) {
    return (
      <div className={cn(`grid ${gridColsClass} gap-6`, className)}>
        {Array.from({ length: itemsPerPage }).map((_, index) => (
          <LoadingProductCard key={index} />
        ))}
      </div>
    );
  }

  if (products.length === 0) {
    return (
      <div className="text-center py-12 px-4 border border-gray-100/50 rounded-2xl bg-white/50 backdrop-blur-sm">
        <div className="text-4xl mb-4">🔍</div>
        <h3 className="text-xl font-medium mb-2">No products found</h3>
        <p className="text-gray-500 max-w-md mx-auto">
          We couldn&apos;t find any products matching your criteria. Try
          adjusting your filters or search terms.
        </p>
      </div>
    );
  }

  return (
    <div className={cn("space-y-8", className)}>
      <div className={`grid ${gridColsClass} gap-6`}>
        {currentProducts.map((product) => (
          <ProductCard key={product.id} product={product} />
        ))}
      </div>

      {showPagination && totalPages > 1 && (
        <div className="flex justify-center mt-8">
          <nav className="flex items-center gap-1.5">
            <Button
              onClick={() => goToPage(currentPage - 1)}
              disabled={currentPage === 1}
              variant="outline"
              size="sm"
              className="px-3"
            >
              Previous
            </Button>

            {generatePaginationItems(currentPage, totalPages).map((item) => {
              if (item === "...") {
                return (
                  <span key={`ellipsis-${item}`} className="px-2">
                    ...
                  </span>
                );
              }

              const page = Number(item);
              return (
                <Button
                  key={`page-${page}`}
                  onClick={() => goToPage(page)}
                  variant={currentPage === page ? "default" : "outline"}
                  size="sm"
                  className="min-w-[2.5rem] h-10"
                >
                  {page}
                </Button>
              );
            })}

            <Button
              onClick={() => goToPage(currentPage + 1)}
              disabled={currentPage === totalPages}
              variant="outline"
              size="sm"
              className="px-3"
            >
              Next
            </Button>
          </nav>
        </div>
      )}
    </div>
  );
};

// Helper function to generate pagination numbers with ellipsis for large ranges
function generatePaginationItems(
  current: number,
  total: number
): (string | number)[] {
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1);
  }

  if (current < 5) {
    return [1, 2, 3, 4, 5, "...", total];
  }

  if (current > total - 4) {
    return [1, "...", total - 4, total - 3, total - 2, total - 1, total];
  }

  return [1, "...", current - 1, current, current + 1, "...", total];
}

export default ProductGrid;
