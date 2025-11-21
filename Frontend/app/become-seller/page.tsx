"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../context/AuthContext";
import { useForm, SubmitHandler } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";

// Form validation schema
const sellerFormSchema = z.object({
  businessName: z
    .string()
    .min(3, "Business name must be at least 3 characters"),
  businessDescription: z
    .string()
    .min(20, "Please provide a more detailed description"),
  phoneNumber: z.string().min(10, "Please enter a valid phone number"),
  address: z.string().min(5, "Please enter a valid address"),
  taxId: z.string().optional(),
  website: z
    .string()
    .url("Please enter a valid URL")
    .optional()
    .or(z.literal("")),
  acceptTerms: z.boolean().refine((val) => val === true, {
    message: "You must accept the terms and conditions",
  }),
});

type SellerFormValues = z.infer<typeof sellerFormSchema>;

export default function BecomeSeller() {
  const { becomeSellerRequest } = useAuth();
  const router = useRouter();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [currentStep, setCurrentStep] = useState(1);
  const totalSteps = 3;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SellerFormValues>({
    resolver: zodResolver(sellerFormSchema),
    defaultValues: {
      businessName: "",
      businessDescription: "",
      phoneNumber: "",
      address: "",
      taxId: "",
      website: "",
      acceptTerms: false as unknown as true, // Type assertion to satisfy form initialization
    },
  });

  const onSubmit: SubmitHandler<SellerFormValues> = async (data) => {
    setIsSubmitting(true);
    try {
      // In a real implementation, you would send this data to your API
      console.log("Seller form data:", data);

      // Update user role
      await becomeSellerRequest();

      // Redirect to seller dashboard
      router.push("/seller");
    } catch (error) {
      console.error("Error submitting form:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const nextStep = () => {
    setCurrentStep((prev) => Math.min(prev + 1, totalSteps));
  };

  const prevStep = () => {
    setCurrentStep((prev) => Math.max(prev - 1, 1));
  };

  return (
    <div className="container mx-auto px-4 py-12 max-w-3xl">
      <div className="bg-white rounded-xl shadow-lg overflow-hidden">
        <div className="bg-gradient-to-r from-blue-600 to-blue-400 px-6 py-8 text-white">
          <h1 className="text-3xl font-bold">Become a Seller</h1>
          <p className="mt-2 opacity-90">
            Join our marketplace and start selling your products to thousands of
            customers.
          </p>
        </div>

        {/* Progress bar */}
        <div className="px-6 pt-6">
          <div className="flex items-center justify-between mb-2">
            {[1, 2, 3].map((step) => (
              <div key={step} className="flex flex-col items-center">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center ${
                    step === currentStep
                      ? "bg-blue-600 text-white"
                      : step < currentStep
                      ? "bg-green-500 text-white"
                      : "bg-gray-200 text-gray-600"
                  }`}
                >
                  {step < currentStep ? (
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      className="h-6 w-6"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M5 13l4 4L19 7"
                      />
                    </svg>
                  ) : (
                    step
                  )}
                </div>
                <span
                  className={`text-sm mt-1 ${
                    step === currentStep
                      ? "text-blue-600 font-medium"
                      : "text-gray-500"
                  }`}
                >
                  {step === 1
                    ? "Business Info"
                    : step === 2
                    ? "Store Details"
                    : "Review & Submit"}
                </span>
              </div>
            ))}
          </div>
          <div className="w-full bg-gray-200 h-1 rounded-full">
            <div
              className="bg-blue-600 h-1 rounded-full transition-all duration-300"
              style={{
                width: `${((currentStep - 1) / (totalSteps - 1)) * 100}%`,
              }}
            ></div>
          </div>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="p-6">
          {/* Step 1: Business Information */}
          {currentStep === 1 && (
            <div className="space-y-4">
              <h2 className="text-xl font-semibold text-gray-800">
                Business Information
              </h2>
              <p className="text-gray-600 mb-4">Tell us about your business</p>

              <div>
                <label
                  htmlFor="businessName"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Business Name *
                </label>
                <input
                  id="businessName"
                  type="text"
                  className={`w-full px-3 py-2 border rounded-md ${
                    errors.businessName ? "border-red-500" : "border-gray-300"
                  }`}
                  placeholder="Your business name"
                  {...register("businessName")}
                />
                {errors.businessName && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors.businessName.message}
                  </p>
                )}
              </div>

              <div>
                <label
                  htmlFor="businessDescription"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Business Description *
                </label>
                <textarea
                  id="businessDescription"
                  rows={4}
                  className={`w-full px-3 py-2 border rounded-md ${
                    errors.businessDescription
                      ? "border-red-500"
                      : "border-gray-300"
                  }`}
                  placeholder="Describe your business and what you sell"
                  {...register("businessDescription")}
                ></textarea>
                {errors.businessDescription && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors.businessDescription.message}
                  </p>
                )}
              </div>

              <div>
                <label
                  htmlFor="phoneNumber"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Phone Number *
                </label>
                <input
                  id="phoneNumber"
                  type="tel"
                  className={`w-full px-3 py-2 border rounded-md ${
                    errors.phoneNumber ? "border-red-500" : "border-gray-300"
                  }`}
                  placeholder="Your business phone number"
                  {...register("phoneNumber")}
                />
                {errors.phoneNumber && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors.phoneNumber.message}
                  </p>
                )}
              </div>
            </div>
          )}

          {/* Step 2: Store Details */}
          {currentStep === 2 && (
            <div className="space-y-4">
              <h2 className="text-xl font-semibold text-gray-800">
                Store Details
              </h2>
              <p className="text-gray-600 mb-4">Set up your store profile</p>

              <div>
                <label
                  htmlFor="address"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Business Address *
                </label>
                <input
                  id="address"
                  type="text"
                  className={`w-full px-3 py-2 border rounded-md ${
                    errors.address ? "border-red-500" : "border-gray-300"
                  }`}
                  placeholder="Your business address"
                  {...register("address")}
                />
                {errors.address && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors.address.message}
                  </p>
                )}
              </div>

              <div>
                <label
                  htmlFor="taxId"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Tax ID / Business Registration Number (Optional)
                </label>
                <input
                  id="taxId"
                  type="text"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md"
                  placeholder="Your tax ID or business registration number"
                  {...register("taxId")}
                />
              </div>

              <div>
                <label
                  htmlFor="website"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Website (Optional)
                </label>
                <input
                  id="website"
                  type="url"
                  className={`w-full px-3 py-2 border rounded-md ${
                    errors.website ? "border-red-500" : "border-gray-300"
                  }`}
                  placeholder="https://yourbusiness.com"
                  {...register("website")}
                />
                {errors.website && (
                  <p className="mt-1 text-sm text-red-600">
                    {errors.website.message}
                  </p>
                )}
              </div>
            </div>
          )}

          {/* Step 3: Review & Submit */}
          {currentStep === 3 && (
            <div className="space-y-4">
              <h2 className="text-xl font-semibold text-gray-800">
                Review & Submit
              </h2>
              <p className="text-gray-600 mb-4">
                Please review your information before submitting
              </p>

              <div className="bg-gray-50 p-4 rounded-lg">
                <p className="text-sm text-gray-600">
                  By becoming a seller on our platform, you agree to our{" "}
                  <a href="/terms" className="text-blue-600 hover:underline">
                    Terms of Service
                  </a>{" "}
                  and{" "}
                  <a
                    href="/seller-policy"
                    className="text-blue-600 hover:underline"
                  >
                    Seller Policy
                  </a>
                  . You will be responsible for maintaining accurate inventory,
                  fulfilling orders promptly, and providing excellent customer
                  service.
                </p>
              </div>

              <div className="flex items-center">
                <input
                  id="acceptTerms"
                  type="checkbox"
                  className="h-4 w-4 text-blue-600 border-gray-300 rounded"
                  {...register("acceptTerms")}
                />
                <label
                  htmlFor="acceptTerms"
                  className="ml-2 block text-sm text-gray-700"
                >
                  I agree to the terms and conditions *
                </label>
              </div>
              {errors.acceptTerms && (
                <p className="mt-1 text-sm text-red-600">
                  {errors.acceptTerms.message}
                </p>
              )}
            </div>
          )}

          <div className="mt-8 flex justify-between">
            {currentStep > 1 && (
              <button
                type="button"
                onClick={prevStep}
                className="px-4 py-2 border border-gray-300 text-gray-700 rounded-md hover:bg-gray-50"
              >
                Back
              </button>
            )}
            {currentStep < totalSteps ? (
              <button
                type="button"
                onClick={nextStep}
                className="ml-auto px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                Next
              </button>
            ) : (
              <button
                type="submit"
                disabled={isSubmitting}
                className="ml-auto px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 flex items-center"
              >
                {isSubmitting ? (
                  <>
                    <svg
                      className="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
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
                    Processing...
                  </>
                ) : (
                  "Submit Application"
                )}
              </button>
            )}
          </div>
        </form>
      </div>
    </div>
  );
}
