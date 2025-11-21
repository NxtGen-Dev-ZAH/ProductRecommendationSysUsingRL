"use client";

// import Image from 'next/image'; // Not currently used
import Link from "next/link";
import {
  FaFacebook,
  FaTwitter,
  FaInstagram,
  FaLinkedin,
  FaYoutube,
  FaEnvelope,
} from "react-icons/fa";
import {
  FaCcVisa,
  FaCcMastercard,
  FaCcAmex,
  FaCcPaypal,
  FaApplePay,
} from "react-icons/fa";
import { BackgroundGradient } from "../ui/background-gradient";

const Footer = () => {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-gray-900 text-white pt-16 pb-8 relative overflow-hidden">
      {/* Background decorative elements */}
      <div className="absolute inset-0 overflow-hidden -z-10">
        <BackgroundGradient variant="muted" />
      </div>

      <div className="container mx-auto px-4 relative z-10">
        {/* Footer main content */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-12">
          {/* Company Info */}
          <div className="lg:col-span-2">
            <div className="flex items-center mb-6">
              <span className="text-2xl font-bold">
                <span className="text-primary">Shopora</span>
              </span>
            </div>
            <p className="text-gray-400 mb-6 max-w-md leading-relaxed">
              Votre marketplace multi-vendeurs ultime pour découvrir des
              produits premium de vendeurs vérifiés du monde entier. Produits de
              qualité, paiements sécurisés et service exceptionnel.
            </p>
            <div className="flex space-x-4 mb-8">
              <SocialLink href="#" icon={<FaFacebook />} />
              <SocialLink href="#" icon={<FaTwitter />} />
              <SocialLink href="#" icon={<FaInstagram />} />
              <SocialLink href="#" icon={<FaLinkedin />} />
              <SocialLink href="#" icon={<FaYoutube />} />
            </div>

            <div className="space-y-5">
              {/* <div className="flex items-start">
                <FaMapMarkerAlt className="text-primary mt-1 mr-3 flex-shrink-0" />
                <span className="text-gray-400">
                  123 Rue du Commerce, Quartier des Affaires, Paris, 75001
                </span>
              </div> */}
              <div className="flex items-center">
                <FaEnvelope className="text-primary mr-3 flex-shrink-0" />
                <a
                  href="mailto:contact@Shopora.com"
                  className="text-gray-400 hover:text-primary transition-colors duration-200"
                >
                  contact@Shopora.com
                </a>
              </div>
              {/* <div className="flex items-center">
                <FaPhone className="text-primary mr-3 flex-shrink-0" />
                <a
                  href="tel:+1234567890"
                  className="text-gray-400 hover:text-primary transition-colors duration-200"
                >
                  +33 (1) 23 45 67 89
                </a>
              </div> */}
            </div>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="text-lg font-semibold mb-6 relative inline-block">
              Liens Rapides
              <span className="absolute -bottom-2 left-0 w-12 h-1 bg-primary rounded-full"></span>
            </h3>
            <ul className="space-y-3.5">
              <FooterLink href="/" label="Accueil" />
              <FooterLink href="/category" label="Acheter par Catégorie" />
              <FooterLink href="/vendor" label="Vendeurs" />
              <FooterLink href="/search?sort=newest" label="Nouveautés" />
              <FooterLink
                href="/search?discount=true"
                label="Promotions & Réductions"
              />
              <FooterLink href="/about" label="À Propos" />
            </ul>
          </div>

          {/* Customer Service */}
          <div>
            <h3 className="text-lg font-semibold mb-6 relative inline-block">
              Service Client
              <span className="absolute -bottom-2 left-0 w-12 h-1 bg-primary rounded-full"></span>
            </h3>
            <ul className="space-y-3.5">
              <FooterLink
                href="/account/orders"
                label="Suivre Votre Commande"
              />
              <FooterLink
                href="/shipping-policy"
                label="Politique de Livraison"
              />
              <FooterLink
                href="/return-policy"
                label="Retours & Remboursements"
              />
              <FooterLink href="/contact" label="Nous Contacter" />
              <FooterLink href="/faq" label="FAQ" />
              <FooterLink href="/help-center" label="Centre d'Aide" />
            </ul>
          </div>
        </div>

        {/* Payment methods */}
        <div className="border-t border-gray-800 mt-12 pt-8 pb-6">
          <h4 className="text-center text-sm uppercase tracking-wider text-gray-400 mb-6">
            Méthodes de Paiement Sécurisées
          </h4>
          <div className="flex flex-wrap justify-center gap-6">
            <div className="flex flex-col items-center gap-2">
              <FaCcVisa className="text-4xl text-blue-500" />
              <span className="text-xs text-gray-400">Visa</span>
            </div>
            <div className="flex flex-col items-center gap-2">
              <FaCcMastercard className="text-4xl text-[#eb001b]" />
              <span className="text-xs text-gray-400">Mastercard</span>
            </div>
            <div className="flex flex-col items-center gap-2">
              <FaCcAmex className="text-4xl text-[#2e77bc]" />
              <span className="text-xs text-gray-400">Amex</span>
            </div>
            <div className="flex flex-col items-center gap-2">
              <FaCcPaypal className="text-4xl text-[#0079C1]" />
              <span className="text-xs text-gray-400">PayPal</span>
            </div>
            <div className="flex flex-col items-center gap-2">
              <FaApplePay className="text-4xl text-white" />
              <span className="text-xs text-gray-400">Apple Pay</span>
            </div>
          </div>
        </div>

        {/* Copyright and Legal */}
        <div className="border-t border-gray-800 mt-2 pt-8">
          <div className="flex flex-col md:flex-row justify-between items-center">
            <p className="text-gray-500 text-sm mb-4 md:mb-0">
              &copy; {currentYear} Shopora. Tous droits réservés.
            </p>
            <div className="flex flex-wrap gap-6 justify-center">
              <Link
                href="/privacy-policy"
                className="text-gray-500 hover:text-primary text-sm transition-colors duration-200"
              >
                Politique de Confidentialité
              </Link>
              <Link
                href="/terms-of-service"
                className="text-gray-500 hover:text-primary text-sm transition-colors duration-200"
              >
                Conditions d&apos;Utilisation
              </Link>
              <Link
                href="/cookie-policy"
                className="text-gray-500 hover:text-primary text-sm transition-colors duration-200"
              >
                Politique des Cookies
              </Link>
              <Link
                href="/sitemap"
                className="text-gray-500 hover:text-primary text-sm transition-colors duration-200"
              >
                Plan du Site
              </Link>
            </div>
          </div>
        </div>
      </div>
    </footer>
  );
};

// Helper components
const SocialLink = ({
  href,
  icon,
}: {
  href: string;
  icon: React.ReactNode;
}) => (
  <a
    href={href}
    className="w-10 h-10 rounded-full bg-gray-800 flex items-center justify-center text-gray-400 hover:bg-primary hover:text-white transition-all duration-300 transform hover:-translate-y-1"
    target="_blank"
    rel="noopener noreferrer"
  >
    {icon}
  </a>
);

const FooterLink = ({ href, label }: { href: string; label: string }) => (
  <li>
    <Link
      href={href}
      className="text-gray-400 hover:text-primary transition-colors duration-200 flex items-center group"
    >
      <span className="w-0 h-0.5 bg-primary rounded-full mr-2 transition-all duration-300 group-hover:w-2"></span>
      {label}
    </Link>
  </li>
);

export default Footer;
