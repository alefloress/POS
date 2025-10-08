package pos.ui.dialogs;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Dialogo de cobro:
 *  - Muestra Subtotal, Impuesto y Total
 *  - Ingresas efectivo recibido
 *  - Calcula Vuelto
 *  - Confirmar = OK / Cancelar = null
 *
 * Uso:
 *   CheckoutDialog dlg = new CheckoutDialog(parent, subtotal, new BigDecimal("0.18")); // 18% IGV
 *   dlg.setVisible(true);
 *   CheckoutDialog.Result r = dlg.getResult();
 *   if (r != null) { ... }
 */
public class CheckoutDialog extends JDialog {

    public static class Result {
        public final BigDecimal subtotal;
        public final BigDecimal taxAmount;
        public final BigDecimal total;
        public final BigDecimal cashReceived;
        public final BigDecimal change;

        public Result(BigDecimal subtotal, BigDecimal taxAmount, BigDecimal total,
                      BigDecimal cashReceived, BigDecimal change) {
            this.subtotal = subtotal;
            this.taxAmount = taxAmount;
            this.total = total;
            this.cashReceived = cashReceived;
            this.change = change;
        }
    }

    private final JLabel lblSubtotal = new JLabel();
    private final JLabel lblImpuesto = new JLabel();
    private final JLabel lblTotal = new JLabel();
    private final JTextField txtRecibido = new JTextField();
    private final JLabel lblVuelto = new JLabel();

    private final JButton btnConfirmar = new JButton("Cobrar (Enter)");
    private final JButton btnCancelar = new JButton("Cancelar (Esc)");

    private final BigDecimal subtotal;
    private final BigDecimal taxRate; // ej: 0.18
    private Result result;

    public CheckoutDialog(Window parent, BigDecimal subtotal, BigDecimal taxRate) {
        super(parent, "Cobro", ModalityType.APPLICATION_MODAL);
        this.subtotal = nvl(subtotal);
        this.taxRate = nvl(taxRate);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(420, 360);
        setLocationRelativeTo(parent);

        initUI();
        refreshTotals();
        wireEvents();
    }

    public Result getResult() { return result; }

    private void initUI() {
        JPanel content = new JPanel();
        content.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        content.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; c.weightx = 0;

        // Subtotal
        content.add(new JLabel("Subtotal:"), c);
        c.gridx = 1; c.weightx = 1;
        lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);
        content.add(lblSubtotal, c);

        // Impuesto
        c.gridy++; c.gridx = 0; c.weightx = 0;
        content.add(new JLabel("Impuesto (" + taxRate.multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP) + "%):"), c);
        c.gridx = 1; c.weightx = 1;
        lblImpuesto.setHorizontalAlignment(SwingConstants.RIGHT);
        content.add(lblImpuesto, c);

        // Total
        c.gridy++; c.gridx = 0; c.weightx = 0;
        JLabel lblt = new JLabel("TOTAL a pagar:");
        lblt.setFont(lblt.getFont().deriveFont(Font.BOLD));
        content.add(lblt, c);
        c.gridx = 1; c.weightx = 1;
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD));
        content.add(lblTotal, c);

        // Recibido
        c.gridy++; c.gridx = 0; c.weightx = 0;
        content.add(new JLabel("Efectivo recibido:"), c);
        c.gridx = 1; c.weightx = 1;
        txtRecibido.setHorizontalAlignment(SwingConstants.RIGHT);
        content.add(txtRecibido, c);

        // Vuelto
        c.gridy++; c.gridx = 0; c.weightx = 0;
        content.add(new JLabel("Vuelto:"), c);
        c.gridx = 1; c.weightx = 1;
        lblVuelto.setHorizontalAlignment(SwingConstants.RIGHT);
        lblVuelto.setFont(lblVuelto.getFont().deriveFont(Font.BOLD));
        content.add(lblVuelto, c);

        // Botones
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(btnCancelar);
        buttons.add(btnConfirmar);

        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnConfirmar);
    }

    private void wireEvents() {
        // Recalcular vuelto al escribir
        txtRecibido.getDocument().addDocumentListener(new SimpleDocListener(this::refreshChange));

        // Confirmar
        btnConfirmar.addActionListener(e -> confirmar());

        // Cancelar
        btnCancelar.addActionListener(e -> {
            result = null;
            dispose();
        });

        // ESC para cerrar
        getRootPane().registerKeyboardAction(e -> {
            result = null;
            dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // Enter en el campo también intenta cobrar
        txtRecibido.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) confirmar();
            }
        });
    }

    private void confirmar() {
        BigDecimal total = calcTotal();
        BigDecimal recibido = parseMoney(txtRecibido.getText());
        if (recibido.compareTo(total) < 0) {
            JOptionPane.showMessageDialog(this, "El efectivo recibido es menor al total.", "Atención", JOptionPane.WARNING_MESSAGE);
            return;
        }
        BigDecimal vuelto = recibido.subtract(total).max(BigDecimal.ZERO);
        result = new Result(subtotal, calcTax(), total, recibido, vuelto);
        dispose();
    }

    private void refreshTotals() {
        lblSubtotal.setText(formatCurrency(subtotal));
        lblImpuesto.setText(formatCurrency(calcTax()));
        lblTotal.setText(formatCurrency(calcTotal()));
        refreshChange();
    }

    private void refreshChange() {
        BigDecimal total = calcTotal();
        BigDecimal recibido = parseMoney(txtRecibido.getText());
        BigDecimal vuelto = recibido.subtract(total);
        if (vuelto.compareTo(BigDecimal.ZERO) < 0) {
            lblVuelto.setText("—");
        } else {
            lblVuelto.setText(formatCurrency(vuelto));
        }
    }

    private BigDecimal calcTax() {
        return subtotal.multiply(taxRate).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calcTotal() {
        return subtotal.add(calcTax()).setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal parseMoney(String s) {
        if (s == null || s.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            String clean = s.replace(".", "").replace(",", "").replace("$", "").trim();
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private static String formatCurrency(BigDecimal v) {
        if (v == null) return "$0";
        // CLP sin decimales (ajusta si usas otra moneda)
        return "$" + v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    // ---- helper doc listener
    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable r;
        SimpleDocListener(Runnable r) { this.r = r; }
        public void insertUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { r.run(); }
    }
}
