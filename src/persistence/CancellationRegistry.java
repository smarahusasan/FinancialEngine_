package persistence;

import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class CancellationRegistry {
    private static final String FILE = "cancellation_registry.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final AtomicInteger CANCELLATION_ID = new AtomicInteger(1);

    public CancellationRegistry() {
        File file = new File(FILE);
        if (!file.exists()) {
            try (FileWriter fw = new FileWriter(FILE, false)) {
                fw.write("# Cancellation Registry - Format: id_anulare,id_ordin,data,motiv_anulare\n");
            } catch (Exception e) {
                System.err.println("Error initializing cancellation registry: " + e.getMessage());
            }
        }
    }

    public synchronized void logCancellation(int orderId, String reason) {
        try (FileWriter fw = new FileWriter(FILE, true)) {
            int cancellationId = CANCELLATION_ID.getAndIncrement();
            String line = String.format("%d,%d,%s,%s%n",
                cancellationId,
                orderId,
                FORMATTER.format(Instant.now()),
                reason
            );
            fw.write(line);
        } catch (Exception e) {
            System.err.println("Error logging cancellation: " + e.getMessage());
        }
    }
}
