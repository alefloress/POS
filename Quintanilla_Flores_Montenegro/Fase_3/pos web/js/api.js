// ======================================================================
// api.js 
// Capa universal de comunicación con el backend real.
// Este archivo usa la configuración guardada por el SYSADMIN
// (ver sysadmin-config.js) y provee funciones GET/POST/PUT/DELETE.
//
// Tu amigo SOLO necesitará modificar los endpoints (“/usuarios”,
// “/ventas”, “/productos”, etc.).
// ======================================================================


// ======================================================================
// 1) Obtener configuración del sistema (modo + url_api)
// ======================================================================
function getApiConfig() {
    return JSON.parse(localStorage.getItem("pos_config") || `{
        "modo": "local",
        "url_api": ""
    }`);
}


// ======================================================================
// 2) GET genérico
// ======================================================================
async function apiGet(endpoint) {
    const cfg = getApiConfig();

    if (cfg.modo !== "web") {
        throw new Error("Modo backend está en LOCAL → no se llama a API.");
    }

    const resp = await fetch(cfg.url_api + endpoint);

    if (!resp.ok) {
        throw new Error("Error GET " + endpoint + " (HTTP " + resp.status + ")");
    }

    return await resp.json();
}


// ======================================================================
// 3) POST genérico
// ======================================================================
async function apiPost(endpoint, data) {
    const cfg = getApiConfig();

    if (cfg.modo !== "web") {
        throw new Error("Modo backend está en LOCAL → no se llama a API.");
    }

    const resp = await fetch(cfg.url_api + endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    });

    if (!resp.ok) {
        throw new Error("Error POST " + endpoint + " (HTTP " + resp.status + ")");
    }

    return await resp.json();
}


// ======================================================================
// 4) PUT genérico
// ======================================================================
async function apiPut(endpoint, data) {
    const cfg = getApiConfig();

    if (cfg.modo !== "web") {
        throw new Error("Modo backend está en LOCAL → no se llama a API.");
    }

    const resp = await fetch(cfg.url_api + endpoint, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data)
    });

    if (!resp.ok) {
        throw new Error("Error PUT " + endpoint + " (HTTP " + resp.status + ")");
    }

    return await resp.json();
}


// ======================================================================
// 5) DELETE genérico
// ======================================================================
async function apiDelete(endpoint) {
    const cfg = getApiConfig();

    if (cfg.modo !== "web") {
        throw new Error("Modo backend está en LOCAL → no se llama a API.");
    }

    const resp = await fetch(cfg.url_api + endpoint, { method: "DELETE" });

    if (!resp.ok) {
        throw new Error("Error DELETE " + endpoint + " (HTTP " + resp.status + ")");
    }

    return await resp.json();
}


console.log("api.js cargado ✔ (modo = " + getApiConfig().modo + ")");
