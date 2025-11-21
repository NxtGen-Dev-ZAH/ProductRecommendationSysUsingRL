"use client";

import { useState, useEffect, useCallback } from "react";
import {
  FaSearch,
  FaFilter,
  FaEye,
  FaTrash,
  FaBox,
  FaDollarSign,
  FaWarehouse,
  FaImage,
  FaUser,
  FaDownload,
} from "react-icons/fa";
import Image from "next/image";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import AdminLayout from "../../../components/admin/AdminLayout";
import {
  getAdminProducts,
  searchAdminProducts,
  deleteAdminProduct,
  getAdminProductById,
  AdminProductResponse,
} from "../../api/services/admin";

export default function AdminProductsPage() {
  const [products, setProducts] = useState<AdminProductResponse[]>([]);
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
  const [conditionFilter, setConditionFilter] = useState("");
  const [sellTypeFilter, setSellTypeFilter] = useState("");
  const [actionLoading, setActionLoading] = useState<Set<number>>(new Set());
  const [selectedProduct, setSelectedProduct] =
    useState<AdminProductResponse | null>(null);
  const [showProductModal, setShowProductModal] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error" | "info";
    text: string;
  } | null>(null);

  const pageSize = 12;

  const loadProducts = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getAdminProducts(currentPage, pageSize);
      setProducts(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
    } catch (error) {
      console.error("Error loading products:", error);
      setMessage({ type: "error", text: "Failed to load products" });
    } finally {
      setLoading(false);
    }
  }, [currentPage, pageSize]);

  useEffect(() => {
    loadProducts();
  }, [
    currentPage,
    statusFilter,
    conditionFilter,
    sellTypeFilter,
    loadProducts,
  ]);

  const handleSearch = async () => {
    try {
      setLoading(true);
      setCurrentPage(0);
      const response = searchQuery
        ? await searchAdminProducts(searchQuery, 0, pageSize)
        : await getAdminProducts(0, pageSize);
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
    if (
      !confirm(
        "Are you sure you want to delete this product? This action cannot be undone."
      )
    ) {
      return;
    }

    try {
      setActionLoading((prev) => new Set([...prev, productId]));
      await deleteAdminProduct(productId);
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

  const handleViewProduct = async (productId: number) => {
    try {
      setActionLoading((prev) => new Set([...prev, productId]));
      const product = await getAdminProductById(productId);
      setSelectedProduct(product);
      setShowProductModal(true);
    } catch (error) {
      console.error("Error loading product details:", error);
      setMessage({ type: "error", text: "Failed to load product details" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(productId);
        return newSet;
      });
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

  const getStatusBadgeColor = (status: string) => {
    switch (status) {
      case "ACTIVE":
        return "bg-green-100 text-green-800";
      case "INACTIVE":
        return "bg-gray-100 text-gray-800";
      case "OUT_OF_STOCK":
        return "bg-red-100 text-red-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  const getConditionBadgeColor = (condition: string) => {
    switch (condition) {
      case "NEW":
        return "bg-blue-100 text-blue-800";
      case "USED":
        return "bg-yellow-100 text-yellow-800";
      case "OPEN_NEVER_USED":
        return "bg-purple-100 text-purple-800";
      case "REFURBISHED":
        return "bg-orange-100 text-orange-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  return (
    <AdminLayout
      activeNav="products"
      title="Product Management"
      description={`Manage all products in the system (${totalElements} total)`}
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
              variant="outline"
              onClick={() => {
                /* Export functionality */
              }}
            >
              <FaDownload className="w-4 h-4 mr-2" />
              Export
            </Button>
          </div>
        </div>

        {/* Filter Panel */}
        {showFilters && (
          <div className="mt-4 pt-4 border-t border-gray-200">
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Status
                </label>
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-red-500 focus:border-transparent"
                >
                  <option value="">All Status</option>
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="OUT_OF_STOCK">Out of Stock</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Condition
                </label>
                <select
                  value={conditionFilter}
                  onChange={(e) => setConditionFilter(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-red-500 focus:border-transparent"
                >
                  <option value="">All Conditions</option>
                  <option value="NEW">New</option>
                  <option value="USED">Used</option>
                  <option value="OPEN_NEVER_USED">Open Never Used</option>
                  <option value="REFURBISHED">Refurbished</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Sell Type
                </label>
                <select
                  value={sellTypeFilter}
                  onChange={(e) => setSellTypeFilter(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-red-500 focus:border-transparent"
                >
                  <option value="">All Types</option>
                  <option value="DIRECT">Direct</option>
                  <option value="OFFER">Offer</option>
                  <option value="AUCTION">Auction</option>
                  <option value="PREORDER">Pre-order</option>
                </select>
              </div>
              <div className="flex items-end">
                <Button
                  onClick={() => {
                    setStatusFilter("");
                    setConditionFilter("");
                    setSellTypeFilter("");
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
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
          <div className="flex items-center justify-between">
            <span className="text-sm text-red-800">
              {selectedProducts.size} product(s) selected
            </span>
            <div className="flex gap-2">
              <Button
                onClick={() => {
                  /* Bulk delete */
                }}
                size="sm"
                variant="outline"
                className="border-red-300 text-red-600 hover:bg-red-50"
              >
                Delete Selected
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
                className="h-4 w-4 text-red-600 focus:ring-red-500 border-gray-300 rounded"
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
                <AdminProductCard
                  key={product.id}
                  product={product}
                  selected={selectedProducts.has(product.id)}
                  onSelect={() => handleSelectProduct(product.id)}
                  onView={() => handleViewProduct(product.id)}
                  onDelete={() => handleDeleteProduct(product.id)}
                  loading={actionLoading.has(product.id)}
                  getStatusBadgeColor={getStatusBadgeColor}
                  getConditionBadgeColor={getConditionBadgeColor}
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
                  : "Products will appear here as they are created"}
              </p>
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

      {/* Product Details Modal */}
      {showProductModal && selectedProduct && (
        <ProductDetailsModal
          product={selectedProduct}
          onClose={() => {
            setShowProductModal(false);
            setSelectedProduct(null);
          }}
          getStatusBadgeColor={getStatusBadgeColor}
          getConditionBadgeColor={getConditionBadgeColor}
        />
      )}
    </AdminLayout>
  );
}

interface AdminProductCardProps {
  product: AdminProductResponse;
  selected: boolean;
  onSelect: () => void;
  onView: () => void;
  onDelete: () => void;
  loading: boolean;
  getStatusBadgeColor: (status: string) => string;
  getConditionBadgeColor: (condition: string) => string;
}

function AdminProductCard({
  product,
  selected,
  onSelect,
  onView,
  onDelete,
  loading,
  getStatusBadgeColor,
  getConditionBadgeColor,
}: AdminProductCardProps) {
  const primaryImage =
    product.images?.find((img) => img.isPrimary) || product.images?.[0];

  return (
    <div
      className={`border rounded-lg p-4 transition-all relative ${
        selected
          ? "border-red-500 bg-red-50"
          : "border-gray-200 hover:border-gray-300"
      }`}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-3">
        <input
          type="checkbox"
          checked={selected}
          onChange={onSelect}
          className="h-4 w-4 text-red-600 focus:ring-red-500 border-gray-300 rounded"
        />
        <div className="flex gap-1">
          <button
            onClick={onView}
            className="text-gray-400 hover:text-blue-600 transition-colors"
            title="View Details"
            disabled={loading}
          >
            <FaEye className="w-4 h-4" />
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
        {primaryImage ? (
          <Image
            src={primaryImage.fileUrl}
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

        {/* Badges */}
        <div className="flex flex-wrap gap-1">
          <span
            className={`inline-flex items-center px-2 py-1 text-xs font-medium rounded-full ${getStatusBadgeColor(
              product.productStatus
            )}`}
          >
            {product.productStatus}
          </span>
          <span
            className={`inline-flex items-center px-2 py-1 text-xs font-medium rounded-full ${getConditionBadgeColor(
              product.productCondition
            )}`}
          >
            {product.productCondition}
          </span>
        </div>

        {/* Price */}
        <div className="flex items-center gap-2">
          <FaDollarSign className="w-3 h-3 text-gray-400" />
          <span className="text-sm font-medium text-gray-900">
            €{product.price}
            {product.offerPrice && (
              <span className="text-green-600 ml-1">
                (€{product.offerPrice})
              </span>
            )}
          </span>
        </div>

        {/* Quantity */}
        <div className="flex items-center gap-2">
          <FaWarehouse className="w-3 h-3 text-gray-400" />
          <span className="text-sm text-gray-600">
            Stock: {product.quantity}
          </span>
        </div>

        {/* Author */}
        <div className="flex items-center gap-2">
          <FaUser className="w-3 h-3 text-gray-400" />
          <span className="text-sm text-gray-600">ID: {product.authorId}</span>
        </div>
      </div>

      {loading && (
        <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center rounded-lg">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-red-600"></div>
        </div>
      )}
    </div>
  );
}

interface ProductDetailsModalProps {
  product: AdminProductResponse;
  onClose: () => void;
  getStatusBadgeColor: (status: string) => string;
  getConditionBadgeColor: (condition: string) => string;
}

function ProductDetailsModal({
  product,
  onClose,
  getStatusBadgeColor,
  getConditionBadgeColor,
}: ProductDetailsModalProps) {
  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-medium text-gray-900">
              Product Details - {product.name}
            </h3>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600"
            >
              ✕
            </button>
          </div>
        </div>

        <div className="p-6">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Product Images */}
            <div>
              <h4 className="font-medium text-gray-900 mb-3">Images</h4>
              <div className="grid grid-cols-2 gap-2">
                {product.images?.map((image) => (
                  <div
                    key={image.id}
                    className="relative aspect-square bg-gray-100 rounded-md overflow-hidden"
                  >
                    <Image
                      src={image.fileUrl}
                      alt={`Product image ${image.id}`}
                      fill
                      className="object-cover"
                    />
                    {image.isPrimary && (
                      <div className="absolute top-2 left-2 bg-blue-500 text-white text-xs px-2 py-1 rounded">
                        Primary
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Product Details */}
            <div className="space-y-4">
              <div>
                <h4 className="font-medium text-gray-900 mb-2">
                  Basic Information
                </h4>
                <div className="space-y-2 text-sm">
                  <div>
                    <strong>ID:</strong> {product.id}
                  </div>
                  <div>
                    <strong>Name:</strong> {product.name}
                  </div>
                  <div>
                    <strong>Price:</strong> €{product.price}
                  </div>
                  {product.offerPrice && (
                    <div>
                      <strong>Offer Price:</strong> €{product.offerPrice}
                    </div>
                  )}
                  <div>
                    <strong>Quantity:</strong> {product.quantity}
                  </div>
                  <div>
                    <strong>Brand:</strong> {product.brand || "N/A"}
                  </div>
                  <div>
                    <strong>Product Code:</strong>{" "}
                    {product.productCode || "N/A"}
                  </div>
                </div>
              </div>

              <div>
                <h4 className="font-medium text-gray-900 mb-2">
                  Status & Condition
                </h4>
                <div className="flex flex-wrap gap-2">
                  <span
                    className={`inline-flex items-center px-2 py-1 text-xs font-medium rounded-full ${getStatusBadgeColor(
                      product.productStatus
                    )}`}
                  >
                    {product.productStatus}
                  </span>
                  <span
                    className={`inline-flex items-center px-2 py-1 text-xs font-medium rounded-full ${getConditionBadgeColor(
                      product.productCondition
                    )}`}
                  >
                    {product.productCondition}
                  </span>
                </div>
              </div>

              <div>
                <h4 className="font-medium text-gray-900 mb-2">
                  Additional Info
                </h4>
                <div className="space-y-2 text-sm">
                  <div>
                    <strong>Author ID:</strong> {product.authorId}
                  </div>
                  {product.companyId && (
                    <div>
                      <strong>Company ID:</strong> {product.companyId}
                    </div>
                  )}
                  <div>
                    <strong>Category ID:</strong> {product.categoryId}
                  </div>
                  <div>
                    <strong>Sell Type:</strong> {product.productSellType}
                  </div>
                  <div>
                    <strong>Created:</strong>{" "}
                    {new Date(product.createdAt).toLocaleDateString()}
                  </div>
                  <div>
                    <strong>Updated:</strong>{" "}
                    {new Date(product.updatedAt).toLocaleDateString()}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
