package server;

import engine.OrderRepository;
import model.Instrument;
import persistence.OrderRegistry;
import utils.ConfigManager;

import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TradingServer {
    private final ConfigManager config;
    private final ExecutorService pool;
    private final OrderRepository orderRepository;
    private final Map<String, Instrument> instruments;
    private final OrderRegistry orderRegistry;

    public TradingServer(OrderRepository orderRepository, Map<String, Instrument> instruments,
                         OrderRegistry orderRegistry) {
        this.orderRepository = orderRepository;
        this.instruments = instruments;
        this.orderRegistry = orderRegistry;
        this.config=ConfigManager.getInstance();
        this.pool= Executors.newFixedThreadPool(config.getThreadNo());
    }

    public void start() throws Exception {
        ServerSocket server = new ServerSocket(5000);
        System.out.println("Server started on port 5000");
        while (true) {
            pool.submit(new ClientHandler(server.accept(), orderRepository, instruments, orderRegistry));
        }
    }
}
