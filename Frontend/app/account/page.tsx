"use client";

import { useAuth } from "../context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import Link from "next/link";
import {
  FaShoppingBag,
  FaHeart,
  FaAddressCard,
  FaCreditCard,
  FaEdit,
  FaClock,
  FaShieldAlt,
} from "react-icons/fa";
import AccountNav from "../../components/account/AccountNav";

export default function AccountPage() {
  const { user, isAuthenticated, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    // Wait for auth to finish loading before checking authentication
    if (loading) {
      return;
    }

    if (!isAuthenticated) {
      router.push("/auth/login");
      return;
    }
  }, [isAuthenticated, loading, router]);

  // Show loading state while auth is being checked
  if (loading) {
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
            <h1 className="text-2xl font-bold text-gray-900">Mon Compte</h1>
            <p className="text-gray-600">
              Gérez vos paramètres de compte et préférences
            </p>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
            {/* Navigation Sidebar */}
            <div className="lg:col-span-1">
              <AccountNav activeItem="dashboard" />
            </div>

            {/* Main Content */}
            <div className="lg:col-span-3 space-y-6">
              {/* Profile Summary */}
              <div className="bg-white rounded-lg shadow-sm border p-6">
                <div className="flex items-start justify-between mb-4">
                  <div>
                    <h2 className="text-lg font-medium text-gray-900">
                      Informations du Profil
                    </h2>
                    <p className="text-sm text-gray-600">
                      Gérez vos informations personnelles
                    </p>
                  </div>
                  <Link
                    href="/account/profile"
                    className="inline-flex items-center gap-2 px-3 py-2 text-sm border border-gray-300 rounded-md hover:bg-gray-50"
                  >
                    <FaEdit className="w-4 h-4" />
                    Modifier
                  </Link>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Nom
                    </label>
                    <p className="text-gray-900">
                      {user?.firstName} {user?.lastName}
                    </p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      E-mail
                    </label>
                    <p className="text-gray-900">{user?.email}</p>
                  </div>
                </div>
              </div>

              {/* Quick Actions */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                <QuickActionCard
                  icon={<FaShoppingBag className="w-6 h-6" />}
                  title="Mes Commandes"
                  description="Suivre vos commandes"
                  href="/account/orders"
                />
                <QuickActionCard
                  icon={<FaHeart className="w-6 h-6" />}
                  title="Liste de Souhaits"
                  description="Articles sauvegardés"
                  href="/account/wishlist"
                />
                <QuickActionCard
                  icon={<FaAddressCard className="w-6 h-6" />}
                  title="Adresses"
                  description="Gérer les adresses"
                  href="/account/addresses"
                />
                <QuickActionCard
                  icon={<FaCreditCard className="w-6 h-6" />}
                  title="Méthodes de Paiement"
                  description="Gérer cartes & paiements"
                  href="/account/payment"
                />
                <QuickActionCard
                  icon={<FaShieldAlt className="w-6 h-6" />}
                  title="Sécurité"
                  description="Mot de passe & sécurité"
                  href="/account/security"
                />
                <QuickActionCard
                  icon={<FaClock className="w-6 h-6" />}
                  title="Récemment Consultés"
                  description="Articles consultés"
                  href="/account/recently-viewed"
                />
              </div>

              {/* Account Status */}
              <div className="bg-white rounded-lg shadow-sm border p-6">
                <h3 className="text-lg font-medium text-gray-900 mb-4">
                  Statut du Compte
                </h3>
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">
                      Type de Compte
                    </span>
                    <span className="text-sm font-medium text-gray-900">
                      {user?.roles?.includes("SELLER")
                        ? "Compte Vendeur"
                        : "Compte Acheteur"}
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">
                      E-mail Vérifié
                    </span>
                    <span className="text-sm font-medium text-green-600">
                      Vérifié
                    </span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">Membre Depuis</span>
                    <span className="text-sm font-medium text-gray-900">
                      January 2024
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

interface QuickActionCardProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  href: string;
}

function QuickActionCard({
  icon,
  title,
  description,
  href,
}: QuickActionCardProps) {
  return (
    <Link
      href={href}
      className="bg-white rounded-lg shadow-sm border p-4 hover:shadow-md transition-shadow group"
    >
      <div className="flex items-start gap-3">
        <div className="text-blue-600 group-hover:text-blue-700 transition-colors">
          {icon}
        </div>
        <div>
          <h4 className="font-medium text-gray-900 group-hover:text-blue-700 transition-colors">
            {title}
          </h4>
          <p className="text-sm text-gray-600">{description}</p>
        </div>
      </div>
    </Link>
  );
}
