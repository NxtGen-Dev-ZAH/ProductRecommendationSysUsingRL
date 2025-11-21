"use client";

import { useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import { FaSearch } from "react-icons/fa";

interface SearchBarProps {
  placeholder?: string;
  className?: string;
  initialValue?: string;
  onSearch?: (query: string) => void;
}

const SearchBar = ({
  placeholder = "Rechercher des produits...",
  className = "",
  initialValue = "",
  onSearch,
}: SearchBarProps) => {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState(initialValue);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();

    if (searchQuery.trim()) {
      if (onSearch) {
        onSearch(searchQuery.trim());
      } else {
        // Default behavior: navigate to search page
        router.push(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
      }
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className={`relative flex items-center ${className}`}
    >
      <input
        type="text"
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
        placeholder={placeholder}
        className="w-full py-2 pl-4 pr-10 rounded-full border border-gray-300 focus:ring-2 focus:ring-[#60a5fa] focus:border-[#60a5fa] focus:outline-none"
      />
      <button
        type="submit"
        className="absolute right-3 text-gray-500 hover:text-[#3b82f6]"
        aria-label="Search"
      >
        <FaSearch />
      </button>
    </form>
  );
};

export default SearchBar;
