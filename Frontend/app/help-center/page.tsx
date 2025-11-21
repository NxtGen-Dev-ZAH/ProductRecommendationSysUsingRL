  "use client";

import Link from "next/link";
import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";
import {
  FaQuestionCircle,
  FaShippingFast,
  FaCreditCard,
  FaUser,
  FaStore,
  FaShieldAlt,
} from "react-icons/fa";

export default function HelpCenterPage() {
  const helpCategories = [
    {
      icon: <FaQuestionCircle className="text-2xl" />,
      title: "Questions Générales",
      description: "Réponses aux questions les plus courantes",
      link: "/faq",
      color: "bg-blue-500",
    },
    {
      icon: <FaShippingFast className="text-2xl" />,
      title: "Livraison",
      description: "Informations sur les délais et frais de livraison",
      link: "/shipping-policy",
      color: "bg-green-500",
    },
    {
      icon: <FaCreditCard className="text-2xl" />,
      title: "Paiements",
      description: "Modes de paiement et sécurité des transactions",
      link: "/contact",
      color: "bg-purple-500",
    },
    {
      icon: <FaUser className="text-2xl" />,
      title: "Mon Compte",
      description: "Gestion de votre compte et de vos commandes",
      link: "/account",
      color: "bg-orange-500",
    },
    {
      icon: <FaStore className="text-2xl" />,
      title: "Vendeurs",
      description: "Informations pour devenir vendeur",
      link: "/become-seller",
      color: "bg-red-500",
    },
    {
      icon: <FaShieldAlt className="text-2xl" />,
      title: "Sécurité",
      description: "Protection de vos données et transactions",
      link: "/privacy-policy",
      color: "bg-indigo-500",
    },
  ];

  const quickActions = [
    {
      title: "Suivre ma commande",
      description: "Vérifiez l'état de votre commande",
      link: "/account/orders",
    },
    {
      title: "Retourner un produit",
      description: "Initiez un retour ou un échange",
      link: "/return-policy",
    },
    {
      title: "Contacter le support",
      description: "Obtenez de l'aide personnalisée",
      link: "/contact",
    },
    {
      title: "Devenir vendeur",
      description: "Rejoignez notre plateforme",
      link: "/become-seller",
    },
  ];

  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-6xl mx-auto">
            <div className="text-center mb-12">
              <h1 className="text-4xl font-bold text-gray-900 mb-4">
                Centre d&apos;Aide
              </h1>
              <p className="text-xl text-gray-600 max-w-2xl mx-auto">
                Trouvez rapidement les réponses à vos questions et obtenez
                l&apos;aide dont vous avez besoin
              </p>
            </div>

            {/* Quick Actions */}
            <div className="mb-12">
              <h2 className="text-2xl font-semibold text-gray-900 mb-6 text-center">
                Actions Rapides
              </h2>
              <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
                {quickActions.map((action, index) => (
                  <Link
                    key={index}
                    href={action.link}
                    className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-shadow duration-200 group"
                  >
                    <h3 className="font-semibold text-gray-900 mb-2 group-hover:text-primary transition-colors duration-200">
                      {action.title}
                    </h3>
                    <p className="text-gray-600 text-sm">
                      {action.description}
                    </p>
                  </Link>
                ))}
              </div>
            </div>

            {/* Help Categories */}
            <div className="mb-12">
              <h2 className="text-2xl font-semibold text-gray-900 mb-6 text-center">
                Catégories d&apos;Aide
              </h2>
              <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
                {helpCategories.map((category, index) => (
                  <Link
                    key={index}
                    href={category.link}
                    className="bg-white rounded-lg shadow-lg p-6 hover:shadow-xl transition-all duration-200 group"
                  >
                    <div
                      className={`${category.color} text-white rounded-full w-12 h-12 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform duration-200`}
                    >
                      {category.icon}
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2 group-hover:text-primary transition-colors duration-200">
                      {category.title}
                    </h3>
                    <p className="text-gray-600 text-sm">
                      {category.description}
                    </p>
                  </Link>
                ))}
              </div>
            </div>

            {/* Contact Information */}
            <div className="bg-white rounded-lg shadow-lg p-8">
              <h2 className="text-2xl font-semibold text-gray-900 mb-6 text-center">
                Besoin d&apos;Aide Personnalisée ?
              </h2>
              <div className="grid md:grid-cols-3 gap-6">
                <div className="text-center">
                  <div className="bg-primary text-white rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4">
                    <FaQuestionCircle className="text-2xl" />
                  </div>
                  <h3 className="font-semibold text-gray-900 mb-2">Email</h3>
                  <p className="text-gray-600 text-sm mb-2">
                    contact@shopora.com
                  </p>
                  <p className="text-gray-500 text-xs">Réponse sous 24h</p>
                </div>

                <div className="text-center">
                  <div className="bg-primary text-white rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4">
                    <FaCreditCard className="text-2xl" />
                  </div>
                  <h3 className="font-semibold text-gray-900 mb-2">
                    Téléphone
                  </h3>
                  <p className="text-gray-600 text-sm mb-2">
                    +33 (1) 23 45 67 89
                  </p>
                  <p className="text-gray-500 text-xs">Lun-Ven, 9h-18h</p>
                </div>

                <div className="text-center">
                  <div className="bg-primary text-white rounded-full w-16 h-16 flex items-center justify-center mx-auto mb-4">
                    <FaShieldAlt className="text-2xl" />
                  </div>
                  <h3 className="font-semibold text-gray-900 mb-2">
                    Chat en Direct
                  </h3>
                  <p className="text-gray-600 text-sm mb-2">Disponible 24/7</p>
                  <p className="text-gray-500 text-xs">Support instantané</p>
                </div>
              </div>
            </div>

            {/* Additional Resources */}
            <div className="mt-12">
              <h2 className="text-2xl font-semibold text-gray-900 mb-6 text-center">
                Ressources Supplémentaires
              </h2>
              <div className="grid md:grid-cols-2 gap-6">
                <div className="bg-white rounded-lg shadow-lg p-6">
                  <h3 className="font-semibold text-gray-900 mb-3">
                    Guides d&apos;Utilisation
                  </h3>
                  <ul className="space-y-2 text-sm text-gray-600">
                    <li>• Comment créer un compte</li>
                    <li>• Comment passer une commande</li>
                    <li>• Comment gérer vos adresses</li>
                    <li>• Comment suivre vos commandes</li>
                  </ul>
                </div>

                <div className="bg-white rounded-lg shadow-lg p-6">
                  <h3 className="font-semibold text-gray-900 mb-3">
                    Politiques Importantes
                  </h3>
                  <ul className="space-y-2 text-sm text-gray-600">
                    <li>
                      •{" "}
                      <Link
                        href="/privacy-policy"
                        className="text-primary hover:underline"
                      >
                        Politique de confidentialité
                      </Link>
                    </li>
                    <li>
                      •{" "}
                      <Link
                        href="/terms-of-service"
                        className="text-primary hover:underline"
                      >
                        Conditions d&apos;utilisation
                      </Link>
                    </li>
                    <li>
                      •{" "}
                      <Link
                        href="/shipping-policy"
                        className="text-primary hover:underline"
                      >
                        Politique de livraison
                      </Link>
                    </li>
                    <li>
                      •{" "}
                      <Link
                        href="/return-policy"
                        className="text-primary hover:underline"
                      >
                        Politique de retours
                      </Link>
                    </li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </Section>
      </Container>
    </div>
  );
}
