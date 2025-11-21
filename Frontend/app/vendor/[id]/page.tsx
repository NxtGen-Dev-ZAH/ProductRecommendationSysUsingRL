"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
// import Image from 'next/image'; // Commented out as not currently used
import Link from "next/link";
import { motion } from "framer-motion";
import {
  FaStar,
  FaEnvelope,
  FaPhone,
  FaMapMarkerAlt,
  FaCalendarAlt,
} from "react-icons/fa";
import { getVendorWithProducts, Vendor } from "../../api/services/vendor";
import { Product } from "../../api/services/product";
import ProductCard from "../../../components/ProductCard";

export default function VendorDetailPage() {
  const params = useParams();
  const vendorId = parseInt(params.id as string, 10);

  const [vendor, setVendor] = useState<Vendor | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchVendorData = async () => {
      try {
        const data = await getVendorWithProducts(vendorId);
        if (data) {
          setVendor(data.vendor);
          setProducts(data.products || []);
        }
      } catch (error) {
        console.error("Error fetching vendor data:", error);
      } finally {
        setLoading(false);
      }
    };

    if (vendorId) {
      fetchVendorData();
    }
  }, [vendorId]);

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

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-12">
        <div className="animate-pulse h-64 bg-gray-200 rounded-lg mb-8"></div>
        <div className="animate-pulse h-12 bg-gray-200 rounded-lg w-1/3 mb-6"></div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {[...Array(4)].map((_, index) => (
            <div
              key={index}
              className="bg-gray-200 animate-pulse h-64 rounded-lg"
            ></div>
          ))}
        </div>
      </div>
    );
  }

  if (!vendor) {
    return (
      <div className="container mx-auto px-4 py-12 text-center">
        <h1 className="text-2xl font-bold mb-4">Vendor Not Found</h1>
        <p>The vendor you are looking for does not exist.</p>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-12">
      {/* Vendor header */}
      <div className="bg-white rounded-lg shadow-md overflow-hidden mb-12">
        <div className="bg-gradient-to-r from-blue-500 to-indigo-600 h-48 relative">
          <div className="absolute inset-0 flex items-center justify-center text-white text-8xl font-bold opacity-20">
            {vendor.name.substring(0, 2).toUpperCase()}
          </div>
          {vendor.featured && (
            <div className="absolute top-4 right-4 bg-yellow-400 text-sm font-semibold text-white px-4 py-1 rounded-full shadow-md">
              Featured Vendor
            </div>
          )}
        </div>

        <div className="p-8">
          <h1 className="text-3xl font-bold mb-4">{vendor.name}</h1>
          <p className="text-gray-600 mb-6">{vendor.description}</p>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="flex items-center text-gray-700">
              <FaEnvelope className="mr-3 text-blue-500" />
              <span>{vendor.email}</span>
            </div>
            <div className="flex items-center text-gray-700">
              <FaPhone className="mr-3 text-blue-500" />
              <span>{vendor.phone}</span>
            </div>
            <div className="flex items-center text-gray-700">
              <FaMapMarkerAlt className="mr-3 text-blue-500" />
              <span>{vendor.address}</span>
            </div>
            <div className="flex items-center text-gray-700">
              <FaCalendarAlt className="mr-3 text-blue-500" />
              <span>
                Joined {new Date(vendor.joinedDate).toLocaleDateString()}
              </span>
            </div>
          </div>

          {vendor.rating && (
            <div className="mt-6 flex items-center">
              <div className="flex">
                {[...Array(5)].map((_, i) => (
                  <FaStar
                    key={i}
                    className={`w-5 h-5 ${
                      i < Math.floor(vendor.rating!)
                        ? "text-yellow-400"
                        : "text-gray-300"
                    }`}
                  />
                ))}
              </div>
              <span className="ml-2 text-gray-700 font-medium">
                {vendor.rating} out of 5
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Vendor products */}
      <div className="mb-8">
        <h2 className="text-2xl font-bold mb-6">Products by {vendor.name}</h2>

        {products.length > 0 ? (
          <motion.div
            variants={containerVariants}
            initial="hidden"
            animate="visible"
            className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6"
          >
            {products.map((product) => (
              <motion.div key={product.id} variants={itemVariants}>
                <ProductCard product={product} />
              </motion.div>
            ))}
          </motion.div>
        ) : (
          <div className="text-center py-12 bg-gray-50 rounded-lg">
            <h3 className="text-lg font-medium text-gray-900">
              No products available
            </h3>
            <p className="mt-2 text-gray-500">
              This vendor doesn&apos;t have any products yet.
            </p>
          </div>
        )}
      </div>

      {/* Contact section */}
      <div className="bg-gray-50 rounded-lg p-8">
        <h2 className="text-2xl font-bold mb-4">Contact {vendor.name}</h2>
        <p className="text-gray-600 mb-6">
          Have questions about products from {vendor.name}? Reach out directly
          using the contact information above.
        </p>

        <div className="flex flex-col sm:flex-row gap-4">
          <Link
            href={`mailto:${vendor.email}`}
            className="inline-flex items-center justify-center px-6 py-3 bg-blue-600 text-white rounded-md font-medium hover:bg-blue-700 transition-colors"
          >
            <FaEnvelope className="mr-2" />
            Send Email
          </Link>
          <Link
            href={`tel:${vendor.phone}`}
            className="inline-flex items-center justify-center px-6 py-3 border border-gray-300 rounded-md text-gray-700 font-medium hover:bg-gray-100 transition-colors"
          >
            <FaPhone className="mr-2" />
            Call Vendor
          </Link>
        </div>
      </div>
    </div>
  );
}
