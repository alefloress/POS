package pos.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sale {
    private final String id;             // folio
    private final String docType;        // "Boleta" | "Factura"
    private final LocalDateTime ts;
    private final List<SaleItem> items = new ArrayList<>();

    private final String paymentMethod;  // "Efectivo" | "Tarjeta" | "Transferencia" | "Mixto"
    private final int payCash, payCard, payTransfer;

    private final int neto, iva, total;
    private final String customerId;     // opcional (puede ser null)

    public Sale(String id, String docType, LocalDateTime ts, List<SaleItem> items,
                String paymentMethod, int payCash, int payCard, int payTransfer,
                int neto, int iva, int total, String customerId) {
        this.id=id; this.docType=docType; this.ts=ts;
        this.items.addAll(items);
        this.paymentMethod=paymentMethod;
        this.payCash=payCash; this.payCard=payCard; this.payTransfer=payTransfer;
        this.neto=neto; this.iva=iva; this.total=total;
        this.customerId=customerId;
    }

    public String getId(){return id;} public String getDocType(){return docType;}
    public LocalDateTime getTs(){return ts;} public List<SaleItem> getItems(){return new ArrayList<>(items);}
    public String getPaymentMethod(){return paymentMethod;}
    public int getPayCash(){return payCash;} public int getPayCard(){return payCard;} public int getPayTransfer(){return payTransfer;}
    public int getNeto(){return neto;} public int getIva(){return iva;} public int getTotal(){return total;}
    public String getCustomerId(){return customerId;}
}
