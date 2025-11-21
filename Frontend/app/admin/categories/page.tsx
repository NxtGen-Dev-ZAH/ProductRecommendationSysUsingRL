"use client";

import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { FaPlus, FaEdit, FaTrash, FaFolder, FaUpload } from "react-icons/fa";
import Image from "next/image";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import AdminLayout from "../../../components/admin/AdminLayout";
import {
  createAdminCategory,
  updateAdminCategory,
  deleteAdminCategory,
  uploadAdminCategoryImage,
  AdminCategoryRequest,
  AdminCategoryResponse,
} from "../../api/services/admin";
import { getAllCategories } from "../../api/services/category";

const categorySchema = z.object({
  name: z
    .string()
    .min(1, "Category name is required")
    .max(100, "Name too long"),
  description: z.string().optional(),
  parentId: z.number().optional(),
});

type CategoryFormValues = z.infer<typeof categorySchema>;

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState<AdminCategoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingCategory, setEditingCategory] =
    useState<AdminCategoryResponse | null>(null);
  const [selectedImage, setSelectedImage] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<Set<number>>(new Set());
  const [formLoading, setFormLoading] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error" | "info";
    text: string;
  } | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isDirty },
  } = useForm<CategoryFormValues>({
    resolver: zodResolver(categorySchema),
  });

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    try {
      setLoading(true);
      const response = await getAllCategories();
      setCategories(response as AdminCategoryResponse[]);
    } catch (error) {
      console.error("Error loading categories:", error);
      setMessage({ type: "error", text: "Failed to load categories" });
    } finally {
      setLoading(false);
    }
  };

  const handleImageSelect = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (file) {
      setSelectedImage(file);
      setImagePreview(URL.createObjectURL(file));
    }
  };

  const onSubmit = async (data: CategoryFormValues) => {
    try {
      setFormLoading(true);

      const categoryData: AdminCategoryRequest = {
        name: data.name,
        description: data.description,
        parentId: data.parentId,
      };

      let category: AdminCategoryResponse;

      if (editingCategory) {
        // Update existing category
        category = await updateAdminCategory(editingCategory.id, categoryData);
        setMessage({ type: "success", text: "Category updated successfully!" });
      } else {
        // Create new category
        category = await createAdminCategory(categoryData);
        setMessage({ type: "success", text: "Category created successfully!" });
      }

      // Upload image if selected
      if (selectedImage) {
        try {
          await uploadAdminCategoryImage(category.id, selectedImage);
          setMessage({
            type: "success",
            text: `Category ${
              editingCategory ? "updated" : "created"
            } with image successfully!`,
          });
        } catch (imageError) {
          console.error("Error uploading image:", imageError);
          setMessage({
            type: "info",
            text: `Category ${
              editingCategory ? "updated" : "created"
            } but image upload failed`,
          });
        }
      }

      // Reset form and reload categories
      reset();
      setSelectedImage(null);
      setImagePreview(null);
      setEditingCategory(null);
      setShowCreateForm(false);
      await loadCategories();
    } catch (error: unknown) {
      console.error("Error saving category:", error);
      const errorMessage =
        (error as { response?: { data?: { message?: string } } })?.response
          ?.data?.message || "Failed to save category";
      setMessage({ type: "error", text: errorMessage });
    } finally {
      setFormLoading(false);
    }
  };

  const handleEditCategory = (category: AdminCategoryResponse) => {
    setEditingCategory(category);
    setValue("name", category.name);
    setValue("description", category.description || "");
    setValue("parentId", category.parentId);
    setShowCreateForm(true);
    setSelectedImage(null);
    setImagePreview(category.imageUrl || null);
  };

  const handleDeleteCategory = async (categoryId: number) => {
    if (
      !confirm(
        "Are you sure you want to delete this category? This action cannot be undone."
      )
    ) {
      return;
    }

    try {
      setActionLoading((prev) => new Set([...prev, categoryId]));
      await deleteAdminCategory(categoryId);
      setMessage({ type: "success", text: "Category deleted successfully" });
      await loadCategories();
    } catch (error) {
      console.error("Error deleting category:", error);
      setMessage({ type: "error", text: "Failed to delete category" });
    } finally {
      setActionLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(categoryId);
        return newSet;
      });
    }
  };

  const handleCancelForm = () => {
    reset();
    setSelectedImage(null);
    setImagePreview(null);
    setEditingCategory(null);
    setShowCreateForm(false);
  };

  const renderCategoryTree = (
    categories: AdminCategoryResponse[],
    level = 0
  ) => {
    return categories.map((category) => (
      <div key={category.id} className={`ml-${level * 4}`}>
        <CategoryCard
          category={category}
          onEdit={() => handleEditCategory(category)}
          onDelete={() => handleDeleteCategory(category.id)}
          loading={actionLoading.has(category.id)}
          level={level}
        />
        {category.subcategories && category.subcategories.length > 0 && (
          <div className="ml-4">
            {renderCategoryTree(category.subcategories, level + 1)}
          </div>
        )}
      </div>
    ));
  };

  const flattenCategories = (
    categories: AdminCategoryResponse[]
  ): AdminCategoryResponse[] => {
    const flattened: AdminCategoryResponse[] = [];

    const flatten = (cats: AdminCategoryResponse[]) => {
      cats.forEach((cat) => {
        flattened.push(cat);
        if (cat.subcategories) {
          flatten(cat.subcategories);
        }
      });
    };

    flatten(categories);
    return flattened;
  };

  const parentCategories = flattenCategories(categories).filter(
    (cat) => !editingCategory || cat.id !== editingCategory.id
  );

  return (
    <AdminLayout
      activeNav="categories"
      title="Category Management"
      description="Manage product categories and their hierarchy"
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
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-medium text-gray-900">Categories</h3>
          <Button
            onClick={() => setShowCreateForm(!showCreateForm)}
            className="bg-red-600 hover:bg-red-700"
          >
            <FaPlus className="w-4 h-4 mr-2" />
            Add Category
          </Button>
        </div>
      </div>

      {/* Create/Edit Form */}
      {showCreateForm && (
        <div className="bg-white rounded-lg shadow-sm border p-6 mb-6">
          <h4 className="font-medium text-gray-900 mb-4">
            {editingCategory ? "Edit Category" : "Create New Category"}
          </h4>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Category Name *
                </label>
                <Input
                  {...register("name")}
                  placeholder="Enter category name"
                  error={errors.name?.message}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Parent Category
                </label>
                <select
                  {...register("parentId", { valueAsNumber: true })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-red-500 focus:border-transparent"
                >
                  <option value="">No Parent (Root Category)</option>
                  {parentCategories.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description
              </label>
              <textarea
                {...register("description")}
                rows={3}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-red-500 focus:border-transparent"
                placeholder="Enter category description..."
              />
            </div>

            {/* Image Upload */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Category Image
              </label>
              <div className="flex items-center gap-4">
                {imagePreview && (
                  <div className="w-16 h-16 relative rounded-lg overflow-hidden border">
                    <Image
                      src={imagePreview}
                      alt="Category preview"
                      fill
                      className="object-cover"
                    />
                  </div>
                )}
                <div>
                  <input
                    type="file"
                    accept="image/*"
                    onChange={handleImageSelect}
                    className="hidden"
                    id="category-image-upload"
                  />
                  <label
                    htmlFor="category-image-upload"
                    className="cursor-pointer inline-flex items-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                  >
                    <FaUpload className="w-4 h-4 mr-2" />
                    Upload Image
                  </label>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3">
              <Button
                type="button"
                variant="outline"
                onClick={handleCancelForm}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                loading={formLoading}
                disabled={!isDirty && !selectedImage}
                className="bg-red-600 hover:bg-red-700"
              >
                {editingCategory ? "Update Category" : "Create Category"}
              </Button>
            </div>
          </form>
        </div>
      )}

      {/* Categories List */}
      <div className="bg-white rounded-lg shadow-sm border">
        <div className="p-6">
          {loading ? (
            <div className="space-y-4">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="animate-pulse">
                  <div className="h-20 bg-gray-200 rounded-lg"></div>
                </div>
              ))}
            </div>
          ) : categories.length > 0 ? (
            <div className="space-y-4">{renderCategoryTree(categories)}</div>
          ) : (
            <div className="text-center py-8">
              <FaFolder className="w-12 h-12 text-gray-300 mx-auto mb-4" />
              <h4 className="text-lg font-medium text-gray-900 mb-2">
                No categories yet
              </h4>
              <p className="text-gray-600 mb-4">
                Create your first category to get started
              </p>
              <Button
                onClick={() => setShowCreateForm(true)}
                className="bg-red-600 hover:bg-red-700"
              >
                <FaPlus className="w-4 h-4 mr-2" />
                Create Category
              </Button>
            </div>
          )}
        </div>
      </div>
    </AdminLayout>
  );
}

interface CategoryCardProps {
  category: AdminCategoryResponse;
  onEdit: () => void;
  onDelete: () => void;
  loading: boolean;
  level: number;
}

function CategoryCard({
  category,
  onEdit,
  onDelete,
  loading,
  level,
}: CategoryCardProps) {
  const indentClass = level > 0 ? `ml-${level * 6}` : "";

  return (
    <div
      className={`border border-gray-200 rounded-lg p-4 hover:border-gray-300 transition-colors ${indentClass} relative`}
    >
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 bg-gray-100 rounded-lg flex items-center justify-center">
            {category.imageUrl ? (
              <Image
                src={category.imageUrl}
                alt={category.name}
                width={48}
                height={48}
                className="rounded-lg object-cover"
              />
            ) : (
              <FaFolder className="w-6 h-6 text-gray-400" />
            )}
          </div>
          <div>
            <h4 className="font-medium text-gray-900 flex items-center gap-2">
              {level > 0 && <span className="text-gray-400">└─</span>}
              {category.name}
            </h4>
            <div className="flex items-center gap-4 text-sm text-gray-600">
              <span>ID: {category.id}</span>
              {category.parentId && <span>Parent: {category.parentId}</span>}
              {category.subcategories && category.subcategories.length > 0 && (
                <span>{category.subcategories.length} subcategories</span>
              )}
            </div>
            {category.description && (
              <p className="text-sm text-gray-600 mt-1">
                {category.description}
              </p>
            )}
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={onEdit}
            className="text-gray-400 hover:text-blue-600 transition-colors"
            title="Edit Category"
            disabled={loading}
          >
            <FaEdit className="w-4 h-4" />
          </button>
          <button
            onClick={onDelete}
            className="text-gray-400 hover:text-red-600 transition-colors"
            title="Delete Category"
            disabled={loading}
          >
            <FaTrash className="w-4 h-4" />
          </button>
        </div>
      </div>

      {loading && (
        <div className="absolute inset-0 bg-white bg-opacity-75 flex items-center justify-center rounded-lg">
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-red-600"></div>
        </div>
      )}
    </div>
  );
}
