"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import {
  FaPlus,
  FaSearch,
  FaFilter,
  FaEdit,
  FaTrash,
  FaEye,
  FaImage,
  FaBox,
  FaDollarSign,
  FaWarehouse,
} from "react-icons/fa";
import Image from "next/image";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import SellerLayout from "../../../components/seller/SellerLayout";
import {
  getSellerProducts,
  searchSellerProducts,
  deleteSellerProduct,
  updateProductQuantity,
  updateProductPrice,
  bulkUpdateProductStatus,
  SellerProduct,
} from "../../api/services/seller";

export default function SellerProductsPage() {
  const router = useRouter();
  const [products, setProducts] = useState<SellerProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedProducts, setSelectedProducts] = useState<Set<number>>(
    new Set()
  );
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [showFilters, setShowFilters] = useState(false);
  const [statusFilter, setStatusFilter] = useState("");
  const [actionLoading, setActionLoading] = useState<Set<number>>(new Set());
  const [bulkActionLoading, setBulkActionLoading] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error" | "info";
    text: string;
  } | null>(null);

  const pageSize = 12;

  const loadProducts = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getSellerProducts(
        currentPage,
        pageSize,
        searchQuery
      );
      setProducts(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (error) {
      console.error("Error loading products:", error);
      setMessage({ type: "error", text: "Failed to load products" });
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize, searchQuery]);

  useEffect(() => {
    loadProducts();
  }, [currentPage, statusFilter, loadProducts]);

  const handleSearch = async () => {
    try {
      setLoading(true);
      setCurrentPage(0);
      const response = searchQuery
        ? await searchSellerProducts(searchQuery, 0, pageSize)
        : await getSellerProducts(0, pageSize);
      setProducts(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (error) {
      console.error("Error searching products:", error);
      setMessage({ type: "error", text: "Failed to search products" });
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteProduct = async (productId: number) => {
    if (!confirm("Are you sure you want to delete this product?")) {
      return;
    }

    try {
      setActionLoading((prev) => new Set([...prev, productId]));
      await deleteSellerProduct(productId);
      setMessage({ type: "success", text: "Product deleted successfully" });
      await loadProducts();
    } catch (error) {
      console.error("Error deleting product:", error);
      setMessage({ type: "error", text: "Failed to delete product" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(productId);
        return newSet;
      });
    }
  };

  const handleQuickUpdateQuantity = async (
    productId: number,
    newQuantity: number
  ) => {
    try {
      setActionLoading((prev) => new Set([...prev, productId]));
      await updateProductQuantity(productId, newQuantity);
      setMessage({ type: "success", text: "Quantity updated successfully" });
      await loadProducts();
    } catch (error) {
      console.error("Error updating quantity:", error);
      setMessage({ type: "error", text: "Failed to update quantity" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(productId);
        return newSet;
      });
    }
  };

  const handleQuickUpdatePrice = async (
    productId: number,
    newPrice: number
  ) => {
    try {
      setActionLoading((prev) => new Set([...prev, productId]));
      await updateProductPrice(productId, newPrice);
      setMessage({ type: "success", text: "Price updated successfully" });
      await loadProducts();
    } catch (error) {
      console.error("Error updating price:", error);
      setMessage({ type: "error", text: "Failed to update price" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(productId);
        return newSet;
      });
    }
  };

  const handleBulkAction = async (action: string) => {
    if (selectedProducts.size === 0) {
      setMessage({ type: "error", text: "Please select products first" });
      return;
    }

    try {
      setBulkActionLoading(true);
      await bulkUpdateProductStatus(Array.from(selectedProducts), action);
      setMessage({
        type: "success",
        text: `${selectedProducts.size} products ${action} successfully`,
      });
      setSelectedProducts(new Set());
      await loadProducts();
    } catch (error) {
      console.error("Error performing bulk action:", error);
      setMessage({ type: "error", text: "Failed to perform bulk action" });
    } finally {
      setBulkActionLoading(false);
    }
  };

  const handleSelectProduct = (productId: number) => {
    const newSelected = new Set(selectedProducts);
    if (newSelected.has(productId)) {
      newSelected.delete(productId);
    } else {
      newSelected.add(productId);
    }
    setSelectedProducts(newSelected);
  };

  const handleSelectAll = () => {
    if (selectedProducts.size === products.length) {
      setSelectedProducts(new Set());
    } else {
      setSelectedProducts(new Set(products.map((p) => p.id)));
    }
  };

  return (
    <SellerLayout
      activeNav="products"
      title="Product Management"
      description={`Manage your products (${totalElements} total)`}
    >
      {/* Message Display */}
      {message && (
        <div
          className={`mb-6 p-4 rounded-md ${
            message.type === "success"
              ? "bg-green-50 text-green-700 border border-green-200"
              : message.type === "info"
              ? "bg-blue-50 text-blue-700 border border-blue-200"
              : "bg-red-50 text-red-700 border border-red-200"
          }`}
        >
          {message.text}
        </div>
      )}

      {/* Actions Bar */}
      <div className="bg-white rounded-lg shadow-sm border p-4 mb-6">
        <div className="flex flex-col lg:flex-row gap-4 items-start lg:items-center justify-between">
          {/* Search and Filters */}
          <div className="flex flex-col sm:flex-row gap-3 flex-1">
            <div className="relative flex-1 max-w-md">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <FaSearch className="h-4 w-4 text-gray-400" />
              </div>
              <Input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyPress={(e) => e.key === "Enter" && handleSearch()}
                placeholder="Search products..."
                className="pl-10"
              />
            </div>
            <Button onClick={handleSearch} variant="outline">
              Search
            </Button>
            <Button
              onClick={() => setShowFilters(!showFilters)}
              variant="outline"
            >
              <FaFilter className="w-4 h-4 mr-2" />
              Filters
            </Button>
          </div>

          {/* Action Buttons */}
          <div className="flex gap-3">
            <Button
              onClick={() => router.push("/seller/products/new")}
              className="bg-blue-600 hover:bg-blue-700"
            >
              <FaPlus className="w-4 h-4 mr-2" />
              Add Product
            </Button>
          </div>
        </div>

        {/* Filter Panel */}
        {showFilters && (
          <div className="mt-4 pt-4 border-t border-gray-200">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Status
                </label>
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">All Status</option>
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="OUT_OF_STOCK">Out of Stock</option>
                </select>
              </div>
              <div className="flex items-end">
                <Button
                  onClick={() => {
                    setStatusFilter("");
                    setSearchQuery("");
                    setCurrentPage(0);
                    loadProducts();
                  }}
                  variant="outline"
                  size="sm"
                >
                  Clear Filters
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Bulk Actions */}
      {selectedProducts.size > 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <div className="flex items-center justify-between">
            <span className="text-sm text-blue-800">
              {selectedProducts.size} product(s) selected
            </span>
            <div className="flex gap-2">
              <Button
                onClick={() => handleBulkAction("ACTIVE")}
                loading={bulkActionLoading}
                size="sm"
                variant="outline"
              >
                Activate
              </Button>
              <Button
                onClick={() => handleBulkAction("INACTIVE")}
                loading={bulkActionLoading}
                size="sm"
                variant="outline"
              >
                Deactivate
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Products Grid */}
      <div className="bg-white rounded-lg shadow-sm border">
        {/* Header */}
        <div className="p-4 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={
                  selectedProducts.size === products.length &&
                  products.length > 0
                }
                onChange={handleSelectAll}
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <span className="text-sm text-gray-600">Select All</span>
            </div>
            <span className="text-sm text-gray-600">
              Showing {currentPage * pageSize + 1} to{" "}
              {Math.min((currentPage + 1) * pageSize, totalElements)} of{" "}
              {totalElements}
            </span>
          </div>
        </div>

        {/* Products List */}
        <div className="p-4">
          {loading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[...Array(6)].map((_, index) => (
                <div key={index} className="animate-pulse">
                  <div className="bg-gray-200 rounded-lg h-48 mb-4"></div>
                  <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                  <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                </div>
              ))}
            </div>
          ) : products.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {products.map((product) => (
                <ProductCard
                  key={product.id}
                  product={product}
                  selected={selectedProducts.has(product.id)}
                  onSelect={() => handleSelectProduct(product.id)}
                  onEdit={() =>
                    router.push(`/seller/products/${product.id}/edit`)
                  }
                  onDelete={() => handleDeleteProduct(product.id)}
                  onView={() => router.push(`/product/${product.id}`)}
                  onQuickUpdateQuantity={(qty) =>
                    handleQuickUpdateQuantity(product.id, qty)
                  }
                  onQuickUpdatePrice={(price) =>
                    handleQuickUpdatePrice(product.id, price)
                  }
                  loading={actionLoading.has(product.id)}
                />
              ))}
            </div>
          ) : (
            <div className="text-center py-12">
              <FaBox className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">
                {searchQuery ? "No products found" : "No products yet"}
              </h3>
              <p className="text-gray-600 mb-6">
                {searchQuery
                  ? "Try adjusting your search terms"
                  : "Start by adding your first product"}
              </p>
              <Button
                onClick={() => router.push("/seller/products/new")}
                className="bg-blue-600 hover:bg-blue-700"
              >
                <FaPlus className="w-4 h-4 mr-2" />
                Add Product
              </Button>
            </div>
          )}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-4 py-3 border-t border-gray-200">
            <div className="flex items-center justify-between">
              <div className="text-sm text-gray-700">
                Showing {currentPage * pageSize + 1} to{" "}
                {Math.min((currentPage + 1) * pageSize, totalElements)} of{" "}
                {totalElements} products
              </div>
              <div className="flex gap-2">
                <Button
                  onClick={() =>
                    setCurrentPage((prev) => Math.max(0, prev - 1))
                  }
                  disabled={currentPage === 0}
                  variant="outline"
                  size="sm"
                >
                  Previous
                </Button>
                <span className="flex items-center px-3 text-sm text-gray-700">
                  Page {currentPage + 1} of {totalPages}
                </span>
                <Button
                  onClick={() =>
                    setCurrentPage((prev) => Math.min(totalPages - 1, prev + 1))
                  }
                  disabled={currentPage >= totalPages - 1}
                  variant="outline"
                  size="sm"
                >
                  Next
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </SellerLayout>
  );
}

interface ProductCardProps {
  product: SellerProduct;
  selected: boolean;
  onSelect: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onView: () => void;
  onQuickUpdateQuantity: (quantity: number) => void;
  onQuickUpdatePrice: (price: number) => void;
  loading: boolean;
}

function ProductCard({
  product,
  selected,
  onSelect,
  onEdit,
  onDelete,
  onView,
  onQuickUpdateQuantity,
  onQuickUpdatePrice,
  loading,
}: ProductCardProps) {
  const [editingQuantity, setEditingQuantity] = useState(false);
  const [editingPrice, setEditingPrice] = useState(false);
  const [tempQuantity, setTempQuantity] = useState(product.quantity);
  const [tempPrice, setTempPrice] = useState(product.price);

  const handleQuantitySubmit = () => {
    onQuickUpdateQuantity(tempQuantity);
    setEditingQuantity(false);
  };

  const handlePriceSubmit = () => {
    onQuickUpdatePrice(tempPrice);
    setEditingPrice(false);
  };

  return (
    <div
      className={`border rounded-lg p-4 transition-all ${
        selected
          ? "border-blue-500 bg-blue-50"
          : "border-gray-200 hover:border-gray-300"
      }`}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <input
          type="checkbox"
          checked={selected}
          onChange={onSelect}
          className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
        />
        <div className="flex gap-1">
          <button
            onClick={onView}
            className="text-gray-400 hover:text-blue-600 transition-colors"
            title="View Product"
          >
            <FaEye className="w-4 h-4" />
          </button>
          <button
            onClick={onEdit}
            className="text-gray-400 hover:text-blue-600 transition-colors"
            title="Edit Product"
            disabled={loading}
          >
            <FaEdit className="w-4 h-4" />
          </button>
          <button
            onClick={onDelete}
            className="text-gray-400 hover:text-red-600 transition-colors"
            title="Delete Product"
            disabled={loading}
          >
            <FaTrash className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Product Image */}
      <div className="relative aspect-square mb-3 bg-gray-100 rounded-md overflow-hidden">
        {product.imageUrl ? (
          <Image
            src={product.imageUrl}
            alt={product.name}
            fill
            className="object-cover"
          />
        ) : (
          <div className="flex items-center justify-center h-full">
            <FaImage className="w-8 h-8 text-gray-400" />
          </div>
        )}
      </div>

      {/* Product Info */}
      <div className="space-y-2">
        <h3 className="font-medium text-gray-900 line-clamp-2">
          {product.name}
        </h3>

        {/* Status Badge */}
        <span
          className={`inline-flex items-center px-2 py-1 text-xs font-medium rounded-full ${
            product.productStatus === "ACTIVE"
              ? "bg-green-100 text-green-800"
              : product.productStatus === "OUT_OF_STOCK"
              ? "bg-red-100 text-red-800"
              : "bg-gray-100 text-gray-800"
          }`}
        >
          {product.productStatus}
        </span>

        {/* Price */}
        <div className="flex items-center gap-2">
          <FaDollarSign className="w-3 h-3 text-gray-400" />
          {editingPrice ? (
            <div className="flex items-center gap-1">
              <input
                type="number"
                value={tempPrice}
                onChange={(e) => setTempPrice(Number(e.target.value))}
                className="w-20 px-2 py-1 text-sm border border-gray-300 rounded"
                step="0.01"
                min="0"
              />
              <button
                onClick={handlePriceSubmit}
                className="text-green-600 hover:text-green-700"
                disabled={loading}
              >
                ✓
              </button>
              <button
                onClick={() => {
                  setEditingPrice(false);
                  setTempPrice(product.price);
                }}
                className="text-red-600 hover:text-red-700"
              >
                ✕
              </button>
            </div>
          ) : (
            <span
              className="text-sm font-medium text-gray-900 cursor-pointer hover:text-blue-600"
              onClick={() => setEditingPrice(true)}
            >
              €{product.price}
            </span>
          )}
        </div>

        {/* Quantity */}
        <div className="flex items-center gap-2">
          <FaWarehouse className="w-3 h-3 text-gray-400" />
          {editingQuantity ? (
            <div className="flex items-center gap-1">
              <input
                type="number"
                value={tempQuantity}
                onChange={(e) => setTempQuantity(Number(e.target.value))}
                className="w-16 px-2 py-1 text-sm border border-gray-300 rounded"
                min="0"
              />
              <button
                onClick={handleQuantitySubmit}
                className="text-green-600 hover:text-green-700"
                disabled={loading}
              >
                ✓
              </button>
              <button
                onClick={() => {
                  setEditingQuantity(false);
                  setTempQuantity(product.quantity);
                }}
                className="text-red-600 hover:text-red-700"
              >
                ✕
              </button>
            </div>
          ) : (
            <span
              className="text-sm text-gray-600 cursor-pointer hover:text-blue-600"
              onClick={() => setEditingQuantity(true)}
            >
              Stock: {product.quantity}
            </span>
          )}
        </div>
      </div>

      {loading && (
        <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center rounded-lg">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
        </div>
      )}
    </div>
  );
}
