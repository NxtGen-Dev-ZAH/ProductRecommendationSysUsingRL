"use client";

import { useState, useEffect, Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { PasswordInput } from "../../components/ui/input";
import { Button } from "../../components/ui/button";
import { resetPassword } from "../api/services/auth";

const resetPasswordSchema = z
  .object({
    password: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .regex(/[A-Z]/, "Password must contain at least one capital letter")
      .regex(/[0-9]/, "Password must contain at least one number")
      .regex(
        /[^A-Za-z0-9]/,
        "Password must contain at least one special character"
      ),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ["confirmPassword"],
  });

type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;

function ResetPassword() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tokenValid, setTokenValid] = useState<boolean>(!!token);

  // Check if token exists
  useEffect(() => {
    if (!token) {
      setError(
        "Invalid or missing reset token. Please try again with a valid reset link."
      );
      setTokenValid(false);
    }
  }, [token]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ResetPasswordFormValues>({
    resolver: zodResolver(resetPasswordSchema),
  });

  const onSubmit = async (data: ResetPasswordFormValues) => {
    if (!token) {
      setError(
        "Invalid or missing reset token. Please try again with a valid reset link."
      );
      return;
    }

    try {
      setIsSubmitting(true);
      setError(null);

      // Call the auth service to reset the password with the token
      await resetPassword(token, data.password);

      setSuccess(true);

      // Redirect to login page after 3 seconds
      setTimeout(() => {
        router.push("/auth/login");
      }, 3000);
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error && "response" in err
          ? (err as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to reset password. This link may be expired or invalid.";
      
      // Provide more specific error messages
      if (errorMessage?.includes("Invalid token") || errorMessage?.includes("expired")) {
        setError("This reset link has expired or is invalid. Please request a new password reset link.");
      } else {
        setError(errorMessage || "Failed to reset password. Please try again or request a new reset link.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
          Create a new password
        </h2>
        <p className="mt-2 text-center text-sm text-gray-600">
          Your new password must be different from previous passwords.
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
              {error}
            </div>
          )}

          {success ? (
            <div className="text-center">
              <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-4">
                Password updated successfully!
              </div>
              <p className="text-gray-600 mb-4">
                Please login with your new password.
              </p>
              <div className="space-y-3">
                <Link
                  href="/auth/login"
                  className="inline-block w-full bg-[#3b82f6] text-white py-2 px-4 rounded-md hover:bg-[#3b82f6]-dark font-medium text-center"
                >
                  Go to Login
                </Link>
                <p className="text-sm text-gray-500">
                  You&apos;ll be redirected automatically in a few seconds...
                </p>
              </div>
            </div>
          ) : !tokenValid ? (
            <div className="text-center">
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
                Invalid or missing reset token
              </div>
              <p className="text-gray-600 mb-4">
                The password reset link is invalid or has expired. Please
                request a new password reset.
              </p>
              <Link
                href="/auth/forgot-password"
                className="text-[#3b82f6] hover:text-[#3b82f6]-dark font-medium"
              >
                Request a new reset link
              </Link>
            </div>
          ) : (
            <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
              <div>
                <label
                  htmlFor="password"
                  className="block text-sm font-medium text-gray-700"
                >
                  New Password
                </label>
                <div className="mt-1">
                  <PasswordInput
                    id="password"
                    autoComplete="new-password"
                    {...register("password")}
                    className={errors.password ? "border-red-500" : ""}
                  />
                  {errors.password && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.password.message}
                    </p>
                  )}
                </div>
                <div className="mt-2 text-xs text-gray-500">
                  <p>Password requirements:</p>
                  <ul className="list-disc pl-5 mt-1">
                    <li>At least 8 characters</li>
                    <li>At least one capital letter</li>
                    <li>At least one number</li>
                    <li>At least one special character</li>
                  </ul>
                </div>
              </div>

              <div>
                <label
                  htmlFor="confirmPassword"
                  className="block text-sm font-medium text-gray-700"
                >
                  Confirm New Password
                </label>
                <div className="mt-1">
                  <PasswordInput
                    id="confirmPassword"
                    autoComplete="new-password"
                    {...register("confirmPassword")}
                    className={errors.confirmPassword ? "border-red-500" : ""}
                  />
                  {errors.confirmPassword && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.confirmPassword.message}
                    </p>
                  )}
                </div>
              </div>

              <div>
                <Button
                  type="submit"
                  className="w-full flex justify-center py-2 px-4"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? "Resetting password..." : "Reset password"}
                </Button>
              </div>

              <div className="text-center mt-4">
                <Link
                  href="/auth/login"
                  className="text-[#3b82f6] hover:text-[#3b82f6]-dark font-medium"
                >
                  Back to login
                </Link>
              </div>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}

// Loading component for Suspense fallback
function ResetPasswordLoading() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
          Create a new password
        </h2>
        <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
          <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
            <div className="flex justify-center">
              <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Main export with Suspense boundary
export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<ResetPasswordLoading />}>
      <ResetPassword />
    </Suspense>
  );
}
