"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { FaArrowRight, FaTrophy } from "react-icons/fa";
import { Product } from "../../app/api/services/product";
import ProductCard from "../ProductCard";

interface BestSellersSectionProps {
  products: Product[];
  loading: boolean;
}

const BestSellersSection = ({
  products,
  loading,
}: BestSellersSectionProps) => {
  const fadeIn = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  };

  return (
    <section className="container mx-auto px-4 py-16 bg-gradient-to-br from-yellow-50 to-orange-50 rounded-3xl">
      <div className="flex justify-between items-center mb-10">
        <motion.div
          variants={fadeIn}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true }}
          className="flex items-center gap-2"
        >
          <FaTrophy className="text-yellow-500 text-2xl" />
          <h2 className="text-2xl md:text-3xl font-bold">
            <span className="text-[#3b82f6]">Meilleures</span> Ventes
          </h2>
        </motion.div>
        <Link
          href="/product?sort=best-selling"
          className="group flex items-center gap-2 text-[#3b82f6] hover:text-[#3b82f6]-dark transition-colors"
        >
          <span className="text-sm font-medium">Voir Tout</span>
          <FaArrowRight
            size={14}
            className="transition-transform group-hover:translate-x-1"
          />
        </Link>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[...Array(8)].map((_, index) => (
            <div
              key={index}
              className="bg-white h-80 rounded-xl animate-pulse shadow-sm"
            ></div>
          ))}
        </div>
      ) : products.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          <p>Aucun produit disponible pour le moment.</p>
        </div>
      ) : (
        <div className="relative">
          {/* Decorative elements */}
          <div className="hidden md:block absolute -top-6 right-[10%] w-24 h-24 bg-yellow-200 rounded-full opacity-70 blur-xl"></div>
          <div className="hidden md:block absolute -bottom-6 left-[10%] w-32 h-32 bg-orange-200 rounded-full opacity-70 blur-xl"></div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 relative z-10">
            {products.map((product, index) => (
              <motion.div
                key={product.id}
                variants={fadeIn}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                transition={{ duration: 0.4, delay: index * 0.05 }}
              >
                <div className="relative">
                  <ProductCard product={product} />
                  {index < 3 && (
                    <div className="absolute top-2 right-2 bg-yellow-500 text-white text-xs font-bold px-2 py-1 rounded-full flex items-center gap-1">
                      <FaTrophy size={10} />
                      #{index + 1}
                    </div>
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
};

export default BestSellersSection;

