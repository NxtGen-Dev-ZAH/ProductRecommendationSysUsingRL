import { useState } from "react";
import { refreshAuthToken, getStoredRefreshToken } from "../api/services/auth";

export const useTokenRefresh = () => {
  const [isRefreshing, setIsRefreshing] = useState(false);

  const refreshToken = async (): Promise<boolean> => {
    setIsRefreshing(true);
    try {
      const refreshTokenValue = getStoredRefreshToken();
      if (!refreshTokenValue) {
        throw new Error("No refresh token found");
      }

      // Refresh the token (this will get updated roles from the database)
      const authResponse = await refreshAuthToken(refreshTokenValue);
      
      // Store the new token
      localStorage.setItem("token", authResponse.token);
      if (authResponse.refreshToken) {
        localStorage.setItem("refreshToken", authResponse.refreshToken);
      }

      // Reload the page to update the auth context
      window.location.reload();
      return true;
    } catch (error) {
      console.error("Error refreshing token:", error);
      setIsRefreshing(false);
      return false;
    }
  };

  return { refreshToken, isRefreshing };
};






