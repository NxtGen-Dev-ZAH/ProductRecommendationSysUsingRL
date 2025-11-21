"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  FaUser,
  FaShoppingBag,
  FaHeart,
  FaAddressCard,
  FaCreditCard,
  FaSignOutAlt,
  FaCog,
  FaShieldAlt,
  FaUsers,
  FaUserTie,
} from "react-icons/fa";
import { useAuth } from "../../app/context/AuthContext";

interface AccountNavProps {
  activeItem:
    | "dashboard"
    | "profile"
    | "orders"
    | "wishlist"
    | "addresses"
    | "payment"
    | "custom-fields"
    | "security"
    | "followers"
    | "roles";
}

const AccountNav = ({ activeItem }: AccountNavProps) => {
  const router = useRouter();
  const { logout } = useAuth();

  const navItems = [
    {
      id: "dashboard",
      label: "Tableau de Bord",
      href: "/account",
      icon: <FaUser />,
    },
    {
      id: "profile",
      label: "Modifier le Profil",
      href: "/account/profile",
      icon: <FaUser />,
    },
    {
      id: "orders",
      label: "Mes Commandes",
      href: "/account/orders",
      icon: <FaShoppingBag />,
    },
    {
      id: "wishlist",
      label: "Liste de Souhaits",
      href: "/wishlist",
      icon: <FaHeart />,
    },
    {
      id: "addresses",
      label: "Adresses",
      href: "/account/addresses",
      icon: <FaAddressCard />,
    },
    {
      id: "payment",
      label: "Méthodes de Paiement",
      href: "/account/payment",
      icon: <FaCreditCard />,
    },
    {
      id: "custom-fields",
      label: "Champs Personnalisés",
      href: "/account/custom-fields",
      icon: <FaCog />,
    },
    {
      id: "security",
      label: "Sécurité",
      href: "/account/security",
      icon: <FaShieldAlt />,
    },
    {
      id: "followers",
      label: "Social",
      href: "/account/followers",
      icon: <FaUsers />,
    },
    {
      id: "roles",
      label: "Rôles",
      href: "/account/roles",
      icon: <FaUserTie />,
    },
  ];

  const handleLogout = async () => {
    await logout();
    router.push("/");
  };

  return (
    <div className="bg-white border rounded-lg overflow-hidden">
      <div className="bg-gray-50 p-4 border-b">
        <h2 className="font-medium">Menu du Compte</h2>
      </div>

      <nav>
        <ul>
          {navItems.map((item) => (
            <li key={item.id}>
              <Link
                href={item.href}
                className={`flex items-center gap-3 p-4 border-b hover:bg-gray-50 transition-colors ${
                  activeItem === item.id
                    ? "bg-primary/5 text-[#3b82f6] font-medium"
                    : ""
                }`}
              >
                <span className="text-gray-500">{item.icon}</span>
                {item.label}
              </Link>
            </li>
          ))}

          <li>
            <button
              onClick={handleLogout}
              className="w-full flex items-center gap-3 p-4 text-left hover:bg-gray-50 transition-colors"
            >
              <span className="text-gray-500">
                <FaSignOutAlt />
              </span>
              Déconnexion
            </button>
          </li>
        </ul>
      </nav>
    </div>
  );
};

export default AccountNav;
