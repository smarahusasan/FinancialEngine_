package server;

import engine.OrderRepository;
import model.Instrument;
import model.Order;
import model.OrderStatus;
import model.OrderType;
import persistence.OrderRegistry;

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
                String[] p = line.split(",");
                Order o = new Order(
                        ID_GEN.getAndIncrement(),
                        Integer.parseInt(p[0]),
                        p[1],
                        OrderType.valueOf(p[2]),
                        Integer.parseInt(p[3]),
                        Double.parseDouble(p[4])
                );

                Instrument inst = instruments.get(o.getInstrument());
                if (!inst.tryAllocate(o.getVolume())) {
                    o.setStatus(OrderStatus.REJECTED);
                    o.getFuture().complete(OrderStatus.REJECTED);

                    orderRegistry.logOrder(o);
                    continue;
                }

                orderRepository.add(o);
                out.println("PENDING," + o.getId());
                orderRegistry.logOrder(o);

                ExecutorService notifyExec = Executors.newSingleThreadExecutor();

                int orderId = o.getId();
                o.getFuture().thenAcceptAsync(status -> {
                    out.println("FINAL," + orderId + "," + status);
                    orderRegistry.updateOrderStatus(orderId, status.toString());
                }, notifyExec);
            }
        } catch (IOException ignored) {
            System.out.println("Client disconnected");
        }
    }
}
