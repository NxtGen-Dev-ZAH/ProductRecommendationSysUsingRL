"use client";

import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import type {
  Address,
  AddressType,
  UserAddressRequest,
  CompanyAddressRequest,
} from "@/types/api";
import { formatAddress } from "@/app/api/services";
import AddressForm from "./AddressForm";

interface AddressSelectorProps {
  addresses: Address[];
  addressType: AddressType;
  selectedAddressId?: number;
  onSelect: (address: Address) => void;
  onAddNew: (
    addressData: UserAddressRequest | CompanyAddressRequest
  ) => Promise<void>;
  isLoading?: boolean;
  showAddNew?: boolean;
  title?: string;
  description?: string;
}

const AddressSelector: React.FC<AddressSelectorProps> = ({
  addresses,
  addressType,
  selectedAddressId,
  onSelect,
  onAddNew,
  isLoading = false,
  showAddNew = true,
  title,
  description,
}) => {
  const [showForm, setShowForm] = useState(false);
  const [editingAddress, setEditingAddress] = useState<Address | null>(null);

  const getAddressTypeLabel = (type: AddressType) => {
    switch (type) {
      case "BILLING":
        return "Billing Address";
      case "SHIPPING":
        return "Shipping Address";
      case "EXPEDITION":
        return "Expedition Address";
      case "CONTACT":
        return "Contact Address";
      default:
        return "Address";
    }
  };

  const getAddressTypeColor = (type: AddressType) => {
    switch (type) {
      case "BILLING":
        return "border-blue-200 bg-blue-50";
      case "SHIPPING":
        return "border-green-200 bg-green-50";
      case "EXPEDITION":
        return "border-purple-200 bg-purple-50";
      case "CONTACT":
        return "border-orange-200 bg-orange-50";
      default:
        return "border-gray-200 bg-gray-50";
    }
  };

  const handleFormSubmit = async (
    addressData: UserAddressRequest | CompanyAddressRequest
  ) => {
    try {
      if (editingAddress) {
        // Handle edit - this would need to be passed as a prop or handled differently
        console.log("Edit address:", editingAddress.id, addressData);
      } else {
        await onAddNew(addressData);
      }
      setShowForm(false);
      setEditingAddress(null);
    } catch (error) {
      console.error("Failed to save address:", error);
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingAddress(null);
  };

  if (showForm) {
    return (
      <div className="space-y-4">
        <AddressForm
          address={editingAddress || undefined}
          addressType={addressType}
          onSubmit={handleFormSubmit}
          onCancel={handleCancel}
          isEditing={!!editingAddress}
        />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900">
          {title || `Select ${getAddressTypeLabel(addressType)}`}
        </h3>
        {description && (
          <p className="text-sm text-gray-600 mt-1">{description}</p>
        )}
      </div>

      {/* Loading State */}
      {isLoading && (
        <div className="space-y-3">
          {[1, 2].map((i) => (
            <Card key={i} className="p-4">
              <div className="animate-pulse">
                <div className="h-4 bg-gray-200 rounded w-1/4 mb-2"></div>
                <div className="h-3 bg-gray-200 rounded w-3/4 mb-2"></div>
                <div className="h-3 bg-gray-200 rounded w-1/2"></div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Address List */}
      {!isLoading && (
        <div className="space-y-3">
          {addresses.length === 0 ? (
            <Card className="p-6 text-center">
              <div className="text-gray-500 mb-4">
                <svg
                  className="mx-auto h-12 w-12 text-gray-400"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                  />
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                  />
                </svg>
              </div>
              <h4 className="text-lg font-medium text-gray-900 mb-2">
                No {getAddressTypeLabel(addressType).toLowerCase()}s available
              </h4>
              <p className="text-gray-600 mb-4">
                You need to add a{" "}
                {getAddressTypeLabel(addressType).toLowerCase()} to continue.
              </p>
              {showAddNew && (
                <Button
                  onClick={() => setShowForm(true)}
                  className="bg-blue-600 hover:bg-blue-700"
                >
                  Add {getAddressTypeLabel(addressType)}
                </Button>
              )}
            </Card>
          ) : (
            <>
              {addresses.map((address) => (
                <Card
                  key={address.id}
                  className={`p-4 cursor-pointer transition-all duration-200 ${
                    selectedAddressId === address.id
                      ? `ring-2 ring-blue-500 ${getAddressTypeColor(
                          addressType
                        )}`
                      : "hover:shadow-md border-gray-200"
                  }`}
                  onClick={() => onSelect(address)}
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      {/* Address Header */}
                      <div className="flex items-center space-x-2 mb-2">
                        <span
                          className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${
                            address.addressType === "BILLING"
                              ? "bg-blue-100 text-blue-800"
                              : address.addressType === "SHIPPING"
                              ? "bg-green-100 text-green-800"
                              : "bg-purple-100 text-purple-800"
                          }`}
                        >
                          {address.addressType}
                        </span>
                        {address.isDefault && (
                          <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                            Default
                          </span>
                        )}
                      </div>

                      {/* Address Details */}
                      <div className="space-y-1">
                        <h4 className="font-medium text-gray-900">
                          {address.name}
                        </h4>
                        {address.email && (
                          <p className="text-sm text-gray-600">
                            {address.email}
                          </p>
                        )}
                        {address.phoneNumber && (
                          <p className="text-sm text-gray-600">
                            {address.phoneNumber}
                          </p>
                        )}
                        <p className="text-sm text-gray-700">
                          {formatAddress(address)}
                        </p>
                        {address.reference && (
                          <p className="text-sm text-gray-500 italic">
                            Note: {address.reference}
                          </p>
                        )}
                      </div>
                    </div>

                    {/* Selection Indicator */}
                    <div className="ml-4 flex items-center">
                      {selectedAddressId === address.id ? (
                        <div className="w-5 h-5 bg-blue-600 rounded-full flex items-center justify-center">
                          <svg
                            className="w-3 h-3 text-white"
                            fill="currentColor"
                            viewBox="0 0 20 20"
                          >
                            <path
                              fillRule="evenodd"
                              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                              clipRule="evenodd"
                            />
                          </svg>
                        </div>
                      ) : (
                        <div className="w-5 h-5 border-2 border-gray-300 rounded-full"></div>
                      )}
                    </div>
                  </div>
                </Card>
              ))}

              {/* Add New Address Button */}
              {showAddNew && (
                <Card className="p-4 border-dashed border-2 border-gray-300 hover:border-gray-400 transition-colors">
                  <button
                    onClick={() => setShowForm(true)}
                    className="w-full text-center py-4 text-gray-600 hover:text-gray-800 transition-colors"
                  >
                    <div className="flex items-center justify-center space-x-2">
                      <svg
                        className="w-5 h-5"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M12 6v6m0 0v6m0-6h6m-6 0H6"
                        />
                      </svg>
                      <span className="font-medium">
                        Add New {getAddressTypeLabel(addressType)}
                      </span>
                    </div>
                  </button>
                </Card>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default AddressSelector;
