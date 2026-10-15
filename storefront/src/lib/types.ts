export interface Product {
  id: number;
  storeId: number;
  name: string;
  description: string | null;
  price: number;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  category: { id: number; name: string } | null;
  variants: ProductVariant[];
  images: ProductImage[];
  createdAt: string;
}

export interface ProductVariant {
  id: number;
  productId: number;
  sku: string;
  size: string | null;
  color: string | null;
  price: number;
  stock: number;
}

export interface ProductImage {
  id: number;
  url: string;
  alt: string | null;
  sortOrder: number;
}

export interface Category {
  id: number;
  storeId: number;
  name: string;
  description: string | null;
  parentId: number | null;
}

export interface Store {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  status: "ACTIVE" | "SUSPENDED" | "PENDING";
  owner: {
    id: number;
    fullName: string;
  };
  createdAt: string;
}

export interface StoreSettings {
  id: number;
  storeId: number;
  logoUrl: string | null;
  primaryColor: string;
  accentColor: string;
  theme: string;
  tagline: string | null;
  contactEmail: string | null;
}

export interface Review {
  id: number;
  storeId: number;
  customerId: number;
  customerName: string;
  rating: number;
  title: string;
  comment: string;
  flagged: boolean;
  createdAt: string;
}

export interface CustomOrderRequest {
  id: number;
  storeId: number;
  title: string;
  description: string;
  budgetMin: number | null;
  budgetMax: number | null;
  status: string;
  createdAt: string;
}
