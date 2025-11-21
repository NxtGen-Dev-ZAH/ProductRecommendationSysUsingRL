'use client';

import { HTMLAttributes } from 'react';
import { cn } from '../../utils/cn';
import { BackgroundGradient } from './background-gradient';

interface SectionProps extends HTMLAttributes<HTMLElement> {
  size?: 'default' | 'sm' | 'md' | 'lg' | 'xl';
  gradient?: boolean;
  gradientVariant?: 'default' | 'primary' | 'secondary' | 'muted';
}

export function Section({
  className,
  children,
  size = 'default',
  gradient = false,
  gradientVariant = 'default',
  ...props
}: SectionProps) {
  const sectionPadding = {
    default: 'py-12 md:py-16',
    sm: 'py-6 md:py-8',
    md: 'py-10 md:py-12',
    lg: 'py-16 md:py-20',
    xl: 'py-20 md:py-24',
  };

  return (
    <section
      className={cn(
        'relative w-full',
        sectionPadding[size],
        className
      )}
      {...props}
    >
      {gradient && <BackgroundGradient variant={gradientVariant} />}
      {children}
    </section>
  );
} 