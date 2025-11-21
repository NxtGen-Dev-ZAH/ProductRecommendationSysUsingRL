"use client";

import Link from "next/link";
import { useState } from "react";
import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";
import { Button } from "@/components/ui/button";
import { useAuth } from "../context/AuthContext";
import { FaStore, FaPlus } from "react-icons/fa";

export default function SitemapPage() {
  const { isAuthenticated, isSeller, becomeSellerRequest } = useAuth();
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error" | "info";
    text: string;
  } | null>(null);

  const handleBecomeSellerRequest = async () => {
    if (!isAuthenticated) {
      setMessage({
        type: "error",
        text: "You must be logged in to become a seller",
      });
      return;
    }

    if (isSeller) {
      setMessage({
        type: "info",
        text: "You are already a seller",
      });
      return;
    }

    try {
      setLoading(true);
      setMessage(null);

      await becomeSellerRequest();
      setMessage({
        type: "success",
        text: "Seller role activated successfully! You can now start selling products.",
      });
    } catch (error: unknown) {
      console.error("Error requesting seller role:", error);
      const errorMessage =
        error instanceof Error && "response" in error
          ? (error as { response?: { data?: { message?: string } } }).response
              ?.data?.message
          : "Failed to request seller role";
      setMessage({
        type: "error",
        text: errorMessage || "Failed to request seller role",
      });
    } finally {
      setLoading(false);
    }
  };

  const mainPages = [
    { href: "/", label: "Accueil" },
    { href: "/category", label: "Catégories" },
    { href: "/search", label: "Recherche" },
    { href: "/vendor", label: "Vendeurs" },
    { href: "/about", label: "À Propos" },
    { href: "/contact", label: "Contact" },
  ];

  const accountPages = [
    { href: "/account", label: "Mon Compte" },
    { href: "/account/profile", label: "Profil" },
    { href: "/account/orders", label: "Mes Commandes" },
    { href: "/account/addresses", label: "Adresses" },
    { href: "/account/payment", label: "Moyens de Paiement" },
    { href: "/account/security", label: "Sécurité" },
  ];

  const authPages = [
    { href: "/auth/login", label: "Connexion" },
    { href: "/auth/register", label: "Inscription" },
    { href: "/auth/forgot-password", label: "Mot de Passe Oublié" },
  ];

  const sellerPages = [
    { href: "/seller", label: "Tableau de Bord Vendeur" },
    { href: "/seller/products", label: "Mes Produits" },
    { href: "/seller/products/new", label: "Ajouter un Produit" },
  ];

  const adminPages = [
    { href: "/admin", label: "Administration" },
    { href: "/admin/users", label: "Gestion des Utilisateurs" },
    { href: "/admin/products", label: "Gestion des Produits" },
    { href: "/admin/categories", label: "Gestion des Catégories" },
  ];

  const legalPages = [
    { href: "/privacy-policy", label: "Politique de Confidentialité" },
    { href: "/terms-of-service", label: "Conditions d'Utilisation" },
    { href: "/cookie-policy", label: "Politique des Cookies" },
    { href: "/shipping-policy", label: "Politique de Livraison" },
    { href: "/return-policy", label: "Politique de Retours" },
  ];

  const shoppingPages = [
    { href: "/cart", label: "Panier" },
    { href: "/checkout", label: "Commande" },
    { href: "/wishlist", label: "Liste de Souhaits" },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-6xl mx-auto">
            <h1 className="text-4xl font-bold text-gray-900 mb-8 text-center">
              Plan du Site
            </h1>

            {/* Message Display */}
            {message && (
              <div
                className={`mb-6 p-4 rounded-md ${
                  message.type === "success"
                    ? "bg-green-50 text-green-700 border border-green-200"
                    : message.type === "info"
                    ? "bg-blue-50 text-blue-700 border border-blue-200"
                    : "bg-red-50 text-red-700 border border-red-200"
                }`}
              >
                {message.text}
              </div>
            )}

            <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
              {/* Pages Principales */}
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
                  Pages Principales
                </h2>
                <ul className="space-y-2">
                  {mainPages.map((page) => (
                    <li key={page.href}>
                      <Link
                        href={page.href}
                        className="text-gray-600 hover:text-primary transition-colors duration-200 flex items-center group"
                      >
                        <span className="w-0 h-0.5 bg-primary rounded-full mr-2 transition-all duration-300 group-hover:w-2"></span>
                        {page.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Compte Utilisateur */}
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
                  Mon Compte
                </h2>
                <ul className="space-y-2">
                  {accountPages.map((page) => (
                    <li key={page.href}>
                      <Link
                        href={page.href}
                        className="text-gray-600 hover:text-primary transition-colors duration-200 flex items-center group"
                      >
                        <span className="w-0 h-0.5 bg-primary rounded-full mr-2 transition-all duration-300 group-hover:w-2"></span>
                        {page.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Authentification */}
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
                  Authentification
                </h2>
                <ul className="space-y-2">
                  {authPages.map((page) => (
                    <li key={page.href}>
                      <Link
                        href={page.href}
                        className="text-gray-600 hover:text-primary transition-colors duration-200 flex items-center group"
                      >
                        <span className="w-0 h-0.5 bg-primary rounded-full mr-2 transition-all duration-300 group-hover:w-2"></span>
                        {page.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Vendeurs */}
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
                  Vendeurs
                </h2>
                <ul className="space-y-2">
                  {sellerPages.map((page) => (
                    <li key={page.href}>
                      <Link
                        href={page.href}
                        className="text-gray-600 hover:text-primary transition-colors duration-200 flex items-center group"
                      >
                        <span className="w-0 h-0.5 bg-primary rounded-full mr-2 transition-all duration-300 group-hover:w-2"></span>
                        {page.label}
                      </Link>
                    </li>
                  ))}
                  {/* Become Seller Button */}
                  <li className="pt-2">
                    <div className="flex items-center justify-between p-3 border border-gray-200 rounded-lg bg-green-50">
                      <div className="flex items-center gap-3">
                        <FaStore className="w-5 h-5 text-green-600" />
                        <span className="text-gray-700 font-medium">
                          Devenir Vendeur
                        </span>
                      </div>
                      <Button
                        onClick={handleBecomeSellerRequest}
                        loading={loading}
                        disabled={loading || isSeller}
                        size="sm"
                        className="bg-green-600 hover:bg-green-700"
                      >
                        <FaPlus className="w-3 h-3 mr-1" />
                        {isSeller ? "Déjà Vendeur" : "Devenir Vendeur"}
                      </Button>
                    </div>
                  </li>
                </ul>
              </div>

              {/* Administration */}
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
                  Administration
                </h2>
                <ul className="space-y-2">
                  {adminPages.map((page) => (
                    <li key={page.href}>
                      <Link
                        href={page.href}
                        className="text-gray-600 hover:text-primary transition-colors duration-200 flex items-center group"
                      >
                        <span className="w-0 h-0.5 bg-primary rounded-full mr-2 transition-all duration-300 group-hover:w-2"></span>
                        {page.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Achats */}
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
                  Achats
                </h2>
                <ul className="space-y-2">
                  {shoppingPages.map((page) => (
                    <li key={page.href}>
                      <Link
                        href={page.href}
                        className="text-gray-600 hover:text-primary transition-colors duration-200 flex items-center group"
                      >
                        <span className="w-0 h-0.5 bg-primary rounded-full mr-2 transition-all duration-300 group-hover:w-2"></span>
                        {page.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Pages Légales */}
            <div className="mt-8">
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-xl font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
                  Pages Légales
                </h2>
                <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {legalPages.map((page) => (
                    <Link
                      key={page.href}
                      href={page.href}
                      className="text-gray-600 hover:text-primary transition-colors duration-200 flex items-center group"
                    >
                      <span className="w-0 h-0.5 bg-primary rounded-full mr-2 transition-all duration-300 group-hover:w-2"></span>
                      {page.label}
                    </Link>
                  ))}
                </div>
              </div>
            </div>

            {/* Informations Supplémentaires */}
            <div className="mt-8 bg-white rounded-lg shadow-lg p-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-4 border-b border-gray-200 pb-2">
                Informations Supplémentaires
              </h2>
              <div className="grid md:grid-cols-2 gap-6">
                <div>
                  <h3 className="font-semibold text-gray-900 mb-2">
                    Recherche
                  </h3>
                  <p className="text-gray-600 text-sm">
                    Utilisez notre moteur de recherche pour trouver des produits
                    spécifiques ou explorez par catégories pour découvrir de
                    nouveaux articles.
                  </p>
                </div>
                <div>
                  <h3 className="font-semibold text-gray-900 mb-2">
                    Navigation
                  </h3>
                  <p className="text-gray-600 text-sm">
                    Toutes les pages sont accessibles via le menu principal, le
                    pied de page, ou en utilisant la barre de recherche.
                  </p>
                </div>
              </div>
            </div>

            {/* Contact */}
            <div className="mt-8 bg-primary/5 rounded-lg p-6 text-center">
              <h2 className="text-xl font-semibold text-gray-900 mb-2">
                Vous ne trouvez pas ce que vous cherchez ?
              </h2>
              <p className="text-gray-600 mb-4">
                Contactez notre équipe de support pour obtenir de l&apos;aide.
              </p>
              <Link
                href="/contact"
                className="inline-flex items-center px-6 py-3 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors duration-200"
              >
                Nous Contacter
              </Link>
            </div>
          </div>
        </Section>
      </Container>
    </div>
  );
}
