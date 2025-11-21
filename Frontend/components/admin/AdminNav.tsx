"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  FaHome,
  FaUsers,
  FaBox,
  FaFolder,
  FaShoppingCart,
  FaChartLine,
  FaCog,
  FaShieldAlt,
  FaBell,
  FaUserTie,
  FaDatabase,
} from "react-icons/fa";

interface AdminNavProps {
  activeItem?: string;
}

export default function AdminNav({ activeItem }: AdminNavProps) {
  const pathname = usePathname();

  const navItems = [
    {
      key: "dashboard",
      label: "Dashboard",
      href: "/admin",
      icon: <FaHome className="w-5 h-5" />,
    },
    {
      key: "users",
      label: "User Management",
      href: "/admin/users",
      icon: <FaUsers className="w-5 h-5" />,
    },
    {
      key: "products",
      label: "Product Management",
      href: "/admin/products",
      icon: <FaBox className="w-5 h-5" />,
    },
    {
      key: "categories",
      label: "Category Management",
      href: "/admin/categories",
      icon: <FaFolder className="w-5 h-5" />,
    },
    {
      key: "orders",
      label: "Order Management",
      href: "/admin/orders",
      icon: <FaShoppingCart className="w-5 h-5" />,
    },
    {
      key: "analytics",
      label: "Analytics",
      href: "/admin/analytics",
      icon: <FaChartLine className="w-5 h-5" />,
    },
    {
      key: "roles",
      label: "Role Management",
      href: "/admin/roles",
      icon: <FaUserTie className="w-5 h-5" />,
    },
    {
      key: "system",
      label: "System Management",
      href: "/admin/system",
      icon: <FaDatabase className="w-5 h-5" />,
    },
    {
      key: "notifications",
      label: "Notifications",
      href: "/admin/notifications",
      icon: <FaBell className="w-5 h-5" />,
    },
    {
      key: "security",
      label: "Security",
      href: "/admin/security",
      icon: <FaShieldAlt className="w-5 h-5" />,
    },
    {
      key: "settings",
      label: "Settings",
      href: "/admin/settings",
      icon: <FaCog className="w-5 h-5" />,
    },
  ];

  const isActive = (href: string, key: string) => {
    if (activeItem) {
      return activeItem === key;
    }
    return pathname === href || pathname.startsWith(href + "/");
  };

  return (
    <nav className="bg-white rounded-lg shadow-sm border">
      <div className="p-4">
        <h3 className="text-sm font-medium text-gray-900 mb-3">Admin Panel</h3>
        <ul className="space-y-1">
          {navItems.map((item) => (
            <li key={item.key}>
              <Link
                href={item.href}
                className={`flex items-center gap-3 px-3 py-2 text-sm rounded-md transition-colors ${
                  isActive(item.href, item.key)
                    ? "bg-red-50 text-red-700 border-r-2 border-red-700"
                    : "text-gray-700 hover:bg-gray-50 hover:text-gray-900"
                }`}
              >
                <span
                  className={
                    isActive(item.href, item.key)
                      ? "text-red-700"
                      : "text-gray-400"
                  }
                >
                  {item.icon}
                </span>
                {item.label}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </nav>
  );
}
