"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  FaBox,
  FaShoppingCart,
  FaDollarSign,
  FaEye,
  FaPlus,
  FaEdit,
  FaChartLine,
  FaStar,
  FaExclamationTriangle,
  FaBuilding,
  FaMapMarkerAlt,
} from "react-icons/fa";
import { Button } from "../../components/ui/button";
import Image from "next/image";
import SellerLayout from "../../components/seller/SellerLayout";
import {
  getSellerDashboardStats,
  getSellerProducts,
  getSellerOrders,
  SellerDashboardStats,
  SellerProduct,
} from "../api/services/seller";
import { Order } from "../api/services/order";
import {
  getCompanyAddresses,
  addCompanyAddress,
  updateCompanyAddress,
  deleteCompanyAddress,
  type CompanyAddressRequest,
  type CompanyAddressResponse,
  type AddressType,
} from "../api/services";
import { AddressList, AddressForm } from "../../components/address";

// Extended Order interface for seller dashboard
interface SellerOrder extends Order {
  orderNumber?: string;
  customerName?: string;
  itemCount?: number;
}

export default function SellerDashboard() {
  const router = useRouter();
  const [stats, setStats] = useState<SellerDashboardStats | null>(null);
  const [products, setProducts] = useState<SellerProduct[]>([]);
  const [orders, setOrders] = useState<SellerOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<
    "overview" | "products" | "orders" | "analytics" | "company"
  >("overview");
  const [error, setError] = useState<string | null>(null);

  // Company address management state
  const [companyAddresses, setCompanyAddresses] = useState<
    CompanyAddressResponse[]
  >([]);
  const [addressesLoading, setAddressesLoading] = useState(false);
  const [showAddressForm, setShowAddressForm] = useState(false);
  const [editingAddress, setEditingAddress] =
    useState<CompanyAddressResponse | null>(null);
  const [selectedAddressType, setSelectedAddressType] =
    useState<AddressType>("EXPEDITION");

  useEffect(() => {
    loadDashboardData();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      const [dashboardStats, sellerProducts, sellerOrders] = await Promise.all([
        getSellerDashboardStats(),
        getSellerProducts(0, 5),
        getSellerOrders(0, 5),
      ]);

      setStats(dashboardStats);
      setProducts(sellerProducts.content || []);
      // Transform orders to include additional properties needed for display
      const transformedOrders = (sellerOrders.content || []).map(
        (order: Order) => ({
          ...order,
          orderNumber: `${order.id}`,
          customerName: order.shippingAddress
            ? `${order.shippingAddress.firstName} ${order.shippingAddress.lastName}`
            : "Customer",
          itemCount: order.items.length,
        })
      );

      setOrders(transformedOrders);
    } catch (error) {
      console.error("Error loading dashboard data:", error);
      setError("Failed to load dashboard data");
    } finally {
      setLoading(false);
    }
  };

  // Company address management functions
  const loadCompanyAddresses = async () => {
    try {
      setAddressesLoading(true);
      // Note: In a real app, you'd get the company ID from the user context or API
      // For now, we'll use a placeholder company ID
      const companyId = 1; // This should come from user context
      const addresses = await getCompanyAddresses(companyId);
      setCompanyAddresses(addresses);
    } catch (error) {
      console.error("Failed to load company addresses:", error);
      setError("Failed to load company addresses");
    } finally {
      setAddressesLoading(false);
    }
  };

  const handleAddNewAddress = () => {
    setEditingAddress(null);
    setShowAddressForm(true);
  };

  const handleEditAddress = (address: CompanyAddressResponse) => {
    setEditingAddress(address);
    setSelectedAddressType(address.addressType);
    setShowAddressForm(true);
  };

  const handleCancelAddressForm = () => {
    setShowAddressForm(false);
    setEditingAddress(null);
  };

  const handleAddressFormSubmit = async (
    addressData: CompanyAddressRequest
  ) => {
    try {
      const companyId = 1; // This should come from user context

      if (editingAddress) {
        await updateCompanyAddress(companyId, editingAddress.id, addressData);
      } else {
        await addCompanyAddress(companyId, addressData);
      }

      await loadCompanyAddresses();
      setShowAddressForm(false);
      setEditingAddress(null);
    } catch (error) {
      console.error("Failed to save address:", error);
      setError("Failed to save address. Please try again.");
    }
  };

  const handleDeleteAddress = async (addressId: number) => {
    if (!confirm("Are you sure you want to delete this address?")) return;

    try {
      const companyId = 1; // This should come from user context
      await deleteCompanyAddress(companyId, addressId);
      await loadCompanyAddresses();
    } catch (error) {
      console.error("Failed to delete address:", error);
      setError("Failed to delete address. Please try again.");
    }
  };

  // Load company addresses when company tab is selected
  useEffect(() => {
    if (activeTab === "company") {
      loadCompanyAddresses();
    }
  }, [activeTab]);

  return (
    <SellerLayout
      activeNav="dashboard"
      title="Seller Dashboard"
      description="Overview of your store performance"
    >
      {error && (
        <div className="mb-6 p-4 bg-red-50 text-red-700 border border-red-200 rounded-md">
          {error}
        </div>
      )}

      {/* Navigation Tabs */}
      <div className="bg-white rounded-lg shadow-sm border mb-6">
        <div className="border-b border-gray-200">
          <nav className="flex space-x-8 px-6">
            {[
              {
                key: "overview",
                label: "Overview",
                icon: <FaChartLine className="w-4 h-4" />,
              },
              {
                key: "products",
                label: "Products",
                icon: <FaBox className="w-4 h-4" />,
              },
              {
                key: "orders",
                label: "Orders",
                icon: <FaShoppingCart className="w-4 h-4" />,
              },
              {
                key: "analytics",
                label: "Analytics",
                icon: <FaChartLine className="w-4 h-4" />,
              },
              {
                key: "company",
                label: "Company",
                icon: <FaBuilding className="w-4 h-4" />,
              },
            ].map((tab) => (
              <button
                key={tab.key}
                onClick={() =>
                  setActiveTab(
                    tab.key as
                      | "overview"
                      | "products"
                      | "orders"
                      | "analytics"
                      | "company"
                  )
                }
                className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center gap-2 ${
                  activeTab === tab.key
                    ? "border-blue-600 text-blue-600"
                    : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                }`}
              >
                {tab.icon}
                {tab.label}
              </button>
            ))}
          </nav>
        </div>
      </div>

      {/* Overview Tab */}
      {activeTab === "overview" && (
        <div className="space-y-6">
          {/* Quick Stats */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <StatCard
              title="Total Products"
              value={stats?.totalProducts || 0}
              icon={<FaBox className="w-8 h-8" />}
              color="blue"
              loading={loading}
            />
            <StatCard
              title="Total Orders"
              value={stats?.totalOrders || 0}
              icon={<FaShoppingCart className="w-8 h-8" />}
              color="green"
              loading={loading}
            />
            <StatCard
              title="Revenue"
              value={
                stats?.totalRevenue
                  ? `€${stats.totalRevenue.toLocaleString()}`
                  : "€0"
              }
              icon={<FaDollarSign className="w-8 h-8" />}
              color="purple"
              loading={loading}
            />
            <StatCard
              title="Avg Order Value"
              value={
                stats?.averageOrderValue
                  ? `€${stats.averageOrderValue.toFixed(2)}`
                  : "€0.00"
              }
              icon={<FaStar className="w-8 h-8" />}
              color="yellow"
              loading={loading}
            />
          </div>

          {/* Recent Activity */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">
                  Recent Products
                </h3>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => router.push("/seller/products/new")}
                >
                  <FaPlus className="w-4 h-4 mr-2" />
                  Add Product
                </Button>
              </div>
              {loading ? (
                <div className="space-y-3">
                  {[1, 2, 3].map((i) => (
                    <div key={i} className="animate-pulse">
                      <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                      <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                    </div>
                  ))}
                </div>
              ) : products.length > 0 ? (
                <div className="space-y-3">
                  {products.slice(0, 5).map((product) => (
                    <div
                      key={product.id}
                      className="flex items-center justify-between p-3 bg-gray-50 rounded-md"
                    >
                      <div className="flex items-center gap-3">
                        <Image
                          src={product.imageUrl || "/placeholder-product.jpg"}
                          alt={product.name}
                          width={40}
                          height={40}
                          className="w-10 h-10 rounded-md object-cover"
                        />
                        <div>
                          <p className="text-sm font-medium text-gray-900">
                            {product.name}
                          </p>
                          <p className="text-xs text-gray-500">
                            €{product.price} • Stock: {product.quantity}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center gap-1">
                        <button
                          className="text-gray-400 hover:text-blue-600 transition-colors"
                          onClick={() => router.push(`/product/${product.id}`)}
                        >
                          <FaEye className="w-4 h-4" />
                        </button>
                        <button
                          className="text-gray-400 hover:text-blue-600 transition-colors"
                          onClick={() =>
                            router.push(`/seller/products/${product.id}/edit`)
                          }
                        >
                          <FaEdit className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-4">
                  <FaBox className="w-12 h-12 text-gray-300 mx-auto mb-2" />
                  <p className="text-gray-500">No products yet</p>
                  <Button
                    className="mt-2"
                    onClick={() => router.push("/seller/products/new")}
                  >
                    Add Your First Product
                  </Button>
                </div>
              )}
            </div>

            <div className="bg-white rounded-lg shadow-sm border p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-medium text-gray-900">
                  Recent Orders
                </h3>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => router.push("/seller/orders")}
                >
                  View All
                </Button>
              </div>
              {loading ? (
                <div className="space-y-3">
                  {[1, 2, 3].map((i) => (
                    <div key={i} className="animate-pulse">
                      <div className="h-4 bg-gray-200 rounded w-2/3 mb-2"></div>
                      <div className="h-3 bg-gray-200 rounded w-1/3"></div>
                    </div>
                  ))}
                </div>
              ) : orders.length > 0 ? (
                <div className="space-y-3">
                  {orders.slice(0, 5).map((order) => (
                    <div
                      key={order.id}
                      className="flex items-center justify-between p-3 bg-gray-50 rounded-md"
                    >
                      <div>
                        <p className="text-sm font-medium text-gray-900">
                          Order #{order.orderNumber}
                        </p>
                        <p className="text-xs text-gray-500">
                          {order.customerName} • ${order.total} •{" "}
                          {order.itemCount} items
                        </p>
                      </div>
                      <span
                        className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          order.status === "PENDING"
                            ? "bg-yellow-100 text-yellow-800"
                            : order.status === "PROCESSING"
                            ? "bg-blue-100 text-blue-800"
                            : order.status === "SHIPPED"
                            ? "bg-green-100 text-green-800"
                            : order.status === "DELIVERED"
                            ? "bg-green-100 text-green-800"
                            : "bg-gray-100 text-gray-800"
                        }`}
                      >
                        {order.status}
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-4">
                  <FaShoppingCart className="w-12 h-12 text-gray-300 mx-auto mb-2" />
                  <p className="text-gray-500">No orders yet</p>
                </div>
              )}
            </div>
          </div>

          {/* Low Stock Alert */}
          {stats?.lowStockProducts && stats.lowStockProducts.length > 0 && (
            <div className="bg-white rounded-lg shadow-sm border p-6">
              <div className="flex items-center gap-2 mb-4">
                <FaExclamationTriangle className="w-5 h-5 text-amber-500" />
                <h3 className="text-lg font-medium text-gray-900">
                  Low Stock Alert
                </h3>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {stats.lowStockProducts.map((product) => (
                  <div
                    key={product.id}
                    className="p-4 bg-amber-50 border border-amber-200 rounded-lg"
                  >
                    <div className="flex items-center gap-3">
                      <Image
                        src={product.imageUrl || "/placeholder-product.jpg"}
                        alt={product.name}
                        width={40}
                        height={40}
                        className="w-10 h-10 rounded-md object-cover"
                      />
                      <div>
                        <p className="font-medium text-gray-900">
                          {product.name}
                        </p>
                        <p className="text-sm text-amber-700">
                          Only {product.quantity} left in stock
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Products Tab */}
      {activeTab === "products" && (
        <div className="bg-white rounded-lg shadow-sm border p-6">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-medium text-gray-900">
              Product Management
            </h3>
            <Button onClick={() => router.push("/seller/products/new")}>
              <FaPlus className="w-4 h-4 mr-2" />
              Add New Product
            </Button>
          </div>
          <div className="text-center py-12">
            <FaBox className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h4 className="text-lg font-medium text-gray-900 mb-2">
              Product Management
            </h4>
            <p className="text-gray-600 mb-4">
              This section will contain detailed product management
              functionality
            </p>
            <Button onClick={() => router.push("/seller/products")}>
              Go to Product Management
            </Button>
          </div>
        </div>
      )}

      {/* Orders Tab */}
      {activeTab === "orders" && (
        <div className="bg-white rounded-lg shadow-sm border p-6">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-medium text-gray-900">
              Order Management
            </h3>
            <div className="flex gap-2">
              <select className="px-3 py-2 border border-gray-300 rounded-md text-sm">
                <option value="all">All Orders</option>
                <option value="pending">Pending</option>
                <option value="processing">Processing</option>
                <option value="shipped">Shipped</option>
                <option value="delivered">Delivered</option>
              </select>
            </div>
          </div>
          <div className="text-center py-12">
            <FaShoppingCart className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h4 className="text-lg font-medium text-gray-900 mb-2">
              Order Management
            </h4>
            <p className="text-gray-600 mb-4">
              This section will contain detailed order management functionality
            </p>
            <Button onClick={() => router.push("/seller/orders")}>
              Go to Order Management
            </Button>
          </div>
        </div>
      )}

      {/* Analytics Tab */}
      {activeTab === "analytics" && (
        <div className="bg-white rounded-lg shadow-sm border p-6">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-medium text-gray-900">
              Sales Analytics
            </h3>
            <select className="px-3 py-2 border border-gray-300 rounded-md text-sm">
              <option value="7">Last 7 Days</option>
              <option value="30">Last 30 Days</option>
              <option value="90">Last 90 Days</option>
              <option value="365">This Year</option>
            </select>
          </div>
          <div className="text-center py-12">
            <FaChartLine className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h4 className="text-lg font-medium text-gray-900 mb-2">
              Analytics Dashboard
            </h4>
            <p className="text-gray-600 mb-4">
              This section will contain detailed sales analytics and reports
            </p>
            <Button onClick={() => router.push("/seller/analytics")}>
              Go to Analytics
            </Button>
          </div>
        </div>
      )}

      {/* Company Tab */}
      {activeTab === "company" && (
        <div className="space-y-6">
          {/* Company Information */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <div className="flex items-center gap-3 mb-6">
              <FaBuilding className="w-6 h-6 text-blue-600" />
              <h3 className="text-lg font-medium text-gray-900">
                Company Information
              </h3>
            </div>
            <div className="text-center py-8">
              <FaBuilding className="w-16 h-16 text-gray-300 mx-auto mb-4" />
              <h4 className="text-lg font-medium text-gray-900 mb-2">
                Company Profile
              </h4>
              <p className="text-gray-600 mb-4">
                Manage your company information and settings
              </p>
              <Button onClick={() => router.push("/seller/company/settings")}>
                Manage Company Profile
              </Button>
            </div>
          </div>

          {/* Company Addresses */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <div className="flex items-center gap-3 mb-6">
              <FaMapMarkerAlt className="w-6 h-6 text-green-600" />
              <h3 className="text-lg font-medium text-gray-900">
                Company Addresses
              </h3>
            </div>

            {/* Address Form */}
            {showAddressForm && (
              <div className="mb-6">
                <AddressForm
                  address={editingAddress || undefined}
                  addressType={selectedAddressType}
                  onSubmit={handleAddressFormSubmit}
                  onCancel={handleCancelAddressForm}
                  isEditing={!!editingAddress}
                />
              </div>
            )}

            {/* Address Type Selection */}
            {!showAddressForm && (
              <div className="mb-6">
                <div className="flex items-center gap-4 mb-4">
                  <label className="text-sm font-medium text-gray-700">
                    Address Type:
                  </label>
                  <select
                    value={selectedAddressType}
                    onChange={(e) =>
                      setSelectedAddressType(e.target.value as AddressType)
                    }
                    className="px-3 py-2 border border-gray-300 rounded-md text-sm"
                  >
                    <option value="EXPEDITION">Expedition Address</option>
                    <option value="BILLING">Billing Address</option>
                    <option value="SHIPPING">Shipping Address</option>
                  </select>
                  <Button onClick={handleAddNewAddress} size="sm">
                    <FaPlus className="w-4 h-4 mr-2" />
                    Add Address
                  </Button>
                </div>
              </div>
            )}

            {/* Address List */}
            <AddressList
              addresses={companyAddresses.filter(
                (addr) => addr.addressType === selectedAddressType
              )}
              addressType={selectedAddressType}
              onEdit={handleEditAddress}
              onDelete={handleDeleteAddress}
              onAddNew={handleAddNewAddress}
              isLoading={addressesLoading}
            />
          </div>
        </div>
      )}
    </SellerLayout>
  );
}

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: "blue" | "green" | "purple" | "yellow";
  loading: boolean;
}

function StatCard({ title, value, icon, color, loading }: StatCardProps) {
  const colorClasses = {
    blue: "bg-blue-500 text-white",
    green: "bg-green-500 text-white",
    purple: "bg-purple-500 text-white",
    yellow: "bg-yellow-500 text-white",
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-sm border p-6">
        <div className="animate-pulse">
          <div className="flex items-center justify-between mb-4">
            <div className="h-4 bg-gray-200 rounded w-1/2"></div>
            <div className="h-8 w-8 bg-gray-200 rounded"></div>
          </div>
          <div className="h-8 bg-gray-200 rounded w-1/3 mb-2"></div>
          <div className="h-3 bg-gray-200 rounded w-1/4"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-sm border p-6">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-medium text-gray-600">{title}</h3>
        <div className={`p-2 rounded-lg ${colorClasses[color]}`}>{icon}</div>
      </div>
      <div className="flex items-end justify-between">
        <div>
          <p className="text-2xl font-bold text-gray-900">{value}</p>
        </div>
      </div>
    </div>
  );
}
