package client;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

public class TradingBot implements Runnable {

    private final int id;
    private final Random rnd = new Random();
    private final String[] instruments = {"AAPL", "BTC", "ETH"};

    public TradingBot(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        try (Socket s = new Socket("localhost", 5000);
             PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {

            while (!Thread.currentThread().isInterrupted()) {
                String inst = instruments[rnd.nextInt(instruments.length)];
                String type = rnd.nextBoolean() ? "BUY" : "SELL";
                int vol = rnd.nextInt(10) + 1;
                double price = 100 + rnd.nextInt(50);

                out.println(id + "," + inst + "," + type + "," + vol + "," + price);
                Thread.sleep(1000);
            }
        } catch (Exception ignored) {}
    }
}
