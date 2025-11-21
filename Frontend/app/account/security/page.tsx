"use client";

import { useAuth } from "../../context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  FaLock,
  FaEye,
  FaEyeSlash,
  FaShieldAlt,
  FaTrash,
  FaExclamationTriangle,
} from "react-icons/fa";
import AccountNav from "../../../components/account/AccountNav";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import {
  updateUserPassword,
  deleteUserAccount,
  getMyPrivacySettings,
  updatePrivacySettings,
  PrivacySettings,
} from "../../api/services/user";

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Current password is required"),
    newPassword: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .regex(/[A-Z]/, "Password must contain at least one capital letter")
      .regex(/[0-9]/, "Password must contain at least one number")
      .regex(
        /[^A-Za-z0-9]/,
        "Password must contain at least one special character"
      ),
    confirmPassword: z.string().min(1, "Please confirm your password"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords don't match",
    path: ["confirmPassword"],
  });

type PasswordFormValues = z.infer<typeof passwordSchema>;

export default function SecurityPage() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    new: false,
    confirm: false,
  });
  const [privacySettings, setPrivacySettings] =
    useState<PrivacySettings | null>(null);
  const [privacyLoading, setPrivacyLoading] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
  });

  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    loadPrivacySettings();
  }, [isAuthenticated, authLoading, router]);

  const loadPrivacySettings = async () => {
    try {
      const settings = await getMyPrivacySettings();
      setPrivacySettings(settings);
    } catch (error) {
      console.error("Error loading privacy settings:", error);
    }
  };

  const onPasswordSubmit = async (data: PasswordFormValues) => {
    try {
      setLoading(true);
      setMessage(null);

      await updateUserPassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
        confirmPassword: data.confirmPassword,
      });

      setMessage({ type: "success", text: "Password changed successfully!" });
      reset();
    } catch (error: unknown) {
      console.error("Error changing password:", error);
      const errorMessage =
        error instanceof Error && "response" in error
          ? (error as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to change password. Please try again.";
      setMessage({
        type: "error",
        text: errorMessage || "Failed to change password. Please try again.",
      });
    } finally {
      setLoading(false);
    }
  };

  const handlePrivacySettingChange = async (
    key: keyof PrivacySettings,
    value: boolean | string
  ) => {
    if (!privacySettings) return;

    try {
      setPrivacyLoading(true);
      const updatedSettings = await updatePrivacySettings({
        ...privacySettings,
        [key]: value,
      });
      setPrivacySettings(updatedSettings);
    } catch (error) {
      console.error("Error updating privacy settings:", error);
      setMessage({ type: "error", text: "Failed to update privacy settings" });
    } finally {
      setPrivacyLoading(false);
    }
  };

  const handleDeleteAccount = async () => {
    const password = prompt(
      "Please enter your password to confirm account deletion:"
    );
    if (!password) return;

    try {
      await deleteUserAccount(password);
      setMessage({
        type: "success",
        text: "Account deletion request submitted. Please check your email for confirmation.",
      });
      setShowDeleteDialog(false);
    } catch (error: unknown) {
      console.error("Error requesting account deletion:", error);
      const errorMessage =
        error instanceof Error && "response" in error
          ? (error as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to request account deletion.";
      setMessage({
        type: "error",
        text: errorMessage || "Failed to request account deletion.",
      });
    }
  };

  const togglePasswordVisibility = (field: "current" | "new" | "confirm") => {
    setShowPasswords((prev) => ({
      ...prev,
      [field]: !prev[field],
    }));
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
            <h1 className="text-2xl font-bold text-gray-900">
              Security Settings
            </h1>
            <p className="text-gray-600">
              Manage your password and security preferences
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="dashboard" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3 space-y-6">
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

              {/* Change Password */}
              <div className="bg-white rounded-lg shadow-sm border p-6">
                <div className="flex items-center gap-3 mb-6">
                  <FaLock className="w-5 h-5 text-gray-600" />
                  <h3 className="text-lg font-medium text-gray-900">
                    Change Password
                  </h3>
                </div>

                <form
                  onSubmit={handleSubmit(onPasswordSubmit)}
                  className="space-y-4"
                >
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Current Password
                    </label>
                    <div className="relative">
                      <Input
                        {...register("currentPassword")}
                        type={showPasswords.current ? "text" : "password"}
                        placeholder="Enter your current password"
                        error={errors.currentPassword?.message}
                      />
                      <button
                        type="button"
                        onClick={() => togglePasswordVisibility("current")}
                        className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                      >
                        {showPasswords.current ? <FaEyeSlash /> : <FaEye />}
                      </button>
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      New Password
                    </label>
                    <div className="relative">
                      <Input
                        {...register("newPassword")}
                        type={showPasswords.new ? "text" : "password"}
                        placeholder="Enter your new password"
                        error={errors.newPassword?.message}
                      />
                      <button
                        type="button"
                        onClick={() => togglePasswordVisibility("new")}
                        className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                      >
                        {showPasswords.new ? <FaEyeSlash /> : <FaEye />}
                      </button>
                    </div>
                    <p className="mt-1 text-xs text-gray-500">
                      Password must be at least 8 characters with uppercase,
                      number, and special character
                    </p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Confirm New Password
                    </label>
                    <div className="relative">
                      <Input
                        {...register("confirmPassword")}
                        type={showPasswords.confirm ? "text" : "password"}
                        placeholder="Confirm your new password"
                        error={errors.confirmPassword?.message}
                      />
                      <button
                        type="button"
                        onClick={() => togglePasswordVisibility("confirm")}
                        className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                      >
                        {showPasswords.confirm ? <FaEyeSlash /> : <FaEye />}
                      </button>
                    </div>
                  </div>

                  <div className="flex justify-end pt-4">
                    <Button
                      type="submit"
                      loading={loading}
                      disabled={!isDirty || loading}
                    >
                      {loading ? "Changing Password..." : "Change Password"}
                    </Button>
                  </div>
                </form>
              </div>

              {/* Privacy Settings */}
              {privacySettings && (
                <div className="bg-white rounded-lg shadow-sm border p-6">
                  <div className="flex items-center gap-3 mb-6">
                    <FaShieldAlt className="w-5 h-5 text-gray-600" />
                    <h3 className="text-lg font-medium text-gray-900">
                      Privacy Settings
                    </h3>
                  </div>

                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="font-medium text-gray-900">
                          Profile Visibility
                        </p>
                        <p className="text-sm text-gray-600">
                          Control who can see your profile
                        </p>
                      </div>
                      <select
                        value={privacySettings.profileVisibility}
                        onChange={(e) =>
                          handlePrivacySettingChange(
                            "profileVisibility",
                            e.target.value as string
                          )
                        }
                        className="px-3 py-2 border border-gray-300 rounded-md focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        disabled={privacyLoading}
                      >
                        <option value="public">Public</option>
                        <option value="friends_only">Friends Only</option>
                        <option value="private">Private</option>
                      </select>
                    </div>

                    {[
                      {
                        key: "showEmail",
                        label: "Show Email Address",
                        description: "Allow others to see your email",
                      },
                      {
                        key: "showPhone",
                        label: "Show Phone Number",
                        description: "Allow others to see your phone number",
                      },
                      {
                        key: "showLocation",
                        label: "Show Location",
                        description: "Allow others to see your location",
                      },
                      {
                        key: "allowFollowing",
                        label: "Allow Following",
                        description: "Let other users follow you",
                      },
                      {
                        key: "allowMessaging",
                        label: "Allow Messaging",
                        description: "Receive messages from other users",
                      },
                      {
                        key: "emailNotifications",
                        label: "Email Notifications",
                        description: "Receive notifications via email",
                      },
                      {
                        key: "smsNotifications",
                        label: "SMS Notifications",
                        description: "Receive notifications via SMS",
                      },
                    ].map(({ key, label, description }) => (
                      <div
                        key={key}
                        className="flex items-center justify-between"
                      >
                        <div>
                          <p className="font-medium text-gray-900">{label}</p>
                          <p className="text-sm text-gray-600">{description}</p>
                        </div>
                        <label className="relative inline-flex items-center cursor-pointer">
                          <input
                            type="checkbox"
                            checked={
                              privacySettings[
                                key as keyof PrivacySettings
                              ] as boolean
                            }
                            onChange={(e) =>
                              handlePrivacySettingChange(
                                key as keyof PrivacySettings,
                                e.target.checked
                              )
                            }
                            className="sr-only peer"
                            disabled={privacyLoading}
                          />
                          <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                        </label>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Account Deletion */}
              <div className="bg-white rounded-lg shadow-sm border p-6">
                <div className="flex items-center gap-3 mb-6">
                  <FaExclamationTriangle className="w-5 h-5 text-red-600" />
                  <h3 className="text-lg font-medium text-gray-900">
                    Danger Zone
                  </h3>
                </div>

                <div className="border border-red-200 rounded-lg p-4 bg-red-50">
                  <div className="flex items-start justify-between">
                    <div>
                      <h4 className="font-medium text-red-900 mb-1">
                        Delete Account
                      </h4>
                      <p className="text-sm text-red-700">
                        Permanently delete your account and all associated data.
                        This action cannot be undone.
                      </p>
                    </div>
                    <Button
                      variant="outline"
                      onClick={() => setShowDeleteDialog(true)}
                      className="ml-4 border-red-300 text-red-700 hover:bg-red-100"
                    >
                      <FaTrash className="w-4 h-4 mr-2" />
                      Delete Account
                    </Button>
                  </div>
                </div>

                {/* Delete Confirmation Dialog */}
                {showDeleteDialog && (
                  <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
                    <div className="bg-white rounded-lg p-6 max-w-md w-full">
                      <div className="flex items-center gap-3 mb-4">
                        <FaExclamationTriangle className="w-6 h-6 text-red-600" />
                        <h3 className="text-lg font-medium text-gray-900">
                          Confirm Account Deletion
                        </h3>
                      </div>
                      <p className="text-gray-600 mb-6">
                        Are you absolutely sure you want to delete your account?
                        This action cannot be undone and will permanently
                        delete:
                      </p>
                      <ul className="list-disc list-inside text-sm text-gray-600 mb-6 space-y-1">
                        <li>Your profile and personal information</li>
                        <li>Order history and data</li>
                        <li>Saved addresses and payment methods</li>
                        <li>Wishlist and preferences</li>
                      </ul>
                      <div className="flex justify-end gap-4">
                        <Button
                          variant="outline"
                          onClick={() => setShowDeleteDialog(false)}
                        >
                          Cancel
                        </Button>
                        <Button
                          onClick={handleDeleteAccount}
                          className="bg-red-600 text-white hover:bg-red-700"
                        >
                          Yes, Delete My Account
                        </Button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
