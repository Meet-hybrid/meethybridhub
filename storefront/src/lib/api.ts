const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

class StorefrontApiClient {
  private token: string | null = null;

  private getHeaders(): Record<string, string> {
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
    };
    if (this.token) {
      headers["Authorization"] = `Bearer ${this.token}`;
    }
    if (typeof window !== "undefined") {
      const hostname = window.location.hostname.toLowerCase();
      if (hostname.includes("divinezsignature") || hostname.includes("divinez-signature")) {
        headers["X-Store-Slug"] = "divinez-signature";
      } else {
        headers["X-Store-Slug"] = "meethybridhub";
      }
    }
    return headers;
  }

  private async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const res = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: { ...this.getHeaders(), ...(options.headers as Record<string, string>) },
    });
    if (res.status === 204) return undefined as T;
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.message || `Request failed: ${res.status}`);
    }
    return res.json();
  }

  // Store discovery
  getStores() {
    return this.request<any[]>("/api/v1/discovery/stores");
  }

  getStore(slug: string) {
    return this.request<any>(`/api/v1/discovery/stores/${slug}`);
  }

  getStoreSettings(slug: string) {
    return this.request<any>(`/api/v1/discovery/stores/${slug}/settings`);
  }

  // Products
  getProducts(slug: string, params?: { page?: number; size?: number; category?: string; search?: string }) {
    const qs = new URLSearchParams();
    if (params?.page) qs.set("page", String(params.page));
    if (params?.size) qs.set("size", String(params.size));
    if (params?.category) qs.set("category", params.category);
    if (params?.search) qs.set("search", params.search);
    return this.request<any>(`/api/v1/public/stores/${encodeURIComponent(slug)}/products?${qs}`);
  }

  getProduct(slug: string, id: number) {
    return this.request<any>(`/api/v1/public/stores/${encodeURIComponent(slug)}/products/${id}`);
  }

  createOrder(data: { customerEmail: string; shippingAddress: string; billingAddress?: string; notes?: string; items: { variantId: number; quantity: number }[] }) {
    return this.request<any>("/api/v1/orders", { method: "POST", body: JSON.stringify(data) });
  }

  createInstallmentPlan(orderId: number, installmentCount: number) {
    return this.request<any>(`/api/v1/orders/${orderId}/installments`, { method: "POST", body: JSON.stringify({ installmentCount }) });
  }

  // Categories
  getCategories(slug: string) {
    return this.request<any[]>(`/api/v1/public/stores/${encodeURIComponent(slug)}/categories`);
  }

  // Reviews
  getStoreReviews(storeId: number) {
    return this.request<any[]>(`/api/v1/discovery/stores/${storeId}/reviews`);
  }

  // Custom Orders
  submitCustomOrder(data: {
    title: string;
    description: string;
    budgetMin?: number;
    budgetMax?: number;
  }) {
    return this.request<any>("/api/v1/custom-orders", {
      method: "POST",
      body: JSON.stringify(data),
    });
  }
}

export const storefrontApi = new StorefrontApiClient();
