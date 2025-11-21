"use client";

import { ReactNode, Suspense } from "react";

interface SuspenseWrapperProps {
  children: ReactNode;
  fallback?: ReactNode;
}

export default function SuspenseWrapper({
  children,
  fallback = <DefaultLoading />,
}: SuspenseWrapperProps) {
  return <Suspense fallback={fallback}>{children}</Suspense>;
}

function DefaultLoading() {
  return (
    <div className="flex justify-center items-center py-12">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-primary"></div>
    </div>
  );
}
