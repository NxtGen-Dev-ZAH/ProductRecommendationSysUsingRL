"use client";

import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";

export default function CookiePolicyPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-4xl mx-auto">
            <h1 className="text-4xl font-bold text-gray-900 mb-8 text-center">
              Politique des Cookies
            </h1>

            <div className="prose prose-lg max-w-none space-y-8">
              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Qu&apos;est-ce qu&apos;un Cookie ?
                </h2>
                <p className="text-gray-600">
                  Un cookie est un petit fichier texte stocké sur votre
                  ordinateur, tablette ou smartphone lorsque vous visitez notre
                  site web. Les cookies nous permettent de reconnaître votre
                  appareil et de mémoriser vos préférences pour améliorer votre
                  expérience de navigation.
                </p>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Types de Cookies Utilisés
                </h2>
                <div className="space-y-6">
                  <div className="border-l-4 border-green-500 pl-4">
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Cookies Essentiels
                    </h3>
                    <p className="text-gray-600 mb-2">
                      Ces cookies sont nécessaires au fonctionnement de base du
                      site web.
                    </p>
                    <ul className="text-gray-600 space-y-1 text-sm">
                      <li>• Gestion de la session utilisateur</li>
                      <li>• Sécurité et authentification</li>
                      <li>• Fonctionnalités du panier d&apos;achat</li>
                      <li>• Préférences de langue</li>
                    </ul>
                  </div>

                  <div className="border-l-4 border-blue-500 pl-4">
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Cookies Analytiques
                    </h3>
                    <p className="text-gray-600 mb-2">
                      Ces cookies nous aident à comprendre comment vous utilisez
                      notre site.
                    </p>
                    <ul className="text-gray-600 space-y-1 text-sm">
                      <li>• Statistiques de visite</li>
                      <li>• Pages les plus consultées</li>
                      <li>• Temps passé sur le site</li>
                      <li>• Sources de trafic</li>
                    </ul>
                  </div>

                  <div className="border-l-4 border-purple-500 pl-4">
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Cookies de Fonctionnalité
                    </h3>
                    <p className="text-gray-600 mb-2">
                      Ces cookies améliorent les fonctionnalités du site.
                    </p>
                    <ul className="text-gray-600 space-y-1 text-sm">
                      <li>• Mémorisation des préférences</li>
                      <li>• Personnalisation de l&apos;interface</li>
                      <li>• Sauvegarde des paramètres</li>
                    </ul>
                  </div>

                  <div className="border-l-4 border-orange-500 pl-4">
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Cookies Marketing
                    </h3>
                    <p className="text-gray-600 mb-2">
                      Ces cookies sont utilisés pour la publicité personnalisée.
                    </p>
                    <ul className="text-gray-600 space-y-1 text-sm">
                      <li>• Publicités ciblées</li>
                      <li>• Suivi des conversions</li>
                      <li>• Réseaux sociaux</li>
                      <li>• Partenaires publicitaires</li>
                    </ul>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Cookies Tiers
                </h2>
                <p className="text-gray-600 mb-4">
                  Nous utilisons également des cookies de partenaires tiers pour
                  améliorer notre service :
                </p>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Google Analytics
                    </h3>
                    <p className="text-gray-600 text-sm">
                      Analyse du trafic et du comportement des utilisateurs sur
                      notre site.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Réseaux Sociaux
                    </h3>
                    <p className="text-gray-600 text-sm">
                      Boutons de partage et intégration des réseaux sociaux.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Services de Paiement
                    </h3>
                    <p className="text-gray-600 text-sm">
                      Cookies nécessaires au traitement sécurisé des paiements.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Durée de Conservation
                </h2>
                <div className="space-y-4">
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Cookies de Session
                    </h3>
                    <p className="text-gray-600">
                      Supprimés automatiquement à la fermeture de votre
                      navigateur.
                    </p>
                  </div>
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Cookies Persistants
                    </h3>
                    <p className="text-gray-600">
                      Conservés pendant une durée déterminée (généralement 1 à 2
                      ans maximum).
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Gestion de Vos Cookies
                </h2>
                <div className="space-y-6">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Paramètres du Navigateur
                    </h3>
                    <p className="text-gray-600 mb-4">
                      Vous pouvez contrôler et gérer les cookies dans les
                      paramètres de votre navigateur :
                    </p>
                    <div className="grid md:grid-cols-2 gap-4">
                      <div>
                        <h4 className="font-semibold text-gray-900 mb-2">
                          Chrome
                        </h4>
                        <p className="text-gray-600 text-sm">
                          Paramètres → Confidentialité et sécurité → Cookies et
                          autres données de site
                        </p>
                      </div>
                      <div>
                        <h4 className="font-semibold text-gray-900 mb-2">
                          Firefox
                        </h4>
                        <p className="text-gray-600 text-sm">
                          Options → Vie privée et sécurité → Cookies et données
                          de sites
                        </p>
                      </div>
                      <div>
                        <h4 className="font-semibold text-gray-900 mb-2">
                          Safari
                        </h4>
                        <p className="text-gray-600 text-sm">
                          Préférences → Confidentialité → Cookies et données de
                          sites web
                        </p>
                      </div>
                      <div>
                        <h4 className="font-semibold text-gray-900 mb-2">
                          Edge
                        </h4>
                        <p className="text-gray-600 text-sm">
                          Paramètres → Cookies et autorisations de site →
                          Cookies et données stockées
                        </p>
                      </div>
                    </div>
                  </div>

                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                    <h3 className="font-semibold text-blue-900 mb-2">
                      Bannière de Consentement
                    </h3>
                    <p className="text-blue-800">
                      Lors de votre première visite, une bannière vous permet de
                      choisir quels types de cookies accepter. Vous pouvez
                      modifier vos préférences à tout moment.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Conséquences du Refus des Cookies
                </h2>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Cookies Essentiels
                    </h3>
                    <p className="text-gray-600">
                      Le refus des cookies essentiels peut empêcher le bon
                      fonctionnement du site (connexion, panier, etc.).
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Autres Cookies
                    </h3>
                    <p className="text-gray-600">
                      Le refus des cookies analytiques et marketing n&apos;affecte
                      pas le fonctionnement de base du site, mais peut limiter
                      la personnalisation de votre expérience.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Mise à Jour de Cette Politique
                </h2>
                <p className="text-gray-600">
                  Cette politique des cookies peut être mise à jour pour
                    refléter les changements dans nos pratiques ou pour d&apos;autres
                  raisons opérationnelles, légales ou réglementaires. Nous vous
                  encourageons à consulter cette page régulièrement.
                </p>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Contact
                </h2>
                <p className="text-gray-600 mb-4">
                  Si vous avez des questions concernant notre utilisation des
                  cookies :
                </p>
                <div className="space-y-2">
                  <p className="text-gray-600">
                    <strong>Email:</strong> cookies@shopora.com
                  </p>
                  <p className="text-gray-600">
                    <strong>Téléphone:</strong> +33 (1) 23 45 67 89
                  </p>
                  <p className="text-gray-600">
                    <strong>Adresse:</strong> 123 Rue du Commerce, Paris, 75001,
                    France
                  </p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <p className="text-sm text-gray-500">
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
