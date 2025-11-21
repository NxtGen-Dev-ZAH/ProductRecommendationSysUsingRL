"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { FaTimes, FaUpload, FaStar, FaArrowLeft } from "react-icons/fa";
import Image from "next/image";
import { Button } from "../../../../components/ui/button";
import { Input } from "../../../../components/ui/input";
import SellerLayout from "../../../../components/seller/SellerLayout";
import {
  createSellerProduct,
  SellerProductRequest,
} from "../../../api/services/seller";
import { getAllCategories, Category } from "../../../api/services/category";

const productSchema = z.object({
  name: z.string().min(1, "Product name is required").max(200, "Name too long"),
  description: z.string().optional(),
  price: z.number().min(0.01, "Price must be greater than 0"),
  offerPrice: z.number().optional(),
  quantity: z.number().min(0, "Quantity cannot be negative"),
  categoryId: z.number().min(1, "Please select a category"),
  brand: z.string().optional(),
  warranty: z.string().optional(),
  inventoryLocation: z.string().optional(),
  productCode: z.string().optional(),
  manufacturingPieceNumber: z.string().optional(),
  manufacturingDate: z.string().optional(),
  expirationDate: z.string().optional(),
  EAN: z.string().optional(),
  manufacturingPlace: z.string().optional(),
  productStatus: z.enum(["ACTIVE", "INACTIVE", "OUT_OF_STOCK"]),
  productSellType: z.enum([
    "DIRECT",
    "OFFER",
    "AUCTION",
    "PREORDER",
    "SUBSCRIPTION",
    "RENTAL",
    "BUNDLE",
    "DIGITAL",
  ]),
  productCondition: z.enum(["NEW", "USED", "OPEN_NEVER_USED", "REFURBISHED"]),
  productConditionComment: z.string().optional(),
});

type ProductFormValues = z.infer<typeof productSchema>;

export default function NewProductPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedImages, setSelectedImages] = useState<File[]>([]);
  const [imagePreviews, setImagePreviews] = useState<string[]>([]);
  const [message, setMessage] = useState<{
    type: "success" | "error" | "info";
    text: string;
  } | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isDirty },
  } = useForm<ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      productStatus: "ACTIVE",
      productSellType: "DIRECT",
      productCondition: "NEW",
      quantity: 1,
    },
  });

  const watchOfferPrice = watch("offerPrice");
  const watchPrice = watch("price");

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    try {
      const response = await getAllCategories();
      setCategories(response);
    } catch (error) {
      console.error("Error loading categories:", error);
      setMessage({ type: "error", text: "Failed to load categories" });
    }
  };

  const handleImageSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || []);
    if (files.length === 0) return;

    // Limit to 10 images
    const newImages = [...selectedImages, ...files].slice(0, 10);
    setSelectedImages(newImages);

    // Create previews
    const newPreviews = newImages.map((file) => URL.createObjectURL(file));
    setImagePreviews(newPreviews);
  };

  const removeImage = (index: number) => {
    const newImages = selectedImages.filter((_, i) => i !== index);
    const newPreviews = imagePreviews.filter((_, i) => i !== index);

    setSelectedImages(newImages);
    setImagePreviews(newPreviews);

    // Revoke URL to prevent memory leaks
    URL.revokeObjectURL(imagePreviews[index]);
  };

  const onSubmit = async (data: ProductFormValues) => {
    try {
      setLoading(true);
      setMessage(null);

      const productData: SellerProductRequest = {
        ...data,
        // Convert string dates to proper format if needed
        manufacturingDate: data.manufacturingDate || undefined,
        expirationDate: data.expirationDate || undefined,
      };

      await createSellerProduct(productData, selectedImages);

      setMessage({ type: "success", text: "Product created successfully!" });

      // Redirect after a short delay
      setTimeout(() => {
        router.push("/seller/products");
      }, 1500);
    } catch (error: unknown) {
      console.error("Error creating product:", error);
      const errorMessage =
        error instanceof Error
          ? error.message
          : (error as { response?: { data?: { message?: string } } })?.response
              ?.data?.message || "Failed to create product";
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setLoading(false);
    }
  };

  return (
    <SellerLayout
      activeNav="products"
      title="Add New Product"
      description="Create a new product for your store"
    >
      <div className="space-y-6">
        {/* Back Button */}
        <div>
          <Button
            variant="outline"
            onClick={() => router.back()}
            className="mb-4"
          >
            <FaArrowLeft className="w-4 h-4 mr-2" />
            Back to Products
          </Button>
        </div>

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

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
          {/* Basic Information */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Basic Information
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Product Name *
                </label>
                <Input
                  {...register("name")}
                  placeholder="Enter product name"
                  error={errors.name?.message}
                />
              </div>

              <div className="md:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Description
                </label>
                <textarea
                  {...register("description")}
                  rows={4}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  placeholder="Describe your product..."
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Category *
                </label>
                <select
                  {...register("categoryId", { valueAsNumber: true })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="">Select a category</option>
                  {categories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
                {errors.categoryId && (
                  <p className="text-sm text-red-600 mt-1">
                    {errors.categoryId.message}
                  </p>
                )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Brand
                </label>
                <Input {...register("brand")} placeholder="Product brand" />
              </div>
            </div>
          </div>

          {/* Pricing and Inventory */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Pricing & Inventory
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Price *
                </label>
                <Input
                  {...register("price", { valueAsNumber: true })}
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                  error={errors.price?.message}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Offer Price
                </label>
                <Input
                  {...register("offerPrice", { valueAsNumber: true })}
                  type="number"
                  step="0.01"
                  min="0"
                  placeholder="0.00"
                />
                {watchOfferPrice &&
                  watchPrice &&
                  watchOfferPrice >= watchPrice && (
                    <p className="text-sm text-amber-600 mt-1">
                      Offer price should be less than regular price
                    </p>
                  )}
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Quantity *
                </label>
                <Input
                  {...register("quantity", { valueAsNumber: true })}
                  type="number"
                  min="0"
                  placeholder="0"
                  error={errors.quantity?.message}
                />
              </div>
            </div>
          </div>

          {/* Product Details */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Product Details
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Product Status
                </label>
                <select
                  {...register("productStatus")}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="OUT_OF_STOCK">Out of Stock</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Sell Type
                </label>
                <select
                  {...register("productSellType")}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="DIRECT">Direct Sale</option>
                  <option value="OFFER">Best Offer</option>
                  <option value="AUCTION">Auction</option>
                  <option value="PREORDER">Pre-order</option>
                  <option value="SUBSCRIPTION">Subscription</option>
                  <option value="RENTAL">Rental</option>
                  <option value="BUNDLE">Bundle</option>
                  <option value="DIGITAL">Digital</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Condition
                </label>
                <select
                  {...register("productCondition")}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  <option value="NEW">New</option>
                  <option value="USED">Used</option>
                  <option value="OPEN_NEVER_USED">Open Box - Never Used</option>
                  <option value="REFURBISHED">Refurbished</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Warranty
                </label>
                <Input
                  {...register("warranty")}
                  placeholder="e.g., 1 year manufacturer warranty"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Product Code
                </label>
                <Input
                  {...register("productCode")}
                  placeholder="SKU or product code"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  EAN/Barcode
                </label>
                <Input
                  {...register("EAN")}
                  placeholder="EAN or barcode number"
                />
              </div>
            </div>

            <div className="mt-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Condition Notes
              </label>
              <textarea
                {...register("productConditionComment")}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="Additional notes about product condition..."
              />
            </div>
          </div>

          {/* Additional Information */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Additional Information
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Manufacturing Date
                </label>
                <Input {...register("manufacturingDate")} type="date" />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Expiration Date
                </label>
                <Input {...register("expirationDate")} type="date" />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Manufacturing Place
                </label>
                <Input
                  {...register("manufacturingPlace")}
                  placeholder="Country or location of manufacture"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Inventory Location
                </label>
                <Input
                  {...register("inventoryLocation")}
                  placeholder="Warehouse or storage location"
                />
              </div>
            </div>
          </div>

          {/* Product Images */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Product Images
            </h3>

            {/* Image Upload */}
            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Upload Images (Max 10)
              </label>
              <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center hover:border-gray-400 transition-colors">
                <input
                  type="file"
                  multiple
                  accept="image/*"
                  onChange={handleImageSelect}
                  className="hidden"
                  id="image-upload"
                />
                <label
                  htmlFor="image-upload"
                  className="cursor-pointer flex flex-col items-center"
                >
                  <FaUpload className="w-8 h-8 text-gray-400 mb-2" />
                  <span className="text-sm text-gray-600">
                    Click to upload or drag and drop
                  </span>
                  <span className="text-xs text-gray-500 mt-1">
                    PNG, JPG, GIF up to 10MB each
                  </span>
                </label>
              </div>
            </div>

            {/* Image Previews */}
            {imagePreviews.length > 0 && (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-4">
                {imagePreviews.map((preview, index) => (
                  <div key={index} className="relative group">
                    <div className="aspect-square relative bg-gray-100 rounded-lg overflow-hidden">
                      <Image
                        src={preview}
                        alt={`Preview ${index + 1}`}
                        fill
                        className="object-cover"
                      />
                    </div>
                    <button
                      type="button"
                      onClick={() => removeImage(index)}
                      className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <FaTimes className="w-3 h-3" />
                    </button>
                    {index === 0 && (
                      <div className="absolute top-2 left-2 bg-blue-500 text-white text-xs px-2 py-1 rounded">
                        <FaStar className="w-3 h-3 inline mr-1" />
                        Primary
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Form Actions */}
          <div className="bg-white rounded-lg shadow-sm border p-6">
            <div className="flex justify-end gap-4">
              <Button
                type="button"
                variant="outline"
                onClick={() => router.back()}
                disabled={loading}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                loading={loading}
                disabled={!isDirty || loading}
                className="bg-blue-600 hover:bg-blue-700"
              >
                {loading ? "Creating Product..." : "Create Product"}
              </Button>
            </div>
          </div>
        </form>
      </div>
    </SellerLayout>
  );
}
