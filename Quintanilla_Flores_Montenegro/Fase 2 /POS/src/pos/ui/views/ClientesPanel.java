package pos.ui.views;

import pos.model.Client;
import pos.store.InMemoryStore;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ClientesPanel extends JPanel {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final JTextField txtBuscar = new JTextField(22);
    private final JTable tabla = new JTable(new ClientesModel());
    private final ClientesModel modelo = (ClientesModel) tabla.getModel();

    public ClientesPanel() {
        setLayout(new BorderLayout(10,10));
        setBackground(new Color(0xF9FAFB));

        // Título
        JLabel title = new JLabel("Clientes");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setBorder(BorderFactory.createEmptyBorder(12,12,0,12));
        add(title, BorderLayout.NORTH);

        // Barra superior
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        barra.setOpaque(false);

        txtBuscar.putClientProperty("JTextField.placeholderText", "Buscar por nombre, teléfono, email o ID...");
        JButton btnBuscar   = new JButton("Buscar");
        JButton btnNuevo    = new JButton("Nuevo");
        JButton btnEditar   = new JButton("Editar");
        JButton btnEliminar = new JButton("Eliminar");
        JButton btnRapido   = new JButton("Registro rápido");

        barra.add(txtBuscar);
        barra.add(btnBuscar);
        barra.add(btnNuevo);
        barra.add(btnEditar);
        barra.add(btnEliminar);
        barra.add(btnRapido);
        add(barra, BorderLayout.PAGE_START);

        // Tabla
        tabla.setRowHeight(22);
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Carga inicial
        recargar();

        // Acciones
        btnBuscar.addActionListener(e -> buscar());
        btnNuevo.addActionListener(e -> nuevoCliente());
        btnEditar.addActionListener(e -> editarCliente());
        btnEliminar.addActionListener(e -> eliminarCliente());
        btnRapido.addActionListener(e -> registroRapido());
    }

    // ===== Lógica =====
    private void recargar() {
        // Si tu InMemoryStore no tiene getAllClients(), cambia por allClients()
        modelo.set(InMemoryStore.getAllClients());
    }

    private void buscar() {
        String q = txtBuscar.getText().trim().toLowerCase();
        if (q.isEmpty()) { recargar(); return; }
        List<Client> filtrados = InMemoryStore.getAllClients().stream()
                .filter(c ->
                        c.getName().toLowerCase().contains(q) ||
                        (c.getPhone()!=null && c.getPhone().toLowerCase().contains(q)) ||
                        (c.getEmail()!=null && c.getEmail().toLowerCase().contains(q)) ||
                        c.getId().toLowerCase().contains(q))
                .collect(Collectors.toList());
        modelo.set(filtrados);
    }

    private void nuevoCliente() {
        JTextField id        = new JTextField(InMemoryStore.nextClientId());
        JTextField nombre    = new JTextField();
        JTextField fono      = new JTextField();
        JTextField email     = new JTextField();
        JTextField direccion = new JTextField();
        JTextField fecha     = new JTextField(LocalDate.now().toString());

        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("ID:"));        p.add(id);
        p.add(new JLabel("Nombre:"));    p.add(nombre);
        p.add(new JLabel("Teléfono:"));  p.add(fono);
        p.add(new JLabel("Email:"));     p.add(email);
        p.add(new JLabel("Dirección:")); p.add(direccion);
        p.add(new JLabel("Creado (AAAA-MM-DD):")); p.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, p, "Nuevo cliente", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                LocalDate created = fecha.getText().isBlank() ? LocalDate.now() : LocalDate.parse(fecha.getText());
                Client c = new Client(
                        id.getText(),
                        nombre.getText(),
                        fono.getText(),
                        email.getText(),
                        direccion.getText(),
                        created
                );
                // Si no tienes addClient(), usa upsertClient(c)
                InMemoryStore.addClient(c);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Datos inválidos");
            }
        }
    }

    private void editarCliente() {
        int row = tabla.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un cliente para editar"); return; }
        Client c = modelo.getAt(row);

        JTextField nombre    = new JTextField(c.getName());
        JTextField fono      = new JTextField(c.getPhone());
        JTextField email     = new JTextField(c.getEmail());
        JTextField direccion = new JTextField(c.getAddress());
        JTextField fecha     = new JTextField(c.getCreatedAt()==null ? "" : c.getCreatedAt().toString());

        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("ID:"));        p.add(new JLabel(c.getId()));
        p.add(new JLabel("Nombre:"));    p.add(nombre);
        p.add(new JLabel("Teléfono:"));  p.add(fono);
        p.add(new JLabel("Email:"));     p.add(email);
        p.add(new JLabel("Dirección:")); p.add(direccion);
        p.add(new JLabel("Creado:"));    p.add(fecha);

        int res = JOptionPane.showConfirmDialog(this, p, "Editar cliente", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                c.setName(nombre.getText());
                c.setPhone(fono.getText());
                c.setEmail(email.getText());
                c.setAddress(direccion.getText());
                c.setCreatedAt(fecha.getText().isBlank() ? null : LocalDate.parse(fecha.getText()));
                // Si no tienes saveClient(), usa updateClient(c) o upsertClient(c)
                InMemoryStore.saveClient(c);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al editar cliente");
            }
        }
    }

    private void eliminarCliente() {
        int row = tabla.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Selecciona un cliente para eliminar"); return; }
        Client c = modelo.getAt(row);
        int ok = JOptionPane.showConfirmDialog(this, "¿Eliminar " + c.getName() + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            // Si no tienes removeClient(), usa removeClient(c.getId()) que agregamos antes
            InMemoryStore.removeClient(c.getId());
            recargar();
        }
    }

    private void registroRapido() {
        JTextField nombre = new JTextField();
        JTextField fono   = new JTextField();

        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("Nombre:"));   p.add(nombre);
        p.add(new JLabel("Teléfono:")); p.add(fono);

        int res = JOptionPane.showConfirmDialog(this, p, "Registro rápido", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                Client c = new Client(
                        InMemoryStore.nextClientId(),
                        nombre.getText(),
                        fono.getText(),
                        "",                 // email opcional
                        "",                 // dirección opcional
                        LocalDate.now()
                );
                InMemoryStore.addClient(c);
                recargar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error en los datos ingresados");
            }
        }
    }

    // ===== Modelo de tabla =====
    private static class ClientesModel extends AbstractTableModel {
        private final String[] cols = {"ID","Nombre","Teléfono","Email","Dirección","Creado"};
        private List<Client> data = List.of();

        public void set(List<Client> rows) { data = rows; fireTableDataChanged(); }
        public Client getAt(int row) { return data.get(row); }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            Client x = data.get(r);
            switch (c) {
                case 0: return x.getId();
                case 1: return x.getName();
                case 2: return x.getPhone();
                case 3: return x.getEmail();
                case 4: return x.getAddress();
                case 5: return x.getCreatedAt()==null ? "-" : DF.format(x.getCreatedAt());
                default: return "";
            }
        }
    }
}
