"use client";

import { useEffect, useState, useCallback } from "react";
import AdminLayout from "../../../components/admin/AdminLayout";
import {
  FaUsers,
  FaSearch,
  FaFilter,
  FaEye,
  FaBan,
  FaUnlock,
  FaUserPlus,
  FaUserMinus,
  FaDownload,
} from "react-icons/fa";
import Image from "next/image";
import Link from "next/link";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import {
  getAllUsers,
  blockUser,
  unblockUser,
  deleteUser,
  restoreUser,
  addUserRole,
  removeUserRole,
  performBulkUserAction,
  exportUserData,
  AdminUser,
  AdminUserFilters,
} from "../../api/services/admin";

type SortField = "name" | "email" | "registrationDate" | "lastLoginDate";
type SortDirection = "asc" | "desc";

export default function AdminUsersPage() {

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [pageSize] = useState(10);

  const [filters, setFilters] = useState<AdminUserFilters>({});
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedRole, setSelectedRole] = useState<string>("");
  const [selectedStatus, setSelectedStatus] = useState<string>("");

  const [selectedUsers, setSelectedUsers] = useState<Set<number>>(new Set());
  const [actionLoading, setActionLoading] = useState<Set<number>>(new Set());
  const [bulkActionLoading, setBulkActionLoading] = useState(false);

  const [showFilters, setShowFilters] = useState(false);
  const [sortField] = useState<SortField>("registrationDate");
  const [sortDirection] = useState<SortDirection>("desc");

  const [message, setMessage] = useState<{
    type: "success" | "error" | "info";
    text: string;
  } | null>(null);

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getAllUsers(currentPage, pageSize, {
        ...filters,
        search: searchQuery || undefined,
        role: selectedRole || undefined,
        status:
          (selectedStatus as "active" | "inactive" | "blocked") || undefined,
      });

      setUsers(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (error) {
      console.error("Error loading users:", error);
      setMessage({ type: "error", text: "Failed to load users" });
    } finally {
      setLoading(false);
    }
  }, [
    currentPage,
    pageSize,
    filters,
    searchQuery,
    selectedRole,
    selectedStatus,
  ]);

  useEffect(() => {
    loadUsers();
  }, [
    currentPage,
    filters,
    sortField,
    sortDirection,
    loadUsers,
  ]);

  const handleSearch = () => {
    setCurrentPage(0);
    setFilters((prev) => ({ ...prev, search: searchQuery }));
  };

  const handleFilterChange = () => {
    setCurrentPage(0);
    setFilters({
      search: searchQuery || undefined,
      role: selectedRole || undefined,
      status:
        (selectedStatus as "active" | "inactive" | "blocked") || undefined,
    });
  };

  const clearFilters = () => {
    setSearchQuery("");
    setSelectedRole("");
    setSelectedStatus("");
    setFilters({});
    setCurrentPage(0);
  };

  const handleUserAction = async (
    userId: number,
    action: string,
    role?: string
  ) => {
    try {
      setActionLoading((prev) => new Set([...prev, userId]));

      let message = "";
      switch (action) {
        case "block":
          await blockUser(userId);
          message = "User blocked successfully";
          break;
        case "unblock":
          await unblockUser(userId);
          message = "User unblocked successfully";
          break;
        case "delete":
          if (
            confirm(
              "Are you sure you want to delete this user? This action cannot be undone."
            )
          ) {
            await deleteUser(userId);
            message = "User deleted successfully";
          } else {
            return;
          }
          break;
        case "restore":
          await restoreUser(userId);
          message = "User restored successfully";
          break;
        case "add_role":
          if (role) {
            await addUserRole(userId, role);
            message = `${role} role added successfully`;
          }
          break;
        case "remove_role":
          if (role) {
            await removeUserRole(userId, role);
            message = `${role} role removed successfully`;
          }
          break;
      }

      setMessage({ type: "success", text: message });
      await loadUsers(); // Reload to show updated data
    } catch (error: unknown) {
      console.error(`Error performing ${action}:`, error);
      setMessage({
        type: "error",
        text:
          (error as { response?: { data?: { message?: string } } })?.response
            ?.data?.message || `Failed to ${action} user`,
      });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(userId);
        return newSet;
      });
    }
  };

  const handleBulkAction = async (action: string, role?: string) => {
    if (selectedUsers.size === 0) {
      setMessage({ type: "error", text: "Please select users first" });
      return;
    }

    const confirmMessage = `Are you sure you want to ${action} ${selectedUsers.size} selected user(s)?`;
    if (action === "delete" && !confirm(confirmMessage)) {
      return;
    }

    try {
      setBulkActionLoading(true);
      const result = await performBulkUserAction({
        userIds: Array.from(selectedUsers),
        action: action as
          | "block"
          | "unblock"
          | "delete"
          | "restore"
          | "add_role"
          | "remove_role",
        role,
      });

      setMessage({
        type: "success",
        text: `${result.successCount} user(s) ${action}ed successfully`,
      });
      setSelectedUsers(new Set());
      await loadUsers();
    } catch (error: unknown) {
      console.error("Error performing bulk action:", error);
      setMessage({
        type: "error",
        text:
          (error as { response?: { data?: { message?: string } } })?.response
            ?.data?.message || "Failed to perform bulk action",
      });
    } finally {
      setBulkActionLoading(false);
    }
  };

  const handleExportUsers = async () => {
    try {
      const result = await exportUserData(filters, "csv");
      window.open(result.downloadUrl, "_blank");
    } catch (error) {
      console.error("Error exporting users:", error);
      setMessage({ type: "error", text: "Failed to export users" });
    }
  };

  const handleSelectUser = (userId: number) => {
    const newSelected = new Set(selectedUsers);
    if (newSelected.has(userId)) {
      newSelected.delete(userId);
    } else {
      newSelected.add(userId);
    }
    setSelectedUsers(newSelected);
  };

  const handleSelectAll = () => {
    if (selectedUsers.size === users.length) {
      setSelectedUsers(new Set());
    } else {
      setSelectedUsers(new Set(users.map((user) => user.id)));
    }
  };

  return (
    <AdminLayout
      activeNav="users"
      title="User Management"
      description={`Manage users, roles, and permissions (${totalElements} total users)`}
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
                placeholder="Search users by name or email..."
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
            <Button onClick={handleExportUsers} variant="outline" size="sm">
              <FaDownload className="w-4 h-4 mr-2" />
              Export
            </Button>
          </div>
        </div>

        {/* Filter Panel */}
        {showFilters && (
          <div className="mt-4 pt-4 border-t border-gray-200">
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Role
                </label>
                <select
                  value={selectedRole}
                  onChange={(e) => setSelectedRole(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">All Roles</option>
                  <option value="BUYER">Buyer</option>
                  <option value="SELLER">Seller</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Status
                </label>
                <select
                  value={selectedStatus}
                  onChange={(e) => setSelectedStatus(e.target.value)}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">All Status</option>
                  <option value="active">Active</option>
                  <option value="inactive">Inactive</option>
                  <option value="blocked">Blocked</option>
                </select>
              </div>
              <div className="flex items-end gap-2">
                <Button onClick={handleFilterChange} size="sm">
                  Apply Filters
                </Button>
                <Button onClick={clearFilters} variant="outline" size="sm">
                  Clear
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Bulk Actions */}
      {selectedUsers.size > 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <div className="flex items-center justify-between">
            <span className="text-sm text-blue-800">
              {selectedUsers.size} user(s) selected
            </span>
            <div className="flex gap-2">
              <Button
                onClick={() => handleBulkAction("block")}
                loading={bulkActionLoading}
                size="sm"
                variant="outline"
                className="border-red-300 text-red-600 hover:bg-red-50"
              >
                <FaBan className="w-4 h-4 mr-2" />
                Block
              </Button>
              <Button
                onClick={() => handleBulkAction("unblock")}
                loading={bulkActionLoading}
                size="sm"
                variant="outline"
              >
                <FaUnlock className="w-4 h-4 mr-2" />
                Unblock
              </Button>
              <Button
                onClick={() => handleBulkAction("add_role", "SELLER")}
                loading={bulkActionLoading}
                size="sm"
                variant="outline"
              >
                <FaUserPlus className="w-4 h-4 mr-2" />
                Add Seller
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Users Table */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      <input
                        type="checkbox"
                        checked={
                          selectedUsers.size === users.length &&
                          users.length > 0
                        }
                        onChange={handleSelectAll}
                        className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                      />
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      User
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Roles
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Registered
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Last Login
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Actions
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {loading ? (
                    [...Array(5)].map((_, index) => (
                      <tr key={index} className="animate-pulse">
                        <td className="px-6 py-4">
                          <div className="h-4 bg-gray-200 rounded w-4"></div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="h-4 bg-gray-200 rounded w-32"></div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="h-4 bg-gray-200 rounded w-24"></div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="h-4 bg-gray-200 rounded w-20"></div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="h-4 bg-gray-200 rounded w-28"></div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="h-4 bg-gray-200 rounded w-28"></div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="h-4 bg-gray-200 rounded w-32"></div>
                        </td>
                      </tr>
                    ))
                  ) : users.length > 0 ? (
                    users.map((user) => (
                      <tr key={user.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4">
                          <input
                            type="checkbox"
                            checked={selectedUsers.has(user.id)}
                            onChange={() => handleSelectUser(user.id)}
                            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                          />
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center">
                            <div className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden flex-shrink-0">
                              {user.profilePictureUrl ? (
                                <Image
                                  src={user.profilePictureUrl}
                                  alt={`${user.firstName} ${user.lastName}`}
                                  width={40}
                                  height={40}
                                  className="w-full h-full object-cover"
                                />
                              ) : (
                                <span className="text-sm font-medium text-gray-600">
                                  {user.firstName.charAt(0)}
                                  {user.lastName.charAt(0)}
                                </span>
                              )}
                            </div>
                            <div className="ml-4">
                              <div className="font-medium text-gray-900">
                                {user.firstName} {user.lastName}
                              </div>
                              <div className="text-sm text-gray-500">
                                {user.email}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex flex-wrap gap-1">
                            {user.roles && user.roles.length > 0 ? (
                              user.roles.map((role) => (
                                <span
                                  key={role}
                                  className={`px-2 py-1 text-xs rounded-full ${
                                    role === "ADMIN" || role === "APP_ADMIN" || role === "ROLE_APP_ADMIN"
                                      ? "bg-purple-100 text-purple-800"
                                      : role === "SELLER" || role === "ROLE_SELLER"
                                      ? "bg-green-100 text-green-800"
                                      : "bg-blue-100 text-blue-800"
                                  }`}
                                >
                                  {role}
                                </span>
                              ))
                            ) : (
                              <span className="px-2 py-1 text-xs rounded-full bg-gray-100 text-gray-800">
                                No roles
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex flex-col gap-1">
                            <span
                              className={`px-2 py-1 text-xs rounded-full ${
                                user.isBlocked
                                  ? "bg-red-100 text-red-800"
                                  : user.isActivated
                                  ? "bg-green-100 text-green-800"
                                  : "bg-yellow-100 text-yellow-800"
                              }`}
                            >
                              {user.isBlocked
                                ? "Blocked"
                                : user.isActivated
                                ? "Active"
                                : "Pending"}
                            </span>
                            {!user.emailVerified && (
                              <span className="px-2 py-1 text-xs bg-orange-100 text-orange-800 rounded-full">
                                Email not verified
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-900">
                          {new Date(user.registrationDate).toLocaleDateString()}
                        </td>
                        <td className="px-6 py-4 text-sm text-gray-900">
                          {user.lastLoginDate
                            ? new Date(user.lastLoginDate).toLocaleDateString()
                            : "Never"}
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex gap-2">
                            <Link
                              href={`/admin/users/${user.id}`}
                              className="text-blue-600 hover:text-blue-900"
                              title="View Details"
                            >
                              <FaEye className="w-4 h-4" />
                            </Link>
                            <button
                              onClick={() =>
                                handleUserAction(
                                  user.id,
                                  user.isBlocked ? "unblock" : "block"
                                )
                              }
                              disabled={actionLoading.has(user.id)}
                              className={`${
                                user.isBlocked
                                  ? "text-green-600 hover:text-green-900"
                                  : "text-red-600 hover:text-red-900"
                              }`}
                              title={
                                user.isBlocked ? "Unblock User" : "Block User"
                              }
                            >
                              {user.isBlocked ? (
                                <FaUnlock className="w-4 h-4" />
                              ) : (
                                <FaBan className="w-4 h-4" />
                              )}
                            </button>
                            {user.roles && (user.roles.includes("SELLER") || user.roles.includes("ROLE_SELLER")) ? (
                              <button
                                onClick={() =>
                                  handleUserAction(
                                    user.id,
                                    "remove_role",
                                    "SELLER"
                                  )
                                }
                                disabled={actionLoading.has(user.id)}
                                className="text-orange-600 hover:text-orange-900"
                                title="Remove Seller Role"
                              >
                                <FaUserMinus className="w-4 h-4" />
                              </button>
                            ) : (
                              <button
                                onClick={() =>
                                  handleUserAction(
                                    user.id,
                                    "add_role",
                                    "SELLER"
                                  )
                                }
                                disabled={actionLoading.has(user.id)}
                                className="text-green-600 hover:text-green-900"
                                title="Add Seller Role"
                              >
                                <FaUserPlus className="w-4 h-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan={7} className="px-6 py-12 text-center">
                        <FaUsers className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                        <h3 className="text-lg font-medium text-gray-900 mb-2">
                          No users found
                        </h3>
                        <p className="text-gray-600">
                          Try adjusting your search or filter criteria
                        </p>
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="px-6 py-3 border-t border-gray-200">
              <div className="flex items-center justify-between">
                <div className="text-sm text-gray-700">
                  Showing {currentPage * pageSize + 1} to{" "}
                  {Math.min((currentPage + 1) * pageSize, totalElements)} of{" "}
                  {totalElements} users
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
                      setCurrentPage((prev) =>
                        Math.min(totalPages - 1, prev + 1)
                      )
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
    </AdminLayout>
  );
}
