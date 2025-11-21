"use client";

import { useAuth } from "../../context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  FaPlus,
  FaEdit,
  FaTrash,
  FaSave,
  FaTimes,
  FaCog,
} from "react-icons/fa";
import AccountNav from "../../../components/account/AccountNav";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import {
  getAllCustomFields,
  createCustomField,
  updateCustomField,
  deleteCustomField,
  UserCustomField,
} from "../../api/services/user";

const customFieldSchema = z.object({
  fieldKey: z
    .string()
    .min(1, "Field key is required")
    .max(50, "Field key must be less than 50 characters"),
  fieldValue: z.string().min(1, "Field value is required"),
  fieldType: z.enum(["text", "number", "boolean", "date"], {
    required_error: "Please select a field type",
  }),
  isPublic: z.boolean().default(false),
});

type CustomFieldFormValues = z.infer<typeof customFieldSchema>;

export default function CustomFieldsPage() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const [fields, setFields] = useState<UserCustomField[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingField, setEditingField] = useState<UserCustomField | null>(
    null
  );
  const [showAddForm, setShowAddForm] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors, isDirty },
  } = useForm<CustomFieldFormValues>({
    resolver: zodResolver(customFieldSchema),
    defaultValues: {
      fieldType: "text",
      isPublic: false,
    },
  });

  useEffect(() => {
    // Wait for auth to finish loading before checking authentication
    if (authLoading) {
      return;
    }

    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    loadCustomFields();
  }, [isAuthenticated, authLoading, router]);

  const loadCustomFields = async () => {
    try {
      setLoading(true);
      const fieldsData = await getAllCustomFields();
      setFields(fieldsData);
    } catch (error) {
      console.error("Error loading custom fields:", error);
      setMessage({ type: "error", text: "Failed to load custom fields" });
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = async (data: CustomFieldFormValues) => {
    try {
      setMessage(null);

      if (editingField) {
        // Update existing field
        const updatedField = await updateCustomField(editingField.id!, data);
        setFields((prev) =>
          prev.map((field) =>
            field.id === editingField.id ? updatedField : field
          )
        );
        setMessage({
          type: "success",
          text: "Custom field updated successfully!",
        });
        setEditingField(null);
      } else {
        // Create new field
        const newField = await createCustomField(data);
        setFields((prev) => [...prev, newField]);
        setMessage({
          type: "success",
          text: "Custom field created successfully!",
        });
        setShowAddForm(false);
      }

      reset();
    } catch (error: unknown) {
      console.error("Error saving custom field:", error);
      const errorMessage =
        error instanceof Error
          ? error.message
          : (error as { response?: { data?: { message?: string } } })?.response
              ?.data?.message || "Failed to save custom field";
      setMessage({ type: "error", text: errorMessage });
    }
  };

  const handleEdit = (field: UserCustomField) => {
    setEditingField(field);
    setValue("fieldKey", field.fieldKey);
    setValue("fieldValue", field.fieldValue);
    setValue("fieldType", field.fieldType);
    setValue("isPublic", field.isPublic || false);
    setShowAddForm(false);
  };

  const handleDelete = async (fieldId: number) => {
    if (!confirm("Are you sure you want to delete this custom field?")) {
      return;
    }

    try {
      await deleteCustomField(fieldId);
      setFields((prev) => prev.filter((field) => field.id !== fieldId));
      setMessage({
        type: "success",
        text: "Custom field deleted successfully!",
      });
    } catch (error) {
      console.error("Error deleting custom field:", error);
      setMessage({ type: "error", text: "Failed to delete custom field" });
    }
  };

  const handleCancel = () => {
    setEditingField(null);
    setShowAddForm(false);
    reset();
  };

  const renderFieldValue = (field: UserCustomField) => {
    switch (field.fieldType) {
      case "boolean":
        return field.fieldValue === "true" ? "Yes" : "No";
      case "date":
        return new Date(field.fieldValue).toLocaleDateString();
      default:
        return field.fieldValue;
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

  return (
    <div className="bg-gray-50 min-h-screen">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-6xl mx-auto">
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-gray-900">Custom Fields</h1>
            <p className="text-gray-600">
              Manage your custom profile fields and preferences
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="dashboard" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">
              {/* Message Display */}
              {message && (
                <div
                  className={`mb-6 p-4 rounded-md ${
                    message.type === "success"
                      ? "bg-green-50 text-green-700 border border-green-200"
                      : "bg-red-50 text-red-700 border border-red-200"
                  }`}
                >
                  {message.text}
                </div>
              )}

              {/* Custom Fields List */}
              <div className="bg-white rounded-lg shadow-sm border mb-6">
                <div className="p-6 border-b border-gray-200">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <FaCog className="w-5 h-5 text-gray-600" />
                      <h3 className="text-lg font-medium text-gray-900">
                        Your Custom Fields ({fields.length})
                      </h3>
                    </div>
                    <Button
                      onClick={() => {
                        setShowAddForm(true);
                        setEditingField(null);
                        reset();
                      }}
                      disabled={showAddForm || !!editingField}
                    >
                      <FaPlus className="w-4 h-4 mr-2" />
                      Add Field
                    </Button>
                  </div>
                </div>

                <div className="p-6">
                  {loading ? (
                    <div className="space-y-4">
                      {[...Array(3)].map((_, index) => (
                        <div key={index} className="animate-pulse">
                          <div className="h-4 bg-gray-200 rounded w-1/4 mb-2"></div>
                          <div className="h-6 bg-gray-200 rounded w-1/2 mb-4"></div>
                        </div>
                      ))}
                    </div>
                  ) : fields.length === 0 && !showAddForm ? (
                    <div className="text-center py-12">
                      <FaCog className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                      <h4 className="text-lg font-medium text-gray-900 mb-2">
                        No Custom Fields
                      </h4>
                      <p className="text-gray-600 mb-6">
                        Create custom fields to store additional information
                        about yourself.
                      </p>
                      <Button
                        onClick={() => {
                          setShowAddForm(true);
                          reset();
                        }}
                      >
                        <FaPlus className="w-4 h-4 mr-2" />
                        Add Your First Field
                      </Button>
                    </div>
                  ) : (
                    <div className="space-y-4">
                      {fields.map((field) => (
                        <div
                          key={field.id}
                          className="border border-gray-200 rounded-lg p-4 hover:border-gray-300 transition-colors"
                        >
                          {editingField?.id === field.id ? (
                            // Edit form
                            <form
                              onSubmit={handleSubmit(onSubmit)}
                              className="space-y-4"
                            >
                              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div>
                                  <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Field Key
                                  </label>
                                  <Input
                                    {...register("fieldKey")}
                                    placeholder="e.g., website, hobby, skill"
                                    error={errors.fieldKey?.message}
                                  />
                                </div>
                                <div>
                                  <label className="block text-sm font-medium text-gray-700 mb-1">
                                    Field Type
                                  </label>
                                  <select
                                    {...register("fieldType")}
                                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                  >
                                    <option value="text">Text</option>
                                    <option value="number">Number</option>
                                    <option value="boolean">Yes/No</option>
                                    <option value="date">Date</option>
                                  </select>
                                  {errors.fieldType && (
                                    <p className="mt-1 text-sm text-red-600">
                                      {errors.fieldType.message}
                                    </p>
                                  )}
                                </div>
                              </div>
                              <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                  Field Value
                                </label>
                                <Input
                                  {...register("fieldValue")}
                                  placeholder="Enter the value"
                                  error={errors.fieldValue?.message}
                                />
                              </div>
                              <div className="flex items-center">
                                <input
                                  {...register("isPublic")}
                                  type="checkbox"
                                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                />
                                <label className="ml-2 text-sm text-gray-700">
                                  Make this field publicly visible on your
                                  profile
                                </label>
                              </div>
                              <div className="flex justify-end gap-2">
                                <Button
                                  type="button"
                                  variant="outline"
                                  onClick={handleCancel}
                                >
                                  <FaTimes className="w-4 h-4 mr-2" />
                                  Cancel
                                </Button>
                                <Button type="submit" disabled={!isDirty}>
                                  <FaSave className="w-4 h-4 mr-2" />
                                  Save Changes
                                </Button>
                              </div>
                            </form>
                          ) : (
                            // Display field
                            <div className="flex items-center justify-between">
                              <div className="flex-1">
                                <div className="flex items-center gap-2 mb-1">
                                  <h4 className="font-medium text-gray-900">
                                    {field.fieldKey}
                                  </h4>
                                  {field.isPublic && (
                                    <span className="px-2 py-1 text-xs bg-green-100 text-green-800 rounded-full">
                                      Public
                                    </span>
                                  )}
                                  <span className="px-2 py-1 text-xs bg-gray-100 text-gray-600 rounded-full">
                                    {field.fieldType}
                                  </span>
                                </div>
                                <p className="text-gray-700">
                                  {renderFieldValue(field)}
                                </p>
                              </div>
                              <div className="flex gap-2">
                                <button
                                  onClick={() => handleEdit(field)}
                                  className="p-2 text-blue-600 hover:bg-blue-50 rounded-md transition-colors"
                                  title="Edit field"
                                >
                                  <FaEdit className="w-4 h-4" />
                                </button>
                                <button
                                  onClick={() => handleDelete(field.id!)}
                                  className="p-2 text-red-600 hover:bg-red-50 rounded-md transition-colors"
                                  title="Delete field"
                                >
                                  <FaTrash className="w-4 h-4" />
                                </button>
                              </div>
                            </div>
                          )}
                        </div>
                      ))}

                      {/* Add Form */}
                      {showAddForm && (
                        <div className="border border-blue-200 rounded-lg p-4 bg-blue-50">
                          <form
                            onSubmit={handleSubmit(onSubmit)}
                            className="space-y-4"
                          >
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                              <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                  Field Key
                                </label>
                                <Input
                                  {...register("fieldKey")}
                                  placeholder="e.g., website, hobby, skill"
                                  error={errors.fieldKey?.message}
                                />
                              </div>
                              <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">
                                  Field Type
                                </label>
                                <select
                                  {...register("fieldType")}
                                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                                >
                                  <option value="text">Text</option>
                                  <option value="number">Number</option>
                                  <option value="boolean">Yes/No</option>
                                  <option value="date">Date</option>
                                </select>
                                {errors.fieldType && (
                                  <p className="mt-1 text-sm text-red-600">
                                    {errors.fieldType.message}
                                  </p>
                                )}
                              </div>
                            </div>
                            <div>
                              <label className="block text-sm font-medium text-gray-700 mb-1">
                                Field Value
                              </label>
                              <Input
                                {...register("fieldValue")}
                                placeholder="Enter the value"
                                error={errors.fieldValue?.message}
                              />
                            </div>
                            <div className="flex items-center">
                              <input
                                {...register("isPublic")}
                                type="checkbox"
                                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                              />
                              <label className="ml-2 text-sm text-gray-700">
                                Make this field publicly visible on your profile
                              </label>
                            </div>
                            <div className="flex justify-end gap-2">
                              <Button
                                type="button"
                                variant="outline"
                                onClick={handleCancel}
                              >
                                <FaTimes className="w-4 h-4 mr-2" />
                                Cancel
                              </Button>
                              <Button type="submit">
                                <FaPlus className="w-4 h-4 mr-2" />
                                Add Field
                              </Button>
                            </div>
                          </form>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
