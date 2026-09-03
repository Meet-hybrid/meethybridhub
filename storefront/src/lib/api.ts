const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

class StorefrontApiClient {
  private token: string | null = null;
  private refreshTokenValue: string | null = null;

  constructor() {
    if (typeof window !== "undefined") {
      this.token = localStorage.getItem("access_token");
      this.refreshTokenValue = localStorage.getItem("refresh_token");
    }
  }

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

  // ── Auth ──────────────────────────────────────────────────────

  async login(email: string, password: string): Promise<AuthTokens> {
    const res = await this.request<{ accessToken: string; refreshToken: string; message: string }>(
      "/api/v1/auth/login",
      { method: "POST", body: JSON.stringify({ email, password }) }
    );
    this.setTokens(res.accessToken, res.refreshToken);
    return { accessToken: res.accessToken, refreshToken: res.refreshToken };
  }

  async register(email: string, password: string, fullName: string): Promise<AuthTokens> {
    const res = await this.request<{ accessToken: string; refreshToken: string; message: string }>(
      "/api/v1/auth/register",
      { method: "POST", body: JSON.stringify({ email, password, fullName }) }
    );
    this.setTokens(res.accessToken, res.refreshToken);
    return { accessToken: res.accessToken, refreshToken: res.refreshToken };
  }

  async refreshAccessToken(): Promise<AuthTokens | null> {
    if (!this.refreshTokenValue) return null;
    try {
      const res = await this.request<{ accessToken: string; refreshToken: string; message: string }>(
        "/api/v1/auth/refresh",
        { method: "POST", body: JSON.stringify({ refreshToken: this.refreshTokenValue }) }
      );
      this.setTokens(res.accessToken, res.refreshToken);
      return { accessToken: res.accessToken, refreshToken: res.refreshToken };
    } catch {
      this.clearTokens();
      return null;
    }
  }

  async logout(): Promise<void> {
    try {
      await this.request<{ message: string }>("/api/v1/auth/logout", {
        method: "POST",
        body: JSON.stringify({ refreshToken: this.refreshTokenValue }),
      });
    } catch {
      // Ignore errors on logout
    }
    this.clearTokens();
  }

  setTokens(accessToken: string, refreshToken: string) {
    this.token = accessToken;
    this.refreshTokenValue = refreshToken;
    if (typeof window !== "undefined") {
      localStorage.setItem("access_token", accessToken);
      localStorage.setItem("refresh_token", refreshToken);
    }
  }

  clearTokens() {
    this.token = null;
    this.refreshTokenValue = null;
    if (typeof window !== "undefined") {
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
    }
  }

  isAuthenticated(): boolean {
    return !!this.token;
  }

  // ── Store discovery ──────────────────────────────────────────

  getStores() {
    return this.request<any[]>("/api/v1/discovery/stores");
  }

  getStore(slug: string) {
    return this.request<any>(`/api/v1/discovery/stores/${slug}`);
  }

  getStoreSettings(slug: string) {
    return this.request<any>(`/api/v1/discovery/stores/${slug}/settings`);
  }

  // ── Products ─────────────────────────────────────────────────

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

  // ── Orders ───────────────────────────────────────────────────

  createOrder(data: { customerEmail: string; shippingAddress: string; billingAddress?: string; notes?: string; items: { variantId: number; quantity: number }[] }) {
    return this.request<any>("/api/v1/orders", { method: "POST", body: JSON.stringify(data) });
  }

  getOrders(page = 0, size = 10) {
    return this.request<any>(`/api/v1/orders?page=${page}&size=${size}`);
  }

  getOrder(orderId: number) {
    return this.request<any>(`/api/v1/orders/${orderId}`);
  }

  cancelOrder(orderId: number) {
    return this.request<any>(`/api/v1/orders/${orderId}/cancel`, { method: "PUT" });
  }

  createInstallmentPlan(orderId: number, installmentCount: number) {
    return this.request<any>(`/api/v1/orders/${orderId}/installments`, { method: "POST", body: JSON.stringify({ installmentCount }) });
  }

  // ── Categories ───────────────────────────────────────────────

  getCategories(slug: string) {
    return this.request<any[]>(`/api/v1/public/stores/${encodeURIComponent(slug)}/categories`);
  }

  // ── Reviews ──────────────────────────────────────────────────

  getStoreReviews(storeId: number) {
    return this.request<any[]>(`/api/v1/discovery/stores/${storeId}/reviews`);
  }

  // ── Custom Orders ────────────────────────────────────────────

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
