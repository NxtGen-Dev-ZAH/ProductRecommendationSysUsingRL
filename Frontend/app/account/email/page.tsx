"use client";

import { useAuth } from "../../context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { FaEnvelope, FaLock, FaExclamationTriangle } from "react-icons/fa";
import AccountNav from "../../../components/account/AccountNav";
import { Button } from "../../../components/ui/button";
import { Input, PasswordInput } from "../../../components/ui/input";
import { changeUserEmail } from "../../api/services/user";

const emailChangeSchema = z
  .object({
    newEmail: z
      .string()
      .min(1, "New email is required")
      .email("Please enter a valid email address"),
    confirmEmail: z.string().min(1, "Please confirm your new email"),
    currentPassword: z.string().min(1, "Password is required to change email"),
  })
  .refine((data) => data.newEmail === data.confirmEmail, {
    message: "Email addresses don&apos;t match",
    path: ["confirmEmail"],
  });

type EmailChangeFormValues = z.infer<typeof emailChangeSchema>;

export default function EmailChangePage() {
  const { user, isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error" | "warning";
    text: string;
  } | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<EmailChangeFormValues>({
    resolver: zodResolver(emailChangeSchema),
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
  }, [isAuthenticated, authLoading, router]);

  const onSubmit = async (data: EmailChangeFormValues) => {
    try {
      setLoading(true);
      setMessage(null);

      await changeUserEmail(data.newEmail, data.currentPassword);

      setMessage({
        type: "success",
        text: "Email change request submitted successfully! Please check both your old and new email addresses for verification instructions.",
      });
      reset();
    } catch (error: unknown) {
      console.error("Error changing email:", error);
      const errorMessage =
        error instanceof Error && "response" in error
          ? (error as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to change email. Please try again.";
      setMessage({
        type: "error",
        text: errorMessage || "Failed to change email. Please try again.",
      });
    } finally {
      setLoading(false);
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
            <h1 className="text-2xl font-bold text-gray-900">
              Change Email Address
            </h1>
            <p className="text-gray-600">
              Update your email address for your account
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="security" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">
              <div className="bg-white rounded-lg shadow-sm border p-6">
                {/* Warning Notice */}
                <div className="mb-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <div className="flex items-start gap-3">
                    <FaExclamationTriangle className="w-5 h-5 text-yellow-600 mt-0.5" />
                    <div>
                      <h4 className="font-medium text-yellow-800 mb-1">
                        Important Information
                      </h4>
                      <div className="text-sm text-yellow-700 space-y-1">
                        <p>
                          • Changing your email address will require
                          verification from both your current and new email
                          addresses.
                        </p>
                        <p>
                          • You will receive verification emails at both
                          addresses.
                        </p>
                        <p>
                          • Your email change will not be active until both
                          verifications are completed.
                        </p>
                        <p>
                          • You will need to use your current email address to
                          log in until the change is verified.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Current Email Display */}
                <div className="mb-8">
                  <h3 className="text-lg font-medium text-gray-900 mb-4">
                    Current Email Address
                  </h3>
                  <div className="flex items-center gap-3 p-4 bg-gray-50 rounded-lg">
                    <FaEnvelope className="w-5 h-5 text-gray-400" />
                    <span className="font-medium text-gray-900">
                      {user?.email}
                    </span>
                  </div>
                </div>

                {/* Message Display */}
                {message && (
                  <div
                    className={`mb-6 p-4 rounded-md ${
                      message.type === "success"
                        ? "bg-green-50 text-green-700 border border-green-200"
                        : message.type === "warning"
                        ? "bg-yellow-50 text-yellow-700 border border-yellow-200"
                        : "bg-red-50 text-red-700 border border-red-200"
                    }`}
                  >
                    {message.text}
                  </div>
                )}

                {/* Email Change Form */}
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                  <h3 className="text-lg font-medium text-gray-900">
                    Change to New Email
                  </h3>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      <FaEnvelope className="inline w-4 h-4 mr-2" />
                      New Email Address
                    </label>
                    <Input
                      {...register("newEmail")}
                      type="email"
                      placeholder="Enter your new email address"
                      error={errors.newEmail?.message}
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      <FaEnvelope className="inline w-4 h-4 mr-2" />
                      Confirm New Email Address
                    </label>
                    <Input
                      {...register("confirmEmail")}
                      type="email"
                      placeholder="Confirm your new email address"
                      error={errors.confirmEmail?.message}
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      <FaLock className="inline w-4 h-4 mr-2" />
                      Current Password
                    </label>
                    <PasswordInput
                      {...register("currentPassword")}
                      placeholder="Enter your current password to verify"
                      error={errors.currentPassword?.message}
                    />
                    <p className="text-sm text-gray-500 mt-1">
                      Required for security verification
                    </p>
                  </div>

                  <div className="flex justify-end gap-4 pt-6 border-t">
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => router.back()}
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      loading={loading}
                      disabled={!isDirty || loading}
                    >
                      {loading ? "Processing..." : "Change Email"}
                    </Button>
                  </div>
                </form>

                {/* Help Section */}
                <div className="mt-8 pt-6 border-t border-gray-200">
                  <h4 className="font-medium text-gray-900 mb-3">Need Help?</h4>
                  <div className="text-sm text-gray-600 space-y-2">
                    <p>
                      • If you don&apos;t receive the verification emails, check
                      your spam folder
                    </p>
                    <p>
                      • Make sure both email addresses are accessible to you
                    </p>
                    <p>
                      • If you encounter issues, please contact our support team
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
