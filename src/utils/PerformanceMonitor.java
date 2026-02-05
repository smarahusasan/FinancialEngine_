package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PerformanceMonitor {
    private static final Map<String, List<Long>> timings = new ConcurrentHashMap<>();
    private static final Map<String, Long> counters = new ConcurrentHashMap<>();
    private static long startTime = System.currentTimeMillis();

    public static void recordTiming(String operation, long durationNanos) {
        timings.computeIfAbsent(operation, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(durationNanos);
    }

    public static void incrementCounter(String operation) {
        counters.merge(operation, 1L, Long::sum);
    }

    public static void printStatistics() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PERFORMANCE STATISTICS");
        System.out.println("=".repeat(80));

        long totalRunTime = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("Total runtime: " + totalRunTime + " seconds\n");

        System.out.println("RESPONSE TIMES:");
        System.out.println("-".repeat(80));
        System.out.printf("%-35s %8s %14s %14s %14s %14s %14s%n",
                "Operation", "Count", "Mean", "Median", "Min", "Max", "p95");
        System.out.println("-".repeat(80));

        timings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String operation = entry.getKey();
                    List<Long> times = entry.getValue();

                    if (times.isEmpty()) return;

                    List<Long> sortedTimes = times.stream()
                            .sorted()
                            .collect(Collectors.toList());

                    double mean = sortedTimes.stream()
                            .mapToLong(Long::longValue)
                            .average()
                            .orElse(0.0) / 1_000_000.0;

                    double min = sortedTimes.get(0) / 1_000_000.0;
                    double max = sortedTimes.get(sortedTimes.size() - 1) / 1_000_000.0;
                    double median = sortedTimes.get(sortedTimes.size() / 2) / 1_000_000.0;
                    double p95 = sortedTimes.get((int)(sortedTimes.size() * 0.95)) / 1_000_000.0;

                    System.out.printf("%-35s %8d %14.6f %14.6f %14.6f %14.6f %14.6f%n",
                            operation, times.size(), mean, median, min, max, p95);
                });

        if (!counters.isEmpty()) {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("OPERATION COUNTERS:");
            System.out.println("-".repeat(80));
            System.out.printf("%-45s %15s %15s%n", "Operation", "Total Count", "Per Second");
            System.out.println("-".repeat(80));

            counters.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        double perSecond = totalRunTime > 0 ? entry.getValue() / (double)totalRunTime : 0;
                        System.out.printf("%-45s %15d %15.2f%n",
                                entry.getKey(), entry.getValue(), perSecond);
                    });
        }

        System.out.println("=".repeat(80) + "\n");
    }

    public static void exportToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            writer.println("Performance Report");
            writer.println("Generated: " + LocalDateTime.now().format(formatter));
            writer.println("=".repeat(80));
            writer.println();

            long totalRunTime = (System.currentTimeMillis() - startTime) / 1000;
            writer.println("Total runtime: " + totalRunTime + " seconds");
            writer.println();

            writer.println("RESPONSE TIMES:");
            writer.println("-".repeat(80));
            writer.printf("%-35s %8s %14s %14s %14s %14s %14s%n",
                    "Operation", "Count", "Mean", "Median", "Min", "Max", "p95");
            writer.println("-".repeat(80));

            timings.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        String operation = entry.getKey();
                        List<Long> times = entry.getValue();

                        if (times.isEmpty()) return;

                        List<Long> sortedTimes = times.stream()
                                .sorted()
                                .collect(Collectors.toList());

                        double mean = sortedTimes.stream()
                                .mapToLong(Long::longValue)
                                .average()
                                .orElse(0.0) / 1_000_000.0;

                        double min = sortedTimes.get(0) / 1_000_000.0;
                        double max = sortedTimes.get(sortedTimes.size() - 1) / 1_000_000.0;
                        double median = sortedTimes.get(sortedTimes.size() / 2) / 1_000_000.0;
                        double p95 = sortedTimes.get((int)(sortedTimes.size() * 0.95)) / 1_000_000.0;

                        writer.printf("%-30s %8d %7.2fms %7.2fms %7.2fms %7.2fms %9.2fms%n",
                                operation, times.size(), mean, median, min, max, p95);
                    });

            if (!counters.isEmpty()) {
                writer.println();
                writer.println("=".repeat(80));
                writer.println("OPERATION COUNTERS:");
                writer.println("-".repeat(80));
                writer.printf("%-40s %15s %15s%n", "Operation", "Total Count", "Per Second");
                writer.println("-".repeat(80));

                counters.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> {
                            double perSecond = totalRunTime > 0 ? entry.getValue() / (double)totalRunTime : 0;
                            writer.printf("%-40s %15d %15.2f%n",
                                    entry.getKey(), entry.getValue(), perSecond);
                        });
            }

            writer.println("=".repeat(80));
            System.out.println("Performance report exported to: " + filename);

        } catch (IOException e) {
            System.err.println("Error exporting performance report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void reset() {
        timings.clear();
        counters.clear();
        startTime = System.currentTimeMillis();
    }

    public static class Timer implements AutoCloseable {
        private final String operation;
        private final long startTime;

        public Timer(String operation) {
            this.operation = operation;
            this.startTime = System.nanoTime();
        }

        @Override
        public void close() {
            long duration = System.nanoTime() - startTime;
            recordTiming(operation, duration);
        }
    }

    public static Timer startTimer(String operation) {
        return new Timer(operation);
    }
}