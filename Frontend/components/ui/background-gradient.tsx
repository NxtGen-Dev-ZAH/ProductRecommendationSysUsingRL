'use client';

import { HTMLAttributes } from 'react';
import { cn } from '../../utils/cn';

interface BackgroundGradientProps extends HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'primary' | 'secondary' | 'muted';
}

export function BackgroundGradient({
  className,
  variant = 'default',
  ...props
}: BackgroundGradientProps) {
  const gradientClasses = {
    default: [
      'absolute top-0 left-1/2 w-1/3 h-1/3 bg-cyan-500/15 rounded-full blur-3xl',
      'absolute bottom-0 right-1/4 w-1/4 h-1/4 bg-indigo-400/30 rounded-full blur-3xl',
      'absolute top-1/3 right-0 w-1/4 h-1/3 bg-pink-400/30 rounded-full blur-3xl',
    ],
    primary: [
      'absolute top-0 left-1/2 w-1/3 h-1/3 bg-primary/15 rounded-full blur-3xl',
      'absolute bottom-0 right-1/4 w-1/4 h-1/4 bg-indigo-400/20 rounded-full blur-3xl',
      'absolute top-1/3 right-0 w-1/4 h-1/3 bg-cyan-400/20 rounded-full blur-3xl',
    ],
    secondary: [
      'absolute top-0 left-1/2 w-1/3 h-1/3 bg-secondary/15 rounded-full blur-3xl',
      'absolute bottom-0 right-1/4 w-1/4 h-1/4 bg-emerald-400/20 rounded-full blur-3xl',
      'absolute top-1/3 right-0 w-1/4 h-1/3 bg-teal-400/20 rounded-full blur-3xl',
    ],
    muted: [
      'absolute top-0 left-1/2 w-1/3 h-1/3 bg-gray-400/10 rounded-full blur-3xl',
      'absolute bottom-0 right-1/4 w-1/4 h-1/4 bg-gray-500/10 rounded-full blur-3xl',
      'absolute top-1/3 right-0 w-1/4 h-1/3 bg-gray-600/10 rounded-full blur-3xl',
    ],
  };

  const selectedGradient = gradientClasses[variant];

  return (
    <div className={cn('absolute inset-0 overflow-hidden -z-10', className)} {...props}>
      <div className={cn(selectedGradient[0])} />
      <div className={cn(selectedGradient[1])} />
      <div className={cn(selectedGradient[2])} />
    </div>
  );
} 