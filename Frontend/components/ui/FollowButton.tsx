"use client";

import { useState } from "react";
import { FaUserPlus, FaUserMinus } from "react-icons/fa";
import { Button } from "./button";
import { toggleFollowUser } from "../../app/api/services/user";
import { useAuth } from "../../app/context/AuthContext";
import { useRouter } from "next/navigation";

interface FollowButtonProps {
  userEmail: string;
  isFollowing: boolean;
  onFollowChange?: (isFollowing: boolean) => void;
  size?: "sm" | "default" | "lg";
  variant?: "default" | "outline" | "ghost";
  showIcon?: boolean;
}

export function FollowButton({
  userEmail,
  isFollowing,
  onFollowChange,
  size = "default",
  variant = "default",
  showIcon = true,
}: FollowButtonProps) {
  const [loading, setLoading] = useState(false);
  const [followState, setFollowState] = useState(isFollowing);
  const { isAuthenticated } = useAuth();
  const router = useRouter();

  const handleFollowToggle = async () => {
    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }

    try {
      setLoading(true);
      const result = await toggleFollowUser(userEmail);

      setFollowState(result.isFollowing);
      onFollowChange?.(result.isFollowing);
    } catch (error) {
      console.error("Error toggling follow:", error);
      // Revert the optimistic update on error
      setFollowState(!followState);
    } finally {
      setLoading(false);
    }
  };

  const buttonVariant = followState
    ? variant === "default"
      ? "outline"
      : variant
    : variant;

  const buttonClassName =
    followState && variant !== "ghost"
      ? "border-red-300 text-red-600 hover:bg-red-50"
      : "";

  return (
    <Button
      onClick={handleFollowToggle}
      loading={loading}
      variant={buttonVariant}
      size={size}
      className={buttonClassName}
      disabled={loading}
    >
      {showIcon &&
        (followState ? (
          <FaUserMinus className="w-4 h-4 mr-2" />
        ) : (
          <FaUserPlus className="w-4 h-4 mr-2" />
        ))}
      {loading ? "..." : followState ? "Unfollow" : "Follow"}
    </Button>
  );
}
