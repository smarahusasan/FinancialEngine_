package engine;

import model.Instrument;
import model.Order;
import model.OrderStatus;
import model.OrderType;
import persistence.CancellationRegistry;
import persistence.ExecutionRegistry;
import utils.ConfigManager;
import utils.PerformanceMonitor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Random;

public class MatchingEngine implements Runnable {

    private final OrderRepository orderRepository;
    private final Map<String, Instrument> instruments;
    private final ExecutionRegistry executionRegistry;
    private final CancellationRegistry cancellationRegistry;
    private final Random rnd = new Random();
    private final ConfigManager configManager;

    public MatchingEngine(OrderRepository orderRepository,
                          Map<String, Instrument> instruments,
                          ExecutionRegistry executionRegistry,
                          CancellationRegistry cancellationRegistry) {
        this.orderRepository = orderRepository;
        this.instruments = instruments;
        this.executionRegistry = executionRegistry;
        this.cancellationRegistry = cancellationRegistry;
        this.configManager=ConfigManager.getInstance();
    }

    @Override
    public void run() {
        try(PerformanceMonitor.Timer timer=PerformanceMonitor.startTimer("matching_engine_cycle")){
            long startPriceUpdate = System.nanoTime();
            instruments.values().forEach(i -> i.updatePrice(configManager.getMu(), configManager.getSigma(), configManager.getDt(), rnd));
            long endPriceUpdate = System.nanoTime();
            PerformanceMonitor.recordTiming("price_update", endPriceUpdate - startPriceUpdate);

            Instant now = Instant.now();

            for (Order o : orderRepository.all()) {
                if (o.getStatus() != OrderStatus.PENDING) continue;

                Instrument inst = instruments.get(o.getInstrument());
                long age = Duration.between(o.getTimestamp(), now).getSeconds();

                if (age > configManager.getOrderExpTimeSeconds()) cancel(o, inst);
                else if (canExecute(o, inst)) execute(o, inst);
            }

            PerformanceMonitor.incrementCounter("matching_cycles_completed");
        } catch (Exception e) {
            PerformanceMonitor.incrementCounter("matching_engine_errors");
            e.printStackTrace();
        }
    }

    private boolean canExecute(Order o, Instrument i) {
        long startCheck = System.nanoTime();
        double p = i.getPrice();
        boolean result = (o.getType() == OrderType.BUY && p <= o.getLimitPrice()) ||
                (o.getType() == OrderType.SELL && p >= o.getLimitPrice());
        long endCheck = System.nanoTime();
        PerformanceMonitor.recordTiming("matching_check", endCheck - startCheck);
        return result;
    }

    private void execute(Order o, Instrument i) {
        long startExecution = System.nanoTime();
        o.setStatus(OrderStatus.EXECUTED);
        i.release(o.getVolume());

        long startCommission = System.nanoTime();
        double value = o.getVolume() * i.getPrice();
        double commission = value * configManager.getCommission() / 100.0;
        i.addProfit(commission);
        long endCommission = System.nanoTime();
        PerformanceMonitor.recordTiming("commission_calculation", endCommission - startCommission);

        o.getFuture().complete(OrderStatus.EXECUTED);

        long startRegistry = System.nanoTime();
        executionRegistry.logExecution(o.getId(), o.getVolume(), value, commission);
        long endRegistry = System.nanoTime();
        PerformanceMonitor.recordTiming("execution_registry_write", endRegistry - startRegistry);

        long endExecution = System.nanoTime();
        PerformanceMonitor.recordTiming("order_execution", endExecution - startExecution);
        PerformanceMonitor.incrementCounter("orders_executed");
    }

    private void cancel(Order o, Instrument i) {
        long startCancel = System.nanoTime();

        o.setStatus(OrderStatus.CANCELLED);
        i.release(o.getVolume());
        o.getFuture().complete(OrderStatus.CANCELLED);

        cancellationRegistry.logCancellation(o.getId(), "Order expired after " + configManager.getOrderExpTimeSeconds() + " seconds");

        long endCancel = System.nanoTime();
        PerformanceMonitor.recordTiming("order_cancellation", endCancel - startCancel);
        PerformanceMonitor.incrementCounter("orders_cancelled");
    }
}
