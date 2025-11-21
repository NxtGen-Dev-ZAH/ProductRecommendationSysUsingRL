"use client";

import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";

export default function ShippingPolicyPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-4xl mx-auto">
            <h1 className="text-4xl font-bold text-gray-900 mb-8 text-center">
              Politique de Livraison
            </h1>

            <div className="prose prose-lg max-w-none space-y-8">
              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Délais de Livraison
                </h2>
                <div className="space-y-4">
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Livraison Standard
                    </h3>
                    <p className="text-gray-600">2-5 jours ouvrés</p>
                  </div>
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Livraison Express
                    </h3>
                    <p className="text-gray-600">1-2 jours ouvrés</p>
                  </div>
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Livraison Internationale
                    </h3>
                    <p className="text-gray-600">
                      5-15 jours ouvrés selon la destination
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Zones de Livraison
                </h2>
                <div className="grid md:grid-cols-2 gap-6">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      France Métropolitaine
                    </h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>• Livraison gratuite à partir de 50€</li>
                      <li>• Frais de port: 4,99€</li>
                      <li>• Délai: 2-3 jours ouvrés</li>
                    </ul>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">Europe</h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>• Livraison gratuite à partir de 100€</li>
                      <li>• Frais de port: 9,99€</li>
                      <li>• Délai: 5-10 jours ouvrés</li>
                    </ul>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Suivi de Commande
                </h2>
                <p className="text-gray-600 mb-4">
                  Une fois votre commande expédiée, vous recevrez un email de
                  confirmation avec votre numéro de suivi. Vous pourrez suivre
                  l&apos;état de votre livraison en temps réel.
                </p>
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <p className="text-blue-800">
                    <strong>Conseil:</strong> Gardez votre numéro de suivi
                    précieusement. Il vous permettra de localiser votre colis à
                    tout moment.
                  </p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Livraison et Réception
                </h2>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Livraison à Domicile
                    </h3>
                    <p className="text-gray-600">
                      Le transporteur tentera de vous livrer à l&apos;adresse
                      indiquée. En cas d&apos;absence, un avis de passage sera
                      déposé.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Point Relais
                    </h3>
                    <p className="text-gray-600">
                      Vous pouvez choisir de récupérer votre commande dans un
                      point relais partenaire près de chez vous.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Vérification du Colis
                    </h3>
                    <p className="text-gray-600">
                      Nous vous recommandons de vérifier l&apos;état de votre colis
                      avant de signer le bon de livraison.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Problèmes de Livraison
                </h2>
                <div className="space-y-4">
                  <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                    <h3 className="font-semibold text-yellow-800 mb-2">
                      Colis Endommagé
                    </h3>
                    <p className="text-yellow-700">
                      Si votre colis arrive endommagé, refusez la livraison et
                      contactez-nous immédiatement. Nous organiserons un
                      remplacement.
                    </p>
                  </div>
                  <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                    <h3 className="font-semibold text-red-800 mb-2">
                      Colis Perdu
                    </h3>
                    <p className="text-red-700">
                      En cas de perte de colis, contactez notre service client.
                      Nous enquêterons et vous rembourserons ou renverrons votre
                      commande.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Contact
                </h2>
                <p className="text-gray-600">
                    Pour toute question concernant la livraison, n&apos;hésitez pas à
                  nous contacter :
                </p>
                <div className="mt-4 space-y-2">
                  <p className="text-gray-600">
                    <strong>Email:</strong> shipping@shopora.com
                  </p>
                  <p className="text-gray-600">
                    <strong>Téléphone:</strong> +33 (1) 23 45 67 89
                  </p>
                  <p className="text-gray-600">
                    <strong>Horaires:</strong> Lundi - Vendredi, 9h - 18h
                  </p>
                </div>
              </div>
            </div>
          </div>
        </Section>
      </Container>
    </div>
  );
}
