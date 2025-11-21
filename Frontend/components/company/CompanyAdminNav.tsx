"use client";

import { useRouter, usePathname } from "next/navigation";
import { useAuth } from "../../app/context/AuthContext";
import {
  FaBuilding,
  FaUsers,
  FaBox,
  FaShoppingCart,
  FaChartLine,
  FaCog,
  FaUserTie,
  FaShieldAlt,
} from "react-icons/fa";

export default function CompanyAdminNav() {
  const router = useRouter();
  const pathname = usePathname();
  const { isSuperAdmin } = useAuth();

  const navigationItems = [
    {
      name: "Company Dashboard",
      href: "/seller/company",
      icon: <FaBuilding className="w-5 h-5" />,
      description: "Overview of company performance",
    },
    {
      name: "Company Users",
      href: "/seller/company/users",
      icon: <FaUsers className="w-5 h-5" />,
      description: "Manage company users and roles",
    },
    {
      name: "Company Products",
      href: "/seller/company/products",
      icon: <FaBox className="w-5 h-5" />,
      description: "Manage company product catalog",
    },
    {
      name: "Company Orders",
      href: "/seller/company/orders",
      icon: <FaShoppingCart className="w-5 h-5" />,
      description: "View and manage company orders",
    },
    {
      name: "Company Analytics",
      href: "/seller/company/analytics",
      icon: <FaChartLine className="w-5 h-5" />,
      description: "Company performance analytics",
    },
    {
      name: "Role Management",
      href: "/seller/roles",
      icon: <FaUserTie className="w-5 h-5" />,
      description: "Manage company admin roles",
    },
    {
      name: "Company Settings",
      href: "/seller/company/settings",
      icon: <FaCog className="w-5 h-5" />,
      description: "Company configuration",
    },
  ];

  // Add super admin specific items if user is super admin
  if (isSuperAdmin) {
    navigationItems.push({
      name: "Platform Admin",
      href: "/admin",
      icon: <FaShieldAlt className="w-5 h-5" />,
      description: "Access platform administration",
    });
  }

  const isActive = (href: string) => {
    if (href === "/seller/company") {
      return pathname === href;
    }
    return pathname.startsWith(href);
  };

  return (
    <div className="bg-white rounded-lg shadow-sm border">
      <div className="p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">
          Company Administration
        </h2>
        <nav className="space-y-2">
          {navigationItems.map((item) => (
            <button
              key={item.name}
              onClick={() => router.push(item.href)}
              className={`w-full text-left p-3 rounded-lg transition-colors ${
                isActive(item.href)
                  ? "bg-blue-50 text-blue-700 border border-blue-200"
                  : "text-gray-700 hover:bg-gray-50 hover:text-gray-900"
              }`}
            >
              <div className="flex items-center gap-3">
                <div
                  className={`${
                    isActive(item.href) ? "text-blue-600" : "text-gray-400"
                  }`}
                >
                  {item.icon}
                </div>
                <div>
                  <div className="font-medium">{item.name}</div>
                  <div className="text-xs text-gray-500 mt-1">
                    {item.description}
                  </div>
                </div>
              </div>
            </button>
          ))}
        </nav>
      </div>

      {/* User Info */}
      <div className="border-t border-gray-200 p-6">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center">
            <FaBuilding className="w-4 h-4 text-green-600" />
          </div>
          <div>
            <div className="text-sm font-medium text-gray-900">
              Company Admin
            </div>
            <div className="text-xs text-gray-500">
              {isSuperAdmin ? "Super Admin Access" : "Company Scoped"}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
