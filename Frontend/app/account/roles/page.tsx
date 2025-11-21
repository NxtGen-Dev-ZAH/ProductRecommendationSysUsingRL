"use client";

import { useAuth } from "../../context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import {
  FaShieldAlt,
  FaStore,
  FaBuilding,
  FaUserTie,
  FaPlus,
  FaClock,
  FaCheck,
  FaTimes,
} from "react-icons/fa";
import AccountNav from "../../../components/account/AccountNav";
import { Button } from "../../../components/ui/button";
import {
  // becomeIndividualSeller,
  requestCompanyAdminRole,
  getMyAdminRequests,
  getUserCompanies,
  PendingAdminRequest,
  Company,
} from "../../api/services/roleManagement";

// interface RoleRequest {
//   id: string;
//   type: "seller" | "company_admin";
//   status: "pending" | "approved" | "rejected";
//   requestedAt: string;
//   companyName?: string;
// }

export default function RolesPage() {
  const { user, isAuthenticated, isSeller, isAdmin, becomeSellerRequest, loading: authLoading } =
    useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error" | "info";
    text: string;
  } | null>(null);
  const [pendingRequests, setPendingRequests] = useState<PendingAdminRequest[]>(
    []
  );
  const [userCompanies, setUserCompanies] = useState<Company[]>([]);
  const [loadingData, setLoadingData] = useState(true);

  useEffect(() => {
    // Wait for auth to finish loading before checking authentication
    if (authLoading) {
      return;
    }

    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    loadRoleData();
  }, [isAuthenticated, authLoading, router]);

  const loadRoleData = async () => {
    try {
      setLoadingData(true);
      const [requests, companies] = await Promise.all([
        getMyAdminRequests(),
        getUserCompanies().catch(() => []), // May fail if user is not a seller
      ]);

      setPendingRequests(requests);
      setUserCompanies(companies);
    } catch (error) {
      console.error("Error loading role data:", error);
    } finally {
      setLoadingData(false);
    }
  };

  const handleBecomeSellerRequest = async () => {
    try {
      setLoading(true);
      setMessage(null);

      await becomeSellerRequest();
      setMessage({
        type: "success",
        text: "Seller role activated successfully! You can now start selling products.",
      });
    } catch (error: unknown) {
      console.error("Error requesting seller role:", error);
      const errorMessage =
        error instanceof Error && "response" in error
          ? (error as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to request seller role";
      setMessage({
        type: "error",
        text: errorMessage || "Failed to request seller role",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleRequestCompanyAdmin = async () => {
    try {
      setLoading(true);
      setMessage(null);

      const result = await requestCompanyAdminRole();
      setMessage({
        type: "success",
        text: result.message,
      });

      // Reload pending requests
      await loadRoleData();
    } catch (error: unknown) {
      console.error("Error requesting company admin role:", error);
      const errorMessage =
        error instanceof Error && "response" in error
          ? (error as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to request company admin role";
      setMessage({
        type: "error",
        text: errorMessage || "Failed to request company admin role",
      });
    } finally {
      setLoading(false);
    }
  };

  // Show loading state while auth is being checked
  if (authLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  // Show loading state if not authenticated
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  if (loadingData) {
    return (
      <div className="bg-gray-50 min-h-screen">
        <div className="container mx-auto px-4 py-8">
          <div className="max-w-6xl mx-auto">
            <div className="flex items-center justify-center h-64">
              <div className="text-gray-600">Loading role data...</div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-gray-50 min-h-screen">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-6xl mx-auto">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900">
              Role Management
            </h1>
            <p className="text-gray-600">
              Manage your account roles and permissions
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="roles" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3 space-y-6">
              {/* Message Display */}
              {message && (
                <div
                  className={`p-4 rounded-md ${
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

              {/* Current Roles */}
              <div className="bg-white rounded-lg shadow-sm border p-6">
                <h3 className="text-lg font-medium text-gray-900 mb-6">
                  Current Roles
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  {/* Buyer Role */}
                  <div className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-3">
                      <FaShieldAlt className="w-8 h-8 text-blue-600" />
                      <span className="px-2 py-1 text-xs bg-green-100 text-green-800 rounded-full">
                        Active
                      </span>
                    </div>
                    <h4 className="font-medium text-gray-900">Buyer</h4>
                    <p className="text-sm text-gray-600">
                      Basic shopping and account access
                    </p>
                  </div>

                  {/* Seller Role */}
                  <div className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-3">
                      <FaStore className="w-8 h-8 text-green-600" />
                      <span
                        className={`px-2 py-1 text-xs rounded-full ${
                          isSeller
                            ? "bg-green-100 text-green-800"
                            : "bg-gray-100 text-gray-600"
                        }`}
                      >
                        {isSeller ? "Active" : "Inactive"}
                      </span>
                    </div>
                    <h4 className="font-medium text-gray-900">Seller</h4>
                    <p className="text-sm text-gray-600">
                      Sell products and manage inventory
                    </p>
                  </div>

                  {/* Admin Role */}
                  <div className="border border-gray-200 rounded-lg p-4">
                    <div className="flex items-center justify-between mb-3">
                      <FaUserTie className="w-8 h-8 text-purple-600" />
                      <span
                        className={`px-2 py-1 text-xs rounded-full ${
                          isAdmin
                            ? "bg-purple-100 text-purple-800"
                            : "bg-gray-100 text-gray-600"
                        }`}
                      >
                        {isAdmin ? "Active" : "Inactive"}
                      </span>
                    </div>
                    <h4 className="font-medium text-gray-900">Admin</h4>
                    <p className="text-sm text-gray-600">
                      Platform administration access
                    </p>
                  </div>
                </div>
              </div>

              {/* Role Actions */}
              <div className="bg-white rounded-lg shadow-sm border p-6">
                <h3 className="text-lg font-medium text-gray-900 mb-6">
                  Available Actions
                </h3>
                <div className="space-y-4">
                  {/* Become Seller */}
                  {!isSeller && (
                    <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                      <div className="flex items-center gap-4">
                        <FaStore className="w-8 h-8 text-green-600" />
                        <div>
                          <h4 className="font-medium text-gray-900">
                            Become a Seller
                          </h4>
                          <p className="text-sm text-gray-600">
                            Start selling products on our platform
                          </p>
                        </div>
                      </div>
                      <Button
                        onClick={handleBecomeSellerRequest}
                        loading={loading}
                        disabled={loading}
                      >
                        <FaPlus className="w-4 h-4 mr-2" />
                        Request Seller Role
                      </Button>
                    </div>
                  )}

                  {/* Request Company Admin */}
                  {isSeller && (
                    <div className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                      <div className="flex items-center gap-4">
                        <FaBuilding className="w-8 h-8 text-orange-600" />
                        <div>
                          <h4 className="font-medium text-gray-900">
                            Company Admin
                          </h4>
                          <p className="text-sm text-gray-600">
                            Manage company sellers and operations
                          </p>
                        </div>
                      </div>
                      <Button
                        onClick={handleRequestCompanyAdmin}
                        loading={loading}
                        disabled={loading}
                        variant="outline"
                      >
                        <FaPlus className="w-4 h-4 mr-2" />
                        Request Admin Role
                      </Button>
                    </div>
                  )}

                  {/* Seller Onboarding Link */}
                  {!isSeller && (
                    <div className="flex items-center justify-between p-4 border border-blue-200 rounded-lg bg-blue-50">
                      <div className="flex items-center gap-4">
                        <FaStore className="w-8 h-8 text-blue-600" />
                        <div>
                          <h4 className="font-medium text-gray-900">
                            Complete Seller Setup
                          </h4>
                          <p className="text-sm text-gray-600">
                            Complete your seller profile with business
                            information
                          </p>
                        </div>
                      </div>
                      <Button
                        onClick={() => router.push("/become-seller")}
                        variant="outline"
                      >
                        Start Setup
                      </Button>
                    </div>
                  )}
                </div>
              </div>

              {/* Pending Requests */}
              {pendingRequests.length > 0 && (
                <div className="bg-white rounded-lg shadow-sm border p-6">
                  <h3 className="text-lg font-medium text-gray-900 mb-6">
                    Pending Requests ({pendingRequests.length})
                  </h3>
                  <div className="space-y-4">
                    {pendingRequests.map((request) => (
                      <div
                        key={request.id}
                        className="flex items-center justify-between p-4 border border-gray-200 rounded-lg"
                      >
                        <div className="flex items-center gap-4">
                          <div className="flex items-center justify-center w-10 h-10 rounded-full bg-gray-100">
                            {request.status === "pending" && (
                              <FaClock className="w-5 h-5 text-yellow-600" />
                            )}
                            {request.status === "approved" && (
                              <FaCheck className="w-5 h-5 text-green-600" />
                            )}
                            {request.status === "denied" && (
                              <FaTimes className="w-5 h-5 text-red-600" />
                            )}
                          </div>
                          <div>
                            <h4 className="font-medium text-gray-900">
                              Company Admin Request
                              {request.companyName &&
                                ` - ${request.companyName}`}
                            </h4>
                            <div className="flex items-center gap-4 text-sm text-gray-600">
                              <span>
                                Requested:{" "}
                                {new Date(
                                  request.requestedAt
                                ).toLocaleDateString()}
                              </span>
                              <span
                                className={`px-2 py-1 text-xs rounded-full ${
                                  request.status === "pending"
                                    ? "bg-yellow-100 text-yellow-800"
                                    : request.status === "approved"
                                    ? "bg-green-100 text-green-800"
                                    : "bg-red-100 text-red-800"
                                }`}
                              >
                                {request.status.charAt(0).toUpperCase() +
                                  request.status.slice(1)}
                              </span>
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Company Memberships */}
              {userCompanies.length > 0 && (
                <div className="bg-white rounded-lg shadow-sm border p-6">
                  <h3 className="text-lg font-medium text-gray-900 mb-6">
                    Company Memberships ({userCompanies.length})
                  </h3>
                  <div className="space-y-4">
                    {userCompanies.map((company) => (
                      <div
                        key={company.id}
                        className="flex items-center justify-between p-4 border border-gray-200 rounded-lg"
                      >
                        <div className="flex items-center gap-4">
                          <FaBuilding className="w-8 h-8 text-gray-600" />
                          <div>
                            <h4 className="font-medium text-gray-900">
                              {company.name}
                            </h4>
                            <div className="flex items-center gap-4 text-sm text-gray-600">
                              <span>
                                {company.admins.some(
                                  (admin) => admin.email === user?.email
                                )
                                  ? "Admin"
                                  : "Seller"}
                              </span>
                              <span
                                className={`px-2 py-1 text-xs rounded-full ${
                                  company.isActive
                                    ? "bg-green-100 text-green-800"
                                    : "bg-gray-100 text-gray-600"
                                }`}
                              >
                                {company.isActive ? "Active" : "Inactive"}
                              </span>
                            </div>
                          </div>
                        </div>
                        <Button
                          onClick={() =>
                            router.push(`/seller/company/${company.id}`)
                          }
                          variant="outline"
                          size="sm"
                        >
                          Manage
                        </Button>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
