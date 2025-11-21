"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  FaHome,
  FaBox,
  FaShoppingCart,
  FaChartLine,
  FaUsers,
  FaCog,
  FaBuilding,
  FaWarehouse,
  FaStar,
  FaUserTie,
  FaTicketAlt,
} from "react-icons/fa";

interface SellerNavProps {
  activeItem?: string;
}

export default function SellerNav({ activeItem }: SellerNavProps) {
  const pathname = usePathname();

  const navItems = [
    {
      key: "dashboard",
      label: "Dashboard",
      href: "/seller",
      icon: <FaHome className="w-5 h-5" />,
    },
    {
      key: "products",
      label: "Products",
      href: "/seller/products",
      icon: <FaBox className="w-5 h-5" />,
    },
    {
      key: "inventory",
      label: "Inventory",
      href: "/seller/inventory",
      icon: <FaWarehouse className="w-5 h-5" />,
    },
    {
      key: "orders",
      label: "Orders",
      href: "/seller/orders",
      icon: <FaShoppingCart className="w-5 h-5" />,
    },
    {
      key: "analytics",
      label: "Analytics",
      href: "/seller/analytics",
      icon: <FaChartLine className="w-5 h-5" />,
    },
    {
      key: "customers",
      label: "Customers",
      href: "/seller/customers",
      icon: <FaUsers className="w-5 h-5" />,
    },
    {
      key: "reviews",
      label: "Reviews",
      href: "/seller/reviews",
      icon: <FaStar className="w-5 h-5" />,
    },
    {
      key: "coupons",
      label: "Coupons",
      href: "/seller/coupons",
      icon: <FaTicketAlt className="w-5 h-5" />,
    },
    {
      key: "company",
      label: "Company",
      href: "/seller/company",
      icon: <FaBuilding className="w-5 h-5" />,
    },
    {
      key: "roles",
      label: "Role Management",
      href: "/seller/roles",
      icon: <FaUserTie className="w-5 h-5" />,
    },
    {
      key: "settings",
      label: "Settings",
      href: "/seller/settings",
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
        <h3 className="text-sm font-medium text-gray-900 mb-3">
          Seller Dashboard
        </h3>
        <ul className="space-y-1">
          {navItems.map((item) => (
            <li key={item.key}>
              <Link
                href={item.href}
                className={`flex items-center gap-3 px-3 py-2 text-sm rounded-md transition-colors ${
                  isActive(item.href, item.key)
                    ? "bg-blue-50 text-blue-700 border-r-2 border-blue-700"
                    : "text-gray-700 hover:bg-gray-50 hover:text-gray-900"
                }`}
              >
                <span
                  className={
                    isActive(item.href, item.key)
                      ? "text-blue-700"
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
