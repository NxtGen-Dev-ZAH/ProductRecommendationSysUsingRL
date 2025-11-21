"use client";

import { motion } from "framer-motion";
import { FaQuoteLeft, FaStar } from "react-icons/fa";

const testimonials = [
  {
    name: "John Davis",
    role: "Regular Customer",
    text: "The product quality and the attention to detail exceeded my expectations. The delivery was prompt, and customer service was excellent. Highly recommend!",
    rating: 5,
    bgColor: "bg-blue-50",
  },
  {
    name: "Sarah Martinez",
    role: "Premium Member",
    text: "I love the multi-vendor approach. It gives me so many options and the quality has been consistently outstanding. The platform is also very easy to navigate.",
    rating: 5,
    bgColor: "bg-emerald-50",
  },
  {
    name: "Alex Kimura",
    role: "Business Owner",
    text: "The customer service is exceptional. Had an issue with a bulk order and it was resolved immediately. Their team went above and beyond to ensure satisfaction.",
    rating: 5,
    bgColor: "bg-purple-50",
  },
];

const TestimonialsSection = () => {
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
        className="text-center mb-12"
      >
        <span className="inline-block px-4 py-1.5 mb-4 text-sm font-medium rounded-full bg-primary/10 text-[#3b82f6]">
          Testimonials
        </span>
        <h2 className="text-2xl md:text-3xl font-bold">
          What Our Customers Say
        </h2>
      </motion.div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {testimonials.map((testimonial, index) => (
          <motion.div
            key={index}
            variants={fadeIn}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            transition={{ duration: 0.4, delay: index * 0.1 }}
            className={`${testimonial.bgColor} p-8 rounded-2xl shadow-sm hover:shadow-md transition-shadow relative`}
          >
            <FaQuoteLeft className="text-[#3b82f6]/10 text-6xl absolute right-6 top-6" />

            <div className="flex items-center mb-6">
              <div className="w-14 h-14 rounded-full bg-white flex items-center justify-center text-[#3b82f6] font-bold text-xl shadow-sm mr-4">
                {testimonial.name.charAt(0)}
              </div>
              <div>
                <h3 className="font-semibold text-gray-900">
                  {testimonial.name}
                </h3>
                <p className="text-sm text-gray-600">{testimonial.role}</p>
              </div>
            </div>

            <div className="flex text-yellow-400 mb-4">
              {[...Array(testimonial.rating)].map((_, i) => (
                <FaStar key={i} />
              ))}
            </div>

            <p className="text-gray-700 relative z-10">
              &quot;{testimonial.text}&quot;
            </p>
          </motion.div>
        ))}
      </div>
    </section>
  );
};

export default TestimonialsSection;
