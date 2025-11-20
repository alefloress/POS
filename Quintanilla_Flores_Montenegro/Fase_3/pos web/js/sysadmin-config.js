// =====================================================================
// sysadmin-config.js  —  CONFIGURACIÓN GLOBAL DEL POS WEB
// Controla:
//    • modo de operación (local / web)
//    • URL del backend (API real de tu compañero)
//    • función apiFetch() que centraliza TODAS las llamadas HTTP
//
// Esta capa es FUNDAMENTAL. El POS completo depende de este archivo
// para decidir si trabaja con:
//    ✔ datos locales (localStorage / mocks)
//    ✔ API real (backend oficial)
// =====================================================================


// =====================================================================
// 1) OBTENER CONFIGURACIÓN ACTUAL DESDE localStorage
// =====================================================================
function getConfig() {
    // si no existe "pos_config", retorna este objeto por defecto
    return JSON.parse(localStorage.getItem("pos_config") || `{
        "modo": "local",             // Valores posibles: local | web
        "url_api": ""                // Ejemplo: "http://192.168.1.50:8080/api"
    }`);
}


// =====================================================================
// 2) GUARDAR CONFIGURACIÓN EN localStorage
// =====================================================================
function saveConfig(cfg) {
    localStorage.setItem("pos_config", JSON.stringify(cfg));
}


// =====================================================================
// 3) GUARDAR PARÁMETROS (desde el panel SYSADMIN)
// =====================================================================
function guardarParametros() {
    const modo = document.getElementById("cfg_modo").value;
    const url  = document.getElementById("cfg_api").value.trim();

    // Validaciones
    if (modo === "web") {
        if (!url || url.length < 8) {
            alert("Debes ingresar una URL válida para modo WEB.");
            return;
        }
        if (!url.startsWith("http")) {
            alert("La URL debe comenzar con http:// o https://");
            return;
        }
    }

    const cfg = { modo, url_api: url };
    saveConfig(cfg);

    alert("Parámetros guardados correctamente");
    console.log("Nueva configuración SYSADMIN:", cfg);
}


// =====================================================================
// 4) FUNCIONES UTILITARIAS
// =====================================================================

// Retorna la URL base siempre limpia (sin / final duplicado)
function getApiBase() {
    const cfg = getConfig();
    if (!cfg.url_api) return "";
    return cfg.url_api.endsWith("/")
        ? cfg.url_api.slice(0, -1)
        : cfg.url_api;
}

// Ver si estamos en modo WEB real
function isWebMode() {
    return getConfig().modo === "web";
}


// =====================================================================
// 5) apiFetch(endpoint, options)
//    Función central DE TODA LA WEB para consumir la API real
//
//    ✔ apiFetch("/usuarios")
//    ✔ apiFetch("/ventas", { method:"POST", body: JSON.stringify(...) })
//
// Es la ÚNICA función que debe llamarse para acceder al backend.
// =====================================================================
async function apiFetch(endpoint, options = {}) {

    const cfg = getConfig();

    if (cfg.modo !== "web") {
        console.warn("apiFetch() llamado en modo LOCAL → no usa backend real.");
        return null;
    }

    const fullUrl = getApiBase() + endpoint;

    try {
        const response = await fetch(fullUrl, {
            headers: { "Content-Type": "application/json" },
            ...options
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status} en ${fullUrl}`);
        }

        return await response.json();

    } catch (err) {
        console.error("❌ Error en apiFetch:", err);
        alert("Error conectando al backend:\n" + err.message);
        return null;
    }
}


// Debug consola
console.log("✔ sysadmin-config.js cargado correctamente");
