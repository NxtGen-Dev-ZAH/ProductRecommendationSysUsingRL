'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { FaArrowRight, FaHistory } from 'react-icons/fa';
import { Product } from '../../app/api/services/product';
import ProductCard from '../ProductCard';

interface RecentlyViewedProps {
  products: Product[];
  loading: boolean;
}

const RecentlyViewed = ({ products, loading }: RecentlyViewedProps) => {
  const [recentProducts, setRecentProducts] = useState<Product[]>([]);
  
  const fadeIn = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 }
  };

  // Simulate recently viewed products by randomly selecting from available products
  useEffect(() => {
    if (products.length > 0 && !loading) {
      // In a real app, this would come from local storage or user history API
      const randomProducts = [...products]
        .sort(() => 0.5 - Math.random())
        .slice(0, 4);
      
      setRecentProducts(randomProducts);
    }
  }, [products, loading]);

  if (recentProducts.length === 0) return null;

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
          <FaHistory className="text-cyan-500 text-xl" />
          <h2 className="text-2xl md:text-3xl font-bold">
            Recently <span className="text-cyan-500">Viewed</span>
          </h2>
        </motion.div>
        <Link href="/product" className="group flex items-center gap-2 text-[#3b82f6] hover:text-[#3b82f6]-dark transition-colors">
          <span className="text-sm font-medium">View All Products</span>
          <FaArrowRight size={14} className="transition-transform group-hover:translate-x-1" />
        </Link>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {recentProducts.map((product, index) => (
          <motion.div
            key={product.id}
            variants={fadeIn}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            transition={{ duration: 0.3, delay: index * 0.1 }}
          >
            <ProductCard product={product} />
          </motion.div>
        ))}
      </div>
    </section>
  );
};

export default RecentlyViewed; 