'use client';

import { HTMLAttributes } from 'react';
import { cn } from '../../utils/cn';

interface ContainerProps extends HTMLAttributes<HTMLDivElement> {
  size?: 'default' | 'sm' | 'md' | 'lg' | 'xl' | 'full';
  centered?: boolean;
}

export function Container({
  className,
  children,
  size = 'default',
  centered = false,
  ...props
}: ContainerProps) {
  const containerSize = {
    default: 'max-w-7xl',
    sm: 'max-w-3xl',
    md: 'max-w-5xl',
    lg: 'max-w-6xl',
    xl: 'max-w-8xl',
    full: 'max-w-full',
  };

  return (
    <div
      className={cn(
        'w-full px-4 mx-auto sm:px-6 lg:px-8',
        containerSize[size],
        centered && 'flex flex-col items-center',
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
} 