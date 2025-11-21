"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  FaBuilding,
  FaUsers,
  FaBox,
  FaShoppingCart,
  FaDollarSign,
  FaChartLine,
  FaUserPlus,
  FaBoxOpen,
  FaExclamationTriangle,
  FaShieldAlt,
} from "react-icons/fa";
import CompanyAdminLayout from "../../../components/company/CompanyAdminLayout";
import { useAuth } from "../../context/AuthContext";

// Extended interface for company dashboard stats
interface CompanyDashboardStats {
  totalUsers?: number;
  totalProducts?: number;
  totalOrders?: number;
  totalRevenue?: number;
  activeUsers?: number;
  pendingOrders?: number;
  recentActivities?: Array<{
    description: string;
    timestamp: string;
    type: "user" | "product" | "order" | "system";
  }>;
  systemAlerts?: Array<{
    message: string;
    severity: "high" | "medium" | "low";
    timestamp: string;
  }>;
}

export default function CompanyAdminDashboard() {
  const router = useRouter();
  const { isSuperAdmin } = useAuth();
  const [stats, setStats] = useState<CompanyDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadDashboardStats();
  }, []);

  const loadDashboardStats = async () => {
    try {
      setLoading(true);
      // Mock data for now - in real implementation, this would come from API
      const mockStats: CompanyDashboardStats = {
        totalUsers: 25,
        totalProducts: 156,
        totalOrders: 89,
        totalRevenue: 45600,
        activeUsers: 18,
        pendingOrders: 12,
        recentActivities: [
          {
            description: "New user added to company",
            timestamp: "2 minutes ago",
            type: "user",
          },
          {
            description: "Product inventory updated",
            timestamp: "15 minutes ago",
            type: "product",
          },
          {
            description: "Order #1234 was placed",
            timestamp: "1 hour ago",
            type: "order",
          },
          {
            description: "Company settings updated",
            timestamp: "3 hours ago",
            type: "system",
          },
          {
            description: "User role permissions changed",
            timestamp: "5 hours ago",
            type: "user",
          },
        ],
        systemAlerts: [
          {
            message: "Low inventory for 3 company products",
            severity: "medium",
            timestamp: "Today, 09:45 AM",
          },
          {
            message: "2 pending user role requests",
            severity: "low",
            timestamp: "Today, 08:30 AM",
          },
          {
            message: "Company storage limit at 85%",
            severity: "high",
            timestamp: "Yesterday, 11:20 PM",
          },
        ],
      };
      setStats(mockStats);
    } catch (error) {
      console.error("Error loading dashboard stats:", error);
      setError("Failed to load dashboard statistics");
    } finally {
      setLoading(false);
    }
  };

  return (
    <CompanyAdminLayout
      title="Company Administration Dashboard"
      description="Manage your company's users, products, and operations"
    >
      <div className="space-y-6">
        {error && (
          <div className="p-4 bg-red-50 text-red-700 border border-red-200 rounded-md">
            {error}
          </div>
        )}

        {/* Access Level Indicator */}
        <div className="bg-gradient-to-r from-green-50 to-blue-50 border border-green-200 rounded-lg p-4">
          <div className="flex items-center gap-3">
            <FaShieldAlt className="w-6 h-6 text-green-600" />
            <div>
              <h3 className="font-medium text-gray-900">
                {isSuperAdmin ? "Super Admin Access" : "Company Admin Access"}
              </h3>
              <p className="text-sm text-gray-600">
                {isSuperAdmin
                  ? "You have platform-wide administrative access"
                  : "You have company-scoped administrative access"}
              </p>
            </div>
          </div>
        </div>

        {/* Primary Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard
            title="Company Users"
            value={stats?.totalUsers || 0}
            icon={<FaUsers className="w-8 h-8" />}
            color="blue"
            loading={loading}
          />
          <StatCard
            title="Company Products"
            value={stats?.totalProducts || 0}
            icon={<FaBox className="w-8 h-8" />}
            color="green"
            loading={loading}
          />
          <StatCard
            title="Total Orders"
            value={stats?.totalOrders || 0}
            icon={<FaShoppingCart className="w-8 h-8" />}
            color="purple"
            loading={loading}
          />
          <StatCard
            title="Company Revenue"
            value={
              stats?.totalRevenue
                ? `€${stats.totalRevenue.toLocaleString()}`
                : "€0"
            }
            icon={<FaDollarSign className="w-8 h-8" />}
            color="yellow"
            loading={loading}
          />
        </div>

        {/* Secondary Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard
            title="Active Users"
            value={stats?.activeUsers || 0}
            icon={<FaUsers className="w-6 h-6" />}
            color="green"
            loading={loading}
            small
          />
          <StatCard
            title="Pending Orders"
            value={stats?.pendingOrders || 0}
            icon={<FaShoppingCart className="w-6 h-6" />}
            color="orange"
            loading={loading}
            small
          />
          <StatCard
            title="System Alerts"
            value={stats?.systemAlerts?.length || 0}
            icon={<FaExclamationTriangle className="w-6 h-6" />}
            color="red"
            loading={loading}
            small
          />
          <StatCard
            title="Admin Level"
            value={isSuperAdmin ? "Super" : "Company"}
            icon={<FaShieldAlt className="w-6 h-6" />}
            color="indigo"
            loading={loading}
            small
          />
        </div>

        {/* Recent Activity */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Recent Company Activity
            </h3>
            {loading ? (
              <div className="space-y-3">
                {[1, 2, 3, 4, 5].map((i) => (
                  <div key={i} className="animate-pulse">
                    <div className="h-4 bg-gray-200 rounded w-3/4 mb-2"></div>
                    <div className="h-3 bg-gray-200 rounded w-1/2"></div>
                  </div>
                ))}
              </div>
            ) : stats?.recentActivities ? (
              <div className="space-y-3">
                {stats.recentActivities.map(
                  (
                    activity: {
                      description: string;
                      timestamp: string;
                      type: string;
                    },
                    index: number
                  ) => (
                    <div
                      key={index}
                      className="flex items-start gap-3 p-3 bg-gray-50 rounded-md"
                    >
                      <div
                        className={`w-2 h-2 rounded-full mt-2 ${
                          activity.type === "user"
                            ? "bg-blue-500"
                            : activity.type === "product"
                            ? "bg-green-500"
                            : activity.type === "order"
                            ? "bg-purple-500"
                            : "bg-gray-500"
                        }`}
                      ></div>
                      <div>
                        <p className="text-sm text-gray-900">
                          {activity.description}
                        </p>
                        <p className="text-xs text-gray-500">
                          {activity.timestamp}
                        </p>
                      </div>
                    </div>
                  )
                )}
              </div>
            ) : (
              <p className="text-gray-500">No recent activity</p>
            )}
          </div>

          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Company Alerts
            </h3>
            {loading ? (
              <div className="space-y-3">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="animate-pulse">
                    <div className="h-4 bg-gray-200 rounded w-2/3 mb-2"></div>
                    <div className="h-3 bg-gray-200 rounded w-1/3"></div>
                  </div>
                ))}
              </div>
            ) : stats?.systemAlerts ? (
              <div className="space-y-3">
                {stats.systemAlerts.map(
                  (
                    alert: {
                      severity: string;
                      message: string;
                      timestamp: string;
                    },
                    index: number
                  ) => (
                    <div
                      key={index}
                      className={`flex items-start gap-3 p-3 rounded-md ${
                        alert.severity === "high"
                          ? "bg-red-50 border border-red-200"
                          : alert.severity === "medium"
                          ? "bg-yellow-50 border border-yellow-200"
                          : "bg-blue-50 border border-blue-200"
                      }`}
                    >
                      <FaExclamationTriangle
                        className={`w-4 h-4 mt-0.5 ${
                          alert.severity === "high"
                            ? "text-red-500"
                            : alert.severity === "medium"
                            ? "text-yellow-500"
                            : "text-blue-500"
                        }`}
                      />
                      <div>
                        <p className="text-sm font-medium text-gray-900">
                          {alert.message}
                        </p>
                        <p className="text-xs text-gray-500">
                          {alert.timestamp}
                        </p>
                      </div>
                    </div>
                  )
                )}
              </div>
            ) : (
              <div className="text-center py-4">
                <p className="text-gray-500">No company alerts</p>
              </div>
            )}
          </div>
        </div>

        {/* Quick Actions */}
        <div className="bg-white rounded-lg shadow-sm border p-6">
          <h3 className="text-lg font-medium text-gray-900 mb-6">
            Quick Actions
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <QuickActionButton
              title="Manage Company Users"
              description="Add, edit, or remove company users"
              icon={<FaUserPlus className="w-6 h-6" />}
              onClick={() => router.push("/seller/company/users")}
              color="blue"
            />
            <QuickActionButton
              title="Manage Company Products"
              description="Add, edit, or delete company products"
              icon={<FaBoxOpen className="w-6 h-6" />}
              onClick={() => router.push("/seller/company/products")}
              color="green"
            />
            <QuickActionButton
              title="View Company Orders"
              description="Monitor and manage company orders"
              icon={<FaShoppingCart className="w-6 h-6" />}
              onClick={() => router.push("/seller/company/orders")}
              color="purple"
            />
            <QuickActionButton
              title="Role Management"
              description="Manage company admin roles and permissions"
              icon={<FaShieldAlt className="w-6 h-6" />}
              onClick={() => router.push("/seller/roles")}
              color="indigo"
            />
            <QuickActionButton
              title="Company Analytics"
              description="View detailed company analytics"
              icon={<FaChartLine className="w-6 h-6" />}
              onClick={() => router.push("/seller/company/analytics")}
              color="yellow"
            />
            <QuickActionButton
              title="Company Settings"
              description="Configure company settings"
              icon={<FaBuilding className="w-6 h-6" />}
              onClick={() => router.push("/seller/company/settings")}
              color="gray"
            />
          </div>
        </div>
      </div>
    </CompanyAdminLayout>
  );
}

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: "blue" | "green" | "purple" | "yellow" | "red" | "orange" | "indigo";
  loading: boolean;
  small?: boolean;
}

function StatCard({
  title,
  value,
  icon,
  color,
  loading,
  small = false,
}: StatCardProps) {
  const colorClasses = {
    blue: "bg-blue-500 text-white",
    green: "bg-green-500 text-white",
    purple: "bg-purple-500 text-white",
    yellow: "bg-yellow-500 text-white",
    red: "bg-red-500 text-white",
    orange: "bg-orange-500 text-white",
    indigo: "bg-indigo-500 text-white",
  };

  if (loading) {
    return (
      <div
        className={`bg-white rounded-lg shadow-sm border ${
          small ? "p-4" : "p-6"
        }`}
      >
        <div className="animate-pulse">
          <div className="flex items-center justify-between mb-4">
            <div className="h-4 bg-gray-200 rounded w-1/2"></div>
            <div
              className={`${small ? "h-6 w-6" : "h-8 w-8"} bg-gray-200 rounded`}
            ></div>
          </div>
          <div
            className={`${
              small ? "h-6" : "h-8"
            } bg-gray-200 rounded w-1/3 mb-2`}
          ></div>
          <div className="h-3 bg-gray-200 rounded w-1/4"></div>
        </div>
      </div>
    );
  }

  return (
    <div
      className={`bg-white rounded-lg shadow-sm border ${
        small ? "p-4" : "p-6"
      }`}
    >
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-medium text-gray-600">{title}</h3>
        <div
          className={`${small ? "p-1.5" : "p-2"} rounded-lg ${
            colorClasses[color]
          }`}
        >
          {icon}
        </div>
      </div>
      <div className="flex items-end justify-between">
        <div>
          <p
            className={`${
              small ? "text-xl" : "text-2xl"
            } font-bold text-gray-900`}
          >
            {value}
          </p>
        </div>
      </div>
    </div>
  );
}

interface QuickActionButtonProps {
  title: string;
  description: string;
  icon: React.ReactNode;
  onClick: () => void;
  color: "blue" | "green" | "purple" | "yellow" | "indigo" | "gray" | "orange";
}

function QuickActionButton({
  title,
  description,
  icon,
  onClick,
  color,
}: QuickActionButtonProps) {
  const colorClasses = {
    blue: "text-blue-600 bg-blue-50 hover:bg-blue-100",
    green: "text-green-600 bg-green-50 hover:bg-green-100",
    purple: "text-purple-600 bg-purple-50 hover:bg-purple-100",
    yellow: "text-yellow-600 bg-yellow-50 hover:bg-yellow-100",
    indigo: "text-indigo-600 bg-indigo-50 hover:bg-indigo-100",
    gray: "text-gray-600 bg-gray-50 hover:bg-gray-100",
    orange: "text-orange-600 bg-orange-50 hover:bg-orange-100",
  };

  return (
    <button
      onClick={onClick}
      className={`p-4 rounded-lg border border-gray-200 transition-colors text-left ${colorClasses[color]}`}
    >
      <div className="flex items-center gap-3 mb-2">
        {icon}
        <h4 className="font-medium">{title}</h4>
      </div>
      <p className="text-sm text-gray-600">{description}</p>
    </button>
  );
}
