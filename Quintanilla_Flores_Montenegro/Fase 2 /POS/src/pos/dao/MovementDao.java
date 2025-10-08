package pos.dao;

import pos.db.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovementDao {

    /** Inserta un movimiento en el historial. */
    public void insert(String code, String type, int qty, int prevStock, int newStock,
                       String reason, String user, LocalDateTime when) {
        final String sql = """
            INSERT INTO inventory_movements
                (code, type, qty, reason, prev_stock, new_stock, user, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, type);               // "ENTRY" | "EXIT" | "ADJUST" | "DELETE"
            ps.setInt(3, qty);
            ps.setString(4, reason == null ? "" : reason);
            ps.setInt(5, prevStock);
            ps.setInt(6, newStock);
            ps.setString(7, user);
            ps.setString(8, when.toString());    // ISO-8601
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** (Opcional) Inserta usando una conexión existente (para transacciones). */
    public void insert(Connection cn, String code, String type, int qty, int prevStock, int newStock,
                       String reason, String user, LocalDateTime when) throws SQLException {
        final String sql = """
            INSERT INTO inventory_movements
                (code, type, qty, reason, prev_stock, new_stock, user, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, type);
            ps.setInt(3, qty);
            ps.setString(4, reason == null ? "" : reason);
            ps.setInt(5, prevStock);
            ps.setInt(6, newStock);
            ps.setString(7, user);
            ps.setString(8, when.toString());
            ps.executeUpdate();
        }
    }

    /** Lista últimos N movimientos por código (con nombre de producto). */
    public List<String[]> listByCode(String code, int limit) {
        final String sql = """
            SELECT m.type, m.qty, m.reason, m.prev_stock, m.new_stock, m.user, m.created_at,
                   i.name AS product_name
              FROM inventory_movements m
              LEFT JOIN inventory i ON i.code = m.code
             WHERE m.code = ?
             ORDER BY datetime(m.created_at) DESC
             LIMIT ?
        """;
        List<String[]> out = new ArrayList<>();
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new String[]{
                        rs.getString("type"),
                        Integer.toString(rs.getInt("qty")),
                        rs.getString("reason"),
                        Integer.toString(rs.getInt("prev_stock")),
                        Integer.toString(rs.getInt("new_stock")),
                        rs.getString("user"),
                        rs.getString("created_at"),
                        rs.getString("product_name") == null ? "" : rs.getString("product_name")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    /** Lista movimientos recientes (general) con código y nombre de producto. */
    public List<String[]> listRecent(int limit) {
        final String sql = """
            SELECT m.code,
                   i.name AS product_name,
                   m.type, m.qty, m.reason, m.prev_stock, m.new_stock, m.user, m.created_at
              FROM inventory_movements m
              LEFT JOIN inventory i ON i.code = m.code
             ORDER BY datetime(m.created_at) DESC
             LIMIT ?
        """;
        List<String[]> out = new ArrayList<>();
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new String[]{
                        rs.getString("code"),
                        rs.getString("product_name") == null ? "" : rs.getString("product_name"),
                        rs.getString("type"),
                        Integer.toString(rs.getInt("qty")),
                        rs.getString("reason"),
                        Integer.toString(rs.getInt("prev_stock")),
                        Integer.toString(rs.getInt("new_stock")),
                        rs.getString("user"),
                        rs.getString("created_at")
                    });
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }
}
