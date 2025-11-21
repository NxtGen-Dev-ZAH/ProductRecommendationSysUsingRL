"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
// import Image from 'next/image'; // Not currently used
import { motion } from "framer-motion";
import { FaStar, FaTag, FaCalendarAlt } from "react-icons/fa";
import { getAllVendors, Vendor } from "../api/services/vendor";

export default function VendorsPage() {
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchVendors = async () => {
      try {
        const data = await getAllVendors();
        setVendors(data);
      } catch (error) {
        console.error("Error fetching vendors:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchVendors();
  }, []);

  // Animation variants
  const containerVariants = {
    hidden: { opacity: 0 },
    visible: {
      opacity: 1,
      transition: {
        staggerChildren: 0.1,
      },
    },
  };

  const itemVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  };

  return (
    <div className="container mx-auto px-4 py-12">
      <div className="text-center mb-12">
        <h1 className="text-3xl md:text-4xl font-bold mb-4">Our Vendors</h1>
        <p className="text-gray-600 max-w-2xl mx-auto">
          We partner with trusted vendors to bring you the highest quality
          products. Browse our list of vendors and discover their unique
          offerings.
        </p>
      </div>

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          {[...Array(6)].map((_, index) => (
            <div
              key={index}
              className="bg-gray-200 h-60 rounded-lg animate-pulse"
            ></div>
          ))}
        </div>
      ) : (
        <motion.div
          variants={containerVariants}
          initial="hidden"
          animate="visible"
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8"
        >
          {vendors.map((vendor) => (
            <motion.div
              key={vendor.id}
              variants={itemVariants}
              className="bg-white rounded-lg shadow-md hover:shadow-lg transition-shadow duration-300 overflow-hidden"
            >
              <Link href={`/vendor/${vendor.id}`}>
                <div className="h-40 bg-gradient-to-r from-blue-500 to-indigo-600 relative">
                  <div className="absolute inset-0 flex items-center justify-center text-white text-5xl font-bold opacity-30">
                    {vendor.name.substring(0, 2).toUpperCase()}
                  </div>
                  {vendor.featured && (
                    <div className="absolute top-0 right-0 bg-yellow-400 text-xs font-semibold text-white px-3 py-1 shadow-md">
                      Featured
                    </div>
                  )}
                </div>
                <div className="p-6">
                  <h2 className="text-xl font-bold text-gray-900 mb-2">
                    {vendor.name}
                  </h2>
                  <p className="text-gray-600 text-sm mb-4 line-clamp-2">
                    {vendor.description}
                  </p>
                  <div className="flex items-center space-x-4 text-sm text-gray-500">
                    <div className="flex items-center">
                      <FaStar className="text-yellow-400 mr-1" />
                      <span>{vendor.rating || "N/A"}</span>
                    </div>
                    <div className="flex items-center">
                      <FaTag className="mr-1" />
                      <span>Products</span>
                    </div>
                    <div className="flex items-center">
                      <FaCalendarAlt className="mr-1" />
                      <span>
                        Since {new Date(vendor.joinedDate).getFullYear()}
                      </span>
                    </div>
                  </div>
                </div>
              </Link>
            </motion.div>
          ))}
        </motion.div>
      )}
    </div>
  );
}
