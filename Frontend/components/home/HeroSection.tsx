"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { FaCheck } from "react-icons/fa";
import { Button } from "../ui/button";
import Image from "next/image";

const HeroSection = () => {
  return (
    <section className=" relative bg-gradient-to-r from-gray-50 via-gray-100 to-gray-50 pt-8 pb-10 overflow-hidden">
      {/* Abstract background elements */}
      <div className="absolute inset-0 overflow-hidden">
        <div className="absolute top-0 left-1/2 w-1/3 h-1/3 bg-cyan-500/15 rounded-full blur-3xl"></div>
        <div className="absolute bottom-0 right-1/4 w-1/4 h-1/4 bg-indigo-400/30 rounded-full blur-3xl"></div>
        <div className="absolute top-1/3 right-0 w-1/4 h-1/3 bg-pink-400/30 rounded-full blur-3xl"></div>
      </div>

      <div className="container mx-auto px-4 relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
          <motion.div
            initial={{ opacity: 0, x: -30 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.6 }}
            className="max-w-xl"
          >
            <span className="inline-block px-4 py-1.5 mb-6 text-sm font-medium rounded-full bg-cyan-500/10 text-cyan-500">
              Marketplace Premium
            </span>
            <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold mb-6 text-gray-900 leading-tight">
              Découvrez des <span className="text-cyan-500">Produits</span>{" "}
              Exceptionnels
            </h1>
            <p className="text-lg text-gray-700 mb-8 leading-relaxed">
              Achetez des collections sélectionnées de vendeurs vérifiés du
              monde entier. Qualité premium, prix compétitifs et expérience
              d&apos;achat sécurisée.
            </p>
            <div className="flex flex-wrap gap-4">
              <Link href="/product">
                <Button
                  size="lg"
                  className="font-medium rounded-full px-8 shadow-md hover:shadow-lg transition-all"
                >
                  Explorer les Produits
                </Button>
              </Link>
              <Link href="/vendor">
                <Button
                  variant="outline"
                  size="lg"
                  className="font-medium rounded-full px-8 border-2 hover:bg-gray-50 transition-all"
                >
                  Rencontrer Nos Vendeurs
                </Button>
              </Link>
            </div>
            <div className="flex flex-wrap items-center gap-6 mt-8">
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 rounded-full bg-green-100 flex items-center justify-center">
                  <FaCheck className="text-green-600 text-xs" />
                </div>
                <span className="text-sm text-gray-700">
                  Livraison Gratuite Mondiale
                </span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 rounded-full bg-green-100 flex items-center justify-center">
                  <FaCheck className="text-green-600 text-xs" />
                </div>
                <span className="text-sm text-gray-700">
                  Paiement 100% Sécurisé
                </span>
              </div>
              <div className="flex items-center gap-2">
                <div className="w-5 h-5 rounded-full bg-green-100 flex items-center justify-center">
                  <FaCheck className="text-green-600 text-xs" />
                </div>
                <span className="text-sm text-gray-700">Support Premium</span>
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="relative h-[400px] lg:h-[500px] flex items-center justify-center"
          >
            <div className="relative w-full h-full">
              {/* Hero image decorative elements */}
              <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-[350px] h-[350px] rounded-full bg-primary/5 animate-pulse"></div>
              <div className="absolute -top-4 -right-4 w-24 h-24 bg-yellow-200 rounded-full opacity-30 blur-xl"></div>
              <div className="absolute -bottom-4 -left-4 w-24 h-24 bg-primary/40 rounded-full opacity-30 blur-xl"></div>

              {/* Main hero image */}
              <div className="w-full h-full bg-white rounded-2xl overflow-hidden shadow-xl relative z-10">
                {/* Replace with actual hero image */}
                <Image
                  src="/banner.png"
                  alt="Hero Image"
                  width={750}
                  height={500}
                  className="w-full h-full object-contain"
                  priority
                />
              </div>

              {/* Decorative floating elements */}
              <div className="absolute top-10 right-10 w-20 h-20 bg-white p-2 rounded-lg shadow-lg transform rotate-6 z-20">
                <div className="w-full h-full bg-primary/10 rounded flex items-center justify-center">
                  <span className="text-[#3b82f6] font-bold">50%</span>
                </div>
              </div>
              <div className="absolute bottom-10 left-0 w-32 h-16 bg-white px-3 py-2 rounded-lg shadow-lg transform -rotate-3 z-20">
                <div className="flex flex-col">
                  <span className="text-xs text-gray-500">Premium</span>
                  <span className="text-sm font-bold">Nouveautés</span>
                </div>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </section>
  );
};

export default HeroSection;
