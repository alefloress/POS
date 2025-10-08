package pos.ui.views;

import pos.store.InMemoryStore;
import pos.model.User;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AjustesPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private final JComboBox<UserItem> cmbUsuarios = new JComboBox<>();

    public AjustesPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(new Color(0xF9FAFB));

        // Título
        JLabel title = new JLabel("Ajustes del Sistema");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,0,12));
        add(title, BorderLayout.NORTH);

        // Contenido principal
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new GridLayout(1, 3, 12, 12)); // tres tarjetas: Info / Cuenta / Sesión
        content.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        add(content, BorderLayout.CENTER);

        // ===== Tarjeta 1: Información del sistema =====
        JPanel infoCard = card("Información");
        infoCard.add(row("Negocio:", new JLabel("Mi Mini Market")));
        infoCard.add(row("Fecha:", new JLabel(DF.format(LocalDate.now()))));
        infoCard.add(row("Versión:", new JLabel("v1.0 Beta")));
        infoCard.add(Box.createVerticalStrut(8));
        infoCard.add(new JLabel(" ")); // separador óptico
        content.add(infoCard);

        // ===== Tarjeta 2: Cuenta / Seguridad =====
        JPanel cuentaCard = card("Cuenta / Seguridad");

        // Selector de usuario
        cargarUsuarios();
        JPanel r1 = row("Usuario:", cmbUsuarios);
        cuentaCard.add(r1);

        // Campo nueva contraseña
        JPasswordField txtNueva = new JPasswordField();
        JPanel r2 = row("Nueva contraseña:", txtNueva);
        cuentaCard.add(r2);

        // Botón cambiar
        JButton btnCambiar = new JButton("Cambiar contraseña");
        btnCambiar.addActionListener(e -> {
            UserItem sel = (UserItem) cmbUsuarios.getSelectedItem();
            if (sel == null) { JOptionPane.showMessageDialog(this, "Selecciona un usuario."); return; }
            String nueva = new String(txtNueva.getPassword()).trim();
            if (nueva.isBlank()) { JOptionPane.showMessageDialog(this, "Ingresa una contraseña válida."); return; }

            boolean ok = InMemoryStore.resetUserPassword(sel.id, nueva);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Contraseña actualizada para " + sel.username);
                txtNueva.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar la contraseña.");
            }
        });
        cuentaCard.add(wrapRight(btnCambiar));

        content.add(cuentaCard);

        // ===== Tarjeta 3: Sesión =====
        JPanel sesionCard = card("Sesión");

        JTextArea tips = new JTextArea(
                "Consejos:\n" +
                "• Guarda tu trabajo antes de cerrar sesión.\n" +
                "• Cambia tu contraseña con regularidad.\n" +
                "• Usa roles (ADMIN/CAJERO) según corresponda."
        );
        tips.setWrapStyleWord(true);
        tips.setLineWrap(true);
        tips.setEditable(false);
        tips.setOpaque(false);
        sesionCard.add(wrap(tips));

        JButton btnLogout = new JButton("Cerrar sesión");
        btnLogout.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this,
                    "¿Deseas cerrar sesión ahora?",
                    "Confirmar",
                    JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) {
                // Si tienes un LoginFrame, aquí podrías volver a él.
                // Por ahora, cerramos la aplicación para simular logout.
                System.exit(0);
            }
        });
        sesionCard.add(wrapRight(btnLogout));

        content.add(sesionCard);
    }

    // ===== Helpers UI =====
    private JPanel card(String title) {
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(12,12,12,12)
        ));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 14f));
        t.setBorder(BorderFactory.createEmptyBorder(0,0,8,0));
        card.add(t);
        return card;
    }

    private JPanel row(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(6,0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        p.add(l, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        return p;
    }

    private JPanel wrap(JComponent c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        return p;
    }

    private JPanel wrapRight(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        p.setOpaque(false);
        p.add(c);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return p;
    }

    private void cargarUsuarios() {
        cmbUsuarios.removeAllItems();
        List<User> users = InMemoryStore.getAllUsers();
        for (User u : users) {
            cmbUsuarios.addItem(new UserItem(u.getId(), u.getUsername()));
        }
        if (cmbUsuarios.getItemCount() > 0) cmbUsuarios.setSelectedIndex(0);
    }

    // Item para mostrar en combo
    private static class UserItem {
        final String id;
        final String username;
        UserItem(String id, String username) { this.id = id; this.username = username; }
        @Override public String toString() { return id + " — " + username; }
    }
}
