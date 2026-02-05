package server;

import engine.OrderRepository;
import model.Instrument;
import model.Order;
import model.OrderStatus;
import model.OrderType;
import persistence.OrderRegistry;
import utils.PerformanceMonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandler implements Runnable {

    private static final AtomicInteger ID_GEN = new AtomicInteger(1);

    private final Socket socket;
    private final OrderRepository orderRepository;
    private final Map<String, Instrument> instruments;
    private final OrderRegistry orderRegistry;

    public ClientHandler(Socket socket, OrderRepository orderRepository,
                        Map<String, Instrument> instruments,
                        OrderRegistry orderRegistry) {
        this.socket = socket;
        this.orderRepository = orderRepository;
        this.instruments = instruments;
        this.orderRegistry = orderRegistry;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("total_request_processing")) {
                    long startParsing = System.nanoTime();
                    String[] p = line.split(",");
                    Order o = new Order(
                            ID_GEN.getAndIncrement(),
                            Integer.parseInt(p[0]),
                            p[1],
                            OrderType.valueOf(p[2]),
                            Integer.parseInt(p[3]),
                            Double.parseDouble(p[4])
                    );
                    long endParsing = System.nanoTime();
                    PerformanceMonitor.recordTiming("request_parsing", endParsing - startParsing);

                    Instrument inst = instruments.get(o.getInstrument());

                    // Verificare lichiditate
                    long startLiquidity = System.nanoTime();
                    boolean hasLiquidity = inst.tryAllocate(o.getVolume());
                    long endLiquidity = System.nanoTime();
                    PerformanceMonitor.recordTiming("liquidity_check", endLiquidity - startLiquidity);

                    if (!hasLiquidity) {
                        o.setStatus(OrderStatus.REJECTED);
                        o.getFuture().complete(OrderStatus.REJECTED);

                        orderRegistry.logOrder(o);

                        PerformanceMonitor.incrementCounter("orders_rejected_no_liquidity");
                        continue;
                    }

                    long startAdd = System.nanoTime();
                    orderRepository.add(o);
                    long endAdd = System.nanoTime();
                    PerformanceMonitor.recordTiming("add_to_repository", endAdd - startAdd);

                    out.println("PENDING," + o.getId());

                    // Logging în registry
                    long startRegistry = System.nanoTime();
                    orderRegistry.logOrder(o);
                    long endRegistry = System.nanoTime();
                    PerformanceMonitor.recordTiming("order_registry_write", endRegistry - startRegistry);

                    PerformanceMonitor.incrementCounter("orders_accepted");

                    ExecutorService notifyExec = Executors.newSingleThreadExecutor();

                    int orderId = o.getId();
                    o.getFuture().thenAcceptAsync(status -> {
                        out.println("FINAL," + orderId + "," + status);
                        orderRegistry.updateOrderStatus(orderId, status.toString());
                    }, notifyExec);
                } catch (Exception e) {
                    PerformanceMonitor.incrementCounter("request_processing_errors");
                    throw e;
                }
            }
        } catch (IOException ignored) {
            System.out.println("Client disconnected");
        }
    }
}
