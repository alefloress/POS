// assets/js/sysadmin-panel.js

// =============================
//  Helpers de UI
// =============================
function setTablaMensaje(msg, clase = "text-danger") {
  const tbody = document.getElementById("tablaUsuarios");
  if (!tbody) return;
  tbody.innerHTML = `
    <tr>
      <td colspan="4" class="text-center ${clase}">${msg}</td>
    </tr>
  `;
}

// =============================
//  Cargar lista de admins
// =============================
async function cargarAdmins() {
  const tbody = document.getElementById("tablaUsuarios");
  if (!tbody) return;

  setTablaMensaje("Cargando usuarios...", "text-secondary");

  try {
    const admins = await SDK.SysAdmin.listAdmins(); // GET /v1/sysadmin/admins

    if (!admins || admins.length === 0) {
      setTablaMensaje("No hay administradores registrados", "text-secondary");
      return;
    }

    const rows = admins
      .map((a) => {
        const estadoBadge = a.is_active
          ? '<span class="badge bg-success">Activo</span>'
          : '<span class="badge bg-secondary">Inactivo</span>';

        return `
          <tr>
            <td>${a.username}</td>
            <td>${a.role}</td>
            <td>${estadoBadge}</td>
            <td>
              <button
                class="btn btn-sm btn-outline-danger"
                onclick="eliminarAdmin(${a.id}, '${a.username}')"
              >
                Eliminar
              </button>
            </td>
          </tr>
        `;
      })
      .join("");

    tbody.innerHTML = rows;
  } catch (err) {
    console.error("Error cargando admins:", err);
    setTablaMensaje("Error al cargar los usuarios", "text-danger");
  }
}

// =============================
//  Crear nuevo admin
// =============================
async function crearUsuario() {
  const userInput = document.getElementById("newUser");
  const passInput = document.getElementById("newPass");
  const rolSelect = document.getElementById("newRol");

  const username = (userInput?.value || "").trim();
  const password = (passInput?.value || "").trim();
  const rol = rolSelect ? rolSelect.value : "ADMIN";

  if (!username || !password) {
    alert("Completa usuario y contraseña");
    return;
  }

  if (rol !== "ADMIN") {
    alert("Por ahora solo se crean usuarios ADMIN desde este panel.");
    return;
  }

  // Por ahora usamos store_id fijo = 1.
  // Más adelante lo puedes leer de un selector o de la sesión del tenant.
  const storeId = 1;

  try {
    await SDK.SysAdmin.createAdmin({
      store_id: storeId,
      username,
      password,
      is_active: true,
    });

    if (userInput) userInput.value = "";
    if (passInput) passInput.value = "";

    await cargarAdmins();
    alert("Usuario creado con éxito");
  } catch (err) {
    console.error("Error creando admin:", err);
    alert("Error al crear el usuario: " + err.message);
  }
}

// =============================
//  Eliminar admin
// =============================
async function eliminarAdmin(id, username) {
  if (!confirm(`¿Eliminar usuario ${username}?`)) return;

  try {
    await SDK.SysAdmin.deleteAdmin(id); // DELETE /v1/sysadmin/admins/{id}
    await cargarAdmins();
  } catch (err) {
    console.error("Error eliminando admin:", err);
    alert("Error al eliminar el usuario: " + err.message);
  }
}

// =============================
//  Inicialización del panel
// =============================
document.addEventListener("DOMContentLoaded", async () => {
  try {
    // proteger la pantalla (usa guardSysAdmin de sysadmin.js)
    if (typeof guardSysAdmin === "function") {
      guardSysAdmin();
    }
  } catch (e) {
    // guardSysAdmin ya redirige si hay problema
    return;
  }

  // cargar lista de admins al entrar
  cargarAdmins();
});
