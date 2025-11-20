// js/sdk.js
// SDK real que habla con la API FastAPI (sin mocks ni seeds)

(function () {
  // ==========================
  // Constantes de almacenamiento
  // ==========================
  const STORAGE = {
    token: "pos_token",
    user: "pos_user",
    apiBase: "pos_api_base", // lo usas en la pantalla de conexión / endpoints
  };

  // ==========================
  // Helpers de localStorage
  // ==========================
  function readJSON(key, def = null) {
    try {
      const raw = localStorage.getItem(key);
      if (!raw) return def;
      return JSON.parse(raw);
    } catch {
      return def;
    }
  }

  function writeJSON(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  }

  function getApiBase() {
    // si no has configurado nada, usa este por defecto
    return (
      localStorage.getItem(STORAGE.apiBase) || "http://localhost:8000/v1"
    );
  }

  // ==========================
  //   Cliente genérico HTTP
  // ==========================
  async function apiRequest(endpoint, options = {}) {
    const base = getApiBase();
    const token = localStorage.getItem(STORAGE.token);

    const headers = {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    };

    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const resp = await fetch(base + endpoint, {
      ...options,
      headers,
    });

    const text = await resp.text();
    let data = null;

    if (text) {
      try {
        data = JSON.parse(text);
      } catch (e) {
        console.error("Respuesta NO es JSON:", text);
        throw e;
      }
    }

    if (!resp.ok) {
      let msg = `HTTP ${resp.status} ${resp.statusText}`;

      if (data) {
        // detail puede ser string, objeto o array (FastAPI 422)
        if (Array.isArray(data.detail)) {
          const parts = data.detail.map(d => d.msg || JSON.stringify(d));
          msg = parts.join(" | ");
        } else if (typeof data.detail === "string") {
          msg = data.detail;
        } else if (typeof data.detail === "object" && data.detail.msg) {
          msg = data.detail.msg;
        } else if (data.message) {
          msg = data.message;
        }
      }

      const err = new Error(msg);
      err.status = resp.status;
      err.data = data;
      throw err;
    }
    return data;
  }
    // Normaliza el método de pago que viene del UI
    function normalizePaymentMethod(method) {
    const m = (method || "").toString().toLowerCase().trim();

    if (m === "efectivo") return "CASH";

    // Débito y crédito las mandamos como tarjeta
    if (m === "débito" || m === "debito" || m === "crédito" || m === "credito") {
        return "CARD";
    }

    if (m === "transferencia") return "TRANSFER";

    // Fallback
    return "CASH";
    }

  // ==========================
  //           SDK
  // ==========================
  const SDK = {
    // =====================================================
    // Auth   (/auth/login, /auth/whoami)
    // =====================================================
    Auth: {
      /**
       * Login genérico (admin o sysadmin).
       * Devuelve: { id, username, full_name, role, store_id, store_name, token }
       */
      async login({ username, password }) {
        // 1) Pedir token
        const tok = await apiRequest("/auth/login", {
          method: "POST",
          body: JSON.stringify({ username, password }),
        });

        const accessToken = tok.access_token;
        if (!accessToken) {
          throw new Error("Respuesta de login sin access_token");
        }

        // Guardar token
        localStorage.setItem(STORAGE.token, accessToken);

        // 2) Pedir info de usuario / tienda
        const me = await apiRequest("/auth/whoami", { method: "GET" });

        // Guardar espejo para usar en la UI
        writeJSON(STORAGE.user, me);

        return {
          ...me,
          token: accessToken,
        };
      },

      /**
       * Devuelve info de usuario actual (cacheada o desde la API).
       */
      async me() {
        const cached = readJSON(STORAGE.user, null);
        if (cached) return cached;

        const me = await apiRequest("/auth/whoami", { method: "GET" });
        writeJSON(STORAGE.user, me);
        return me;
      },

      async logout() {
        localStorage.removeItem(STORAGE.token);
        localStorage.removeItem(STORAGE.user);
        return true;
      },
    },

    // =====================================================
    // Products  (CRUD real via /inventory/products)
    // =====================================================
    Products: {
      // Lista de productos
      async list() {
        const data = await apiRequest("/inventory/products", {
          method: "GET",
        });

        return (data || []).map((p) => ({
          id: String(p.id),
          nombre: p.name ?? "",
          stock: Number(p.stock ?? 0),
          precio: Number(p.sale_price ?? 0),
          vence: p.expiry_date ?? null,
          code: p.code ?? "",
        }));
      },

      // Crear producto
      async create(prod) {
        const body = {
          code: prod.code ?? prod.id ?? "",
          name: prod.nombre ?? prod.name ?? "",
          sale_price: Number(prod.precio ?? prod.price ?? 0),
          stock: Number(prod.stock ?? 0),
          expiry_date: prod.vence || null,
        };

        const p = await apiRequest("/inventory/products", {
          method: "POST",
          body: JSON.stringify(body),
        });

        return {
          id: String(p.id),
          nombre: p.name ?? "",
          stock: Number(p.stock ?? 0),
          precio: Number(p.sale_price ?? 0),
          vence: p.expiry_date ?? null,
          code: p.code ?? "",
        };
      },

      // Actualizar producto
      async update(id, prod) {
        const body = {
          code: prod.code ?? prod.id ?? "",
          name: prod.nombre ?? prod.name ?? "",
          sale_price: Number(prod.precio ?? prod.price ?? 0),
          stock: Number(prod.stock ?? 0),
          expiry_date: prod.vence || null,
        };

        const p = await apiRequest(`/inventory/products/${id}`, {
          method: "PUT",
          body: JSON.stringify(body),
        });

        return {
          id: String(p.id),
          nombre: p.name ?? "",
          stock: Number(p.stock ?? 0),
          precio: Number(p.sale_price ?? 0),
          vence: p.expiry_date ?? null,
          code: p.code ?? "",
        };
      },

      // Eliminar producto
      async remove(id) {
        await apiRequest(`/inventory/products/${id}`, { method: "DELETE" });
        return true;
      },
    },

    // =====================================================
    // Suppliers  (de momento sólo lectura si la API lo permite)
    // =====================================================
    Suppliers: {
      async list() {
        const data = await apiRequest("/inventory/suppliers", {
          method: "GET",
        });

        return (data || []).map((s) => ({
          id: String(s.id),
          nombre: s.name ?? "",
          contacto: s.contact_name ?? "",
          mail: s.contact_email ?? "",
          tel: s.phone ?? "",
          addr: s.address ?? "",
        }));
      },

      // Si aún no tienes endpoints para CRUD de proveedores, puedes
      // implementar más tarde create/update/remove. Por ahora
      // lanzamos error claro para no confundir.
      async create() {
        throw new Error("Crear proveedor aún no está implementado en la API");
      },
      async update() {
        throw new Error("Actualizar proveedor aún no está implementado en la API");
      },
      async remove() {
        throw new Error("Eliminar proveedor aún no está implementado en la API");
      },
    },

    // =====================================================
    // Cash  (/cash/open, /cash/close, /cash/active)
    // =====================================================
    Cash: {
    // Devuelve la sesión activa o null
    async current() {
        try {
        const c = await apiRequest("/cash/active", { method: "GET" });
        return c; // CashSessionOut
        } catch (err) {
        // Si no hay caja abierta, el backend normalmente responde 404
        if (err.status === 404) return null;
        throw err;
        }
    },

    // Abrir caja
    async open({ registerId, openingAmount = 0 } = {}) {
        // Por ahora asumimos una sola caja física con ID 1
        const body = {
        register_id: Number(registerId ?? 1),
        opening_amount: Number(openingAmount ?? 0),
        };

        const evt = await apiRequest("/cash/open", {
        method: "POST",
        body: JSON.stringify(body),
        });
        return evt; // CashSessionOut
    },

    // Cerrar caja activa
    async close({ declaredAmount, closingAmount = 0, note = null } = {}) {
        // Preguntamos la sesión activa
        const cur = await SDK.Cash.current();
        if (!cur || cur.status !== "OPEN") {
        const e = new Error("No hay caja abierta.");
        e.status = 400;
        throw e;
        }

        const closing = Number(closingAmount ?? 0);
        const declared = Number(declaredAmount ?? closing);

        const body = {
        session_id: cur.id,
        declared_amount: declared,
        closing_amount: closing,
        note,
        };

        const evt = await apiRequest("/cash/close", {
        method: "POST",
        body: JSON.stringify(body),
        });
        return evt; // CashSessionOut
    },
    },


    // =====================================================
    // Sales  (/sales, /sales/{id})
    // =====================================================
    Sales: {
    async create(payload) {
        // payload esperado:
        // {
        //   sessionId: number,
        //   method: string,        // "Efectivo", "Débito", etc (desde el UI)
        //   items: [{ id, nombre, precio, qty }],
        //   customerName?, customerTaxId?
        // }

        const body = {
        session_id: Number(payload.sessionId),
        payment_method: normalizePaymentMethod(payload.method),
        status: "SALE",  // fijo por ahora
        customer_name: payload.customerName ?? null,
        customer_tax_id: payload.customerTaxId ?? null,
        items: (payload.items || []).map((i) => ({
            product_id: Number(i.id),
            quantity: Number(i.qty || 1),
            unit_price: Number(i.precio ?? i.price ?? 0),
        })),
        };

        return apiRequest("/sales", {
        method: "POST",
        body: JSON.stringify(body),
        });
    },

    async getOne(id) {
        return apiRequest(`/sales/${id}`, { method: "GET" });
    },
    },


    // =====================================================
    // SysAdmin  (/sysadmin/*)
    // =====================================================
    SysAdmin: {
      async listAdmins() {
        return apiRequest("/sysadmin/admins", { method: "GET" });
      },

    async createAdmin({ username, password, full_name = null, is_active = true }) {
    const body = {
        username,
        password,
        // si no mandas full_name desde el formulario, va como null
        full_name,
        is_active,
    };

    return apiRequest("/sysadmin/admins", {
        method: "POST",
        body: JSON.stringify(body),
    });
    },
      async deleteAdmin(id) {
        await apiRequest(`/sysadmin/admins/${id}`, { method: "DELETE" });
        return true;
      },

      async createTenant(payload) {
        // payload: { store_name, plan_id?, contact_name, contact_email,
        //            admin_username, admin_password, admin_full_name?, admin_is_active? }
        return apiRequest("/sysadmin/tenants", {
          method: "POST",
          body: JSON.stringify(payload),
        });
      },
    },

    // =====================================================
    // Admin (usuarios de la tienda) – opcional, por si lo usas
    // =====================================================
    Admin: {
      async listUsers() {
        return apiRequest("/admin/users", { method: "GET" });
      },

      async createUser(user) {
        return apiRequest("/admin/users", {
          method: "POST",
          body: JSON.stringify(user),
        });
      },

      async updateUser(id, user) {
        return apiRequest(`/admin/users/${id}`, {
          method: "PUT",
          body: JSON.stringify(user),
        });
      },

      async deleteUser(id) {
        await apiRequest(`/admin/users/${id}`, { method: "DELETE" });
        return true;
      },
    },
  };

  // Exponer globalmente
  window.SDK = SDK;
  window.apiRequest = apiRequest;
  window.SDK_VERSION = "api-1.0";
})();
