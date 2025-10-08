package pos.util;

/**
 * Utilidad para calcular IGV (Impuesto General a las Ventas).
 * Por defecto usa 18%, pero puedes cambiarlo.
 */
public class Tax {

    public static final double IGV_RATE = 0.18; // 18%

    /** Calcula el IGV a partir de un subtotal. */
    public static double calculate(double subtotal) {
        return subtotal * IGV_RATE;
    }

    /** Calcula el total (subtotal + IGV). */
    public static double totalWithTax(double subtotal) {
        return subtotal + calculate(subtotal);
    }
}