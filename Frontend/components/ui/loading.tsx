'use client';

import { cn } from '../../utils/cn';
import { cva, type VariantProps } from 'class-variance-authority';

const loaderVariants = cva(
  'animate-spin',
  {
    variants: {
      variant: {
        default: 'border-current border-t-transparent',
        primary: 'border-primary border-t-transparent',
        secondary: 'border-secondary border-t-transparent',
        dots: '',
        pulse: '',
        bars: '',
      },
      size: {
        sm: 'w-4 h-4 border-2',
        default: 'w-6 h-6 border-2',
        lg: 'w-8 h-8 border-2',
        xl: 'w-12 h-12 border-4',
      }
    },
    defaultVariants: {
      variant: 'default',
      size: 'default',
    },
  }
);

export interface LoaderProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof loaderVariants> {
  text?: string;
}

// Spinner Loader
export const Spinner = ({ className, variant, size, ...props }: LoaderProps) => {
  if (variant === 'dots') {
    return (
      <div className={cn('flex space-x-1', className)} {...props}>
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            className={cn(
              'bg-current rounded-full animate-bounce',
              size === 'sm' && 'w-1.5 h-1.5',
              size === 'default' && 'w-2 h-2',
              size === 'lg' && 'w-2.5 h-2.5',
              size === 'xl' && 'w-3 h-3'
            )}
            style={{ animationDelay: `${i * 0.1}s` }}
          />
        ))}
      </div>
    );
  }

  if (variant === 'pulse') {
    return (
      <div
        className={cn(
          'bg-current rounded-full animate-pulse',
          size === 'sm' && 'w-4 h-4',
          size === 'default' && 'w-6 h-6',
          size === 'lg' && 'w-8 h-8',
          size === 'xl' && 'w-12 h-12',
          className
        )}
        {...props}
      />
    );
  }

  if (variant === 'bars') {
    return (
      <div className={cn('flex space-x-1', className)} {...props}>
        {[0, 1, 2, 3].map((i) => (
          <div
            key={i}
            className={cn(
              'bg-current animate-pulse',
              size === 'sm' && 'w-1 h-4',
              size === 'default' && 'w-1.5 h-6',
              size === 'lg' && 'w-2 h-8',
              size === 'xl' && 'w-2.5 h-12'
            )}
            style={{ 
              animationDelay: `${i * 0.1}s`,
              animationDuration: '0.8s'
            }}
          />
        ))}
      </div>
    );
  }

  return (
    <div
      className={cn(
        loaderVariants({ variant, size }),
        'rounded-full border-solid',
        className
      )}
      {...props}
    />
  );
};

// Loading Screen Component
export interface LoadingScreenProps {
  text?: string;
  variant?: 'overlay' | 'fullscreen' | 'inline';
  size?: 'sm' | 'default' | 'lg' | 'xl';
  className?: string;
}

export const LoadingScreen = ({ 
  text = 'Loading...', 
  variant = 'overlay',
  size = 'lg',
  className 
}: LoadingScreenProps) => {
  const baseClasses = 'flex flex-col items-center justify-center';
  
  const variantClasses = {
    overlay: 'fixed inset-0 bg-white/80 backdrop-blur-sm z-50',
    fullscreen: 'min-h-screen bg-background',
    inline: 'py-8'
  };

  return (
    <div className={cn(baseClasses, variantClasses[variant], className)}>
      <Spinner variant="primary" size={size} />
      {text && (
        <p className="mt-4 text-sm text-muted-foreground font-medium">
          {text}
        </p>
      )}
    </div>
  );
};

// Skeleton Components
export interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'circular' | 'rectangular';
}

export const Skeleton = ({ 
  className, 
  variant = 'default',
  ...props 
}: SkeletonProps) => {
  return (
    <div
      className={cn(
        'animate-pulse bg-muted',
        variant === 'default' && 'rounded-md',
        variant === 'circular' && 'rounded-full',
        variant === 'rectangular' && 'rounded-none',
        className
      )}
      {...props}
    />
  );
};

// Product Card Skeleton
export const ProductCardSkeleton = () => {
  return (
    <div className="bg-white rounded-2xl shadow-sm p-4 space-y-4">
      <Skeleton className="w-full h-48" />
      <div className="space-y-2">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-6 w-1/4" />
      </div>
      <div className="flex justify-between items-center">
        <Skeleton className="h-8 w-16" />
        <Skeleton className="h-9 w-20" />
      </div>
    </div>
  );
};

// Loading Button
export interface LoadingButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  loading?: boolean;
  loadingText?: string;
  children: React.ReactNode;
}

export const LoadingButton = ({ 
  loading = false, 
  loadingText,
  children,
  disabled,
  className,
  ...props 
}: LoadingButtonProps) => {
  return (
    <button
      className={cn(
        'relative inline-flex items-center justify-center',
        className
      )}
      disabled={loading || disabled}
      {...props}
    >
      {loading && (
        <Spinner size="sm" className="absolute left-1/2 transform -translate-x-1/2" />
      )}
      <span className={cn(loading && 'opacity-0')}>
        {loading && loadingText ? loadingText : children}
      </span>
    </button>
  );
};

// Page Loading Indicator
export const PageLoader = ({ text }: { text?: string }) => {
  return (
    <div className="fixed top-0 left-0 right-0 z-50">
      <div className="h-1 bg-gradient-to-r from-primary/20 via-primary to-primary/20">
        <div className="h-full bg-primary animate-pulse" />
      </div>
      {text && (
        <div className="absolute top-4 left-1/2 transform -translate-x-1/2 bg-white/90 backdrop-blur-sm px-4 py-2 rounded-full shadow-lg">
          <span className="text-sm text-muted-foreground">{text}</span>
        </div>
      )}
    </div>
  );
}; 