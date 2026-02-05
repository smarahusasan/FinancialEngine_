package engine;

import model.Instrument;
import model.Order;
import model.OrderStatus;
import utils.PerformanceMonitor;

import java.io.FileWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class AuditService implements Runnable {
    private static final String FILE = "audit.log";
    private final Map<String, Instrument> instruments;
    private final OrderRepository orderRepository;

    public AuditService(Map<String, Instrument> instruments, OrderRepository orderRepository) {
        this.instruments = instruments;
        this.orderRepository = orderRepository;
    }

    public synchronized void log(String msg) {
        try (FileWriter fw = new FileWriter(FILE, true)) {
            fw.write(msg + "\n");
        } catch (Exception ignored) {}
    }

    @Override
    public void run() {
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("audit_cycle")) {
            log("\nAUDIT " + Instant.now());

            instruments.values().forEach(i ->
                    log(i.getName() +
                            " avail=" + i.available() +
                            " profit=" + i.getProfit() +
                            " price=" + String.format("%.2f", i.getPrice()))
            );

            List<Order> pendingOrders = orderRepository.all().stream()
                    .filter(o -> o.getStatus() == OrderStatus.PENDING)
                    .toList();

            if (!pendingOrders.isEmpty()) {
                log("Pending orders: " + pendingOrders.size());
                pendingOrders.forEach(o ->
                        log("  Order " + o.getId() +
                                " Client=" + o.getClientId() +
                                " " + o.getInstrument() +
                                " " + o.getType() +
                                " vol=" + o.getVolume() +
                                " price=" + String.format("%.2f", o.getLimitPrice()))
                );
            } else {
                log("No pending orders");
            }

            log("");

            PerformanceMonitor.incrementCounter("audit_cycles_completed");
        } catch (Exception e) {
            PerformanceMonitor.incrementCounter("audit_errors");
            e.printStackTrace();
        }
    }
}
