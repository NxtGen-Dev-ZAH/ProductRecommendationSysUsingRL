"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { Button } from "../ui/button";

const PromoBanner = () => {
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
        className="relative overflow-hidden rounded-3xl"
      >
        <div className="bg-gradient-to-r from-cyan-500 via-fuchsia-700 to-indigo-600 p-8 md:p-12 lg:p-16">
          <div className="flex flex-col md:flex-row gap-8 lg:gap-16 items-center relative z-10">
            <div className="md:w-1/2">
              <div className="inline-block px-4 py-1 text-xs font-semibold bg-white/20 rounded-full text-white mb-4 backdrop-blur-sm">
                Exclusive Sale
              </div>
              <h2 className="text-3xl md:text-4xl lg:text-5xl font-bold mb-4 text-white leading-tight">
                Special Offers <br />
                Up To <span className="text-yellow-300">50% Off</span>
              </h2>
              <p className="text-white/90 mb-8 text-lg max-w-md">
                Get incredible discounts on selected premium items from our top
                vendors. Limited time opportunity - shop now before they&apos;re
                gone!
              </p>
              <Link href="/search?discount=true">
                <Button
                  variant="secondary"
                  size="lg"
                  className="font-medium px-8 hover:bg-white hover:text-[#3b82f6] transition-all rounded-full"
                >
                  Explore Offers
                </Button>
              </Link>
            </div>

            <div className="md:w-1/2 aspect-[4/3] w-full bg-white/10 rounded-2xl overflow-hidden flex items-center justify-center backdrop-blur-sm relative">
              {/* Background decorative elements */}
              <div className="absolute top-0 right-0 w-20 h-20 bg-yellow-300/10 rounded-full"></div>
              <div className="absolute bottom-0 left-0 w-32 h-32 bg-white/10 rounded-full"></div>

              {/* Centered sale banner */}
              <div className="relative z-10 text-center p-6">
                <div className="text-8xl font-bold text-white/90">50%</div>
                <div className="text-xl font-medium text-white/80 mt-2">
                  OFF
                </div>
                <div className="mt-4 bg-white/20 px-4 py-2 rounded-full backdrop-blur-sm">
                  <span className="text-white/90 text-sm font-medium">
                    Limited Time Only
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* Abstract shapes */}
          <div className="absolute top-0 right-0 w-1/3 h-full">
            <svg
              className="h-full w-full"
              viewBox="0 0 100 100"
              preserveAspectRatio="none"
              fill="none"
            >
              <circle cx="80" cy="50" r="40" fill="rgba(255,255,255,0.05)" />
              <circle cx="100" cy="20" r="30" fill="rgba(255,255,255,0.07)" />
            </svg>
          </div>
          <div className="absolute bottom-0 left-0 w-1/2 h-1/2">
            <svg
              className="h-full w-full"
              viewBox="0 0 100 100"
              preserveAspectRatio="none"
              fill="none"
            >
              <circle cx="10" cy="90" r="30" fill="rgba(255,255,255,0.05)" />
              <circle cx="40" cy="100" r="20" fill="rgba(255,255,255,0.07)" />
            </svg>
          </div>
        </div>
      </motion.div>
    </section>
  );
};

export default PromoBanner;
