"use client";

import { motion } from "framer-motion";

// Mock data for brands
const brands = [
  { id: 1, name: "Apple", logo: "A" },
  { id: 2, name: "Samsung", logo: "S" },
  { id: 3, name: "Sony", logo: "S" },
  { id: 4, name: "LG", logo: "LG" },
  { id: 5, name: "Nike", logo: "N" },
  { id: 6, name: "Adidas", logo: "A" },
  { id: 7, name: "Microsoft", logo: "M" },
  { id: 8, name: "Google", logo: "G" },
];

const BrandShowcase = () => {
  const fadeIn = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  };

  return (
    <section className="container mx-auto px-4 py-16">
      <motion.div
        variants={fadeIn}
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true }}
        className="text-center mb-10"
      >
        <span className="text-[#3b82f6] text-sm font-medium uppercase tracking-wider">
          Trusted Partners
        </span>
        <h2 className="text-2xl md:text-3xl font-bold mt-2">
          Our Featured Brands
        </h2>
        <p className="text-gray-600 mt-4 max-w-2xl mx-auto">
          We partner with the world&apos;s leading brands to bring you the best
          quality products at competitive prices.
        </p>
      </motion.div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
        {brands.map((brand, index) => (
          <motion.div
            key={brand.id}
            variants={fadeIn}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            transition={{ duration: 0.3, delay: index * 0.05 }}
            className="p-6 border bg-white shadow-sm rounded-lg flex items-center justify-center aspect-video hover:shadow-md transition-all cursor-pointer"
          >
            <div className="text-2xl md:text-4xl font-bold text-gray-300 text-center flex flex-col items-center">
              <span className="text-[#3b82f6]">{brand.logo}</span>
              <span className="text-sm text-gray-500 mt-2 font-normal">
                {brand.name}
              </span>
            </div>
          </motion.div>
        ))}
      </div>
    </section>
  );
};

export default BrandShowcase;
