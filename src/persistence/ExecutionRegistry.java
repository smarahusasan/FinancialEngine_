package persistence;

import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionRegistry {
    private static final String FILE = "execution_registry.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final AtomicInteger EXECUTION_ID = new AtomicInteger(1);

    public ExecutionRegistry() {
        File file = new File(FILE);
        if (!file.exists()) {
            try (FileWriter fw = new FileWriter(FILE, false)) {
                fw.write("# Execution Registry - Format: id_execuție,id_ordin,data,volum_executat,preț_final,comision\n");
            } catch (Exception e) {
                System.err.println("Error initializing execution registry: " + e.getMessage());
            }
        }
    }

    public synchronized void logExecution(int orderId, int volumeExecuted, 
                                         double totalValue, double commission) {
        try (FileWriter fw = new FileWriter(FILE, true)) {
            int executionId = EXECUTION_ID.getAndIncrement();
            String line = String.format("%d,%d,%s,%d,%.2f,%.2f%n",
                executionId,
                orderId,
                FORMATTER.format(Instant.now()),
                volumeExecuted,
                totalValue,
                commission
            );
            fw.write(line);
        } catch (Exception e) {
            System.err.println("Error logging execution: " + e.getMessage());
        }
    }
}
