'use client';

import { Card } from '../ui/card';
import { Skeleton } from '../ui/loading';
import { cn } from '../../utils/cn';

interface LoadingProductCardProps {
  variant?: 'default' | 'compact' | 'featured';
  className?: string;
}

const LoadingProductCard = ({ variant = 'default', className }: LoadingProductCardProps) => {
  return (
    <Card 
      className={cn(
        'overflow-hidden border-gray-100/50 bg-white/80 backdrop-blur-sm',
        variant === 'compact' && 'max-w-sm',
        variant === 'featured' && 'border-primary/20',
        className
      )}
    >
      {/* Image Skeleton */}
      <div className="relative aspect-square overflow-hidden bg-gray-50">
        <Skeleton className="absolute inset-0" />
        
        {/* Skeleton Wishlist Button */}
        <div className="absolute top-3 right-3 z-10">
          <Skeleton variant="circular" className="w-10 h-10" />
        </div>
        
        {/* Skeleton Discount Badge */}
        <div className="absolute top-3 left-3 z-10">
          <Skeleton className="w-16 h-6 rounded-full" />
        </div>
      </div>

      {/* Content Skeleton */}
      <div className="p-4 space-y-3">
        {/* Vendor Skeleton */}
        <Skeleton className="h-3 w-20" />
        
        {/* Product Name Skeleton */}
        <div className="space-y-2">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-3/4" />
        </div>
        
        {/* Rating Skeleton */}
        <div className="flex items-center gap-1">
          {[...Array(5)].map((_, i) => (
            <Skeleton key={i} variant="circular" className="w-3 h-3" />
          ))}
          <Skeleton className="h-3 w-8 ml-1" />
        </div>
        
        {/* Description Skeleton - Only for featured variant */}
        {variant === 'featured' && (
          <div className="space-y-1">
            <Skeleton className="h-3 w-full" />
            <Skeleton className="h-3 w-2/3" />
          </div>
        )}
        
        {/* Price and Stock Skeleton */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Skeleton className="h-5 w-16" />
            <Skeleton className="h-4 w-12" />
          </div>
          <Skeleton className="h-4 w-12" />
        </div>
        
        {/* Button Skeleton */}
        <Skeleton className="h-10 w-full rounded-2xl" />
      </div>
    </Card>
  );
};

export default LoadingProductCard; 