package server;

import engine.OrderRepository;
import model.Instrument;
import persistence.OrderRegistry;

import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TradingServer {

    private final ExecutorService pool = Executors.newFixedThreadPool(8);
    private final OrderRepository orderRepository;
    private final Map<String, Instrument> instruments;
    private final OrderRegistry orderRegistry;

    public TradingServer(OrderRepository orderRepository, Map<String, Instrument> instruments,
                         OrderRegistry orderRegistry) {
        this.orderRepository = orderRepository;
        this.instruments = instruments;
        this.orderRegistry = orderRegistry;
    }

    public void start() throws Exception {
        ServerSocket server = new ServerSocket(5000);
        System.out.println("Server started on port 5000");
        while (true) {
            pool.submit(new ClientHandler(server.accept(), orderRepository, instruments, orderRegistry));
        }
    }
}
