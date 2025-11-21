// Generate a placeholder image using SVG data URL
export const generatePlaceholderImage = (width: number = 400, height: number = 400, text: string = 'No Image'): string => {
  const svg = `
    <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
      <rect width="100%" height="100%" fill="#f3f4f6"/>
      <text x="50%" y="50%" font-family="Arial, sans-serif" font-size="16" fill="#9ca3af" text-anchor="middle" dy=".3em">
        ${text}
      </text>
    </svg>
  `;
  
  return `data:image/svg+xml;base64,${btoa(svg)}`;
};

// Get a placeholder image for products
export const getProductPlaceholder = (): string => {
  return generatePlaceholderImage(400, 400, 'Product Image');
};
