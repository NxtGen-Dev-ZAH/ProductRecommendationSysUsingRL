'use client';

import Link from 'next/link';
import { motion } from 'framer-motion';
import { FaArrowRight, FaChartLine } from 'react-icons/fa';
import { Product } from '../../app/api/services/product';
import ProductCard from '../ProductCard';

interface TrendingProductsSectionProps {
  products: Product[];
  loading: boolean;
}

const TrendingProductsSection = ({ products, loading }: TrendingProductsSectionProps) => {
  const fadeIn = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 }
  };

  return (
    <section className="container mx-auto px-4 py-16">
      <div className="flex justify-between items-center mb-10">
        <motion.div 
          variants={fadeIn}
          initial="hidden"
          whileInView="visible"
          viewport={{ once: true }}
          className="flex items-center gap-2"
        >
          <FaChartLine className="text-[#3b82f6] text-2xl" />
          <h2 className="text-2xl md:text-3xl font-bold">
            Trending <span className="text-[#3b82f6]">Now</span>
          </h2>
        </motion.div>
        <Link href="/product?trending=true" className="group flex items-center gap-2 text-[#3b82f6] hover:text-[#3b82f6]-dark transition-colors">
          <span className="text-sm font-medium">View All</span>
          <FaArrowRight size={14} className="transition-transform group-hover:translate-x-1" />
        </Link>
      </div>

      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          {[...Array(5)].map((_, index) => (
            <div key={index} className="bg-gray-200 h-60 rounded-xl animate-pulse"></div>
          ))}
        </div>
      ) : (
        <div className="relative">
          {/* Decorative elements */}
          <div className="hidden md:block absolute -top-8 left-[20%] w-16 h-16 bg-orange-100 rounded-full opacity-70 blur-md"></div>
          <div className="hidden md:block absolute -bottom-8 right-[30%] w-20 h-20 bg-indigo-100 rounded-full opacity-70 blur-md"></div>
          
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4 relative z-10">
            {products.slice(0, 5).map((product, index) => (
              <motion.div
                key={product.id}
                variants={fadeIn}
                initial="hidden"
                whileInView="visible"
                viewport={{ once: true }}
                transition={{ duration: 0.3, delay: index * 0.05 }}
              >
                <ProductCard 
                  product={product} 
                  className="h-full"
                />
                {product.discount && product.discount > 0 && (
                  <div className="absolute top-2 left-2 bg-red-500 text-white text-xs font-bold px-2 py-1 rounded-md">
                    -{product.discount}%
                  </div>
                )}
              </motion.div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
};

export default TrendingProductsSection; 