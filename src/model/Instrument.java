package model;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Instrument {
    private final String name;
    private final int maxLiquidity;
    private final AtomicInteger used = new AtomicInteger(0);
    private double price;
    private double profit = 0.0;

    public Instrument(String name, int maxLiquidity, double price) {
        this.name = name;
        this.maxLiquidity = maxLiquidity;
        this.price = price;
    }

    public boolean tryAllocate(int volume) {
        while (true) {
            int current = used.get();
            //daca se depaseste lichiditatea maxima
            if (current + volume > maxLiquidity) return false;
            // daca valoarea used!=current inseamna ca alt client a modificat valoare asa ca va da fail si
            // se va face retry prin while
            if (used.compareAndSet(current, current + volume)) return true;
        }
    }

    public void release(int volume) {
        used.addAndGet(-volume);
    }

    public synchronized void updatePrice(double mu, double sigma, double dt, Random rnd) {
        double epsilon = rnd.nextGaussian();
        price += mu * dt + sigma * Math.sqrt(dt) * epsilon;
    }

    public synchronized double getPrice() {
        return price;
    }

    public int available() {
        return maxLiquidity - used.get();
    }

    public String getName() { return name; }

    public synchronized void addProfit(double p) {
        profit += p;
    }

    public synchronized double getProfit() {
        return profit;
    }
}
