package pos.db;

import java.sql.*;

public final class DatabaseInit {

    public static void initialize() {
        try (Connection cn = Database.get(); Statement st = cn.createStatement()) {

            // ================== INVENTORY ==================
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory (
                    id       INTEGER PRIMARY KEY AUTOINCREMENT,
                    code     TEXT,
                    name     TEXT    NOT NULL,
                    category TEXT,
                    price    INTEGER NOT NULL,
                    stock    INTEGER NOT NULL,
                    expiry   TEXT    -- yyyy-MM-dd o NULL
                )
            """);

            // Migraciones defensivas (si la tabla ya existía con menos columnas)
            try { st.execute("ALTER TABLE inventory ADD COLUMN code TEXT"); }     catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN category TEXT"); } catch (SQLException ignore) {}
            try { st.execute("ALTER TABLE inventory ADD COLUMN expiry TEXT"); }   catch (SQLException ignore) {}

            // Índice único por código (si ya existe, se ignora la excepción)
            try { st.execute("CREATE UNIQUE INDEX ux_inventory_code ON inventory(code)"); } catch (SQLException ignore) {}

            // ================== INVENTORY_MOVEMENTS (historial) ==================
            st.execute("""
                CREATE TABLE IF NOT EXISTS inventory_movements (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    code        TEXT    NOT NULL,          -- código del producto
                    type        TEXT    NOT NULL,          -- ENTRY | EXIT | ADJUST
                    qty         INTEGER NOT NULL,
                    reason      TEXT,
                    prev_stock  INTEGER NOT NULL,
                    new_stock   INTEGER NOT NULL,
                    user        TEXT,
                    created_at  TEXT    NOT NULL           -- ISO-8601 (yyyy-MM-ddTHH:mm:ss)
                )
            """);

            // Índice para consultas por producto/fecha
            try { st.execute("CREATE INDEX ix_mov_code_date ON inventory_movements(code, created_at)"); }
            catch (SQLException ignore) {}

            System.out.println("Esquema de base listo (inventory + inventory_movements) ✅");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private DatabaseInit() { }
}
