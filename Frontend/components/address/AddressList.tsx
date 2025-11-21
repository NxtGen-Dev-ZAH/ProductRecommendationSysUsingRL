"use client";

import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import type { Address, AddressType } from "@/types/api";
import { formatAddress } from "@/app/api/services";

interface AddressListProps {
  addresses: Address[];
  addressType: AddressType;
  onEdit: (address: Address) => void;
  onDelete: (addressId: number) => void;
  onAddNew: () => void;
  onSelect?: (address: Address) => void;
  showActions?: boolean;
  showSelectButton?: boolean;
  selectedAddressId?: number;
  isLoading?: boolean;
}

const AddressList: React.FC<AddressListProps> = ({
  addresses,
  addressType,
  onEdit,
  onDelete,
  onAddNew,
  onSelect,
  showActions = true,
  showSelectButton = false,
  selectedAddressId,
  isLoading = false,
}) => {
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const handleDelete = async (addressId: number) => {
    if (window.confirm("Are you sure you want to delete this address?")) {
      setDeletingId(addressId);
      try {
        await onDelete(addressId);
      } finally {
        setDeletingId(null);
      }
    }
  };

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
        return "bg-blue-100 text-blue-800";
      case "SHIPPING":
        return "bg-green-100 text-green-800";
      case "EXPEDITION":
        return "bg-purple-100 text-purple-800";
      case "CONTACT":
        return "bg-orange-100 text-orange-800";
      default:
        return "bg-gray-100 text-gray-800";
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-4">
        {[1, 2, 3].map((i) => (
          <Card key={i} className="p-6">
            <div className="animate-pulse">
              <div className="h-4 bg-gray-200 rounded w-1/4 mb-2"></div>
              <div className="h-3 bg-gray-200 rounded w-3/4 mb-2"></div>
              <div className="h-3 bg-gray-200 rounded w-1/2"></div>
            </div>
          </Card>
        ))}
      </div>
    );
  }

  if (addresses.length === 0) {
    return (
      <Card className="p-8 text-center">
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
        <h3 className="text-lg font-medium text-gray-900 mb-2">
          No {getAddressTypeLabel(addressType).toLowerCase()}s found
        </h3>
        <p className="text-gray-600 mb-4">
          You haven&apos;t added any {getAddressTypeLabel(addressType).toLowerCase()}
          s yet.
        </p>
        {showActions && (
          <Button onClick={onAddNew} className="bg-blue-600 hover:bg-blue-700">
            Add {getAddressTypeLabel(addressType)}
          </Button>
        )}
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">
            {getAddressTypeLabel(addressType)}s ({addresses.length})
          </h3>
          <p className="text-sm text-gray-600">
            Manage your {getAddressTypeLabel(addressType).toLowerCase()}s
          </p>
        </div>
        {showActions && (
          <Button onClick={onAddNew} className="bg-blue-600 hover:bg-blue-700">
            Add New
          </Button>
        )}
      </div>

      {/* Address List */}
      <div className="space-y-4">
        {addresses.map((address) => (
          <Card
            key={address.id}
            className={`p-6 transition-all duration-200 ${
              selectedAddressId === address.id
                ? "ring-2 ring-blue-500 bg-blue-50"
                : "hover:shadow-md"
            }`}
          >
            <div className="flex justify-between items-start">
              <div className="flex-1">
                {/* Address Header */}
                <div className="flex items-center space-x-3 mb-3">
                  <span
                    className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getAddressTypeColor(
                      address.addressType
                    )}`}
                  >
                    {address.addressType}
                  </span>
                  {address.isDefault && (
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                      Default
                    </span>
                  )}
                </div>

                {/* Address Details */}
                <div className="space-y-1">
                  <h4 className="font-medium text-gray-900">{address.name}</h4>
                  {address.email && (
                    <p className="text-sm text-gray-600">{address.email}</p>
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

              {/* Actions */}
              <div className="flex flex-col space-y-2 ml-4">
                {showSelectButton && onSelect && (
                  <Button
                    size="sm"
                    variant={
                      selectedAddressId === address.id ? "default" : "outline"
                    }
                    onClick={() => onSelect(address)}
                    className="w-full"
                  >
                    {selectedAddressId === address.id ? "Selected" : "Select"}
                  </Button>
                )}

                {showActions && (
                  <div className="flex space-x-2">
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => onEdit(address)}
                    >
                      Edit
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleDelete(address.id)}
                      disabled={deletingId === address.id}
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    >
                      {deletingId === address.id ? "Deleting..." : "Delete"}
                    </Button>
                  </div>
                )}
              </div>
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
};

export default AddressList;

