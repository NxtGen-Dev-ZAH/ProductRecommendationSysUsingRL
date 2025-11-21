"use client";

import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";

export default function PrivacyPolicyPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-4xl mx-auto">
            <h1 className="text-4xl font-bold text-gray-900 mb-8 text-center">
              Politique de Confidentialité
            </h1>

            <div className="prose prose-lg max-w-none space-y-8">
              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Introduction
                </h2>
                <p className="text-gray-600">
                  Shopora s&apos;engage à protéger votre vie privée et vos données
                  personnelles. Cette politique de confidentialité explique
                  comment nous collectons, utilisons et protégeons vos
                  informations personnelles conformément au Règlement Général
                  sur la Protection des Données (RGPD).
                </p>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Données Collectées
                </h2>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Données d&apos;Identification
                    </h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>• Nom et prénom</li>
                      <li>• Adresse email</li>
                      <li>• Numéro de téléphone</li>
                      <li>• Date de naissance</li>
                    </ul>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Données de Livraison
                    </h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>• Adresse de livraison</li>
                      <li>• Adresse de facturation</li>
                      <li>• Préférences de livraison</li>
                    </ul>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Données de Navigation
                    </h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>• Adresse IP</li>
                      <li>• Type de navigateur</li>
                      <li>• Pages visitées</li>
                      <li>• Temps passé sur le site</li>
                    </ul>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Utilisation des Données
                </h2>
                <div className="space-y-4">
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Traitement des Commandes
                    </h3>
                    <p className="text-gray-600">
                      Nous utilisons vos données pour traiter vos commandes,
                      gérer les livraisons et assurer le service client.
                    </p>
                  </div>
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Communication
                    </h3>
                    <p className="text-gray-600">
                      Envoi de confirmations de commande, mises à jour de
                      livraison et communications importantes concernant votre
                      compte.
                    </p>
                  </div>
                  <div className="border-l-4 border-primary pl-4">
                    <h3 className="font-semibold text-gray-900">
                      Amélioration du Service
                    </h3>
                    <p className="text-gray-600">
                      Analyse des données pour améliorer notre site web, nos
                      produits et nos services.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Partage des Données
                </h2>
                <p className="text-gray-600 mb-4">
                  Nous ne vendons jamais vos données personnelles. Nous pouvons
                  partager vos informations uniquement dans les cas suivants :
                </p>
                <ul className="text-gray-600 space-y-2">
                  <li>
                    • Avec nos partenaires de livraison pour assurer la
                    livraison de vos commandes
                  </li>
                  <li>
                    • Avec nos processeurs de paiement pour traiter les
                    transactions
                  </li>
                  <li>
                    • Lorsque requis par la loi ou pour protéger nos droits
                    légaux
                  </li>
                  <li>• Avec votre consentement explicite</li>
                </ul>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Sécurité des Données
                </h2>
                <div className="space-y-4">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Mesures Techniques
                    </h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>
                        • Chiffrement SSL/TLS pour toutes les transmissions
                      </li>
                      <li>• Serveurs sécurisés avec pare-feu</li>
                      <li>• Sauvegardes régulières et chiffrées</li>
                      <li>
                        • Authentification à deux facteurs pour les employés
                      </li>
                    </ul>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Mesures Organisationnelles
                    </h3>
                    <ul className="text-gray-600 space-y-1">
                      <li>
                        • Formation du personnel sur la protection des données
                      </li>
                      <li>• Accès restreint aux données personnelles</li>
                      <li>• Audit régulier des systèmes de sécurité</li>
                    </ul>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Vos Droits
                </h2>
                <div className="grid md:grid-cols-2 gap-6">
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Droit d&apos;Accès
                    </h3>
                    <p className="text-gray-600 text-sm">
                      Vous pouvez demander une copie de vos données
                      personnelles.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Droit de Rectification
                    </h3>
                    <p className="text-gray-600 text-sm">
                      Vous pouvez corriger des données inexactes ou incomplètes.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Droit d&apos;Effacement
                    </h3>
                    <p className="text-gray-600 text-sm">
                      Vous pouvez demander la suppression de vos données.
                    </p>
                  </div>
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">
                      Droit de Portabilité
                    </h3>
                    <p className="text-gray-600 text-sm">
                      Vous pouvez récupérer vos données dans un format
                      structuré.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Cookies
                </h2>
                <p className="text-gray-600 mb-4">
                  Nous utilisons des cookies pour améliorer votre expérience sur
                  notre site. Vous pouvez gérer vos préférences de cookies dans
                  les paramètres de votre navigateur.
                </p>
                <div className="space-y-2">
                  <p className="text-gray-600">
                    <strong>Cookies Essentiels:</strong> Nécessaires au
                    fonctionnement du site
                  </p>
                  <p className="text-gray-600">
                    <strong>Cookies Analytiques:</strong> Nous aident à
                    comprendre l&apos;utilisation du site
                  </p>
                  <p className="text-gray-600">
                    <strong>Cookies Marketing:</strong> Utilisés pour la
                    publicité personnalisée
                  </p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Contact
                </h2>
                <p className="text-gray-600 mb-4">
                  Pour toute question concernant cette politique de
                  confidentialité ou pour exercer vos droits :
                </p>
                <div className="space-y-2">
                  <p className="text-gray-600">
                    <strong>Email:</strong> privacy@shopora.com
                  </p>
                  <p className="text-gray-600">
                    <strong>Adresse:</strong> 123 Rue du Commerce, Paris, 75001,
                    France
                  </p>
                  <p className="text-gray-600">
                    <strong>Délégué à la Protection des Données:</strong>{" "}
                    dpo@shopora.com
                  </p>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Modifications
                </h2>
                <p className="text-gray-600">
                  Cette politique de confidentialité peut être mise à jour
                  périodiquement. Nous vous informerons de tout changement
                  important par email ou via notre site web. La date de dernière
                  mise à jour est indiquée en bas de cette page.
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
