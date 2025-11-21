"use client";

import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";

export default function TermsOfServicePage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-4xl mx-auto">
            <h1 className="text-4xl font-bold text-gray-900 mb-8 text-center">
              Conditions d&apos;Utilisation
            </h1>

            <div className="prose prose-lg max-w-none space-y-8">
              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Acceptation des Conditions
                </h2>
                <p className="text-gray-600">
                  En accédant et en utilisant le site web Shopora, vous acceptez
                  d&apos;être lié par ces conditions d&apos;utilisation. Si vous
                  n&apos;acceptez pas ces conditions, veuillez ne pas utiliser notre
                  site.
                </p>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Description du Service
                </h2>
                <p className="text-gray-600 mb-4">
                  Shopora est une plateforme de commerce électronique
                  multi-vendeurs qui permet :
                </p>
                <ul className="text-gray-600 space-y-2">
                  <li>• L&apos;achat de produits auprès de vendeurs vérifiés</li>
                  <li>• La vente de produits pour les vendeurs autorisés</li>
                  <li>• La gestion des commandes et des paiements</li>
                  <li>• L&apos;accès à des services de support client</li>
                </ul>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Compte Utilisateur
                </h2>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Création de Compte
                    </h3>
                    <p className="text-gray-600">
                      Pour utiliser certains services, vous devez créer un
                      compte avec des informations exactes et à jour. Vous êtes
                      responsable de maintenir la confidentialité de vos
                      identifiants.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Responsabilités
                    </h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>• Fournir des informations exactes et complètes</li>
                      <li>• Maintenir la sécurité de votre mot de passe</li>
                      <li>
                        • Notifier immédiatement toute utilisation non autorisée
                      </li>
                      <li>
                        • Être responsable de toutes les activités sous votre
                        compte
                      </li>
                    </ul>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Commandes et Paiements
                </h2>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Processus de Commande
                    </h3>
                    <p className="text-gray-600">
                      Les commandes sont traitées selon la disponibilité des
                      stocks. Nous nous réservons le droit de refuser ou
                      d&apos;annuler toute commande à notre discrétion.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Prix et Taxes
                    </h3>
                    <p className="text-gray-600">
                      Tous les prix sont affichés en euros TTC. Les taxes
                      applicables sont incluses dans le prix affiché.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Méthodes de Paiement
                    </h3>
                    <p className="text-gray-600">
                      Nous acceptons les cartes bancaires, PayPal et autres
                      méthodes de paiement sécurisées. Les paiements sont
                      traités de manière sécurisée.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Propriété Intellectuelle
                </h2>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Contenu de Shopora
                    </h3>
                    <p className="text-gray-600">
                      Tous les contenus du site (textes, images, logos, design)
                      sont protégés par les droits d&apos;auteur et appartiennent à
                      Shopora ou à ses partenaires.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Utilisation Autorisée
                    </h3>
                    <p className="text-gray-600">
                      Vous pouvez consulter et utiliser le site pour vos achats
                      personnels. Toute reproduction ou distribution non
                      autorisée est interdite.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Comportement Interdit
                </h2>
                <p className="text-gray-600 mb-4">
                  Il est interdit d&apos;utiliser notre site pour :
                </p>
                <ul className="text-gray-600 space-y-2">
                  <li>• Violer toute loi ou réglementation applicable</li>
                  <li>
                    • Transmettre des contenus illégaux, offensants ou
                    inappropriés
                  </li>
                  <li>
                    • Tenter d&apos;accéder de manière non autorisée aux systèmes
                  </li>
                  <li>• Utiliser des robots ou scripts automatisés</li>
                  <li>• Interférer avec le fonctionnement normal du site</li>
                  <li>
                    • Collecter des informations sur d&apos;autres utilisateurs
                  </li>
                </ul>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Limitation de Responsabilité
                </h2>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Disponibilité du Service
                    </h3>
                    <p className="text-gray-600">
                      Nous nous efforçons de maintenir le site accessible
                      24h/24, mais nous ne garantissons pas une disponibilité
                      ininterrompue.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Produits des Vendeurs
                    </h3>
                    <p className="text-gray-600">
                      Shopora agit en tant qu&apos;intermédiaire. La responsabilité
                      des produits incombe aux vendeurs individuels.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Dommages
                    </h3>
                    <p className="text-gray-600">
                      Dans la mesure permise par la loi, notre responsabilité
                      est limitée au montant de votre commande.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Résiliation
                </h2>
                <p className="text-gray-600 mb-4">
                  Nous nous réservons le droit de suspendre ou de résilier votre
                  compte en cas de violation de ces conditions d&apos;utilisation.
                </p>
                <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                  <p className="text-yellow-800">
                    <strong>Note:</strong> Vous pouvez fermer votre compte à
                    tout moment en nous contactant via notre service client.
                  </p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Droit Applicable
                </h2>
                <p className="text-gray-600">
                  Ces conditions d&apos;utilisation sont régies par le droit
                  français. Tout litige sera soumis à la juridiction des
                  tribunaux français.
                </p>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Contact
                </h2>
                <p className="text-gray-600 mb-4">
                    Pour toute question concernant ces conditions d&apos;utilisation :
                </p>
                <div className="space-y-2">
                  <p className="text-gray-600">
                    <strong>Email:</strong> legal@shopora.com
                  </p>
                  <p className="text-gray-600">
                    <strong>Adresse:</strong> 123 Rue du Commerce, Paris, 75001,
                    France
                  </p>
                  <p className="text-gray-600">
                    <strong>Téléphone:</strong> +33 (1) 23 45 67 89
                  </p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Modifications
                </h2>
                <p className="text-gray-600">
                  Nous nous réservons le droit de modifier ces conditions
                    d&apos;utilisation à tout moment. Les modifications prendront effet
                  dès leur publication sur le site. Il est de votre
                  responsabilité de consulter régulièrement ces conditions.
                </p>
                <p className="text-sm text-gray-500 mt-4">
                  Dernière mise à jour :{" "}
                  {new Date().toLocaleDateString("fr-FR")}
                </p>
              </div>
            </div>
          </div>
        </Section>
      </Container>
    </div>
  );
}
