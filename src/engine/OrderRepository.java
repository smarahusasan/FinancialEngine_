package engine;

import model.Order;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class OrderRepository {
    private final List<Order> orders = new CopyOnWriteArrayList<>();

    public void add(Order o) {
        orders.add(o);
    }

    public List<Order> all() {
        return orders;
    }
}
