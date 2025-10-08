package pos.db;

import pos.model.Product;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    /** Crea la tabla INVENTORY si no existe y asegura columnas/índice por code. */
    public void createTableIfNotExists() throws SQLException {
        try (Connection cn = Database.get(); Statement st = cn.createStatement()) {

            // Esquema completo alineado a pos.model.Product
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    code     TEXT,
                    name     TEXT    NOT NULL,
                    category TEXT,
                    price    INTEGER NOT NULL,
                    stock    INTEGER NOT NULL,
                    expiry   TEXT
                )
            """);

            // Migraciones defensivas (si la tabla ya existía con menos columnas)
            try { st.execute("ALTER TABLE inventory ADD COLUMN code TEXT"); }     catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN category TEXT"); } catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN expiry TEXT"); }   catch (SQLException ignore) {}

            // Índice único por code (si falla porque ya existe, lo ignoramos)
            try { st.execute("CREATE UNIQUE INDEX ux_inventory_code ON inventory(code)"); } catch (SQLException ignore) {}
        }
    }

    /** Inserta o actualiza por 'code'. Si existe el code → UPDATE, si no → INSERT. */
    public void upsert(Product p) throws SQLException {
        // 1) Intentar UPDATE por code
        final String sqlUpdate = """
            UPDATE inventory
               SET name = ?, category = ?, price = ?, stock = ?, expiry = ?
             WHERE code = ?
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sqlUpdate)) {

            ps.setString(1, p.getName());
            ps.setString(2, p.getCategory());
            ps.setInt(3,    p.getPrice());
            ps.setInt(4,    p.getStock());
            if (p.getExpiry() != null) ps.setString(5, p.getExpiry().toString());
            else                        ps.setNull(5, Types.VARCHAR);
            ps.setString(6, p.getCode());

            int updated = ps.executeUpdate();
            if (updated > 0) return; // ya estaba, quedó actualizado
        }

        // 2) Si no actualizó nada, hacer INSERT
        final String sqlInsert = """
            INSERT INTO inventory (code, name, category, price, stock, expiry)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sqlInsert)) {

            ps.setString(1, p.getCode());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setInt(4,    p.getPrice());
            ps.setInt(5,    p.getStock());
            if (p.getExpiry() != null) ps.setString(6, p.getExpiry().toString());
            else                        ps.setNull(6, Types.VARCHAR);

            ps.executeUpdate();
        }
    }

    /** Lista todos los productos de la tabla. */
    public List<Product> listAll() throws SQLException {
        final String sql = "SELECT id, code, name, category, price, stock, expiry FROM inventory ORDER BY id DESC";
        List<Product> out = new ArrayList<>();

        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                out.add(fromRow(rs));
            }
        }
        return out;
    }

    /** Buscar por code (útil si lo necesitas). */
    public Product findByCode(String code) throws SQLException {
        final String sql = "SELECT id, code, name, category, price, stock, expiry FROM inventory WHERE code = ?";
        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromRow(rs);
            }
        }
        return null;
    }

    /** Eliminar por code (opcional). */
    public void deleteByCode(String code) throws SQLException {
        final String sql = "DELETE FROM inventory WHERE code = ?";
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.executeUpdate();
        }
    }

    // ===== Mapping fila -> Product =====
    private Product fromRow(ResultSet rs) throws SQLException {
        int id          = rs.getInt("id");
        String code     = rs.getString("code");
        String name     = rs.getString("name");
        String category = rs.getString("category");
        int price       = rs.getInt("price");
        int stock       = rs.getInt("stock");
        String exStr    = rs.getString("expiry");
        LocalDate exp   = (exStr == null || exStr.isBlank()) ? null : LocalDate.parse(exStr);
        return new Product(id, code, name, category, price, stock, exp);
        // Nota: tu clase Product maneja id/code/name/category/price/stock/expiry (LocalDate)
    }
}
