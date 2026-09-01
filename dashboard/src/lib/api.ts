const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

class ApiClient {
  private token: string | null = null;

  setToken(token: string | null) {
    this.token = token;
    if (token) {
      if (typeof window !== "undefined") localStorage.setItem("token", token);
    } else {
      if (typeof window !== "undefined") localStorage.removeItem("token");
    }
  }

  getToken(): string | null {
    if (this.token) return this.token;
    if (typeof window !== "undefined") {
      this.token = localStorage.getItem("token");
    }
    return this.token;
  }

  private async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const token = this.getToken();
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...((options.headers as Record<string, string>) || {}),
    };
    if (token) headers["Authorization"] = `Bearer ${token}`;

    const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
    if (res.status === 204) return undefined as T;
    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      throw new Error(body.message || `Request failed: ${res.status}`);
    }
    return res.json();
  }

  // Auth
  login(email: string, password: string) {
    return this.request<{ accessToken: string; refreshToken: string; user: any }>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
  }

  register(data: { email: string; password: string; fullName: string }) {
    return this.request<{ id: number; email: string }>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
    });
  }

  getMe() {
    return this.request<any>("/api/v1/auth/me");
  }

  // Store
  getMyStore() {
    return this.request<any>("/api/v1/stores/me");
  }

  getStoreSettings() {
    return this.request<any>("/api/v1/stores/me/settings");
  }

  updateStoreSettings(data: any) {
    return this.request<any>("/api/v1/stores/me/settings", {
      method: "PUT",
      body: JSON.stringify(data),
    });
  }

  // Products
  getProducts(params?: { page?: number; size?: number; status?: string }) {
    const qs = new URLSearchParams();
    if (params?.page) qs.set("page", String(params.page));
    if (params?.size) qs.set("size", String(params.size));
    if (params?.status) qs.set("status", params.status);
    return this.request<any>(`/api/v1/products?${qs}`);
  }

  getProduct(id: number) {
    return this.request<any>(`/api/v1/products/${id}`);
  }

  createProduct(data: any) {
    return this.request<any>("/api/v1/products", { method: "POST", body: JSON.stringify(data) });
  }

  updateProduct(id: number, data: any) {
    return this.request<any>(`/api/v1/products/${id}`, { method: "PUT", body: JSON.stringify(data) });
  }

  deleteProduct(id: number) {
    return this.request<void>(`/api/v1/products/${id}`, { method: "DELETE" });
  }

  // Categories
  getCategories() {
    return this.request<any[]>("/api/v1/categories");
  }

  createCategory(data: any) {
    return this.request<any>("/api/v1/categories", { method: "POST", body: JSON.stringify(data) });
  }

  // Orders
  getOrders(params?: { page?: number; size?: number; status?: string }) {
    const qs = new URLSearchParams();
    if (params?.page) qs.set("page", String(params.page));
    if (params?.size) qs.set("size", String(params.size));
    if (params?.status) qs.set("status", params.status);
    return this.request<any>(`/api/v1/orders?${qs}`);
  }

  getOrder(id: number) {
    return this.request<any>(`/api/v1/orders/${id}`);
  }

  updateOrderStatus(id: number, status: string) {
    return this.request<any>(`/api/v1/orders/${id}/status`, {
      method: "PUT",
      body: JSON.stringify({ status }),
    });
  }

  // Custom Orders
  getCustomOrders(params?: { status?: string }) {
    const qs = new URLSearchParams();
    if (params?.status) qs.set("status", params.status);
    return this.request<any>(`/api/v1/custom-orders?${qs}`);
  }

  getCustomOrder(id: number) {
    return this.request<any>(`/api/v1/custom-orders/${id}`);
  }

  updateCustomOrderStatus(id: number, status: string) {
    return this.request<any>(`/api/v1/custom-orders/${id}/status`, {
      method: "PUT",
      body: JSON.stringify({ status }),
    });
  }

  createQuote(requestId: number, data: any) {
    return this.request<any>(`/api/v1/custom-orders/${requestId}/quotes`, {
      method: "POST",
      body: JSON.stringify(data),
    });
  }

  getMessages(requestId: number) {
    return this.request<any[]>(`/api/v1/custom-orders/${requestId}/messages`);
  }

  sendMessage(requestId: number, content: string) {
    return this.request<any>(`/api/v1/custom-orders/${requestId}/messages`, {
      method: "POST",
      body: JSON.stringify({ content }),
    });
  }

  // Customers
  getCustomers() {
    return this.request<any[]>("/api/v1/admin/users");
  }

  // Reviews
  getStoreReviews() {
    return this.request<any[]>("/api/v1/discovery/stores/me/reviews");
  }

  // Analytics
  getStoreAnalytics(days?: number) {
    const qs = days ? `?days=${days}` : "";
    return this.request<any>(`/api/v1/admin/analytics/stores/me${qs}`);
  }

  // Super Admin
  getPlatformAnalytics() {
    return this.request<any>("/api/v1/admin/analytics/platform");
  }

  getPlatformConfig() {
    return this.request<any>("/api/v1/admin/config");
  }

  updatePlatformConfig(data: any) {
    return this.request<any>("/api/v1/admin/config", {
      method: "PUT",
      body: JSON.stringify(data),
    });
  }

  getDisputes(params?: { status?: string }) {
    const qs = new URLSearchParams();
    if (params?.status) qs.set("status", params.status);
    return this.request<any>(`/api/v1/admin/disputes?${qs}`);
  }
}

export const api = new ApiClient();
