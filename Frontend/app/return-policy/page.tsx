"use client";

import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";

export default function ReturnPolicyPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-4xl mx-auto">
            <h1 className="text-4xl font-bold text-gray-900 mb-8 text-center">
              Politique de Retours et Remboursements
            </h1>

            <div className="prose prose-lg max-w-none space-y-8">
              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Droit de Rétractation
                </h2>
                <p className="text-gray-600 mb-4">
                  Conformément à la législation française, vous disposez d&apos;un
                  délai de 14 jours à compter de la réception de votre commande
                  pour exercer votre droit de rétractation, sans avoir à
                  justifier de motifs ni à payer de pénalités.
                </p>
                <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                  <p className="text-green-800">
                    <strong>Important:</strong> Les frais de retour sont à votre
                    charge, sauf en cas de produit défectueux ou d&apos;erreur de
                    notre part.
                  </p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Conditions de Retour
                </h2>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Produits Éligibles
                    </h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>
                        • Produits non utilisés et dans leur emballage d&apos;origine
                      </li>
                      <li>• Produits non personnalisés</li>
                      <li>• Produits non périssables</li>
                      <li>• Produits non endommagés par l&apos;utilisateur</li>
                    </ul>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Produits Non Éligibles
                    </h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>• Produits personnalisés ou sur mesure</li>
                      <li>• Produits périssables</li>
                      <li>• Produits d&apos;hygiène personnelle</li>
                      <li>• Logiciels et contenus numériques</li>
                    </ul>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Procédure de Retour
                </h2>
                <div className="space-y-4">
                  <div className="flex items-start">
                    <div className="bg-primary text-white rounded-full w-8 h-8 flex items-center justify-center text-sm font-semibold mr-4 flex-shrink-0">
                      1
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        Contactez-nous
                      </h3>
                      <p className="text-gray-600">
                        Envoyez-nous un email à returns@shopora.com avec votre
                        numéro de commande et la raison du retour.
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start">
                    <div className="bg-primary text-white rounded-full w-8 h-8 flex items-center justify-center text-sm font-semibold mr-4 flex-shrink-0">
                      2
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        Recevez l&apos;autorisation
                      </h3>
                      <p className="text-gray-600">
                        Nous vous enverrons un numéro de retour et les
                        instructions d&apos;expédition.
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start">
                    <div className="bg-primary text-white rounded-full w-8 h-8 flex items-center justify-center text-sm font-semibold mr-4 flex-shrink-0">
                      3
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        Expédiez le colis
                      </h3>
                      <p className="text-gray-600">
                        Emballez soigneusement les articles et expédiez-les à
                        l&apos;adresse fournie.
                      </p>
                    </div>
                  </div>
                  <div className="flex items-start">
                    <div className="bg-primary text-white rounded-full w-8 h-8 flex items-center justify-center text-sm font-semibold mr-4 flex-shrink-0">
                      4
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        Remboursement
                      </h3>
                      <p className="text-gray-600">
                        Une fois le colis reçu et vérifié, nous procéderons au
                        remboursement.
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Délais de Remboursement
                </h2>
                <div className="space-y-4">
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Carte Bancaire
                    </h3>
                    <p className="text-gray-600">
                      3-5 jours ouvrés après réception du retour
                    </p>
                  </div>
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">PayPal</h3>
                    <p className="text-gray-600">
                      1-3 jours ouvrés après réception du retour
                    </p>
                  </div>
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Virement Bancaire
                    </h3>
                    <p className="text-gray-600">
                      5-7 jours ouvrés après réception du retour
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Échange de Produits
                </h2>
                <p className="text-gray-600 mb-4">
                  Si vous souhaitez échanger un produit contre une autre taille,
                  couleur ou modèle, vous devez d&apos;abord retourner l&apos;article
                  original, puis passer une nouvelle commande.
                </p>
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                  <p className="text-blue-800">
                    <strong>Note:</strong> Les frais de port de l&apos;échange sont à
                    votre charge, sauf en cas de défaut du produit.
                  </p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Produits Défectueux
                </h2>
                <p className="text-gray-600 mb-4">
                  Si vous recevez un produit défectueux ou non conforme à votre
                  commande, nous nous engageons à le remplacer ou à vous
                  rembourser intégralement, frais de retour inclus.
                </p>
                <div className="space-y-2">
                  <p className="text-gray-600">
                    <strong>Contact:</strong> defects@shopora.com
                  </p>
                  <p className="text-gray-600">
                    <strong>Délai de réclamation:</strong> 30 jours après
                    réception
                  </p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Contact
                </h2>
                <p className="text-gray-600 mb-4">
                  Pour toute question concernant les retours et remboursements :
                </p>
                <div className="space-y-2">
                  <p className="text-gray-600">
                    <strong>Email:</strong> returns@shopora.com
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
