export interface User {
  id: number;
  email: string;
  fullName: string;
  roles: string[];
  status: string;
  emailVerified: boolean;
}

export interface Store {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  status: "ACTIVE" | "SUSPENDED" | "PENDING";
  owner: User;
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

export interface Product {
  id: number;
  storeId: number;
  name: string;
  description: string | null;
  price: number;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  category: { id: number; name: string } | null;
  variants: ProductVariant[];
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

export interface Category {
  id: number;
  storeId: number;
  name: string;
  description: string | null;
  parentId: number | null;
}

export interface Order {
  id: number;
  storeId: number;
  customer: User;
  status: "PENDING" | "CONFIRMED" | "PROCESSING" | "SHIPPED" | "DELIVERED" | "CANCELLED";
  totalAmount: number;
  items: OrderItem[];
  createdAt: string;
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface CustomOrderRequest {
  id: number;
  storeId: number;
  customer: User;
  title: string;
  description: string;
  budgetMin: number | null;
  budgetMax: number | null;
  status: "OPEN" | "IN_REVIEW" | "QUOTED" | "ACCEPTED" | "CONVERTED" | "REJECTED";
  createdAt: string;
}

export interface Quote {
  id: number;
  requestId: number;
  price: number;
  estimatedDays: number;
  notes: string | null;
  status: "PENDING" | "ACCEPTED" | "REJECTED";
  createdAt: string;
}

export interface Message {
  id: number;
  requestId: number;
  senderId: number;
  content: string;
  createdAt: string;
}

export interface Review {
  id: number;
  storeId: number;
  customerId: User;
  rating: number;
  title: string;
  comment: string;
  flagged: boolean;
  createdAt: string;
}

export interface CacheStats {
  totalEntries: number;
  diskUsage: number;
  oldestTimestamp: string | null;
  newestTimestamp: string | null;
  networkBreakdown: Record<string, number>;
}

export interface DashboardStats {
  totalOrders: number;
  totalRevenue: number;
  totalProducts: number;
  totalCustomers: number;
  pendingOrders: number;
  recentOrders: Order[];
}
