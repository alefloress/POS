package pos.repo;

import pos.model.Sale;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

public class SaleRepo {
    private static final SaleRepo I = new SaleRepo();
    public static SaleRepo get(){ return I; }

    private final List<Sale> sales = new ArrayList<>();

    public void save(Sale s){ sales.add(s); }
    public List<Sale> all(){ return new ArrayList<>(sales); }

    public List<Sale> between(LocalDate from, LocalDate to){
        LocalDateTime a = from.atStartOfDay();
        LocalDateTime b = to.atTime(LocalTime.MAX);
        return sales.stream()
                .filter(s -> !s.getTs().isBefore(a) && !s.getTs().isAfter(b))
                .collect(Collectors.toList());
    }

    public int total(LocalDate from, LocalDate to){
        return between(from,to).stream().mapToInt(Sale::getTotal).sum();
    }

    public Map<String,Integer> totalsByPayment(LocalDate from, LocalDate to){
        Map<String,Integer> m = new HashMap<>();
        for (Sale s : between(from,to)) m.merge(s.getPaymentMethod(), s.getTotal(), Integer::sum);
        return m;
    }

    public List<Map.Entry<String,Integer>> topProducts(LocalDate from, LocalDate to, int topN){
        Map<String,Integer> qty = new HashMap<>();
        for (Sale s : between(from,to)){
            s.getItems().forEach(it -> qty.merge(it.getProduct().getCode(), it.getQuantity(), Integer::sum));
        }
        return qty.entrySet().stream()
                .sorted((a,b)->Integer.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .collect(Collectors.toList());
    }
}