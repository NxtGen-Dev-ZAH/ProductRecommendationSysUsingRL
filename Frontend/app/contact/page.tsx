"use client";

import { useState } from "react";
import { Container } from "@/components/ui/container";
import { Section } from "@/components/ui/section";
import { BackgroundGradient } from "@/components/ui/background-gradient";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FaEnvelope, FaPhone, FaMapMarkerAlt, FaClock } from "react-icons/fa";

export default function ContactPage() {
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    subject: "",
    message: "",
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    // Handle form submission here
    console.log("Form submitted:", formData);
    alert(
      "Votre message a été envoyé avec succès ! Nous vous répondrons dans les plus brefs délais."
    );
  };

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <BackgroundGradient variant="muted" />

      <Container className="py-16">
        <Section>
          <div className="max-w-6xl mx-auto">
            <h1 className="text-4xl font-bold text-gray-900 mb-8 text-center">
              Nous Contacter
            </h1>

            <div className="grid lg:grid-cols-2 gap-12">
              {/* Contact Information */}
              <div className="space-y-8">
                <div className="bg-white rounded-lg shadow-lg p-8">
                  <h2 className="text-2xl font-semibold text-gray-900 mb-6">
                    Informations de Contact
                  </h2>

                  <div className="space-y-6">
                    <div className="flex items-start">
                      <FaEnvelope className="text-primary mt-1 mr-4 flex-shrink-0" />
                      <div>
                        <h3 className="font-semibold text-gray-900">Email</h3>
                        <p className="text-gray-600">contact@shopora.com</p>
                        <p className="text-gray-600">support@shopora.com</p>
                      </div>
                    </div>

                    <div className="flex items-start">
                      <FaPhone className="text-primary mt-1 mr-4 flex-shrink-0" />
                      <div>
                        <h3 className="font-semibold text-gray-900">
                          Téléphone
                        </h3>
                        <p className="text-gray-600">+33 (1) 23 45 67 89</p>
                        <p className="text-sm text-gray-500">Lun-Ven: 9h-18h</p>
                      </div>
                    </div>

                    <div className="flex items-start">
                      <FaMapMarkerAlt className="text-primary mt-1 mr-4 flex-shrink-0" />
                      <div>
                        <h3 className="font-semibold text-gray-900">Adresse</h3>
                        <p className="text-gray-600">
                          123 Rue du Commerce
                          <br />
                          Quartier des Affaires
                          <br />
                          Paris, 75001, France
                        </p>
                      </div>
                    </div>

                    <div className="flex items-start">
                      <FaClock className="text-primary mt-1 mr-4 flex-shrink-0" />
                      <div>
                        <h3 className="font-semibold text-gray-900">
                          Horaires
                        </h3>
                        <p className="text-gray-600">
                          Lundi - Vendredi: 9h00 - 18h00
                          <br />
                          Samedi: 10h00 - 16h00
                          <br />
                          Dimanche: Fermé
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="bg-white rounded-lg shadow-lg p-8">
                  <h2 className="text-2xl font-semibold text-gray-900 mb-4">
                    FAQ Rapide
                  </h2>
                  <div className="space-y-4">
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        Comment passer une commande ?
                      </h3>
                      <p className="text-gray-600 text-sm">
                        Ajoutez vos produits au panier, procédez au checkout et
                        suivez les étapes de paiement.
                      </p>
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        Quels sont les délais de livraison ?
                      </h3>
                      <p className="text-gray-600 text-sm">
                        Les délais varient selon le vendeur, généralement 2-7
                        jours ouvrés.
                      </p>
                    </div>
                    <div>
                      <h3 className="font-semibold text-gray-900">
                        Comment retourner un produit ?
                      </h3>
                      <p className="text-gray-600 text-sm">
                        Consultez notre politique de retour dans votre compte ou
                        contactez-nous.
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Contact Form */}
              <div className="bg-white rounded-lg shadow-lg p-8">
                <h2 className="text-2xl font-semibold text-gray-900 mb-6">
                  Envoyez-nous un Message
                </h2>

                <form onSubmit={handleSubmit} className="space-y-6">
                  <div className="grid md:grid-cols-2 gap-4">
                    <div>
                      <label
                        htmlFor="name"
                        className="block text-sm font-medium text-gray-700 mb-2"
                      >
                        Nom complet *
                      </label>
                      <Input
                        type="text"
                        id="name"
                        name="name"
                        value={formData.name}
                        onChange={handleChange}
                        required
                        className="w-full"
                      />
                    </div>
                    <div>
                      <label
                        htmlFor="email"
                        className="block text-sm font-medium text-gray-700 mb-2"
                      >
                        Email *
                      </label>
                      <Input
                        type="email"
                        id="email"
                        name="email"
                        value={formData.email}
                        onChange={handleChange}
                        required
                        className="w-full"
                      />
                    </div>
                  </div>

                  <div>
                    <label
                      htmlFor="subject"
                      className="block text-sm font-medium text-gray-700 mb-2"
                    >
                      Sujet *
                    </label>
                    <Input
                      type="text"
                      id="subject"
                      name="subject"
                      value={formData.subject}
                      onChange={handleChange}
                      required
                      className="w-full"
                    />
                  </div>

                  <div>
                    <label
                      htmlFor="message"
                      className="block text-sm font-medium text-gray-700 mb-2"
                    >
                      Message *
                    </label>
                    <textarea
                      id="message"
                      name="message"
                      value={formData.message}
                      onChange={handleChange}
                      required
                      rows={6}
                      className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
                    />
                  </div>

                  <Button type="submit" className="w-full">
                    Envoyer le Message
                  </Button>
                </form>
              </div>
            </div>
          </div>
        </Section>
      </Container>
    </div>
  );
}
