"use client";

import { useAuth } from "../../context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import AccountNav from "../../../components/account/AccountNav";
import { AddressList, AddressForm } from "../../../components/address";
import {
  getUserAddresses,
  addUserAddress,
  updateUserAddress,
  deleteUserAddress,
  type UserAddressRequest,
  type UserAddressResponse,
  type AddressType,
} from "../../api/services";

export default function AddressesPage() {
  const { user, isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const [addresses, setAddresses] = useState<UserAddressResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingAddress, setEditingAddress] =
    useState<UserAddressResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const [selectedAddressType, setSelectedAddressType] =
    useState<AddressType>("SHIPPING");

  useEffect(() => {
    // Wait for auth to finish loading before checking authentication
    if (authLoading) {
      return;
    }

    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    loadAddresses();
  }, [isAuthenticated, authLoading, router]);

  const loadAddresses = async () => {
    try {
      setLoading(true);
      const data = await getUserAddresses();
      setAddresses(data);
      if ((data?.length || 0) === 0) {
        setMessage({ type: "success", text: "You have no addresses yet. Please add one below." });
        setShowForm(true);
      } else {
        setMessage(null);
      }
    } catch (error) {
      console.error("Error loading addresses:", error);
      setMessage({ type: "error", text: "Failed to load addresses" });
    } finally {
      setLoading(false);
    }
  };

  const handleAddNew = () => {
    setEditingAddress(null);
    setShowForm(true);
    setMessage(null);
  };

  const handleEdit = (address: UserAddressResponse) => {
    setEditingAddress(address);
    setSelectedAddressType(address.addressType);
    setShowForm(true);
    setMessage(null);
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingAddress(null);
    setMessage(null);
  };

  const handleFormSubmit = async (addressData: UserAddressRequest) => {
    if (!user?.id) return;

    try {
      setSubmitting(true);
      setMessage(null);

      if (editingAddress) {
        await updateUserAddress(editingAddress.id, addressData);
        setMessage({ type: "success", text: "Address updated successfully!" });
      } else {
        await addUserAddress(addressData);
        setMessage({ type: "success", text: "Address added successfully!" });
      }

      await loadAddresses();
      setShowForm(false);
      setEditingAddress(null);
    } catch (error) {
      console.error("Error saving address:", error);
      setMessage({
        type: "error",
        text: "Failed to save address. Please try again.",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (addressId: number) => {
    if (!user?.id) return;

    if (!confirm("Are you sure you want to delete this address?")) return;

    try {
      await deleteUserAddress(addressId);
      await loadAddresses();
      setMessage({ type: "success", text: "Address deleted successfully!" });
    } catch (error) {
      console.error("Error deleting address:", error);
      setMessage({
        type: "error",
        text: "Failed to delete address. Please try again.",
      });
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
  if (!isAuthenticated || !user?.id) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  // Filter addresses by type for display
  const shippingAddresses = addresses.filter(
    (addr) => addr.addressType === "SHIPPING"
  );
  const billingAddresses = addresses.filter(
    (addr) => addr.addressType === "BILLING"
  );

  return (
    <div className="bg-gray-50 min-h-screen">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-6xl mx-auto">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900">
              Address Management
            </h1>
            <p className="text-gray-600">
              Manage your shipping and billing addresses
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="addresses" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3 space-y-8">
              {/* Message Display */}
              {message && (
                <div
                  className={`p-4 rounded-md ${
                    message.type === "success"
                      ? "bg-green-50 text-green-700 border border-green-200"
                      : "bg-red-50 text-red-700 border border-red-200"
                  }`}
                >
                  {message.text}
                </div>
              )}

              {/* Address Form */}
              {showForm && (
                <AddressForm
                  address={editingAddress || undefined}
                  addressType={selectedAddressType}
                  onSubmit={handleFormSubmit}
                  onCancel={handleCancel}
                  isLoading={submitting}
                  isEditing={!!editingAddress}
                />
              )}

              {/* Shipping Addresses */}
              <AddressList
                addresses={shippingAddresses}
                addressType="SHIPPING"
                onEdit={handleEdit}
                onDelete={handleDelete}
                onAddNew={() => {
                  setSelectedAddressType("SHIPPING");
                  handleAddNew();
                }}
                isLoading={loading}
              />

              {/* Billing Addresses */}
              <AddressList
                addresses={billingAddresses}
                addressType="BILLING"
                onEdit={handleEdit}
                onDelete={handleDelete}
                onAddNew={() => {
                  setSelectedAddressType("BILLING");
                  handleAddNew();
                }}
                isLoading={loading}
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
