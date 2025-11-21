'use client';

import { HTMLAttributes } from 'react';
import { cn } from '../../utils/cn';

interface SkeletonProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'card' | 'text' | 'image' | 'avatar' | 'button';
}

export function Skeleton({
  className,
  variant = 'default',
  ...props
}: SkeletonProps) {
  const variantStyles = {
    default: 'h-4 w-full',
    card: 'h-48 w-full rounded-2xl',
    text: 'h-4 w-3/4',
    image: 'aspect-video w-full rounded-xl',
    avatar: 'h-10 w-10 rounded-full',
    button: 'h-10 w-24 rounded-2xl',
  };

  return (
    <div
      className={cn(
        'animate-pulse bg-muted/60',
        variantStyles[variant],
        className
      )}
      {...props}
    />
  );
}

export function ProductCardSkeleton() {
  return (
    <div className="space-y-3">
      <Skeleton variant="image" />
      <Skeleton variant="text" className="w-2/3" />
      <Skeleton variant="text" className="w-1/2" />
      <Skeleton variant="button" />
    </div>
  );
}

export function ProductGridSkeleton({ count = 4 }) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
      {Array.from({ length: count }).map((_, i) => (
        <ProductCardSkeleton key={i} />
      ))}
    </div>
  );
} 