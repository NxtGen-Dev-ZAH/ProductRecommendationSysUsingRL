"use client";

import React, { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import type {
  Address,
  UserAddressRequest,
  CompanyAddressRequest,
  AddressType,
} from "@/types/api";
import { validateAddress } from "@/app/api/services";

interface AddressFormProps {
  address?: Address;
  addressType: AddressType;
  onSubmit: (
    addressData: UserAddressRequest | CompanyAddressRequest
  ) => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
  isEditing?: boolean;
}

const AddressForm: React.FC<AddressFormProps> = ({
  address,
  addressType,
  onSubmit,
  onCancel,
  isLoading = false,
  isEditing = false,
}) => {
  const [formData, setFormData] = useState({
    name: address?.name || "",
    email: address?.email || "",
    addressLine1: address?.addressLine1 || "",
    addressLine2: address?.addressLine2 || "",
    city: address?.city || "",
    state: address?.state || "",
    postalCode: address?.postalCode || "",
    country: address?.country || "",
    phoneNumber: address?.phoneNumber || "",
    reference: address?.reference || "",
    isDefault: address?.isDefault || false,
  });

  const [errors, setErrors] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (address) {
      setFormData({
        name: address.name || "",
        email: address.email || "",
        addressLine1: address.addressLine1 || "",
        addressLine2: address.addressLine2 || "",
        city: address.city || "",
        state: address.state || "",
        postalCode: address.postalCode || "",
        country: address.country || "",
        phoneNumber: address.phoneNumber || "",
        reference: address.reference || "",
        isDefault: address.isDefault || false,
      });
    }
  }, [address]);

  const handleInputChange = (field: string, value: string | boolean) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));

    // Clear errors when user starts typing
    if (errors.length > 0) {
      setErrors([]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const addressData = {
      ...formData,
      addressType,
    };

    // Validate the form data
    const validation = validateAddress(addressData);
    if (validation.errors.length > 0) {
      setErrors(validation.errors);
      return;
    }

    setIsSubmitting(true);
    try {
      await onSubmit(addressData);
    } catch (error) {
      console.error("Failed to submit address:", error);
      setErrors(["Failed to save address. Please try again."]);
    } finally {
      setIsSubmitting(false);
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

  return (
    <Card className="p-6">
      <div className="mb-6">
        <h3 className="text-lg font-semibold text-gray-900">
          {isEditing ? "Edit" : "Add"} {getAddressTypeLabel(addressType)}
        </h3>
        <p className="text-sm text-gray-600 mt-1">
          {isEditing
            ? "Update your address information below."
            : "Please provide your address information below."}
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Error Messages */}
        {errors.length > 0 && (
          <div className="bg-red-50 border border-red-200 rounded-md p-4">
            <div className="flex">
              <div className="ml-3">
                <h3 className="text-sm font-medium text-red-800">
                  Please correct the following errors:
                </h3>
                <div className="mt-2 text-sm text-red-700">
                  <ul className="list-disc pl-5 space-y-1">
                    {errors.map((error, index) => (
                      <li key={index}>{error}</li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Name */}
        <div>
          <label
            htmlFor="name"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Full Name *
          </label>
          <Input
            id="name"
            type="text"
            value={formData.name}
            onChange={(e) => handleInputChange("name", e.target.value)}
            placeholder="Enter full name"
            required
            className="w-full"
          />
        </div>

        {/* Email */}
        <div>
          <label
            htmlFor="email"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Email Address
          </label>
          <Input
            id="email"
            type="email"
            value={formData.email}
            onChange={(e) => handleInputChange("email", e.target.value)}
            placeholder="Enter email address"
            className="w-full"
          />
        </div>

        {/* Address Line 1 */}
        <div>
          <label
            htmlFor="addressLine1"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Address Line 1 *
          </label>
          <Input
            id="addressLine1"
            type="text"
            value={formData.addressLine1}
            onChange={(e) => handleInputChange("addressLine1", e.target.value)}
            placeholder="Street address, P.O. box, company name"
            required
            className="w-full"
          />
        </div>

        {/* Address Line 2 */}
        <div>
          <label
            htmlFor="addressLine2"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Address Line 2
          </label>
          <Input
            id="addressLine2"
            type="text"
            value={formData.addressLine2}
            onChange={(e) => handleInputChange("addressLine2", e.target.value)}
            placeholder="Apartment, suite, unit, building, floor, etc."
            className="w-full"
          />
        </div>

        {/* City and State */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label
              htmlFor="city"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              City *
            </label>
            <Input
              id="city"
              type="text"
              value={formData.city}
              onChange={(e) => handleInputChange("city", e.target.value)}
              placeholder="Enter city"
              required
              className="w-full"
            />
          </div>
          <div>
            <label
              htmlFor="state"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              State/Province
            </label>
            <Input
              id="state"
              type="text"
              value={formData.state}
              onChange={(e) => handleInputChange("state", e.target.value)}
              placeholder="Enter state or province"
              className="w-full"
            />
          </div>
        </div>

        {/* Postal Code and Country */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label
              htmlFor="postalCode"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Postal Code *
            </label>
            <Input
              id="postalCode"
              type="text"
              value={formData.postalCode}
              onChange={(e) => handleInputChange("postalCode", e.target.value)}
              placeholder="Enter postal code"
              required
              className="w-full"
            />
          </div>
          <div>
            <label
              htmlFor="country"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Country *
            </label>
            <Input
              id="country"
              type="text"
              value={formData.country}
              onChange={(e) => handleInputChange("country", e.target.value)}
              placeholder="Enter country"
              required
              className="w-full"
            />
          </div>
        </div>

        {/* Phone Number */}
        <div>
          <label
            htmlFor="phoneNumber"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Phone Number
          </label>
          <div className="flex">
            <select
              value={formData.country}
              onChange={(e) => {
                handleInputChange("country", e.target.value);
                // Auto-add country code to phone if empty
                if (!formData.phoneNumber && e.target.value) {
                  const countryCodes: { [key: string]: string } = {
                    'France': '+33',
                    'United States': '+1',
                    'United Kingdom': '+44',
                    'Germany': '+49',
                    'Spain': '+34',
                    'Italy': '+39',
                    'Canada': '+1',
                    'Australia': '+61',
                    'Japan': '+81',
                    'China': '+86',
                    'India': '+91',
                    'Brazil': '+55',
                    'Mexico': '+52',
                    'Russia': '+7',
                    'South Korea': '+82',
                    'Netherlands': '+31',
                    'Belgium': '+32',
                    'Switzerland': '+41',
                    'Austria': '+43',
                    'Sweden': '+46',
                    'Norway': '+47',
                    'Denmark': '+45',
                    'Finland': '+358',
                    'Poland': '+48',
                    'Czech Republic': '+420',
                    'Hungary': '+36',
                    'Portugal': '+351',
                    'Greece': '+30',
                    'Turkey': '+90',
                    'Israel': '+972',
                    'South Africa': '+27',
                    'Egypt': '+20',
                    'Nigeria': '+234',
                    'Kenya': '+254',
                    'Morocco': '+212',
                    'Algeria': '+213',
                    'Tunisia': '+216',
                    'Saudi Arabia': '+966',
                    'UAE': '+971',
                    'Qatar': '+974',
                    'Kuwait': '+965',
                    'Bahrain': '+973',
                    'Oman': '+968',
                    'Jordan': '+962',
                    'Lebanon': '+961',
                    'Syria': '+963',
                    'Iraq': '+964',
                    'Iran': '+98',
                    'Pakistan': '+92',
                    'Bangladesh': '+880',
                    'Sri Lanka': '+94',
                    'Nepal': '+977',
                    'Bhutan': '+975',
                    'Myanmar': '+95',
                    'Thailand': '+66',
                    'Vietnam': '+84',
                    'Cambodia': '+855',
                    'Laos': '+856',
                    'Malaysia': '+60',
                    'Singapore': '+65',
                    'Indonesia': '+62',
                    'Philippines': '+63',
                    'Brunei': '+673',
                    'Timor-Leste': '+670',
                    'Papua New Guinea': '+675',
                    'Fiji': '+679',
                    'New Zealand': '+64',
                    'Argentina': '+54',
                    'Chile': '+56',
                    'Colombia': '+57',
                    'Peru': '+51',
                    'Venezuela': '+58',
                    'Ecuador': '+593',
                    'Bolivia': '+591',
                    'Paraguay': '+595',
                    'Uruguay': '+598',
                    'Guyana': '+592',
                    'Suriname': '+597',
                    'French Guiana': '+594',
                  };
                  const code = countryCodes[e.target.value];
                  if (code) {
                    handleInputChange("phoneNumber", code + " ");
                  }
                }
              }}
              className="w-32 px-3 py-2 border border-gray-300 rounded-l-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 bg-white"
            >
              <option value="">Select Country</option>
              <option value="France">🇫🇷 France</option>
              <option value="United States">🇺🇸 United States</option>
              <option value="United Kingdom">🇬🇧 United Kingdom</option>
              <option value="Germany">🇩🇪 Germany</option>
              <option value="Spain">🇪🇸 Spain</option>
              <option value="Italy">🇮🇹 Italy</option>
              <option value="Canada">🇨🇦 Canada</option>
              <option value="Australia">🇦🇺 Australia</option>
              <option value="Japan">🇯🇵 Japan</option>
              <option value="China">🇨🇳 China</option>
              <option value="India">🇮🇳 India</option>
              <option value="Brazil">🇧🇷 Brazil</option>
              <option value="Mexico">🇲🇽 Mexico</option>
              <option value="Russia">🇷🇺 Russia</option>
              <option value="South Korea">🇰🇷 South Korea</option>
              <option value="Netherlands">🇳🇱 Netherlands</option>
              <option value="Belgium">🇧🇪 Belgium</option>
              <option value="Switzerland">🇨🇭 Switzerland</option>
              <option value="Austria">🇦🇹 Austria</option>
              <option value="Sweden">🇸🇪 Sweden</option>
              <option value="Norway">🇳🇴 Norway</option>
              <option value="Denmark">🇩🇰 Denmark</option>
              <option value="Finland">🇫🇮 Finland</option>
              <option value="Poland">🇵🇱 Poland</option>
              <option value="Czech Republic">🇨🇿 Czech Republic</option>
              <option value="Hungary">🇭🇺 Hungary</option>
              <option value="Portugal">🇵🇹 Portugal</option>
              <option value="Greece">🇬🇷 Greece</option>
              <option value="Turkey">🇹🇷 Turkey</option>
              <option value="Israel">🇮🇱 Israel</option>
              <option value="South Africa">🇿🇦 South Africa</option>
              <option value="Egypt">🇪🇬 Egypt</option>
              <option value="Nigeria">🇳🇬 Nigeria</option>
              <option value="Kenya">🇰🇪 Kenya</option>
              <option value="Morocco">🇲🇦 Morocco</option>
              <option value="Algeria">🇩🇿 Algeria</option>
              <option value="Tunisia">🇹🇳 Tunisia</option>
              <option value="Saudi Arabia">🇸🇦 Saudi Arabia</option>
              <option value="UAE">🇦🇪 UAE</option>
              <option value="Qatar">🇶🇦 Qatar</option>
              <option value="Kuwait">🇰🇼 Kuwait</option>
              <option value="Bahrain">🇧🇭 Bahrain</option>
              <option value="Oman">🇴🇲 Oman</option>
              <option value="Jordan">🇯🇴 Jordan</option>
              <option value="Lebanon">🇱🇧 Lebanon</option>
              <option value="Syria">🇸🇾 Syria</option>
              <option value="Iraq">🇮🇶 Iraq</option>
              <option value="Iran">🇮🇷 Iran</option>
              <option value="Pakistan">🇵🇰 Pakistan</option>
              <option value="Bangladesh">🇧🇩 Bangladesh</option>
              <option value="Sri Lanka">🇱🇰 Sri Lanka</option>
              <option value="Nepal">🇳🇵 Nepal</option>
              <option value="Bhutan">🇧🇹 Bhutan</option>
              <option value="Myanmar">🇲🇲 Myanmar</option>
              <option value="Thailand">🇹🇭 Thailand</option>
              <option value="Vietnam">🇻🇳 Vietnam</option>
              <option value="Cambodia">🇰🇭 Cambodia</option>
              <option value="Laos">🇱🇦 Laos</option>
              <option value="Malaysia">🇲🇾 Malaysia</option>
              <option value="Singapore">🇸🇬 Singapore</option>
              <option value="Indonesia">🇮🇩 Indonesia</option>
              <option value="Philippines">🇵🇭 Philippines</option>
              <option value="Brunei">🇧🇳 Brunei</option>
              <option value="Timor-Leste">🇹🇱 Timor-Leste</option>
              <option value="Papua New Guinea">🇵🇬 Papua New Guinea</option>
              <option value="Fiji">🇫🇯 Fiji</option>
              <option value="New Zealand">🇳🇿 New Zealand</option>
              <option value="Argentina">🇦🇷 Argentina</option>
              <option value="Chile">🇨🇱 Chile</option>
              <option value="Colombia">🇨🇴 Colombia</option>
              <option value="Peru">🇵🇪 Peru</option>
              <option value="Venezuela">🇻🇪 Venezuela</option>
              <option value="Ecuador">🇪🇨 Ecuador</option>
              <option value="Bolivia">🇧🇴 Bolivia</option>
              <option value="Paraguay">🇵🇾 Paraguay</option>
              <option value="Uruguay">🇺🇾 Uruguay</option>
              <option value="Guyana">🇬🇾 Guyana</option>
              <option value="Suriname">🇸🇷 Suriname</option>
              <option value="French Guiana">🇬🇫 French Guiana</option>
            </select>
            <Input
              id="phoneNumber"
              type="tel"
              value={formData.phoneNumber}
              onChange={(e) => handleInputChange("phoneNumber", e.target.value)}
              placeholder="+33 1 23 45 67 89"
              className="flex-1 rounded-l-none"
            />
          </div>
          <p className="mt-1 text-xs text-gray-500">
            Use international format with country code (e.g., +33 1 23 45 67 89)
          </p>
        </div>

        {/* Reference/Notes */}
        <div>
          <label
            htmlFor="reference"
            className="block text-sm font-medium text-gray-700 mb-1"
          >
            Delivery Instructions / Notes
          </label>
          <textarea
            id="reference"
            value={formData.reference}
            onChange={(e) => handleInputChange("reference", e.target.value)}
            placeholder="Any special delivery instructions or notes"
            rows={3}
            className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
          />
        </div>

        {/* Default Address Checkbox */}
        <div className="flex items-center">
          <input
            id="isDefault"
            type="checkbox"
            checked={formData.isDefault}
            onChange={(e) => handleInputChange("isDefault", e.target.checked)}
            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
          />
          <label
            htmlFor="isDefault"
            className="ml-2 block text-sm text-gray-700"
          >
            Set as default {addressType.toLowerCase()} address
          </label>
        </div>

        {/* Form Actions */}
        <div className="flex justify-end space-x-3 pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={isSubmitting || isLoading}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            disabled={isSubmitting || isLoading}
            className="bg-blue-600 hover:bg-blue-700"
          >
            {isSubmitting
              ? "Saving..."
              : isEditing
              ? "Update Address"
              : "Add Address"}
          </Button>
        </div>
      </form>
    </Card>
  );
};

export default AddressForm;
