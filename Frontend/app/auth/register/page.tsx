"use client";

import { useState, Suspense, useEffect, useRef } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { FaGoogle } from "react-icons/fa";
import { useAuth } from "../../context/AuthContext";
import { Input, PasswordInput } from "../../../components/ui/input";
import { Button } from "../../../components/ui/button";
import { useToast } from "../../../components/ui/toast";

const registerSchema = z
  .object({
    firstName: z.string().min(2, "First name must be at least 2 characters"),
    lastName: z.string().min(2, "Last name must be at least 2 characters"),
    email: z.string().email("Please enter a valid email address"),
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

type RegisterFormValues = z.infer<typeof registerSchema>;

function RegisterContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const isVendor = searchParams.get("vendor") === "true";

  const { register: registerUser, loginWithGoogle, loading, error } = useAuth();
  const { addToast } = useToast();
  const previousErrorRef = useRef<string | null>(null);
  const [isMounted, setIsMounted] = useState(false);

  // Ensure we're on the client side before showing toasts (fix hydration issue)
  useEffect(() => {
    setIsMounted(true);
  }, []);

  // Handle errors from AuthContext via toast (only after mount to prevent hydration issues)
  useEffect(() => {
    if (isMounted && error && error !== previousErrorRef.current) {
      previousErrorRef.current = error;
      addToast({
        type: "error",
        message: error,
        duration: 5000,
      });
    }
  }, [error, addToast, isMounted]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      email: "",
      password: "",
      confirmPassword: "",
    },
  });

  const onSubmit = async (data: RegisterFormValues) => {
    try {
      // In a real app, we would add vendor data and perhaps redirect to a different onboarding flow
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const { confirmPassword, ...registerData } = data;
      // confirmPassword is not needed for the API call
      const result = await registerUser(registerData);

      if (
        "message" in result &&
        (!("token" in result) || result.token === null)
      ) {
        // Registration successful but requires activation
        if (isMounted) {
          addToast({
            type: "success",
            message: result.message + " Please check your email and then login.",
            duration: 5000,
          });
        }
        setTimeout(() => {
          router.push("/auth/login");
        }, 5000); // Redirect after 5 seconds to give user time to read
      } else if (
        "token" in result &&
        result.token !== null &&
        result.token !== undefined
      ) {
        // Registration successful and user is logged in (immediate activation)
        if (isMounted) {
          addToast({
            type: "success",
            message: "Registration successful! Redirecting...",
            duration: 3000,
          });
        }
        router.push(isVendor ? "/vendor/onboarding" : "/");
      } else {
        // Fallback case
        if (isMounted) {
          addToast({
            type: "success",
            message: "Registration successful! Please check your email to activate your account.",
            duration: 5000,
          });
        }
        setTimeout(() => {
          router.push("/auth/login");
        }, 5000);
      }
    } catch (err: unknown) {
      // Extract error message from various error formats
      let errorMessage = "Registration failed. Please try again.";
      
      if (err instanceof Error) {
        errorMessage = err.message;
      } else if (
        err &&
        typeof err === "object" &&
        "response" in err &&
        err.response &&
        typeof err.response === "object" &&
        "data" in err.response
      ) {
        const errorData = (err.response as { data?: { message?: string } }).data;
        if (errorData?.message) {
          // Clean up error message if it contains duplicate prefixes
          errorMessage = errorData.message.replace(/^USER_ALREADY_EXISTS:\s*/, "");
        }
      }

      if (isMounted) {
        addToast({
          type: "error",
          message: errorMessage,
          duration: 5000,
        });
      }
    }
  };

  const handleGoogleLogin = async () => {
    try {
      await loginWithGoogle();
      if (isMounted) {
        addToast({
          type: "success",
          message: "Successfully signed in with Google!",
          duration: 3000,
        });
      }
      router.push(isVendor ? "/vendor/onboarding" : "/");
    } catch (err: unknown) {
      const errorMessage =
        err instanceof Error
          ? err.message
          : "Failed to login with Google. Please try again.";
      if (isMounted) {
        addToast({
          type: "error",
          message: errorMessage,
          duration: 5000,
        });
      }
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
          {isVendor ? "Créez votre compte vendeur" : "Créez votre compte"}
        </h2>
        <p className="mt-2 text-center text-sm text-gray-600">
          Ou{" "}
          <Link
            href="/auth/login"
            className="font-medium text-[#3b82f6] hover:text-[#3b82f6]-dark"
          >
            connectez-vous à votre compte existant
          </Link>
        </p>
        <p className="mt-1 text-center text-xs text-gray-500">
          Après l&apos;inscription, vous recevrez un email d&apos;activation
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <form className="space-y-6" onSubmit={handleSubmit(onSubmit)}>
            <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
              <div>
                <label
                  htmlFor="firstName"
                  className="block text-sm font-medium text-gray-700"
                >
                  Prénom
                </label>
                <div className="mt-1">
                  <Input
                    id="firstName"
                    type="text"
                    autoComplete="given-name"
                    {...register("firstName")}
                    className={errors.firstName ? "border-red-500" : ""}
                  />
                  {errors.firstName && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.firstName.message}
                    </p>
                  )}
                </div>
              </div>

              <div>
                <label
                  htmlFor="lastName"
                  className="block text-sm font-medium text-gray-700"
                >
                  Nom
                </label>
                <div className="mt-1">
                  <Input
                    id="lastName"
                    type="text"
                    autoComplete="family-name"
                    {...register("lastName")}
                    className={errors.lastName ? "border-red-500" : ""}
                  />
                  {errors.lastName && (
                    <p className="mt-1 text-sm text-red-600">
                      {errors.lastName.message}
                    </p>
                  )}
                </div>
              </div>
            </div>

            <div>
              <label
                htmlFor="email"
                className="block text-sm font-medium text-gray-700"
              >
                Adresse e-mail
              </label>
              <div className="mt-1">
                <Input
                  id="email"
                  type="email"
                  autoComplete="email"
                  {...register("email")}
                  className={errors.email ? "border-red-500" : ""}
                />
                {errors.email && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors.email.message}
                  </p>
                )}
              </div>
            </div>

            <div>
              <label
                htmlFor="password"
                className="block text-sm font-medium text-gray-700"
              >
                Mot de passe
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
                <p>Exigences du mot de passe :</p>
                <ul className="list-disc pl-5 mt-1">
                  <li>Au moins 8 caractères</li>
                  <li>Au moins une lettre majuscule</li>
                  <li>Au moins un chiffre</li>
                  <li>Au moins un caractère spécial</li>
                </ul>
              </div>
            </div>

            <div>
              <label
                htmlFor="confirmPassword"
                className="block text-sm font-medium text-gray-700"
              >
                Confirmer le mot de passe
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

            <div className="flex items-center">
              <input
                id="terms"
                name="terms"
                type="checkbox"
                className="h-4 w-4 text-[#3b82f6] focus:ring-primary-light border-gray-300 rounded"
                required
              />
              <label
                htmlFor="terms"
                className="ml-2 block text-sm text-gray-900"
              >
                J&apos;accepte les{" "}
                <Link
                  href="/terms-of-service"
                  className="text-[#3b82f6] hover:text-[#3b82f6]-dark"
                >
                  Conditions d&apos;Utilisation
                </Link>{" "}
                et la{" "}
                <Link
                  href="/privacy-policy"
                  className="text-[#3b82f6] hover:text-[#3b82f6]-dark"
                >
                  Politique de Confidentialités
                </Link>
              </label>
            </div>

            <div>
              <Button
                type="submit"
                className="w-full flex justify-center py-2 px-4"
                disabled={loading}
              >
                {loading ? (
                  <div className="flex items-center">
                    <svg
                      className="animate-spin -ml-1 mr-3 h-5 w-5 text-white"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      ></circle>
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      ></path>
                    </svg>
                    Création du compte...
                  </div>
                ) : (
                  `Créer un ${isVendor ? "Compte Vendeur" : "Compte"}`
                )}
              </Button>
            </div>
          </form>

          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-300"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-white text-gray-500">
                  Ou continuer avec
                </span>
              </div>
            </div>

            <div className="mt-6">
              <button
                onClick={handleGoogleLogin}
                className="w-full flex items-center justify-center px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                disabled={loading}
              >
                <FaGoogle className="h-5 w-5 text-red-500 mr-2" />
                S&apos;inscrire avec Google
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Loading component for Suspense fallback
function RegisterLoading() {
  return (
    <div className="min-h-screen bg-gray-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
          Créez votre compte
        </h2>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <div className="animate-pulse space-y-6">
            <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
            <div className="h-10 bg-gray-200 rounded"></div>
            <div className="h-4 bg-gray-200 rounded w-1/2 mb-2"></div>
            <div className="h-10 bg-gray-200 rounded"></div>
            <div className="h-10 bg-gray-200 rounded"></div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Main export with Suspense boundary
export default function RegisterPage() {
  return (
    <Suspense fallback={<RegisterLoading />}>
      <RegisterContent />
    </Suspense>
  );
}
