"use client";

import "./globals.css";
import { Inter } from "next/font/google";
import Header from "../components/layout/Header";
import Footer from "../components/layout/Footer";
import { AuthProvider } from "./context/AuthContext";
import { CartProvider } from "./context/CartContext";
import { ToastProvider } from "../components/ui/toast";
import { BackgroundGradient } from "../components/ui/background-gradient";

// Load Inter font with Latin subset
const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="fr" className={inter.variable}>
      <body className="antialiased text-foreground bg-background">
        <AuthProvider>
          <CartProvider>
            <ToastProvider>
              <div className="relative flex flex-col min-h-screen">
                <Header />

                <main className="flex-grow pt-14 relative z-10">
                  <div className="fixed inset-0 -z-10 overflow-hidden">
                    <BackgroundGradient variant="default" />
                  </div>
                  {children}
                </main>

                <Footer />
              </div>
            </ToastProvider>
          </CartProvider>
        </AuthProvider>
      </body>
    </html>
  );
}