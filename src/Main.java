import client.TradingBot;
import engine.AuditService;
import engine.MatchingEngine;
import engine.OrderRepository;
import model.Instrument;
import persistence.CancellationRegistry;
import persistence.ExecutionRegistry;
import persistence.OrderRegistry;
import server.TradingServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws Exception {

        Map<String, Instrument> instruments = new ConcurrentHashMap<>();
        instruments.put("AAPL", new Instrument("AAPL", 200, 100));
        instruments.put("BTC", new Instrument("BTC", 150, 30000));
        instruments.put("ETH", new Instrument("ETH", 150, 2000));

        OrderRepository orderRepository = new OrderRepository();

        OrderRegistry orderRegistry = new OrderRegistry();
        ExecutionRegistry executionRegistry = new ExecutionRegistry();
        CancellationRegistry cancellationRegistry = new CancellationRegistry();

        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(6);

        scheduler.scheduleAtFixedRate(
                new MatchingEngine(orderRepository, instruments,
                    executionRegistry, cancellationRegistry),
                0, 2, TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(
                new AuditService(instruments, orderRepository),
                0, 2, TimeUnit.SECONDS);

        new Thread(() -> {
            try {
                new TradingServer(orderRepository, instruments, orderRegistry).start();
            } catch (Exception ignored) {}
        }).start();

        for (int i = 1; i <= 5; i++) {
            new Thread(new TradingBot(i)).start();
        }

        scheduler.schedule(() -> {
            System.out.println("SERVER SHUTDOWN");
            System.exit(0);
        }, 180, TimeUnit.SECONDS);
    }
}