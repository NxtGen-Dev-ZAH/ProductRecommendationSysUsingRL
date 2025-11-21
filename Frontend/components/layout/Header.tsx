"use client";

import Link from "next/link";
import { useState, useEffect } from "react";
import {
  IoCartOutline,
  IoPersonOutline,
  IoSearchOutline,
  IoMenuOutline,
  IoCloseOutline,
  IoHeartOutline,
  IoNotificationsOutline,
} from "react-icons/io5";
import { cn } from "../../utils/cn";
import { useAuth } from "../../app/context/AuthContext";
import { useCart } from "../../app/context/CartContext";
import SearchBar from "../search/SearchBar";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "../ui/button";
import Image from "next/image";

const Header = () => {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [showSearch, setShowSearch] = useState(false);
  const {
    isAuthenticated,
    user,
    logout,
    isSuperAdmin,
    isCompanyAdmin,
    isSeller,
  } = useAuth();
  const { itemCount } = useCart();

  // Derive a safe avatar initial from name or email
  const avatarLetter = (() => {
    const nameSource = (user?.firstName || user?.lastName || "").trim();
    if (nameSource) return nameSource.charAt(0).toUpperCase();

    const emailSource = (user?.email || user?.emailAddress || "").trim();
    if (emailSource) return emailSource.charAt(0).toUpperCase();

    return ""; // keep empty to avoid showing a question mark dot
  })();

  // Handle scroll effect
  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 10) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Close mobile menu on navigation
  const closeMobileMenu = () => {
    setIsMobileMenuOpen(false);
  };

  return (
    <header
      className={cn(
        "fixed top-0 left-0 right-0 z-50 transition-all duration-300 border-b",
        isScrolled
          ? "bg-white/90 backdrop-blur-md shadow-sm py-2 border-gray-200/30"
          : "bg-white/80 backdrop-blur-sm py-3 border-transparent"
      )}
    >
      <div className="container mx-auto px-4">
        <div className="flex items-center justify-between w-full">
          {/* Logo */}
          <Link
            href="/"
            className="text-2xl font-bold flex items-center justify-start w-1/3"
          >
            <Image
              src="/hero.png"
              alt="Shopora Logo"
              width={50}
              height={50}
              style={{ width: "auto", height: "auto" }}
              priority
            />
            <span className="text-primary font-bold tracking-wide pl-2">
              SHOPORA
            </span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex justify-center items-center w-1/3">
            <div className="flex space-x-1">
              <NavLink href="/" label="Accueil" />
              <NavLink href="/category" label="Catégories" />
              <NavLink href="/vendor" label="Vendeurs" />
              <NavLink href="/search?discount=true" label="Promotions" />
              <NavLink href="/product" label="Produits" />
            </div>
          </nav>

          {/* Search, Cart, and Account */}
          <div className="flex items-center space-x-2 md:space-x-3 w-1/3 justify-end">
            <button
              onClick={() => setShowSearch(!showSearch)}
              className="p-2 rounded-full hover:bg-gray-100/80 transition-colors relative"
              aria-label="Search"
            >
              <IoSearchOutline className="text-gray-700 w-5 h-5 hover:text-primary transition-colors cursor-pointer" />
            </button>

            <Link
              href="/wishlist"
              className="p-2 rounded-full hover:bg-gray-100/80 transition-colors hidden md:flex"
              aria-label="Wishlist"
            >
              <IoHeartOutline className="text-gray-700 w-5 h-5 hover:text-pink-600 transition-colors" />
            </Link>

            <Link
              href="/cart"
              className="p-2 rounded-full hover:bg-gray-100/80 transition-colors relative"
              aria-label="Shopping cart"
            >
              <IoCartOutline className="text-gray-700 w-5 h-5 hover:text-primary transition-colors" />
              {itemCount > 0 && (
                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full min-w-4 h-4 flex items-center justify-center px-1 font-bold z-10 shadow-lg">
                  {itemCount > 99 ? "99+" : itemCount}
                </span>
              )}
            </Link>

            {isAuthenticated ? (
              <div className="relative group">
                <button className="p-2 rounded-full hover:bg-gray-100/80 transition-colors flex items-center">
                  <div className="hidden md:block mr-2">
                    <span className="text-sm font-medium text-gray-700">
                      {user?.firstName?.split(" ")[0]}
                    </span>
                  </div>
                  <Link
                    href="/account"
                    aria-label="Mon Compte"
                    className="w-8 h-8 rounded-full bg-gray-200 flex items-center justify-center text-gray-700 font-medium overflow-hidden"
                  >
                    {avatarLetter}
                  </Link>
                </button>
                <div className="absolute right-0 mt-2 w-64 bg-white/95 backdrop-blur-sm rounded-2xl shadow-lg py-2 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-300 border border-gray-100/50">
                  <div className="px-4 py-3 border-b border-gray-100/50">
                    <p className="font-medium text-gray-800">
                      {user?.firstName} {user?.lastName}
                    </p>
                    <p className="text-sm text-gray-500 truncate">
                      {user?.email}
                    </p>
                  </div>
                  <div className="py-1">
                    <UserMenuItem href="/account" label="Mon Compte" />
                    <UserMenuItem
                      href="/account/orders"
                      label="Mes Commandes"
                    />
                    <UserMenuItem
                      href="/wishlist"
                      label="Ma Liste de Souhaits"
                    />
                    <UserMenuItem href="/notifications" label="Notifications" />
                  </div>

                  {/* Role-based dashboard links */}
                  <div className="border-t border-gray-100/50 pt-1 mt-1">
                    {/* Super Admin Access */}
                    {isSuperAdmin && (
                      <UserMenuItem
                        href="/admin"
                        label="Platform Admin Dashboard"
                      />
                    )}

                    {/* Company Admin Access */}
                    {isCompanyAdmin && (
                      <UserMenuItem
                        href="/seller/company"
                        label="Company Admin Dashboard"
                      />
                    )}

                    {/* Regular Seller Access */}
                    {isSeller && !isCompanyAdmin && (
                      <UserMenuItem href="/seller" label="Seller Dashboard" />
                    )}

                    {/* Become Seller Option */}
                    {!isSeller && !isCompanyAdmin && !isSuperAdmin && (
                      <UserMenuItem
                        href="/become-seller"
                        label="Become a Seller"
                      />
                    )}
                  </div>

                  <div className="border-t border-gray-100/50 mt-1 pt-1">
                    <button
                      onClick={logout}
                      className="flex w-full items-center px-4 py-2 text-sm text-red-600 hover:bg-gray-50/80 transition-colors"
                    >
                      <span className="ml-2">Déconnexion</span>
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <Button
                asChild
                variant="ghost"
                size="sm"
                className="gap-2 hover:bg-primary/10 "
              >
                <Link href="/auth/login">
                  <IoPersonOutline className="text-gray-700 w-5 h-5" />
                  <span className="hidden md:block">Connexion</span>
                </Link>
              </Button>
            )}

            {/* Mobile menu button */}
            <button
              className="md:hidden p-2 rounded-full hover:bg-gray-100/80 transition-colors"
              onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
              aria-label={isMobileMenuOpen ? "Close menu" : "Open menu"}
            >
              {isMobileMenuOpen ? (
                <IoCloseOutline className="text-gray-700 w-6 h-6" />
              ) : (
                <IoMenuOutline className="text-gray-700 w-6 h-6" />
              )}
            </button>
          </div>
        </div>

        {/* Search bar */}
        <AnimatePresence>
          {showSearch && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.2 }}
              className="mt-4 overflow-hidden"
            >
              <SearchBar
                className="w-full md:max-w-2xl mx-auto"
                placeholder="Rechercher des produits, marques, catégories..."
              />
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Mobile Navigation Overlay */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="fixed inset-0 bg-black/40 backdrop-blur-sm z-40 md:hidden"
            onClick={closeMobileMenu}
          />
        )}
      </AnimatePresence>

      {/* Mobile Navigation */}
      <div
        className={`fixed top-0 right-0 h-full w-4/5 max-w-sm bg-white/95 backdrop-blur-sm z-50 transform transition-transform duration-300 ease-in-out shadow-xl md:hidden rounded-l-2xl ${
          isMobileMenuOpen ? "translate-x-0" : "translate-x-full"
        }`}
      >
        <div className="flex justify-between items-center p-4 border-b  border-gray-100/50">
          <Link
            href="/"
            onClick={closeMobileMenu}
            className="text-xl font-bold"
          >
            <span className="text-primary">Shopora</span>
          </Link>
          <button
            onClick={closeMobileMenu}
            className="p-2 rounded-full hover:bg-gray-100/80 transition-colors"
            aria-label="Close menu"
          >
            <IoCloseOutline className="w-6 h-6" />
          </button>
        </div>

        {isAuthenticated && (
          <div className="p-4 border-b border-gray-100/50 bg-white rounded-2xl">
            <div className="flex items-center">
              <Link
                href="/account"
                aria-label="Mon Compte"
                className="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center text-gray-700 font-medium mr-3"
              >
                {avatarLetter}
              </Link>
              <div>
                <p className="font-medium text-gray-900">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-sm text-gray-500">{user?.email}</p>
              </div>
            </div>
          </div>
        )}

        <nav className="p-4 bg-white rounded-2xl">
          <div className="space-y-1 ">
            <MobileNavLink href="/" label="Accueil" onClick={closeMobileMenu} />
            <MobileNavLink
              href="/category"
              label="Catégories"
              onClick={closeMobileMenu}
            />
            <MobileNavLink
              href="/vendor"
              label="Vendeurs"
              onClick={closeMobileMenu}
            />
            <MobileNavLink
              href="/search?discount=true"
              label="Promotions"
              onClick={closeMobileMenu}
            />
            <MobileNavLink
              href="/product"
              label="Produits"
              onClick={closeMobileMenu}
            />
          </div>

          <div className="mt-6 pt-6 border-t border-gray-100/50 space-y-1">
            <MobileNavLink
              href="/account"
              label="Mon Compte"
              onClick={closeMobileMenu}
              icon={<IoPersonOutline />}
            />
            <MobileNavLink
              href="/cart"
              label="Panier"
              onClick={closeMobileMenu}
              icon={<IoCartOutline />}
            />
            <MobileNavLink
              href="/wishlist"
              label="Liste de Souhaits"
              onClick={closeMobileMenu}
              icon={<IoHeartOutline />}
            />
            <MobileNavLink
              href="/notifications"
              label="Notifications"
              onClick={closeMobileMenu}
              icon={<IoNotificationsOutline />}
            />
          </div>

          {isAuthenticated && (
            <div className="mt-6 pt-4 border-t border-gray-100/50">
              {user?.roles?.includes("VENDOR") && (
                <MobileNavLink
                  href="/vendor/dashboard"
                  label="Tableau de Bord Vendeur"
                  onClick={closeMobileMenu}
                />
              )}
              {!user?.roles?.includes("VENDOR") && (
                <MobileNavLink
                  href="/become-vendor"
                  label="Devenir Vendeur"
                  onClick={closeMobileMenu}
                />
              )}
              {/* Mobile Role-based navigation */}
              {isSuperAdmin && (
                <MobileNavLink
                  href="/admin"
                  label="Platform Admin Dashboard"
                  onClick={closeMobileMenu}
                />
              )}
              {isCompanyAdmin && (
                <MobileNavLink
                  href="/seller/company"
                  label="Company Admin Dashboard"
                  onClick={closeMobileMenu}
                />
              )}
              {isSeller && !isCompanyAdmin && (
                <MobileNavLink
                  href="/seller"
                  label="Seller Dashboard"
                  onClick={closeMobileMenu}
                />
              )}

              <button
                onClick={() => {
                  logout();
                  closeMobileMenu();
                }}
                className="flex w-full items-center px-3 py-2.5 text-sm text-red-600 rounded-xl mt-4 hover:bg-red-50/80 transition-colors"
              >
                <span>Déconnexion</span>
              </button>
            </div>
          )}

          {!isAuthenticated && (
            <div className="mt-6 pt-4 border-t border-gray-100/50">
              <Button
                asChild
                className="w-full"
                hover="scale"
                onClick={closeMobileMenu}
              >
                <Link href="/auth/login">Connexion / Inscription</Link>
              </Button>
            </div>
          )}
        </nav>
      </div>
    </header>
  );
};

const NavLink = ({ href, label }: { href: string; label: string }) => (
  <Link
    href={href}
    className="px-3 py-2 rounded-xl text-sm font-medium text-gray-700 transition-colors hover:text-primary hover:bg-primary/10"
  >
    {label}
  </Link>
);

const UserMenuItem = ({ href, label }: { href: string; label: string }) => (
  <Link
    href={href}
    className="flex items-center px-4 py-2 text-sm text-gray-700 hover:bg-gray-50/80 hover:text-primary transition-colors"
  >
    <span className="ml-2">{label}</span>
  </Link>
);

const MobileNavLink = ({
  href,
  label,
  onClick,
  icon,
}: {
  href: string;
  label: string;
  onClick: () => void;
  icon?: React.ReactNode;
}) => (
  <Link
    href={href}
    onClick={onClick}
    className="flex items-center px-3 py-2.5 text-sm font-medium text-gray-700 rounded-xl transition-colors hover:text-primary hover:bg-primary/10"
  >
    {icon && <span className="mr-3">{icon}</span>}
    {label}
  </Link>
);

export default Header;
