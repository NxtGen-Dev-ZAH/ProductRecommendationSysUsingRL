"use client";

import { useState, useEffect } from "react";
import {
  getSellerCoupons,
  createSellerCoupon,
  updateSellerCoupon,
  deleteSellerCoupon,
  toggleCouponStatus,
  validateCouponData,
  formatCouponDisplay,
  getCouponStatusText,
  getCouponStatusColor,
} from "../../api/services/sellerCoupon";
import { SellerCouponResponse } from "../../../types/api";
import { Button } from "../../../components/ui/button";
import { Card } from "../../../components/ui/card";
import { Input } from "../../../components/ui/input";
import { useToast } from "../../../components/ui/toast";

interface CouponFormData {
  code: string;
  description: string;
  discountType: "PERCENTAGE" | "FIXED_AMOUNT";
  discountValue: number;
  minimumOrderAmount?: number;
  maxDiscountAmount?: number;
  startDate: string;
  endDate: string;
  usageLimit?: number;
  applicableCategories?: number[];
  applicableProducts?: number[];
}

export default function SellerCouponsPage() {
  const [coupons, setCoupons] = useState<SellerCouponResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingCoupon, setEditingCoupon] =
    useState<SellerCouponResponse | null>(null);
  const [formData, setFormData] = useState<CouponFormData>({
    code: "",
    description: "",
    discountType: "PERCENTAGE",
    discountValue: 0,
    startDate: new Date().toISOString().split("T")[0],
    endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
      .toISOString()
      .split("T")[0], // 30 days from now
  });
  const { addToast } = useToast();
  const [formErrors, setFormErrors] = useState<string[]>([]);

  useEffect(() => {
    fetchCoupons();
  }, []);

  const fetchCoupons = async () => {
    try {
      setLoading(true);
      const response = await getSellerCoupons();
      setCoupons(response.coupons);
    } catch (error) {
      console.error("Error fetching coupons:", error);
      addToast({
        message: "Failed to fetch coupons",
        type: "error",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validate form data
    const validation = validateCouponData(formData);
    if (!validation.isValid) {
      setFormErrors(validation.errors);
      return;
    }

    try {
      if (editingCoupon) {
        await updateSellerCoupon(editingCoupon.id, formData);
        addToast({
          message: "Coupon updated successfully",
          type: "success",
        });
      } else {
        await createSellerCoupon(formData);
        addToast({
          message: "Coupon created successfully",
          type: "success",
        });
      }

      setShowForm(false);
      setEditingCoupon(null);
      setFormData({
        code: "",
        description: "",
        discountType: "PERCENTAGE",
        discountValue: 0,
        startDate: new Date().toISOString().split("T")[0],
        endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
          .toISOString()
          .split("T")[0],
      });
      setFormErrors([]);
      fetchCoupons();
    } catch (error) {
      console.error("Error saving coupon:", error);
      addToast({
        message: "Failed to save coupon",
        type: "error",
      });
    }
  };

  const handleEdit = (coupon: SellerCouponResponse) => {
    setEditingCoupon(coupon);
    setFormData({
      code: coupon.code,
      description: coupon.description,
      discountType: coupon.discountType,
      discountValue: coupon.discountValue,
      minimumOrderAmount: coupon.minimumOrderAmount,
      maxDiscountAmount: coupon.maxDiscountAmount,
      startDate: coupon.startDate.split("T")[0],
      endDate: coupon.endDate.split("T")[0],
      usageLimit: coupon.usageLimit,
      applicableCategories: coupon.applicableCategories,
      applicableProducts: coupon.applicableProducts,
    });
    setShowForm(true);
    setFormErrors([]);
  };

  const handleDelete = async (couponId: number) => {
    if (!confirm("Are you sure you want to delete this coupon?")) {
      return;
    }

    try {
      await deleteSellerCoupon(couponId);
      addToast({
        message: "Coupon deleted successfully",
        type: "success",
      });
      fetchCoupons();
    } catch (error) {
      console.error("Error deleting coupon:", error);
      addToast({
        message: "Failed to delete coupon",
        type: "error",
      });
    }
  };

  const handleToggleStatus = async (coupon: SellerCouponResponse) => {
    try {
      await toggleCouponStatus(coupon.id, !coupon.active);
      addToast({
        message: `Coupon ${
          !coupon.active ? "activated" : "deactivated"
        } successfully`,
        type: "success",
      });
      fetchCoupons();
    } catch (error) {
      console.error("Error toggling coupon status:", error);
      addToast({
        message: "Failed to update coupon status",
        type: "error",
      });
    }
  };

  const resetForm = () => {
    setFormData({
      code: "",
      description: "",
      discountType: "PERCENTAGE",
      discountValue: 0,
      startDate: new Date().toISOString().split("T")[0],
      endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
        .toISOString()
        .split("T")[0],
    });
    setFormErrors([]);
    setEditingCoupon(null);
    setShowForm(false);
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="flex items-center justify-center h-64">
          <div className="text-lg">Loading coupons...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">
            Coupon Management
          </h1>
          <p className="text-gray-600 mt-2">
            Create and manage discount coupons for your products
          </p>
        </div>
        <Button
          onClick={() => setShowForm(true)}
          className="bg-blue-600 hover:bg-blue-700"
        >
          Create New Coupon
        </Button>
      </div>

      {/* Coupon Form */}
      {showForm && (
        <Card className="mb-8 p-6">
          <h2 className="text-xl font-semibold mb-4">
            {editingCoupon ? "Edit Coupon" : "Create New Coupon"}
          </h2>

          {formErrors.length > 0 && (
            <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-md">
              <ul className="text-red-600 text-sm">
                {formErrors.map((error, index) => (
                  <li key={index}>• {error}</li>
                ))}
              </ul>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Coupon Code *
                </label>
                <Input
                  type="text"
                  value={formData.code}
                  onChange={(e) =>
                    setFormData({ ...formData, code: e.target.value })
                  }
                  placeholder="e.g., SAVE20"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Discount Type *
                </label>
                <select
                  value={formData.discountType}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      discountType: e.target.value as
                        | "PERCENTAGE"
                        | "FIXED_AMOUNT",
                    })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                >
                  <option value="PERCENTAGE">Percentage</option>
                  <option value="FIXED_AMOUNT">Fixed Amount</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Discount Value *
                </label>
                <Input
                  type="number"
                  value={formData.discountValue}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      discountValue: parseFloat(e.target.value) || 0,
                    })
                  }
                  placeholder={
                    formData.discountType === "PERCENTAGE" ? "20" : "10.00"
                  }
                  min="0"
                  step={formData.discountType === "PERCENTAGE" ? "1" : "0.01"}
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Description *
                </label>
                <Input
                  type="text"
                  value={formData.description}
                  onChange={(e) =>
                    setFormData({ ...formData, description: e.target.value })
                  }
                  placeholder="e.g., 20% off all items"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Start Date *
                </label>
                <Input
                  type="date"
                  value={formData.startDate}
                  onChange={(e) =>
                    setFormData({ ...formData, startDate: e.target.value })
                  }
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  End Date *
                </label>
                <Input
                  type="date"
                  value={formData.endDate}
                  onChange={(e) =>
                    setFormData({ ...formData, endDate: e.target.value })
                  }
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Minimum Order Amount
                </label>
                <Input
                  type="number"
                  value={formData.minimumOrderAmount || ""}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      minimumOrderAmount:
                        parseFloat(e.target.value) || undefined,
                    })
                  }
                  placeholder="0.00"
                  min="0"
                  step="0.01"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Usage Limit
                </label>
                <Input
                  type="number"
                  value={formData.usageLimit || ""}
                  onChange={(e) =>
                    setFormData({
                      ...formData,
                      usageLimit: parseInt(e.target.value) || undefined,
                    })
                  }
                  placeholder="Unlimited"
                  min="1"
                />
              </div>
            </div>

            <div className="flex justify-end space-x-4">
              <Button type="button" variant="outline" onClick={resetForm}>
                Cancel
              </Button>
              <Button type="submit" className="bg-blue-600 hover:bg-blue-700">
                {editingCoupon ? "Update Coupon" : "Create Coupon"}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {/* Coupons List */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {coupons.map((coupon) => (
          <Card key={coupon.id} className="p-6">
            <div className="flex justify-between items-start mb-4">
              <div>
                <h3 className="text-lg font-semibold text-gray-900">
                  {coupon.code}
                </h3>
                <p className="text-sm text-gray-600">{coupon.description}</p>
              </div>
              <span
                className={`px-2 py-1 text-xs font-medium rounded-full ${getCouponStatusColor(
                  coupon
                )}`}
              >
                {getCouponStatusText(coupon)}
              </span>
            </div>

            <div className="space-y-2 mb-4">
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Discount:</span>
                <span className="text-sm font-medium">
                  {formatCouponDisplay(coupon)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-sm text-gray-600">Valid:</span>
                <span className="text-sm">
                  {new Date(coupon.startDate).toLocaleDateString()} -{" "}
                  {new Date(coupon.endDate).toLocaleDateString()}
                </span>
              </div>
              {coupon.usageLimit && (
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Usage:</span>
                  <span className="text-sm">
                    {coupon.usedCount || 0} / {coupon.usageLimit}
                  </span>
                </div>
              )}
              {coupon.minimumOrderAmount && (
                <div className="flex justify-between">
                  <span className="text-sm text-gray-600">Min Order:</span>
                  <span className="text-sm">${coupon.minimumOrderAmount}</span>
                </div>
              )}
            </div>

            <div className="flex space-x-2">
              <Button
                size="sm"
                variant="outline"
                onClick={() => handleEdit(coupon)}
                className="flex-1"
              >
                Edit
              </Button>
              <Button
                size="sm"
                variant={coupon.active ? "destructive" : "default"}
                onClick={() => handleToggleStatus(coupon)}
                className="flex-1"
              >
                {coupon.active ? "Deactivate" : "Activate"}
              </Button>
              <Button
                size="sm"
                variant="destructive"
                onClick={() => handleDelete(coupon.id)}
                className="flex-1"
              >
                Delete
              </Button>
            </div>
          </Card>
        ))}
      </div>

      {coupons.length === 0 && (
        <div className="text-center py-12">
          <div className="text-gray-500 text-lg">No coupons created yet</div>
          <p className="text-gray-400 mt-2">
            Create your first coupon to start offering discounts to customers
          </p>
        </div>
      )}
    </div>
  );
}
