package pos.ui;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;


import pos.login.LoginFrame;

public class Sidebar extends JPanel {

    public Sidebar(Consumer<String> onNavigate, String role, String username) {
        // Tamaño y estilos
        setPreferredSize(new Dimension(230, 720));
        setLayout(new GridBagLayout());
        setBackground(new Color(0x111827)); // gris muy oscuro

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 12, 6, 12);

        // Título
        JLabel title = new JLabel("Almacén Sonia");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        addAt(c, title);

        // --- Botones comunes (SIN Productos) ---
        addBtn(c, "Dashboard",  () -> onNavigate.accept(MainFrame.DASHBOARD));
        addBtn(c, "Ventas",     () -> onNavigate.accept(MainFrame.VENTAS));
        addBtn(c, "Clientes",   () -> onNavigate.accept(MainFrame.CLIENTES));
        addBtn(c, "Inventario", () -> onNavigate.accept(MainFrame.INVENTARIO));
        addBtn(c, "Reportes",   () -> onNavigate.accept(MainFrame.REPORTES));

        // --- Solo ADMIN ---
        if ("ADMIN".equalsIgnoreCase(role)) {
            addSeparator(c);
            addBtn(c, "Usuarios", () -> onNavigate.accept(MainFrame.USUARIOS));
            addBtn(c, "Ajustes",  () -> onNavigate.accept(MainFrame.AJUSTES));
        }

        // Cerrar sesión
        addSeparator(c);
        addBtn(c, "Cerrar sesión", () -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) w.dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });

        // Empujar el footer al final
        c.weighty = 1;
        addAt(c, Box.createVerticalGlue());

        // Footer con usuario/rol
        JLabel info = new JLabel("Usuario: " + username + "  |  Rol: " + role);
        info.setForeground(new Color(0x9CA3AF));
        info.setFont(info.getFont().deriveFont(Font.PLAIN, 12f));
        addAt(c, info);
    }

    // --- Helpers de UI ---
    private void addBtn(GridBagConstraints c, String text, Runnable action) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(0x1F2937)); // gris
        b.setPreferredSize(new Dimension(190, 40));
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 14f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        addAt(c, b);
    }

    private void addSeparator(GridBagConstraints c) {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0x374151));
        addAt(c, sep);
    }

    private void addAt(GridBagConstraints c, Component comp) {
        c.gridy++;
        add(comp, c);
    }
}
