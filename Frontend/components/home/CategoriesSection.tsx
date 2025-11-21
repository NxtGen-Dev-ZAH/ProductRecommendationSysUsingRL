"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { FaArrowRight } from "react-icons/fa";
import { Category } from "../../app/api/services/category";

interface CategoriesSectionProps {
  categories: Category[];
  loading: boolean;
}

const CategoriesSection = ({ categories, loading }: CategoriesSectionProps) => {
  const fadeIn = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  };

  // Gradient backgrounds for categories
  const gradients = [
    "from-blue-100 to-indigo-200",
    "from-emerald-100 to-teal-200",
    "from-amber-100 to-orange-200",
    "from-rose-100 to-pink-200",
    "from-purple-100 to-violet-200",
    "from-cyan-100 to-sky-200",
  ];

  return (
    <section className="container mx-auto px-4 py-16">
      <div className="flex justify-between items-center mb-10">
        <motion.h2
          variants={fadeIn}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true }}
          className="text-2xl md:text-3xl font-bold"
        >
          Acheter par <span className="text-[#3b82f6]">Catégories</span>
        </motion.h2>
        <Link
          href="/category"
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
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
          {[...Array(4)].map((_, index) => (
            <div
              key={index}
              className="aspect-[3/4] bg-gray-200 animate-pulse rounded-2xl"
            ></div>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
          {categories.slice(0, 4).map((category, index) => (
            <motion.div
              key={category.id}
              variants={fadeIn}
              initial="hidden"
              whileInView="visible"
              viewport={{ once: true }}
              transition={{ duration: 0.4, delay: index * 0.1 }}
              className="relative overflow-hidden group"
            >
              <Link
                href={`/category/${category.id}`}
                className="block relative aspect-[3/4] overflow-hidden rounded-2xl shadow-sm group-hover:shadow-md transition-all duration-300"
              >
                {/* Category colored background with gradient */}
                <div
                  className={`absolute inset-0 bg-gradient-to-br ${
                    gradients[index % gradients.length]
                  }`}
                ></div>

                {/* Category image would ideally go here */}
                <div className="absolute inset-0 flex items-center justify-center opacity-80 group-hover:opacity-90 transition-opacity">
                  <div className="w-1/2 h-1/2 bg-white/30 backdrop-blur-sm rounded-full flex items-center justify-center">
                    <span className="text-4xl font-light text-gray-800">
                      {category.name.charAt(0)}
                    </span>
                  </div>
                </div>

                {/* Category info */}
                <div className="absolute inset-x-0 bottom-0 p-6 bg-gradient-to-t from-black/60 to-transparent">
                  <h3 className="text-white text-xl font-bold mb-1">
                    {category.name}
                  </h3>
                  <span className="inline-flex items-center text-white/80 text-sm font-medium group-hover:text-white transition-colors">
                    Acheter Maintenant{" "}
                    <FaArrowRight
                      size={10}
                      className="ml-1 transition-transform group-hover:translate-x-1"
                    />
                  </span>
                </div>
              </Link>
            </motion.div>
          ))}
        </div>
      )}
    </section>
  );
};

export default CategoriesSection;
