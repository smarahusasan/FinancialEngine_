package model;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public class Order {
    private final int id;
    private final int clientId;
    private final String instrument;
    private final OrderType type;
    private final int volume;
    private final double limitPrice;
    private final Instant timestamp=Instant.now();
    private final CompletableFuture<OrderStatus> future = new CompletableFuture<>();
    private OrderStatus status=OrderStatus.PENDING;

    public Order(int id, int clientId, String instrument, OrderType type,
                 int volume, double limitPrice) {
        this.id = id;
        this.clientId = clientId;
        this.instrument = instrument;
        this.type = type;
        this.volume = volume;
        this.limitPrice = limitPrice;
    }

    public int getId() { return id; }
    public int getClientId() { return clientId; }
    public String getInstrument() { return instrument; }
    public OrderType getType() { return type; }
    public int getVolume() { return volume; }
    public double getLimitPrice() { return limitPrice; }
    public Instant getTimestamp() { return timestamp; }
    public CompletableFuture<OrderStatus> getFuture() { return future; }

    public synchronized OrderStatus getStatus() { return status; }
    public synchronized void setStatus(OrderStatus s) { status = s; }
}