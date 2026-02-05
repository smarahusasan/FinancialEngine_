import client.TradingBot;
import engine.AuditService;
import engine.MatchingEngine;
import engine.OrderRepository;
import model.Instrument;
import persistence.CancellationRegistry;
import persistence.ExecutionRegistry;
import persistence.OrderRegistry;
import server.TradingServer;
import utils.ConfigManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {

        ConfigManager configManager=ConfigManager.getInstance();

        Map<String, Instrument> instruments = new ConcurrentHashMap<>();
        instruments.put("AAPL", new Instrument("AAPL", 200, 100));
        instruments.put("BTC", new Instrument("BTC", 150, 30000));
        instruments.put("ETH", new Instrument("ETH", 150, 2000));

        OrderRepository orderRepository = new OrderRepository();

        OrderRegistry orderRegistry = new OrderRegistry();
        ExecutionRegistry executionRegistry = new ExecutionRegistry();
        CancellationRegistry cancellationRegistry = new CancellationRegistry();

        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(configManager.getThreadNo());

        scheduler.scheduleAtFixedRate(
                new MatchingEngine(orderRepository, instruments,
                    executionRegistry, cancellationRegistry),
                0, configManager.getAuditIntervalSeconds(), TimeUnit.SECONDS);

        scheduler.scheduleAtFixedRate(
                new AuditService(instruments, orderRepository),
                0, configManager.getAuditIntervalSeconds(), TimeUnit.SECONDS);

        new Thread(() -> {
            try {
                new TradingServer(orderRepository, instruments, orderRegistry).start();
            } catch (Exception ignored) {}
        }).start();

        for (int i = 1; i <= configManager.getBotNo(); i++) {
            new Thread(new TradingBot(i)).start();
        }

        scheduler.schedule(() -> {
            System.out.println("SERVER SHUTDOWN");
            System.exit(0);
        }, configManager.getServerRunningTimeSeconds(), TimeUnit.SECONDS);
    }
}