"use client";

import { useAuth } from "../context/AuthContext";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

export default function BuyerDashboard() {
  const { user, isAuthenticated } = useAuth();
  const router = useRouter();

  // Redirect if not authenticated or not a buyer
  useEffect(() => {
    if (!isAuthenticated) {
      router.push("/auth/login");
    }
  }, [isAuthenticated, router]);

  return (
    <div className="bg-gray-50 min-h-screen">
      <div className="container mx-auto px-4 py-8">
        <header className="mb-8">
          <h1 className="text-3xl font-bold text-gray-800">My Account</h1>
          <p className="text-gray-600 mt-2">Welcome back, {user?.firstName}!</p>
        </header>

        {/* Quick Actions */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <QuickAccessCard
            title="My Orders"
            description="Track, return, or buy items again"
            icon={<OrderIcon />}
            link="/account/orders"
          />
          <QuickAccessCard
            title="My Wishlist"
            description="Items you've saved for later"
            icon={<HeartIcon />}
            link="/wishlist"
          />
          <QuickAccessCard
            title="Recently Viewed"
            description="Products you've looked at"
            icon={<EyeIcon />}
            link="/account/recently-viewed"
          />
        </div>

        {/* Orders and Recommendations */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Recent Orders */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-100">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-lg font-semibold">Recent Orders</h2>
                <Link
                  href="/account/orders"
                  className="text-blue-600 text-sm hover:underline"
                >
                  View All
                </Link>
              </div>

              {/* Sample orders - in a real app, these would come from an API */}
              <div className="space-y-4">
                <OrderItem
                  id="ORD-5123"
                  date="Oct 15, 2023"
                  status="Delivered"
                  items={2}
                  total="€124.00"
                  statusColor="bg-green-100 text-green-800"
                />
                <OrderItem
                  id="ORD-5120"
                  date="Oct 10, 2023"
                  status="Shipped"
                  items={1}
                  total="€85.50"
                  statusColor="bg-blue-100 text-blue-800"
                />
                <OrderItem
                  id="ORD-5115"
                  date="Oct 5, 2023"
                  status="Delivered"
                  items={3}
                  total="€210.75"
                  statusColor="bg-green-100 text-green-800"
                />
              </div>
            </div>
          </div>

          {/* Account Settings */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-100">
              <h2 className="text-lg font-semibold mb-4">Account Settings</h2>
              <div className="space-y-3">
                <SettingsLink
                  icon={<ProfileIcon />}
                  label="Personal Information"
                  href="/account/profile"
                />
                <SettingsLink
                  icon={<AddressIcon />}
                  label="Addresses"
                  href="/account/addresses"
                />
                <SettingsLink
                  icon={<PaymentIcon />}
                  label="Payment Methods"
                  href="/account/payment"
                />
                <SettingsLink
                  icon={<NotificationIcon />}
                  label="Notification Preferences"
                  href="/account/notifications"
                />
                <SettingsLink
                  icon={<SecurityIcon />}
                  label="Security Settings"
                  href="/account/security"
                />
              </div>

              {/* Become a Seller CTA */}
              <div className="mt-6 pt-6 border-t border-gray-100">
                <div className="bg-blue-50 rounded-lg p-4">
                  <h3 className="font-medium text-blue-800 mb-2">
                    Start selling on our platform
                  </h3>
                  <p className="text-sm text-blue-700 mb-3">
                    Turn your passion into profit by becoming a seller.
                  </p>
                  <Link
                    href="/become-seller"
                    className="inline-block w-full text-center bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors"
                  >
                    Become a Seller
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// Component for quick access cards
function QuickAccessCard({
  title,
  description,
  icon,
  link,
}: {
  title: string;
  description: string;
  icon: React.ReactNode;
  link: string;
}) {
  return (
    <Link href={link}>
      <div className="bg-white rounded-lg shadow-sm p-6 border border-gray-100 hover:shadow-md transition-shadow">
        <div className="flex items-start">
          <div className="bg-blue-50 p-3 rounded-lg mr-4">
            <div className="text-blue-600">{icon}</div>
          </div>
          <div>
            <h3 className="font-semibold text-gray-800">{title}</h3>
            <p className="text-sm text-gray-600 mt-1">{description}</p>
          </div>
        </div>
      </div>
    </Link>
  );
}

// Component for order items
function OrderItem({
  id,
  date,
  status,
  items,
  total,
  statusColor,
}: {
  id: string;
  date: string;
  status: string;
  items: number;
  total: string;
  statusColor: string;
}) {
  return (
    <Link href={`/account/orders/${id}`}>
      <div className="flex items-center justify-between p-4 border border-gray-100 rounded-md hover:bg-gray-50">
        <div>
          <p className="font-medium">{id}</p>
          <p className="text-sm text-gray-600">{date}</p>
          <p className="text-xs text-gray-500">
            {items} item{items !== 1 ? "s" : ""}
          </p>
        </div>
        <div className="text-right">
          <p className="font-medium">{total}</p>
          <span
            className={`inline-block px-2 py-1 text-xs font-medium rounded-full mt-1 ${statusColor}`}
          >
            {status}
          </span>
        </div>
      </div>
    </Link>
  );
}

// Component for settings links
function SettingsLink({
  icon,
  label,
  href,
}: {
  icon: React.ReactNode;
  label: string;
  href: string;
}) {
  return (
    <Link href={href}>
      <div className="flex items-center p-3 hover:bg-gray-50 rounded-md transition-colors">
        <div className="text-gray-500 mr-3">{icon}</div>
        <span className="text-gray-700">{label}</span>
      </div>
    </Link>
  );
}

// Icons
function OrderIcon() {
  return (
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
        d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"
      />
    </svg>
  );
}

function HeartIcon() {
  return (
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
        d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
      />
    </svg>
  );
}

function EyeIcon() {
  return (
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
        d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"
      />
    </svg>
  );
}

function ProfileIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className="h-5 w-5"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
      />
    </svg>
  );
}

function AddressIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className="h-5 w-5"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
      />
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
      />
    </svg>
  );
}

function PaymentIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className="h-5 w-5"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"
      />
    </svg>
  );
}

function NotificationIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className="h-5 w-5"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
      />
    </svg>
  );
}

function SecurityIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      className="h-5 w-5"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth={2}
        d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
      />
    </svg>
  );
}
