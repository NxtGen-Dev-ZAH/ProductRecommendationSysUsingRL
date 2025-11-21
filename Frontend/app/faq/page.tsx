"use client";

import { useState } from "react";
import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";
import { FaChevronDown, FaChevronUp } from "react-icons/fa";

interface FAQItem {
  question: string;
  answer: string;
}

export default function FAQPage() {
  const [openItems, setOpenItems] = useState<number[]>([]);

  const toggleItem = (index: number) => {
    setOpenItems((prev) =>
      prev.includes(index)
        ? prev.filter((item) => item !== index)
        : [...prev, index]
    );
  };

  const faqData: FAQItem[] = [
    {
      question: "Comment passer une commande sur Shopora ?",
      answer:
        "Pour passer une commande, parcourez nos catégories ou utilisez la barre de recherche pour trouver vos produits. Ajoutez-les à votre panier, puis procédez au checkout. Suivez les étapes de paiement et confirmez votre commande. Vous recevrez un email de confirmation avec votre numéro de commande.",
    },
    {
      question: "Quels sont les délais de livraison ?",
      answer:
        "Les délais de livraison varient selon le vendeur et la destination. En général : France métropolitaine (2-5 jours ouvrés), Europe (5-10 jours ouvrés), International (7-15 jours ouvrés). Vous pouvez voir les délais spécifiques sur chaque page produit.",
    },
    {
      question: "Quels modes de paiement acceptez-vous ?",
      answer:
        "Nous acceptons les cartes bancaires (Visa, Mastercard, American Express), PayPal, et les virements bancaires. Tous les paiements sont traités de manière sécurisée avec un chiffrement SSL.",
    },
    {
      question: "Comment puis-je suivre ma commande ?",
      answer:
        "Une fois votre commande expédiée, vous recevrez un email avec votre numéro de suivi. Vous pouvez également suivre votre commande depuis votre compte utilisateur dans la section 'Mes Commandes'.",
    },
    {
      question: "Puis-je annuler ma commande ?",
      answer:
        "Vous pouvez annuler votre commande tant qu'elle n'a pas été expédiée. Connectez-vous à votre compte, allez dans 'Mes Commandes' et cliquez sur 'Annuler'. Si la commande est déjà expédiée, vous devrez suivre notre procédure de retour.",
    },
    {
      question: "Comment fonctionnent les retours ?",
      answer:
        "Vous disposez de 14 jours pour retourner un produit non conforme ou défectueux. Contactez notre service client à returns@shopora.com pour obtenir un numéro de retour. Les frais de retour sont à votre charge sauf en cas de défaut du produit.",
    },
    {
      question: "Comment devenir vendeur sur Shopora ?",
      answer:
        "Pour devenir vendeur, cliquez sur 'Devenir Vendeur' dans le menu principal. Remplissez le formulaire d'inscription et soumettez les documents requis. Notre équipe examinera votre candidature et vous contactera dans les 48h.",
    },
    {
      question: "Quels sont les frais pour les vendeurs ?",
      answer:
        "Les frais varient selon le type de vendeur et le volume de ventes. Contactez notre équipe commerciale à sellers@shopora.com pour obtenir des informations détaillées sur nos tarifs et conditions.",
    },
    {
      question: "Comment contacter le service client ?",
      answer:
        "Vous pouvez nous contacter par email à contact@shopora.com, par téléphone au +33 (1) 23 45 67 89 (Lun-Ven, 9h-18h), ou via le formulaire de contact sur notre site. Nous répondons généralement dans les 24h.",
    },
    {
      question: "Mes données personnelles sont-elles sécurisées ?",
      answer:
        "Oui, nous prenons la sécurité de vos données très au sérieux. Nous utilisons un chiffrement SSL/TLS, nos serveurs sont sécurisés, et nous respectons le RGPD. Consultez notre politique de confidentialité pour plus de détails.",
    },
    {
      question: "Puis-je modifier mon adresse de livraison ?",
      answer:
        "Vous pouvez modifier votre adresse de livraison dans votre compte utilisateur, section 'Adresses'. Si votre commande est déjà en cours de traitement, contactez notre service client pour voir si une modification est encore possible.",
    },
    {
      question: "Que faire si je reçois un produit défectueux ?",
      answer:
        "Si vous recevez un produit défectueux, contactez immédiatement notre service client à defects@shopora.com avec photos du défaut. Nous organiserons un remplacement ou un remboursement, frais de retour inclus.",
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-4xl mx-auto">
            <h1 className="text-4xl font-bold text-gray-900 mb-8 text-center">
              Questions Fréquentes
            </h1>

            <div className="space-y-4">
              {faqData.map((item, index) => (
                <div
                  key={index}
                  className="bg-white rounded-lg shadow-lg overflow-hidden"
                >
                  <button
                    onClick={() => toggleItem(index)}
                    className="w-full px-6 py-4 text-left flex items-center justify-between hover:bg-gray-50 transition-colors duration-200"
                  >
                    <h2 className="text-lg font-semibold text-gray-900 pr-4">
                      {item.question}
                    </h2>
                    {openItems.includes(index) ? (
                      <FaChevronUp className="text-primary flex-shrink-0" />
                    ) : (
                      <FaChevronDown className="text-primary flex-shrink-0" />
                    )}
                  </button>

                  {openItems.includes(index) && (
                    <div className="px-6 pb-4">
                      <div className="border-t border-gray-200 pt-4">
                        <p className="text-gray-600 leading-relaxed">
                          {item.answer}
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>

            {/* Contact Section */}
            <div className="mt-12 bg-white rounded-lg shadow-lg p-8 text-center">
              <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                Vous ne trouvez pas la réponse à votre question ?
              </h2>
              <p className="text-gray-600 mb-6">
                Notre équipe de support est là pour vous aider. Contactez-nous
                et nous vous répondrons dans les plus brefs délais.
              </p>
              <div className="flex flex-col sm:flex-row gap-4 justify-center">
                <a
                  href="mailto:contact@shopora.com"
                  className="inline-flex items-center px-6 py-3 bg-primary text-white rounded-lg hover:bg-primary/90 transition-colors duration-200"
                >
                  Envoyer un Email
                </a>
                <a
                  href="tel:+33123456789"
                  className="inline-flex items-center px-6 py-3 border border-primary text-primary rounded-lg hover:bg-primary hover:text-white transition-colors duration-200"
                >
                  Appeler le Support
                </a>
              </div>
            </div>

            {/* Quick Links */}
            <div className="mt-8 grid md:grid-cols-3 gap-6">
              <div className="bg-white rounded-lg shadow-lg p-6 text-center">
                <h3 className="font-semibold text-gray-900 mb-2">
                  Politique de Livraison
                </h3>
                <p className="text-gray-600 text-sm mb-4">
                  Découvrez nos délais et conditions de livraison
                </p>
                <a
                  href="/shipping-policy"
                  className="text-primary hover:text-primary/80 transition-colors duration-200"
                >
                  En savoir plus →
                </a>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-6 text-center">
                <h3 className="font-semibold text-gray-900 mb-2">
                  Politique de Retours
                </h3>
                <p className="text-gray-600 text-sm mb-4">
                  Informations sur les retours et remboursements
                </p>
                <a
                  href="/return-policy"
                  className="text-primary hover:text-primary/80 transition-colors duration-200"
                >
                  En savoir plus →
                </a>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-6 text-center">
                <h3 className="font-semibold text-gray-900 mb-2">
                  Nous Contacter
                </h3>
                <p className="text-gray-600 text-sm mb-4">
                  Besoin d&apos;aide ? Contactez notre équipe
                </p>
                <a
                  href="/contact"
                  className="text-primary hover:text-primary/80 transition-colors duration-200"
                >
                  Nous contacter →
                </a>
              </div>
            </div>
          </div>
        </Section>
      </Container>
    </div>
  );
}
