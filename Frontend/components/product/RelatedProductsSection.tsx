"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { FaArrowRight, FaTags } from "react-icons/fa";
import { Product } from "../../app/api/services/product";
import ProductCard from "../ProductCard";

interface RelatedProductsSectionProps {
  products: Product[];
  loading: boolean;
  categoryId?: number;
}

const RelatedProductsSection = ({
  products,
  loading,
  categoryId,
}: RelatedProductsSectionProps) => {
  const fadeIn = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  };

  // Don't render if no products
  if (!loading && products.length === 0) {
    return null;
  }

  return (
    <section className="container mx-auto px-4 py-12">
      <div className="flex justify-between items-center mb-8">
        <motion.div
          variants={fadeIn}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true }}
          className="flex items-center gap-2"
        >
          <FaTags className="text-[#3b82f6] text-xl" />
          <h2 className="text-xl md:text-2xl font-bold">
            Produits <span className="text-[#3b82f6]">Similaires</span>
          </h2>
        </motion.div>
        {categoryId && (
          <Link
            href={`/category/${categoryId}`}
            className="group flex items-center gap-2 text-[#3b82f6] hover:text-[#3b82f6]-dark transition-colors text-sm"
          >
            <span className="font-medium">Voir la Catégorie</span>
            <FaArrowRight
              size={12}
              className="transition-transform group-hover:translate-x-1"
            />
          </Link>
        )}
      </div>

      {loading ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, index) => (
            <div
              key={index}
              className="bg-gray-200 h-64 rounded-xl animate-pulse"
            ></div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
          {products.map((product, index) => (
            <motion.div
              key={product.id}
              variants={fadeIn}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true }}
              transition={{ duration: 0.3, delay: index * 0.05 }}
            >
              <ProductCard product={product} />
            </motion.div>
          ))}
        </div>
      )}
    </section>
  );
};

export default RelatedProductsSection;

