"use client";

import { motion } from "framer-motion";
import {
  FaShippingFast,
  FaCreditCard,
  FaHeadset,
  FaExchangeAlt,
} from "react-icons/fa";

const features = [
  {
    icon: <FaShippingFast className="text-[#3b82f6] text-2xl" />,
    title: "Fast & Global Delivery",
    description:
      "Free worldwide shipping on all orders over €100 with premium courier partners",
    color: "from-blue-50 to-indigo-100",
  },
  {
    icon: <FaCreditCard className="text-[#3b82f6] text-2xl" />,
    title: "Secure Payments",
    description:
      "Multiple payment methods with bank-level security and fraud protection",
    color: "from-green-50 to-emerald-100",
  },
  {
    icon: <FaExchangeAlt className="text-[#3b82f6] text-2xl" />,
    title: "Easy Returns",
    description:
      "Hassle-free 30-day returns and money-back guarantee on eligible items",
    color: "from-amber-50 to-orange-100",
  },
  {
    icon: <FaHeadset className="text-[#3b82f6] text-2xl" />,
    title: "24/7 Premium Support",
    description:
      "Dedicated support team available around the clock for all your inquiries",
    color: "from-purple-50 to-violet-100",
  },
];

const FeaturesSection = () => {
  const fadeIn = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  };

  return (
    <section className="container mx-auto px-4 py-16">
      <motion.h2
        variants={fadeIn}
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true }}
        className="text-2xl md:text-3xl font-bold text-center mb-12"
      >
        Why Choose <span className="text-[#3b82f6]">Shopora</span>
      </motion.h2>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
        {features.map((feature, index) => (
          <motion.div
            key={index}
            variants={fadeIn}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            transition={{ duration: 0.4, delay: index * 0.1 }}
            className="group"
          >
            <div
              className={`bg-gradient-to-br ${feature.color} rounded-2xl p-6 h-full transition-all duration-300 hover:shadow-lg hover:-translate-y-1`}
            >
              <div className="rounded-full w-14 h-14 bg-white/80 backdrop-blur-sm flex items-center justify-center mb-5 shadow-sm group-hover:shadow transition-all">
                {feature.icon}
              </div>
              <h3 className="text-lg font-bold mb-3 text-gray-900">
                {feature.title}
              </h3>
              <p className="text-gray-700">{feature.description}</p>
            </div>
          </motion.div>
        ))}
      </div>
    </section>
  );
};

export default FeaturesSection;
