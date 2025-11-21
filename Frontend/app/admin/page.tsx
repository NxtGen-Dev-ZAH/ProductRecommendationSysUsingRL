"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  FaUsers,
  FaBox,
  FaShoppingCart,
  FaDollarSign,
  FaChartLine,
  FaUserPlus,
  FaBoxOpen,
  FaShoppingBag,
  FaExclamationTriangle,
  FaFolder,
  FaUserTie,
} from "react-icons/fa";
import AdminLayout from "../../components/admin/AdminLayout";
import {
  getAdminDashboardStats,
  AdminDashboardStats,
} from "../api/services/admin";

// Extended interface to include additional properties needed for the dashboard
interface ExtendedDashboardStats extends AdminDashboardStats {
  userGrowth?: number;
  productGrowth?: number;
  orderGrowth?: number;
  revenueGrowth?: number;
  activeProducts?: number;
  blockedUsers?: number;
  recentActivities?: Array<{
    description: string;
    timestamp: string;
  }>;
  systemAlerts?: Array<{
    message: string;
    severity: "high" | "medium" | "low";
    timestamp: string;
  }>;
}

export default function AdminDashboard() {
  const router = useRouter();
  const [stats, setStats] = useState<ExtendedDashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadDashboardStats();
  }, []);

  const loadDashboardStats = async () => {
    try {
      setLoading(true);
      const dashboardStats = await getAdminDashboardStats();

      // Extend with mock data for properties not provided by the API
      const extendedStats: ExtendedDashboardStats = {
        ...dashboardStats,
        userGrowth: 12.5,
        productGrowth: 8.3,
        orderGrowth: 15.2,
        revenueGrowth: 18.7,
        totalCategories: 89,
        activeProducts: 523,
        blockedUsers: 12,
        pendingOrders: 45,
        recentActivities: [
          { description: "New user registered", timestamp: "2 minutes ago" },
          {
            description: "Order #1234 was placed",
            timestamp: "15 minutes ago",
          },
          { description: "Product inventory updated", timestamp: "1 hour ago" },
          {
            description: "New seller application received",
            timestamp: "3 hours ago",
          },
          { description: "System backup completed", timestamp: "5 hours ago" },
        ],
        systemAlerts: [
          {
            message: "Low inventory for 5 products",
            severity: "medium",
            timestamp: "Today, 09:45 AM",
          },
          {
            message: "Payment gateway maintenance scheduled",
            severity: "low",
            timestamp: "Today, 08:30 AM",
          },
          {
            message: "Server load above 80%",
            severity: "high",
            timestamp: "Yesterday, 11:20 PM",
          },
        ],
      };
      setStats(extendedStats);
    } catch (error) {
      console.error("Error loading dashboard stats:", error);
      setError("Failed to load dashboard statistics");
    } finally {
      setLoading(false);
    }
  };

  return (
    <AdminLayout
      activeNav="dashboard"
      title="Admin Dashboard"
      description="Overview of your platform's performance"
    >
      <div className="space-y-6">
        {error && (
          <div className="p-4 bg-red-50 text-red-700 border border-red-200 rounded-md">
            {error}
          </div>
        )}

        {/* Primary Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard
            title="Total Users"
            value={stats?.totalUsers || 0}
            icon={<FaUsers className="w-8 h-8" />}
            color="blue"
            trend={stats?.userGrowth}
            loading={loading}
          />
          <StatCard
            title="Total Products"
            value={stats?.totalProducts || 0}
            icon={<FaBox className="w-8 h-8" />}
            color="green"
            trend={stats?.productGrowth}
            loading={loading}
          />
          <StatCard
            title="Total Categories"
            value={stats?.totalCategories || 0}
            icon={<FaFolder className="w-8 h-8" />}
            color="purple"
            trend={5.2}
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
            color="yellow"
            trend={stats?.revenueGrowth}
            loading={loading}
          />
        </div>

        {/* Secondary Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <StatCard
            title="Active Products"
            value={stats?.activeProducts || 0}
            icon={<FaBox className="w-6 h-6" />}
            color="green"
            loading={loading}
            small
          />
          <StatCard
            title="Blocked Users"
            value={stats?.blockedUsers || 0}
            icon={<FaUsers className="w-6 h-6" />}
            color="red"
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
            title="Role Requests"
            value="8"
            icon={<FaUserTie className="w-6 h-6" />}
            color="indigo"
            loading={loading}
            small
          />
        </div>

        {/* Recent Activity */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Recent Activity
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
                    activity: { description: string; timestamp: string },
                    index: number
                  ) => (
                    <div
                      key={index}
                      className="flex items-start gap-3 p-3 bg-gray-50 rounded-md"
                    >
                      <div className="w-2 h-2 bg-blue-500 rounded-full mt-2"></div>
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
              System Alerts
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
                <p className="text-gray-500">No system alerts</p>
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
              title="Manage Users"
              description="Add, edit, or delete users"
              icon={<FaUserPlus className="w-6 h-6" />}
              onClick={() => router.push("/admin/users")}
              color="blue"
            />
            <QuickActionButton
              title="Manage Products"
              description="Add, edit, or delete products"
              icon={<FaBoxOpen className="w-6 h-6" />}
              onClick={() => router.push("/admin/products")}
              color="green"
            />
            <QuickActionButton
              title="Manage Categories"
              description="Organize product categories"
              icon={<FaFolder className="w-6 h-6" />}
              onClick={() => router.push("/admin/categories")}
              color="purple"
            />
            <QuickActionButton
              title="View Orders"
              description="Monitor and manage orders"
              icon={<FaShoppingBag className="w-6 h-6" />}
              onClick={() => router.push("/admin/orders")}
              color="orange"
            />
            <QuickActionButton
              title="Role Management"
              description="Manage user roles and permissions"
              icon={<FaUserTie className="w-6 h-6" />}
              onClick={() => router.push("/admin/roles")}
              color="indigo"
            />
            <QuickActionButton
              title="Analytics"
              description="View detailed analytics"
              icon={<FaChartLine className="w-6 h-6" />}
              onClick={() => router.push("/admin/analytics")}
              color="yellow"
            />
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}

interface StatCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  color: "blue" | "green" | "purple" | "yellow" | "red" | "orange" | "indigo";
  trend?: number;
  loading: boolean;
  small?: boolean;
}

function StatCard({
  title,
  value,
  icon,
  color,
  trend,
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
          {trend !== undefined && (
            <p
              className={`text-sm ${
                trend >= 0 ? "text-green-600" : "text-red-600"
              }`}
            >
              {trend >= 0 ? "+" : ""}
              {trend}% from last month
            </p>
          )}
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
