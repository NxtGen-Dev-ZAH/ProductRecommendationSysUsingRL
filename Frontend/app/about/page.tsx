"use client";

import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";

export default function AboutPage() {
  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-4xl mx-auto">
            <h1 className="text-4xl font-bold text-gray-900 mb-8 text-center">
              À Propos de Shopora
            </h1>

            <div className="prose prose-lg max-w-none">
              <div className="bg-white rounded-lg shadow-lg p-8 mb-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Notre Mission
                </h2>
                <p className="text-gray-600 leading-relaxed">
                  Shopora est votre marketplace multi-vendeurs ultime, conçue
                  pour connecter les acheteurs avec des vendeurs vérifiés du
                  monde entier. Notre mission est de fournir une plateforme
                  sécurisée, intuitive et performante où la qualité des produits
                  et l&apos;excellence du service client sont nos priorités absolues.
                </p>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8 mb-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Notre Vision
                </h2>
                <p className="text-gray-600 leading-relaxed">
                  Nous aspirons à devenir la référence mondiale du e-commerce
                  multi-vendeurs, en créant un écosystème où chaque transaction
                  est une expérience positive et où la confiance entre acheteurs
                  et vendeurs est renforcée par la technologie et l&apos;innovation.
                </p>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8 mb-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Nos Valeurs
                </h2>
                <div className="grid md:grid-cols-2 gap-6">
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      Sécurité
                    </h3>
                    <p className="text-gray-600">
                      Protection maximale des données et des transactions de nos
                      utilisateurs.
                    </p>
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      Qualité
                    </h3>
                    <p className="text-gray-600">
                      Vérification rigoureuse des vendeurs et des produits
                      proposés.
                    </p>
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      Innovation
                    </h3>
                    <p className="text-gray-600">
                      Utilisation des dernières technologies pour améliorer
                      l&apos;expérience utilisateur.
                    </p>
                  </div>
                  <div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-2">
                      Service Client
                    </h3>
                    <p className="text-gray-600">
                      Support 24/7 pour répondre à tous vos besoins et
                      questions.
                    </p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                  Notre Équipe
                </h2>
                <p className="text-gray-600 leading-relaxed">
                  Chez Shopora, nous sommes une équipe passionnée de
                  développeurs, designers, et experts en e-commerce, tous unis
                    par la volonté de créer la meilleure expérience d&apos;achat en
                  ligne possible. Notre expertise technique et notre
                  compréhension approfondie des besoins de nos utilisateurs nous
                  permettent de proposer des solutions innovantes et fiables.
                </p>
              </div>
            </div>
          </div>
        </Section>
      </Container>
    </div>
  );
}
