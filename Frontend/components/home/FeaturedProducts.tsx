'use client';

import { useState, useEffect } from 'react';
import { Section } from '../ui/section';
import { Container } from '../ui/container';
import ProductCard from '../ProductCard';
import { ProductGridSkeleton } from '../ui/skeleton';
import { Product } from '../../app/api/services/product';

// Define an extended product type with discountPercentage
interface ExtendedProduct extends Omit<Product, 'id'> {
  id: string; // Override the id to be string for this component
  discountPercentage?: number;
}

// Example featured products data (in a real app, this would come from an API)
const exampleProducts: ExtendedProduct[] = [
  {
    id: '1',
    name: 'Premium Wireless Headphones',
    description: 'Noise-cancelling wireless headphones with premium sound quality',
    price: 249.99,
    imageUrl: '/images/products/headphones.jpg',
    categoryName: 'Audio',
    categoryId: 1,
    quantity: 15,
    active: true,
    discountPercentage: 10
  },
  {
    id: '2',
    name: 'Ergonomic Office Chair',
    description: 'Comfortable office chair with lumbar support',
    price: 299.99,
    imageUrl: '/images/products/chair.jpg',
    categoryName: 'Furniture',
    categoryId: 2,
    quantity: 8,
    active: true,
    discountPercentage: 0
  },
  {
    id: '3',
    name: 'Smart Watch Series 5',
    description: 'Advanced smartwatch with health monitoring features',
    price: 399.99,
    imageUrl: '/images/products/watch.jpg',
    categoryName: 'Wearables',
    categoryId: 3,
    quantity: 20,
    active: true,
    discountPercentage: 15
  },
  {
    id: '4',
    name: 'Ultra HD 4K Monitor',
    description: '32-inch 4K monitor with excellent color accuracy',
    price: 549.99,
    imageUrl: '/images/products/monitor.jpg',
    categoryName: 'Electronics',
    categoryId: 4,
    quantity: 5,
    active: true,
    discountPercentage: 0
  },
  {
    id: '5',
    name: 'Premium Leather Wallet',
    description: 'Handcrafted genuine leather wallet with RFID protection',
    price: 79.99,
    imageUrl: '/images/products/wallet.jpg',
    categoryName: 'Accessories',
    categoryId: 5,
    quantity: 30,
    active: true,
    discountPercentage: 0
  },
  {
    id: '6',
    name: 'Mechanical Keyboard',
    description: 'Mechanical gaming keyboard with RGB backlighting',
    price: 129.99,
    imageUrl: '/images/products/keyboard.jpg',
    categoryName: 'Computing',
    categoryId: 6,
    quantity: 12,
    active: true,
    discountPercentage: 5
  },
  {
    id: '7',
    name: 'Wireless Charging Pad',
    description: 'Fast wireless charging pad compatible with all Qi devices',
    price: 49.99,
    imageUrl: '/images/products/charger.jpg',
    categoryName: 'Accessories',
    categoryId: 5,
    quantity: 25,
    active: true,
    discountPercentage: 0
  },
  {
    id: '8',
    name: 'Professional DSLR Camera',
    description: 'High-performance DSLR camera for professional photography',
    price: 1299.99,
    imageUrl: '/images/products/camera.jpg',
    categoryName: 'Photography',
    categoryId: 7,
    quantity: 3,
    active: true,
    discountPercentage: 8
  }
];

export default function FeaturedProducts() {
  const [products, setProducts] = useState<ExtendedProduct[]>([]);
  const [loading, setLoading] = useState(true);

  // Simulate fetching products from an API
  useEffect(() => {
    const fetchProducts = async () => {
      try {
        // In a real app, this would be an API call
        // Simulate network delay
        await new Promise(resolve => setTimeout(resolve, 1200));
        setProducts(exampleProducts);
      } catch (error) {
        console.error('Error fetching featured products:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  return (
    <Section gradient gradientVariant="primary" className="py-12 sm:py-16">
      <Container>
        <div className="flex flex-col items-center mb-10 text-center">
          <h2 className="text-3xl font-bold mb-3">Featured Products</h2>
          <p className="text-muted-foreground max-w-2xl">
            Discover our handpicked selection of premium products that combine quality, 
            innovation, and style to enhance your everyday life.
          </p>
        </div>

        {loading ? (
          <ProductGridSkeleton count={8} />
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
            {products.map((product) => (
              <ProductCard 
                key={product.id} 
                product={product} 
              />
            ))}
          </div>
        )}
      </Container>
    </Section>
  );
}