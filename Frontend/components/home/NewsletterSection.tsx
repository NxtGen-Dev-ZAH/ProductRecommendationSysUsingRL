'use client';

import { motion } from 'framer-motion';
import { Button } from '../ui/button';
import { useState } from 'react';

const NewsletterSection = () => {
  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fadeIn = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) return;
    
    setIsSubmitting(true);
    // In a real app, you would send this to your API
    setTimeout(() => {
      setIsSubmitting(false);
      setEmail('');
      // Show success message or toast notification
    }, 1000);
  };

  return (
    <section className="container mx-auto px-4 py-16">
      <motion.div 
        variants={fadeIn}
        initial="hidden"
        whileInView="visible"
        viewport={{ once: true }}
        className="relative overflow-hidden rounded-3xl"
      >
        <div className="bg-gradient-to-r from-primary/10 to-indigo-100 p-10 md:p-16 text-center relative overflow-hidden">
          {/* Abstract background */}
          <div className="absolute inset-0 z-0">
            <div className="absolute top-0 left-1/4 w-1/2 h-1/2 bg-primary/20 rounded-full blur-3xl"></div>
            <div className="absolute bottom-0 right-1/4 w-1/3 h-1/3 bg-indigo-400/20 rounded-full blur-3xl"></div>
          </div>
          
          <div className="relative z-10 max-w-3xl mx-auto">
            <span className="inline-block px-4 py-1.5 mb-4 text-sm font-medium rounded-full bg-primary/20 text-[#3b82f6] backdrop-blur-sm">
              Stay Updated
            </span>
            <h2 className="text-2xl md:text-3xl lg:text-4xl font-bold mb-4 text-gray-900">
              Subscribe to Our Newsletter
            </h2>
            <p className="text-gray-700 max-w-2xl mx-auto mb-8 text-lg">
              Be the first to know about new products, exclusive offers, and vendor promotions. 
              Join our community of shoppers and get personalized updates.
            </p>
            
            <form onSubmit={handleSubmit} className="max-w-md mx-auto flex flex-col sm:flex-row gap-3">
              <input
                type="email"
                placeholder="Your email address"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="flex-grow px-6 py-3 rounded-full border border-gray-200 bg-white text-gray-800 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-[#60a5fa] focus:border-transparent shadow-sm"
                required
              />
              <Button 
                type="submit"
                className="rounded-full px-8 py-3 bg-[#60a5fa] hover:bg-primary-dark text-white transition-colors shadow-sm"
                disabled={isSubmitting}
              >
                {isSubmitting ? 'Subscribing...' : 'Subscribe'}
              </Button>
            </form>
            
            <p className="text-xs text-gray-500 mt-4">
              By subscribing, you agree to our Privacy Policy and consent to receive updates from our company.
            </p>
          </div>
          
          {/* Decorative elements */}
          <div className="hidden md:block absolute -bottom-10 -left-10 w-40 h-40 rounded-full border border-primary/20 opacity-60"></div>
          <div className="hidden md:block absolute -top-20 -right-20 w-60 h-60 rounded-full border border-primary/20 opacity-40"></div>
          <div className="hidden md:block absolute top-1/4 right-10 w-6 h-6 rounded-full bg-primary/40"></div>
          <div className="hidden md:block absolute bottom-1/4 left-10 w-4 h-4 rounded-full bg-indigo-500/40"></div>
        </div>
      </motion.div>
    </section>
  );
};

export default NewsletterSection; 