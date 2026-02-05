package utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    private static ConfigManager instance;
    private Properties properties;

    private int threadNo;
    private int orderExpTimeSeconds;
    private int auditIntervalSeconds;
    private int serverRunningTimeSeconds;
    private int botNo;

    private double mu;
    private double sigma;
    private double dt;
    private double commission;

    private ConfigManager() {
        loadConfig();
    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    private void loadConfig() {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);

            threadNo = Integer.parseInt(properties.getProperty("threadNo", "4"));
            orderExpTimeSeconds = Integer.parseInt(properties.getProperty("orderExpTimeSeconds", "60"));
            auditIntervalSeconds = Integer.parseInt(properties.getProperty("auditIntervalSeconds", "10"));
            serverRunningTimeSeconds = Integer.parseInt(properties.getProperty("serverRunningTimeSeconds", "300"));
            botNo = Integer.parseInt(properties.getProperty("botNo", "5"));

            mu = Double.parseDouble(properties.getProperty("mu", "0.05"));
            sigma = Double.parseDouble(properties.getProperty("sigma", "1.0"));
            dt = Double.parseDouble(properties.getProperty("dt", "1.0"));
            commission = Double.parseDouble(properties.getProperty("commission", "0.5"));

            System.out.println("Configuration loaded successfully:");
            System.out.println("Number of threads: " + threadNo);
            System.out.println("Order expires after seconds: " + orderExpTimeSeconds);
            System.out.println("Audit interval seconds: " + auditIntervalSeconds);
            System.out.println("Server running time in seconds: " + serverRunningTimeSeconds);
            System.out.println("Number of bots: " + botNo);
            System.out.println("MU: " + mu);
            System.out.println("SIGMA: " + sigma);
            System.out.println("DT: " + dt);
            System.out.println("COMMISSION: " + commission);

        } catch (IOException e) {
            System.err.println("Error loading config file, using default values");
            e.printStackTrace();
            threadNo = 6;
            orderExpTimeSeconds = 30;
            auditIntervalSeconds = 2;
            serverRunningTimeSeconds = 180;
            botNo = 5;
            mu = 0.05;
            sigma = 1.0;
            dt = 1.0;
            commission = 0.5;
        } catch (NumberFormatException e) {
            System.err.println("Error parsing numbers from config file");
            e.printStackTrace();
        }
    }

    public int getThreadNo() {
        return threadNo;
    }

    public int getOrderExpTimeSeconds() {
        return orderExpTimeSeconds;
    }

    public int getAuditIntervalSeconds() {
        return auditIntervalSeconds;
    }

    public int getServerRunningTimeSeconds() {
        return serverRunningTimeSeconds;
    }

    public int getBotNo() {
        return botNo;
    }

    public double getMu() {
        return mu;
    }

    public double getSigma() {
        return sigma;
    }

    public double getDt() {
        return dt;
    }

    public double getCommission() {
        return commission;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}