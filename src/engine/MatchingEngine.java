package engine;

import model.Instrument;
import model.Order;
import model.OrderStatus;
import model.OrderType;
import persistence.CancellationRegistry;
import persistence.ExecutionRegistry;
import utils.ConfigManager;

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
    private ConfigManager configManager;

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
        instruments.values().forEach(i -> i.updatePrice(configManager.getMu(), configManager.getSigma(), configManager.getDt(), rnd));
        Instant now = Instant.now();

        for (Order o : orderRepository.all()) {
            if (o.getStatus() != OrderStatus.PENDING) continue;

            Instrument inst = instruments.get(o.getInstrument());
            long age = Duration.between(o.getTimestamp(), now).getSeconds();

            if (age > configManager.getOrderExpTimeSeconds()) cancel(o, inst);
            else if (canExecute(o, inst)) execute(o, inst);
        }
    }

    private boolean canExecute(Order o, Instrument i) {
        double p = i.getPrice();
        return (o.getType() == OrderType.BUY && p <= o.getLimitPrice()) ||
                (o.getType() == OrderType.SELL && p >= o.getLimitPrice());
    }

    private void execute(Order o, Instrument i) {
        o.setStatus(OrderStatus.EXECUTED);
        i.release(o.getVolume());

        double value = o.getVolume() * i.getPrice();
        double commission = value * configManager.getCommission() / 100.0;
        i.addProfit(commission);

        o.getFuture().complete(OrderStatus.EXECUTED);

        executionRegistry.logExecution(o.getId(), o.getVolume(), value, commission);
    }

    private void cancel(Order o, Instrument i) {
        o.setStatus(OrderStatus.CANCELLED);
        i.release(o.getVolume());
        o.getFuture().complete(OrderStatus.CANCELLED);

        cancellationRegistry.logCancellation(o.getId(), "Order expired after " + configManager.getOrderExpTimeSeconds() + " seconds");
    }
}
