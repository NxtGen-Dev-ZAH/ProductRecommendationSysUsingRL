"use client";

import { useAuth } from "../../context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import {
  FaCamera,
  FaUser,
  FaEnvelope,
  FaPhone,
  FaMapMarkerAlt,
} from "react-icons/fa";
import AccountNav from "../../../components/account/AccountNav";
import Image from "next/image";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import {
  getCurrentUserProfile,
  updateUserProfile,
  uploadProfilePicture,
} from "../../api/services/user";

const profileSchema = z.object({
  firstName: z.string().min(1, "First name is required"),
  lastName: z.string().min(1, "Last name is required"),
  phoneNumber: z.string().optional(),
  location: z.string().optional(),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

export default function ProfilePage() {
  const { user, isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const [profilePicture, setProfilePicture] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors, isDirty },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
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

    // Load current user data
    const loadUserData = async () => {
      try {
        const userData = await getCurrentUserProfile();
        setValue("firstName", userData.firstName || "");
        setValue("lastName", userData.lastName || "");
        setValue("phoneNumber", userData.phoneNumber || "");
        setValue("location", userData.location || "");
        setProfilePicture(userData.profilePictureUrl || null);
      } catch (error) {
        console.error("Error loading user data:", error);
        setMessage({ type: "error", text: "Failed to load profile data" });
      }
    };

    loadUserData();
  }, [isAuthenticated, authLoading, router, setValue]);

  const onSubmit = async (data: ProfileFormValues) => {
    try {
      setLoading(true);
      setMessage(null);

      await updateUserProfile(data);
      setMessage({ type: "success", text: "Profile updated successfully!" });
    } catch (error) {
      console.error("Error updating profile:", error);
      setMessage({
        type: "error",
        text: "Failed to update profile. Please try again.",
      });
    } finally {
      setLoading(false);
    }
  };

  const handleProfilePictureChange = async (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith("image/")) {
      setMessage({ type: "error", text: "Please select an image file" });
      return;
    }

    // Validate file size (5MB)
    if (file.size > 5 * 1024 * 1024) {
      setMessage({ type: "error", text: "Image size must be less than 5MB" });
      return;
    }

    try {
      setUploading(true);
      setMessage(null);

      const result = await uploadProfilePicture(file);
      setProfilePicture(result.profilePictureUrl || null);
      setMessage({
        type: "success",
        text: "Profile picture updated successfully!",
      });
    } catch (error) {
      console.error("Error uploading profile picture:", error);
      setMessage({
        type: "error",
        text: "Failed to upload profile picture. Please try again.",
      });
    } finally {
      setUploading(false);
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
            <h1 className="text-2xl font-bold text-gray-900">Edit Profile</h1>
            <p className="text-gray-600">
              Update your personal information and profile picture
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="dashboard" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">
              <div className="bg-white rounded-lg shadow-sm border p-6">
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

                {/* Profile Picture Section */}
                <div className="mb-8">
                  <h3 className="text-lg font-medium text-gray-900 mb-4">
                    Profile Picture
                  </h3>
                  <div className="flex items-center gap-6">
                    <div className="relative">
                      <div className="w-24 h-24 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden">
                        {profilePicture ? (
                          <Image
                            src={profilePicture}
                            alt="Profile"
                            width={96}
                            height={96}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <FaUser className="w-8 h-8 text-gray-400" />
                        )}
                      </div>
                      {uploading && (
                        <div className="absolute inset-0 bg-black bg-opacity-50 rounded-full flex items-center justify-center">
                          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-white"></div>
                        </div>
                      )}
                    </div>
                    <div>
                      <label
                        htmlFor="profile-picture"
                        className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 cursor-pointer transition-colors"
                      >
                        <FaCamera className="w-4 h-4" />
                        {uploading ? "Uploading..." : "Change Picture"}
                      </label>
                      <input
                        id="profile-picture"
                        type="file"
                        accept="image/*"
                        onChange={handleProfilePictureChange}
                        className="hidden"
                        disabled={uploading}
                      />
                      <p className="text-sm text-gray-600 mt-2">
                        JPG, PNG or GIF. Max size 5MB.
                      </p>
                    </div>
                  </div>
                </div>

                {/* Profile Form */}
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
                  <h3 className="text-lg font-medium text-gray-900">
                    Personal Information
                  </h3>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        <FaUser className="inline w-4 h-4 mr-2" />
                        First Name
                      </label>
                      <Input
                        {...register("firstName")}
                        placeholder="Enter your first name"
                        error={errors.firstName?.message}
                      />
                    </div>

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        <FaUser className="inline w-4 h-4 mr-2" />
                        Last Name
                      </label>
                      <Input
                        {...register("lastName")}
                        placeholder="Enter your last name"
                        error={errors.lastName?.message}
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      <FaEnvelope className="inline w-4 h-4 mr-2" />
                      Email Address
                    </label>
                    <Input
                      value={user?.email || ""}
                      disabled
                      className="bg-gray-50"
                    />
                    <p className="text-sm text-gray-500 mt-1">
                      Email address cannot be changed. Contact support if
                      needed.
                    </p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      <FaPhone className="inline w-4 h-4 mr-2" />
                      Phone Number
                    </label>
                    <Input
                      {...register("phoneNumber")}
                      placeholder="Enter your phone number (optional)"
                      error={errors.phoneNumber?.message}
                    />
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      <FaMapMarkerAlt className="inline w-4 h-4 mr-2" />
                      Location
                    </label>
                    <Input
                      {...register("location")}
                      placeholder="Enter your location (optional)"
                      error={errors.location?.message}
                    />
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
                      {loading ? "Saving..." : "Save Changes"}
                    </Button>
                  </div>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
