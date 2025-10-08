package pos.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;

public final class Database {

    // ~/.pos_demo/pos_cache.db  (en Windows: C:\Users\TU_USUARIO\.pos_demo\pos_cache.db)
    private static final String DEFAULT_PATH =
            System.getProperty("user.home") + File.separator + ".pos_demo" + File.separator + "pos_cache.db";

    private static final String DEFAULT_URL = "jdbc:sqlite:" + DEFAULT_PATH;

    // Registra explícitamente el driver de SQLite cuando el JAR está en el classpath
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "No se encontró el driver SQLite (sqlite-jdbc). " +
                "Asegúrate de haber agregado el JAR al Build Path.", e);
        }
    }

    /** Permite sobrescribir con -Dpos.db.url=jdbc:sqlite:C:\\ruta\\otro.db */
    public static String url() {
        String override = System.getProperty("pos.db.url");
        return (override != null && !override.isBlank()) ? override : DEFAULT_URL;
    }

    /** Devuelve una conexión lista. Crea la carpeta si no existe. */
    public static Connection get() throws SQLException {
        String u = url();

        // Si es archivo local, asegúrate de crear la carpeta contenedora
        if (u.startsWith("jdbc:sqlite:")) {
            String path = u.substring("jdbc:sqlite:".length());
            File dir = new File(path).getParentFile();
            if (dir != null) dir.mkdirs();
        }

        Connection cn = DriverManager.getConnection(u);

        // PRAGMAs útiles para consistencia (no son obligatorios)
        try (Statement st = cn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
            st.execute("PRAGMA synchronous = NORMAL");
        } catch (SQLException ignore) {}

        return cn;
    }

    private Database() { /* no instanciable */ }
}

