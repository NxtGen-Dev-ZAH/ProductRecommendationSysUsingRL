"use client";

import { useEffect, useState, useCallback } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  FaUser,
  FaMapMarkerAlt,
  FaHeart,
  FaUserPlus,
  FaUserMinus,
  FaShoppingBag,
} from "react-icons/fa";
import Image from "next/image";
import { useAuth } from "../../context/AuthContext";
import { Button } from "../../../components/ui/button";
import {
  getPublicProfile,
  getPublicProfileFavorites,
  getPublicProfileFollowerCount,
  getPublicProfileFollowingCount,
  toggleFollowUser,
  PublicProfile,
  Product,
} from "../../api/services/user";
import ProductCard from "../../../components/ProductCard";

export default function PublicProfilePage() {
  const params = useParams();
  const router = useRouter();
  const { isAuthenticated, user } = useAuth();
  const email = params.email as string;

  const [profile, setProfile] = useState<PublicProfile | null>(null);
  const [favoriteProducts, setFavoriteProducts] = useState<Product[]>([]);
  const [followerCount, setFollowerCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [followLoading, setFollowLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<"favorites" | "info">("favorites");
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  const loadProfileData = useCallback(async () => {
    try {
      setLoading(true);
      const [profileData, favoritesData, followerData, followingData] =
        await Promise.all([
          getPublicProfile(decodeURIComponent(email)),
          getPublicProfileFavorites(decodeURIComponent(email), 0, 12),
          getPublicProfileFollowerCount(decodeURIComponent(email)),
          getPublicProfileFollowingCount(decodeURIComponent(email)),
        ]);

      setProfile(profileData);
      setFavoriteProducts(favoritesData.content);
      setFollowerCount(followerData.count);
      setFollowingCount(followingData.count);
    } catch (error) {
      console.error("Error loading profile data:", error);
      setMessage({ type: "error", text: "Failed to load profile data" });
    } finally {
      setLoading(false);
    }
  }, [email]);

  useEffect(() => {
    if (email) {
      loadProfileData();
    }
  }, [email, loadProfileData]);

  const handleFollowToggle = async () => {
    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    try {
      setFollowLoading(true);
      const result = await toggleFollowUser(decodeURIComponent(email));

      setProfile((prev) =>
        prev ? { ...prev, isFollowing: result.isFollowing } : null
      );
      setFollowerCount((prev) => (result.isFollowing ? prev + 1 : prev - 1));
      setMessage({
        type: "success",
        text: result.message,
      });
    } catch (error) {
      console.error("Error toggling follow:", error);
      setMessage({ type: "error", text: "Failed to update follow status" });
    } finally {
      setFollowLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center">
        <FaUser className="w-16 h-16 text-gray-400 mb-4" />
        <h1 className="text-2xl font-bold text-gray-900 mb-2">
          Profile Not Found
        </h1>
        <p className="text-gray-600 mb-6">
          The profile you&apos;re looking for doesn&apos;t exist or is private.
        </p>
        <Button onClick={() => router.back()}>Go Back</Button>
      </div>
    );
  }

  const isOwnProfile = user?.email === decodeURIComponent(email);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-4xl mx-auto">
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

          {/* Profile Header */}
          <div className="bg-white rounded-lg shadow-sm border p-8 mb-6">
            <div className="flex flex-col md:flex-row items-start md:items-center gap-6">
              {/* Profile Picture */}
              <div className="w-24 h-24 md:w-32 md:h-32 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden flex-shrink-0">
                {profile.profilePictureUrl ? (
                  <Image
                    src={profile.profilePictureUrl}
                    alt={`${profile.firstName} ${profile.lastName}`}
                    width={128}
                    height={128}
                    className="w-full h-full object-cover"
                  />
                ) : (
                  <FaUser className="w-12 h-12 md:w-16 md:h-16 text-gray-400" />
                )}
              </div>

              {/* Profile Info */}
              <div className="flex-1">
                <h1 className="text-2xl md:text-3xl font-bold text-gray-900 mb-2">
                  {profile.firstName} {profile.lastName}
                </h1>

                {profile.location && (
                  <div className="flex items-center gap-2 text-gray-600 mb-4">
                    <FaMapMarkerAlt className="w-4 h-4" />
                    <span>{profile.location}</span>
                  </div>
                )}

                {/* Stats */}
                <div className="flex gap-6 mb-4">
                  <div className="text-center">
                    <div className="font-bold text-xl text-gray-900">
                      {followerCount}
                    </div>
                    <div className="text-sm text-gray-600">Followers</div>
                  </div>
                  <div className="text-center">
                    <div className="font-bold text-xl text-gray-900">
                      {followingCount}
                    </div>
                    <div className="text-sm text-gray-600">Following</div>
                  </div>
                  <div className="text-center">
                    <div className="font-bold text-xl text-gray-900">
                      {profile.favoriteProductsCount}
                    </div>
                    <div className="text-sm text-gray-600">Favorites</div>
                  </div>
                </div>

                {/* Actions */}
                <div className="flex gap-3">
                  {isOwnProfile ? (
                    <Button onClick={() => router.push("/account/profile")}>
                      Edit Profile
                    </Button>
                  ) : isAuthenticated ? (
                    <Button
                      onClick={handleFollowToggle}
                      loading={followLoading}
                      variant={profile.isFollowing ? "outline" : "default"}
                      className={
                        profile.isFollowing
                          ? "border-red-300 text-red-600 hover:bg-red-50"
                          : ""
                      }
                    >
                      {profile.isFollowing ? (
                        <>
                          <FaUserMinus className="w-4 h-4 mr-2" />
                          Unfollow
                        </>
                      ) : (
                        <>
                          <FaUserPlus className="w-4 h-4 mr-2" />
                          Follow
                        </>
                      )}
                    </Button>
                  ) : (
                    <Button onClick={() => router.push("/auth/login")}>
                      <FaUserPlus className="w-4 h-4 mr-2" />
                      Follow
                    </Button>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Content Tabs */}
          <div className="bg-white rounded-lg shadow-sm border">
            {/* Tab Navigation */}
            <div className="border-b border-gray-200">
              <nav className="flex space-x-8 px-6">
                <button
                  onClick={() => setActiveTab("favorites")}
                  className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                    activeTab === "favorites"
                      ? "border-blue-500 text-blue-600"
                      : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                  }`}
                >
                  <FaHeart className="w-4 h-4 inline mr-2" />
                  Favorite Products ({favoriteProducts.length})
                </button>
                <button
                  onClick={() => setActiveTab("info")}
                  className={`py-4 px-1 border-b-2 font-medium text-sm transition-colors ${
                    activeTab === "info"
                      ? "border-blue-500 text-blue-600"
                      : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                  }`}
                >
                  <FaUser className="w-4 h-4 inline mr-2" />
                  Profile Info
                </button>
              </nav>
            </div>

            {/* Tab Content */}
            <div className="p-6">
              {activeTab === "favorites" ? (
                <div>
                  {favoriteProducts.length > 0 ? (
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
                      {favoriteProducts.map((product) => (
                        <ProductCard
                          key={product.id}
                          product={{
                            ...product,
                            quantity: 0,
                            categoryId: 0,
                            discountPercentage: 0,
                            rating: 0,
                            reviewCount: 0,
                            vendor: {
                              id: product.sellerId,
                              name: product.sellerName || "Unknown Vendor",
                            },
                          }}
                        />
                      ))}
                    </div>
                  ) : (
                    <div className="text-center py-12">
                      <FaShoppingBag className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                      <h3 className="text-lg font-medium text-gray-900 mb-2">
                        No Favorite Products
                      </h3>
                      <p className="text-gray-600">
                        {isOwnProfile
                          ? "You haven't favorited any products yet."
                          : "This user hasn't favorited any products yet."}
                      </p>
                    </div>
                  )}
                </div>
              ) : (
                <div className="max-w-2xl">
                  <h3 className="text-lg font-medium text-gray-900 mb-4">
                    Profile Information
                  </h3>
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">
                        Name
                      </label>
                      <p className="text-gray-900">
                        {profile.firstName} {profile.lastName}
                      </p>
                    </div>
                    {profile.location && (
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Location
                        </label>
                        <p className="text-gray-900">{profile.location}</p>
                      </div>
                    )}
                    {profile.joinDate && (
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">
                          Member Since
                        </label>
                        <p className="text-gray-900">
                          {new Date(profile.joinDate).toLocaleDateString()}
                        </p>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
