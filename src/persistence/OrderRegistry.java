package persistence;

import model.Order;

import java.io.File;
import java.io.FileWriter;
import java.time.format.DateTimeFormatter;

public class OrderRegistry {
    private static final String FILE = "order_registry.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    public OrderRegistry() {
        File file = new File(FILE);
        if (!file.exists()) {
            try (FileWriter fw = new FileWriter(FILE, false)) {
                fw.write("# Order Registry - Format: id_ordin,id_client,instrument,tip_ordin,volum_solicitat,preț,status,oră_plasare\n");
            } catch (Exception e) {
                System.err.println("Error initializing order registry: " + e.getMessage());
            }
        }
    }

    public synchronized void logOrder(Order order) {
        try (FileWriter fw = new FileWriter(FILE, true)) {
            String line = String.format("%d,%d,%s,%s,%d,%.2f,%s,%s%n",
                order.getId(),
                order.getClientId(),
                order.getInstrument(),
                order.getType(),
                order.getVolume(),
                order.getLimitPrice(),
                order.getStatus(),
                FORMATTER.format(order.getTimestamp())
            );
            fw.write(line);
        } catch (Exception e) {
            System.err.println("Error logging order: " + e.getMessage());
        }
    }

    public synchronized void updateOrderStatus(int orderId, String status) {
        try (FileWriter fw = new FileWriter(FILE, true)) {
            String line = String.format("UPDATE,%d,%s%n", orderId, status);
            fw.write(line);
        } catch (Exception e) {
            System.err.println("Error updating order status: " + e.getMessage());
        }
    }
}
