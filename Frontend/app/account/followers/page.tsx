"use client";

import { useAuth } from "../../context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect, useState, useCallback } from "react";
import {
  FaUser,
  FaUserPlus,
  FaUserMinus,
  FaSearch,
  FaUsers,
} from "react-icons/fa";
import Image from "next/image";
import Link from "next/link";
import AccountNav from "../../../components/account/AccountNav";
import { Button } from "../../../components/ui/button";
import { Input } from "../../../components/ui/input";
import {
  getUserFollowers,
  getUserFollowing,
  getFollowersCount,
  getFollowingCount,
  toggleFollowUser,
  UserFollowInfo,
} from "../../api/services/user";

type TabType = "followers" | "following";

export default function FollowersPage() {
  const { user, isAuthenticated, loading: authLoading } = useAuth();
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<TabType>("followers");
  const [followers, setFollowers] = useState<UserFollowInfo[]>([]);
  const [following, setFollowing] = useState<UserFollowInfo[]>([]);
  const [followersCount, setFollowersCount] = useState(0);
  const [followingCount, setFollowingCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState("");
  const [followingUsers, setFollowingUsers] = useState<Set<number>>(new Set());
  const [followLoading, setFollowLoading] = useState<Set<number>>(new Set());

  const loadData = useCallback(async () => {
    if (!user) return;

    try {
      setLoading(true);
      const [
        followersData,
        followingData,
        followersCountData,
        followingCountData,
      ] = await Promise.all([
        getUserFollowers(user.id, 0, 50),
        getUserFollowing(user.id, 0, 50),
        getFollowersCount(user.id),
        getFollowingCount(user.id),
      ]);

      setFollowers(followersData.content);
      setFollowing(followingData.content);
      setFollowersCount(followersCountData.count);
      setFollowingCount(followingCountData.count);

      // Create a set of users that the current user is following
      const followingSet = new Set(followingData.content.map((u) => u.id));
      setFollowingUsers(followingSet);
    } catch (error) {
      console.error("Error loading followers/following data:", error);
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => {
    // Wait for auth to finish loading before checking authentication
    if (authLoading) {
      return;
    }

    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    loadData();
  }, [isAuthenticated, authLoading, router, loadData]);

  const handleFollowToggle = async (targetUser: UserFollowInfo) => {
    try {
      setFollowLoading((prev) => new Set([...prev, targetUser.id]));

      const result = await toggleFollowUser(targetUser.email);

      // Update local state
      if (result.isFollowing) {
        setFollowingUsers((prev) => new Set([...prev, targetUser.id]));
        setFollowingCount((prev) => prev + 1);
      } else {
        setFollowingUsers((prev) => {
          const newSet = new Set(prev);
          newSet.delete(targetUser.id);
          return newSet;
        });
        setFollowingCount((prev) => prev - 1);

        // If we unfollowed someone from the following tab, remove them from the list
        if (activeTab === "following") {
          setFollowing((prev) => prev.filter((u) => u.id !== targetUser.id));
        }
      }
    } catch (error) {
      console.error("Error toggling follow:", error);
    } finally {
      setFollowLoading((prev) => {
        const newSet = new Set(prev);
        newSet.delete(targetUser.id);
        return newSet;
      });
    }
  };

  const filteredUsers = (users: UserFollowInfo[]) => {
    if (!searchQuery) return users;
    return users.filter(
      (user) =>
        `${user.firstName} ${user.lastName}`
          .toLowerCase()
          .includes(searchQuery.toLowerCase()) ||
        user.email.toLowerCase().includes(searchQuery.toLowerCase())
    );
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
              Social Connections
            </h1>
            <p className="text-gray-600">
              Manage your followers and people you follow
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="followers" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3">
              <div className="bg-white rounded-lg shadow-sm border">
                {/* Tab Navigation */}
                <div className="border-b border-gray-200">
                  <div className="flex items-center justify-between px-6 py-4">
                    <nav className="flex space-x-8">
                      <button
                        onClick={() => setActiveTab("followers")}
                        className={`flex items-center gap-2 py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                          activeTab === "followers"
                            ? "border-blue-500 text-blue-600"
                            : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                        }`}
                      >
                        <FaUsers className="w-4 h-4" />
                        Followers ({followersCount})
                      </button>
                      <button
                        onClick={() => setActiveTab("following")}
                        className={`flex items-center gap-2 py-2 px-1 border-b-2 font-medium text-sm transition-colors ${
                          activeTab === "following"
                            ? "border-blue-500 text-blue-600"
                            : "border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300"
                        }`}
                      >
                        <FaUserPlus className="w-4 h-4" />
                        Following ({followingCount})
                      </button>
                    </nav>

                    {/* Search */}
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <FaSearch className="h-4 w-4 text-gray-400" />
                      </div>
                      <Input
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="Search users..."
                        className="pl-10 w-64"
                      />
                    </div>
                  </div>
                </div>

                {/* Content */}
                <div className="p-6">
                  {loading ? (
                    <div className="space-y-4">
                      {[...Array(5)].map((_, index) => (
                        <div
                          key={index}
                          className="animate-pulse flex items-center space-x-4"
                        >
                          <div className="w-12 h-12 bg-gray-200 rounded-full"></div>
                          <div className="flex-1 space-y-2">
                            <div className="h-4 bg-gray-200 rounded w-1/4"></div>
                            <div className="h-3 bg-gray-200 rounded w-1/3"></div>
                          </div>
                          <div className="w-20 h-8 bg-gray-200 rounded"></div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <>
                      {activeTab === "followers" ? (
                        <div>
                          <h3 className="text-lg font-medium text-gray-900 mb-6">
                            Your Followers ({followersCount})
                          </h3>
                          {filteredUsers(followers).length > 0 ? (
                            <div className="space-y-4">
                              {filteredUsers(followers).map((follower) => (
                                <div
                                  key={follower.id}
                                  className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:border-gray-300 transition-colors"
                                >
                                  <div className="flex items-center gap-4">
                                    <Link
                                      href={`/profile/${encodeURIComponent(
                                        follower.email
                                      )}`}
                                      className="flex-shrink-0"
                                    >
                                      <div className="w-12 h-12 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden">
                                        {follower.profilePictureUrl ? (
                                          <Image
                                            src={follower.profilePictureUrl}
                                            alt={`${follower.firstName} ${follower.lastName}`}
                                            width={48}
                                            height={48}
                                            className="w-full h-full object-cover"
                                          />
                                        ) : (
                                          <FaUser className="w-6 h-6 text-gray-400" />
                                        )}
                                      </div>
                                    </Link>
                                    <div>
                                      <Link
                                        href={`/profile/${encodeURIComponent(
                                          follower.email
                                        )}`}
                                        className="font-medium text-gray-900 hover:text-blue-600 transition-colors"
                                      >
                                        {follower.firstName} {follower.lastName}
                                      </Link>
                                      <p className="text-sm text-gray-600">
                                        {follower.email}
                                      </p>
                                      {follower.followDate && (
                                        <p className="text-xs text-gray-500">
                                          Following since{" "}
                                          {new Date(
                                            follower.followDate
                                          ).toLocaleDateString()}
                                        </p>
                                      )}
                                    </div>
                                  </div>
                                  <Button
                                    onClick={() => handleFollowToggle(follower)}
                                    loading={followLoading.has(follower.id)}
                                    variant={
                                      followingUsers.has(follower.id)
                                        ? "outline"
                                        : "default"
                                    }
                                    size="sm"
                                    className={
                                      followingUsers.has(follower.id)
                                        ? "border-red-300 text-red-600 hover:bg-red-50"
                                        : ""
                                    }
                                  >
                                    {followingUsers.has(follower.id) ? (
                                      <>
                                        <FaUserMinus className="w-4 h-4 mr-2" />
                                        Unfollow
                                      </>
                                    ) : (
                                      <>
                                        <FaUserPlus className="w-4 h-4 mr-2" />
                                        Follow Back
                                      </>
                                    )}
                                  </Button>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <div className="text-center py-12">
                              <FaUsers className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                              <h4 className="text-lg font-medium text-gray-900 mb-2">
                                {searchQuery
                                  ? "No followers found"
                                  : "No followers yet"}
                              </h4>
                              <p className="text-gray-600">
                                {searchQuery
                                  ? "Try adjusting your search terms"
                                  : "When people follow you, they'll appear here"}
                              </p>
                            </div>
                          )}
                        </div>
                      ) : (
                        <div>
                          <h3 className="text-lg font-medium text-gray-900 mb-6">
                            People You Follow ({followingCount})
                          </h3>
                          {filteredUsers(following).length > 0 ? (
                            <div className="space-y-4">
                              {filteredUsers(following).map((followedUser) => (
                                <div
                                  key={followedUser.id}
                                  className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:border-gray-300 transition-colors"
                                >
                                  <div className="flex items-center gap-4">
                                    <Link
                                      href={`/profile/${encodeURIComponent(
                                        followedUser.email
                                      )}`}
                                      className="flex-shrink-0"
                                    >
                                      <div className="w-12 h-12 rounded-full bg-gray-200 flex items-center justify-center overflow-hidden">
                                        {followedUser.profilePictureUrl ? (
                                          <Image
                                            src={followedUser.profilePictureUrl}
                                            alt={`${followedUser.firstName} ${followedUser.lastName}`}
                                            width={48}
                                            height={48}
                                            className="w-full h-full object-cover"
                                          />
                                        ) : (
                                          <FaUser className="w-6 h-6 text-gray-400" />
                                        )}
                                      </div>
                                    </Link>
                                    <div>
                                      <Link
                                        href={`/profile/${encodeURIComponent(
                                          followedUser.email
                                        )}`}
                                        className="font-medium text-gray-900 hover:text-blue-600 transition-colors"
                                      >
                                        {followedUser.firstName}{" "}
                                        {followedUser.lastName}
                                      </Link>
                                      <p className="text-sm text-gray-600">
                                        {followedUser.email}
                                      </p>
                                      {followedUser.followDate && (
                                        <p className="text-xs text-gray-500">
                                          Following since{" "}
                                          {new Date(
                                            followedUser.followDate
                                          ).toLocaleDateString()}
                                        </p>
                                      )}
                                    </div>
                                  </div>
                                  <Button
                                    onClick={() =>
                                      handleFollowToggle(followedUser)
                                    }
                                    loading={followLoading.has(followedUser.id)}
                                    variant="outline"
                                    size="sm"
                                    className="border-red-300 text-red-600 hover:bg-red-50"
                                  >
                                    <FaUserMinus className="w-4 h-4 mr-2" />
                                    Unfollow
                                  </Button>
                                </div>
                              ))}
                            </div>
                          ) : (
                            <div className="text-center py-12">
                              <FaUserPlus className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                              <h4 className="text-lg font-medium text-gray-900 mb-2">
                                {searchQuery
                                  ? "No users found"
                                  : "Not following anyone yet"}
                              </h4>
                              <p className="text-gray-600 mb-6">
                                {searchQuery
                                  ? "Try adjusting your search terms"
                                  : "Start following other users to see their updates and connect with them"}
                              </p>
                              {!searchQuery && (
                                <Button onClick={() => router.push("/search")}>
                                  Discover Users
                                </Button>
                              )}
                            </div>
                          )}
                        </div>
                      )}
                    </>
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
