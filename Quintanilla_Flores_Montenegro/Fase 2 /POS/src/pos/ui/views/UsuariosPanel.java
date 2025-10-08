package pos.ui.views;

import pos.model.User;
import pos.store.InMemoryStore;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class UsuariosPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final JTextField txtBuscar = new JTextField(20);
    private final JTable tabla = new JTable(new UsersModel());
    private final UsersModel modelo = (UsersModel) tabla.getModel();

    public UsuariosPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(new Color(0xF9FAFB));

        // Título
        JLabel title = new JLabel("Usuarios");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,0,12));
        add(title, BorderLayout.NORTH);

        // Barra superior
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        barra.setOpaque(false);

        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por ID, usuario o rol...");
        JButton btnBuscar   = new JButton("Buscar");
        JButton btnNuevo    = new JButton("Nuevo");
        JButton btnEditar   = new JButton("Editar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnReset    = new JButton("Reset Pass");

        barra.add(txtBuscar);
        barra.add(btnBuscar);
        barra.add(btnNuevo);
        barra.add(btnEditar);
        barra.add(btnEliminar);
        barra.add(btnReset);
        add(barra, BorderLayout.PAGE_START);

        // Tabla
        tabla.setRowHeight(22);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Carga inicial
        recargar();

        // Acciones
        btnBuscar.addActionListener(e -> buscar());
        btnNuevo.addActionListener(e -> nuevoUsuario());
        btnEditar.addActionListener(e -> editarUsuario());
        btnEliminar.addActionListener(e -> eliminarUsuario());
        btnReset.addActionListener(e -> resetPassword());
    }

    // ===== Funciones =====
    private void recargar() {
        modelo.set(InMemoryStore.getAllUsers());
    }

    private void buscar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        if (q.isEmpty()) { recargar(); return; }
        List<User> filtrados = InMemoryStore.getAllUsers().stream()
                .filter(u -> u.getId().toLowerCase().contains(q)
                          || u.getUsername().toLowerCase().contains(q)
                          || u.getRole().toLowerCase().contains(q))
                .collect(Collectors.toList());
        modelo.set(filtrados);
    }

    private void nuevoUsuario() {
        JTextField id   = new JTextField(InMemoryStore.nextUserId());
        JTextField user = new JTextField();
        JComboBox<String> rol = new JComboBox<>(new String[]{"ADMIN","CAJERO"});
        JCheckBox activo = new JCheckBox("Activo", true);
        JTextField pass  = new JTextField("123456");
        JTextField fecha = new JTextField(LocalDate.now().toString());

        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("ID:")); p.add(id);
        p.add(new JLabel("Usuario:")); p.add(user);
        p.add(new JLabel("Rol:")); p.add(rol);
        p.add(new JLabel("Activo:")); p.add(activo);
        p.add(new JLabel("Contraseña:")); p.add(pass);
        p.add(new JLabel("Creado (AAAA-MM-DD):")); p.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, p, "Nuevo usuario", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                LocalDate created = fecha.getText().isBlank() ? LocalDate.now() : LocalDate.parse(fecha.getText());
                User u = new User(id.getText(), user.getText(), (String) rol.getSelectedItem(),
                        activo.isSelected(), created, pass.getText());
                InMemoryStore.addUser(u);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Datos inválidos");
            }
        }
    }

    private void editarUsuario() {
        int row = tabla.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un usuario para editar"); return; }
        User u = modelo.getAt(row);

        JTextField user = new JTextField(u.getUsername());
        JComboBox<String> rol = new JComboBox<>(new String[]{"ADMIN","CAJERO"});
        rol.setSelectedItem(u.getRole());
        JCheckBox activo = new JCheckBox("Activo", u.isActive());
        JTextField fecha = new JTextField(u.getCreatedAt()==null ? "" : u.getCreatedAt().toString());

        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("ID:")); p.add(new JLabel(u.getId()));
        p.add(new JLabel("Usuario:")); p.add(user);
        p.add(new JLabel("Rol:")); p.add(rol);
        p.add(new JLabel("Activo:")); p.add(activo);
        p.add(new JLabel("Creado:")); p.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, p, "Editar usuario", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                u.setUsername(user.getText());
                u.setRole((String) rol.getSelectedItem());
                u.setActive(activo.isSelected());
                u.setCreatedAt(fecha.getText().isBlank() ? null : LocalDate.parse(fecha.getText()));
                InMemoryStore.saveUser(u);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al editar usuario");
            }
        }
    }

    private void eliminarUsuario() {
        int row = tabla.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un usuario para eliminar"); return; }
        User u = modelo.getAt(row);
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar " + u.getUsername() + "?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            InMemoryStore.removeUser(u.getId());
            recargar();
        }
    }

    private void resetPassword() {
        int row = tabla.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un usuario"); return; }
        User u = modelo.getAt(row);

        JTextField pass = new JTextField();
        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("Nueva contraseña:")); p.add(pass);

        int res = JOptionPane.showConfirmDialog(this, p, "Resetear contraseña", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            if (InMemoryStore.resetUserPassword(u.getId(), pass.getText())) {
                JOptionPane.showMessageDialog(this, "Contraseña actualizada");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar");
            }
        }
    }

    // ===== Modelo de tabla =====
    private static class UsersModel extends AbstractTableModel {
        private final String[] cols = {"ID","Usuario","Rol","Activo","Creado"};
        private List<User> data = List.of();

        public void set(List<User> rows) { data = rows; fireTableDataChanged(); }
        public User getAt(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            User u = data.get(r);
            switch (c) {
                case 0: return u.getId();
                case 1: return u.getUsername();
                case 2: return u.getRole();
                case 3: return u.isActive() ? "Sí" : "No";
                case 4: return u.getCreatedAt()==null ? "-" : DF.format(u.getCreatedAt());
                default: return "";
            }
        }
    }
}
