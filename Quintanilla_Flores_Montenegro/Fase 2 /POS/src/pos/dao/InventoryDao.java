package pos.dao;

import pos.db.Database;
import pos.model.Product;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InventoryDao {

  
    public void insert(Product p) {
        final String sql = "INSERT INTO inventory (code, name, category, price, stock, expiry) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection cn = Database.get();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getCode());
            ps.setString(2, p.getName());
            ps.setString(3, p.getCategory());
            ps.setInt(4,    p.getPrice());
            ps.setInt(5,    p.getStock());
            if (p.getExpiry() != null) ps.setString(6, p.getExpiry().toString());
            else                        ps.setNull(6, Types.VARCHAR);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

  
    public List<Product> listAll() {
        final String sql = "SELECT id, code, name, category, price, stock, expiry FROM inventory ORDER BY id DESC";
        List<Product> out = new ArrayList<>();
        try (Connection cn = Database.get();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(fromRow(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return out;
    }

    
    public Product findById(int id) {
        final String sql = "SELECT id, code, name, category, price, stock, expiry FROM inventory WHERE id = ?";
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    
    public Product findByCode(String code) {
        final String sql = "SELECT id, code, name, category, price, stock, expiry FROM inventory WHERE code = ?";
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return fromRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

   
    public void update(Product p) {
        if (p.getId() > 0) {
            final String sql = "UPDATE inventory SET code=?, name=?, category=?, price=?, stock=?, expiry=? WHERE id=?";
            try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, p.getCode());
                ps.setString(2, p.getName());
                ps.setString(3, p.getCategory());
                ps.setInt(4,    p.getPrice());
                ps.setInt(5,    p.getStock());
                if (p.getExpiry() != null) ps.setString(6, p.getExpiry().toString());
                else                        ps.setNull(6, Types.VARCHAR);
                ps.setInt(7, p.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
           
            final String sql = "UPDATE inventory SET name=?, category=?, price=?, stock=?, expiry=? WHERE code=?";
            try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getCategory());
                ps.setInt(3,    p.getPrice());
                ps.setInt(4,    p.getStock());
                if (p.getExpiry() != null) ps.setString(5, p.getExpiry().toString());
                else                        ps.setNull(5, Types.VARCHAR);
                ps.setString(6, p.getCode());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

   
    public void delete(int id) {
        final String sql = "DELETE FROM inventory WHERE id = ?";
        try (Connection cn = Database.get(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    
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
    }
}
