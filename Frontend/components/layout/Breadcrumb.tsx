'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { FaChevronRight, FaHome } from 'react-icons/fa';
import { cn } from '../../utils/cn';

export interface BreadcrumbItem {
  label: string;
  href: string;
}

interface BreadcrumbProps {
  items?: BreadcrumbItem[];
  showHome?: boolean;
  className?: string;
  separator?: React.ReactNode;
}

const Breadcrumb = ({ 
  items = [], 
  showHome = true, 
  className,
  separator = <FaChevronRight className="mx-2 text-gray-400" size={12} />
}: BreadcrumbProps) => {
  const pathname = usePathname();
  
  // Generate breadcrumb items from pathname if none provided
  const breadcrumbItems = items.length > 0 ? items : generateFromPath(pathname);

  return (
    <nav className={cn(
      "flex py-3 px-5 text-gray-700 bg-white/50 backdrop-blur-sm rounded-2xl shadow-sm border border-gray-100/50 mb-6",
      className
    )}>
      {showHome && (
        <div className="flex items-center">
          <Link href="/" className="flex items-center text-sm hover:text-primary transition-colors">
            <FaHome className="mr-1" />
            Home
          </Link>
          {breadcrumbItems.length > 0 && separator}
        </div>
      )}

      {breadcrumbItems.map((item, index) => (
        <div key={item.href} className="flex items-center">
          {index > 0 && separator}
          {index === breadcrumbItems.length - 1 ? (
            <span className="text-sm text-gray-500 font-medium">{item.label}</span>
          ) : (
            <Link 
              href={item.href} 
              className="text-sm hover:text-primary transition-colors"
            >
              {item.label}
            </Link>
          )}
        </div>
      ))}
    </nav>
  );
};

// Helper function to generate breadcrumb items from path
function generateFromPath(path: string): BreadcrumbItem[] {
  if (path === '/') return [];
  
  const segments = path.split('/').filter(Boolean);
  let currentPath = '';
  
  return segments.map((segment) => {
    currentPath += `/${segment}`;
    
    // Handle dynamic routes like [id]
    const label = segment.startsWith('[') && segment.endsWith(']')
      ? segment.slice(1, -1) // Remove brackets
      : segment.charAt(0).toUpperCase() + segment.slice(1).replace(/-/g, ' ');
    
    return {
      label,
      href: currentPath
    };
  });
}

export default Breadcrumb; 