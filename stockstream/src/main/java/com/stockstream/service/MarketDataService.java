package com.stockstream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MarketDataService {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CopyOnWriteArrayList<MarketTicker> tickerData = new CopyOnWriteArrayList<>();
    private String lastUpdated = "Not yet fetched";

    // ─── All Symbols ─────────────────────────────
    // ^ symbol encoded as %5E in URL to fix "Illegal character" error
    private final String[][] SYMBOLS = {
            { "^NSEI", "NIFTY 50", "INDEX" },
            { "^NSEBANK", "NIFTY BANK", "INDEX" },
            { "^CNXIT", "NIFTY IT", "INDEX" },
            { "^BSESN", "SENSEX", "INDEX" },
            { "^NSMIDCP", "NIFTY MIDCAP", "INDEX" },
            { "^CNXAUTO", "NIFTY AUTO", "INDEX" },
            { "^CNXPHARMA", "NIFTY PHARMA", "INDEX" },
            { "^CNXFMCG", "NIFTY FMCG", "INDEX" },
            { "GC=F", "GOLD", "COMMODITY" },
            { "SI=F", "SILVER", "COMMODITY" },
            { "CL=F", "CRUDE OIL", "COMMODITY" },
            { "HG=F", "COPPER", "COMMODITY" },
            { "INR=X", "USD/INR", "CURRENCY" },
            { "EURINR=X", "EUR/INR", "CURRENCY" },
            { "BTC-USD", "BITCOIN", "CRYPTO" },
            { "ETH-USD", "ETHEREUM", "CRYPTO" },
    };

    // ─── Run on Startup in Background Thread ─────
    // Using new Thread so app starts without waiting
    @PostConstruct
    public void init() {
        System.out.println("Initializing market data on startup...");
        new Thread(this::fetchMarketData).start();
    }

    // ─── Auto Refresh Every 5 Minutes ────────────
    @Scheduled(fixedRate = 300000)
    public void fetchMarketData() {
        System.out.println("Fetching live market data...");
        List<MarketTicker> newData = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (String[] sym : SYMBOLS) {
            try {
                String symbol = sym[0];
                String name = sym[1];
                String type = sym[2];

                // FIX 1: Encode ^ as %5E — fixes "Illegal character in path" error
                String encodedSymbol = symbol.replace("^", "%5E");

                String url = "https://query1.finance.yahoo.com/v8/finance/chart/"
                        + encodedSymbol
                        + "?interval=1d&range=1y&includePrePost=false";

                // FIX 2: Remove "Connection" header — Java HttpClient restricts it
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "application/json, text/plain, */*")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .header("Origin", "https://finance.yahoo.com")
                        .header("Referer", "https://finance.yahoo.com/")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Status [" + name + "]: " + response.statusCode());

                if (response.statusCode() != 200) {
                    System.out.println("✗ [" + name + "] Non-200: " + response.statusCode());
                    failCount++;
                    continue;
                }

                JsonNode root = objectMapper.readTree(response.body());
                JsonNode result = root.path("chart").path("result").get(0);

                if (result == null || result.isNull()) {
                    System.out.println("✗ [" + name + "] No data in response");
                    failCount++;
                    continue;
                }

                JsonNode meta = result.path("meta");

                double price = meta.path("regularMarketPrice").asDouble();
                double prevClose = meta.path("chartPreviousClose").asDouble();
                double open = meta.path("regularMarketOpen").asDouble(price);
                double high = meta.path("regularMarketDayHigh").asDouble(price);
                double low = meta.path("regularMarketDayLow").asDouble(price);
                double volume = meta.path("regularMarketVolume").asDouble(0);
                double week52High = meta.path("fiftyTwoWeekHigh").asDouble(price);
                double week52Low = meta.path("fiftyTwoWeekLow").asDouble(price);
                double marketCap = meta.path("marketCap").asDouble(0);
                String currency = meta.path("currency").asText("INR");

                if (price <= 0) {
                    System.out.println("✗ [" + name + "] Zero price — skipping");
                    failCount++;
                    continue;
                }

                double change = price - prevClose;
                double changePct = prevClose > 0 ? (change / prevClose) * 100 : 0;

                MarketTicker ticker = new MarketTicker();
                ticker.setSymbol(name);
                ticker.setYahooSymbol(symbol);
                ticker.setType(type);
                ticker.setValue(formatPrice(name, price, currency));
                ticker.setChange(formatChange(changePct));
                ticker.setPositive(changePct >= 0);
                ticker.setRawPrice(Math.round(price * 100.0) / 100.0);
                ticker.setRawChange(Math.round(changePct * 100.0) / 100.0);
                ticker.setOpen(formatPrice(name, open, currency));
                ticker.setHigh(formatPrice(name, high, currency));
                ticker.setLow(formatPrice(name, low, currency));
                ticker.setVolume(formatVolume(volume));
                ticker.setWeek52High(formatPrice(name, week52High, currency));
                ticker.setWeek52Low(formatPrice(name, week52Low, currency));
                ticker.setMarketCap(formatMarketCap(marketCap));
                ticker.setPrevClose(formatPrice(name, prevClose, currency));

                newData.add(ticker);
                successCount++;
                System.out.println("✓ [" + name + "] " + ticker.getValue() + " " + ticker.getChange());

                // Small delay between requests to avoid rate limiting
                Thread.sleep(500);

            } catch (Exception e) {
                System.out.println("✗ [" + sym[1] + "] Error: " + e.getMessage());
                failCount++;
            }
        }

        if (!newData.isEmpty()) {
            tickerData.clear();
            tickerData.addAll(newData);
            lastUpdated = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));
            System.out.println("✓ Market updated at " + lastUpdated
                    + " — Success: " + successCount + " Failed: " + failCount);
        } else {
            System.out.println("WARNING: No live data fetched — Success: "
                    + successCount + " Failed: " + failCount);
        }
    }

    // ─── Format Price ─────────────────────────────
    private String formatPrice(String name, double price, String currency) {
        if (name.contains("NIFTY") || name.contains("SENSEX")) {
            return String.format("%,.2f", price);
        }
        if (name.equals("GOLD") || name.equals("SILVER") || name.equals("COPPER")) {
            return "₹" + String.format("%,.0f", price * 83.5);
        }
        if (name.equals("CRUDE OIL")) {
            return "$" + String.format("%.2f", price);
        }
        if (name.equals("BITCOIN") || name.equals("ETHEREUM")) {
            return "$" + String.format("%,.0f", price);
        }
        if (name.contains("INR")) {
            return String.format("%.4f", price);
        }
        return String.format("%.2f", price);
    }

    // ─── Format Change % ──────────────────────────
    private String formatChange(double pct) {
        return (pct >= 0 ? "+" : "") + String.format("%.2f", pct) + "%";
    }

    // ─── Format Volume ────────────────────────────
    private String formatVolume(double vol) {
        if (vol <= 0)
            return "N/A";
        if (vol >= 1_000_000_000)
            return String.format("%.2fB", vol / 1_000_000_000);
        if (vol >= 1_000_000)
            return String.format("%.2fM", vol / 1_000_000);
        if (vol >= 1_000)
            return String.format("%.2fK", vol / 1_000);
        return String.format("%.0f", vol);
    }

    // ─── Format Market Cap ────────────────────────
    private String formatMarketCap(double cap) {
        if (cap <= 0)
            return "N/A";
        double inr = cap * 83.5;
        if (inr >= 1_000_000_000_000L)
            return String.format("₹%.2fT", inr / 1_000_000_000_000L);
        if (inr >= 1_000_000_000)
            return String.format("₹%.2fB", inr / 1_000_000_000);
        return String.format("₹%.2fM", inr / 1_000_000);
    }

    // ─── Public Methods ───────────────────────────
    public List<MarketTicker> getTickerData() {
        return new ArrayList<>(tickerData);
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    // ─── Market Ticker Model ─────────────────────
    public static class MarketTicker {
        private String symbol, yahooSymbol, type, value, change;
        private String open, high, low, volume;
        private String week52High, week52Low, marketCap, prevClose;
        private boolean positive;
        private double rawPrice, rawChange;

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String v) {
            this.symbol = v;
        }

        public String getYahooSymbol() {
            return yahooSymbol;
        }

        public void setYahooSymbol(String v) {
            this.yahooSymbol = v;
        }

        public String getType() {
            return type;
        }

        public void setType(String v) {
            this.type = v;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String v) {
            this.value = v;
        }

        public String getChange() {
            return change;
        }

        public void setChange(String v) {
            this.change = v;
        }

        public boolean isPositive() {
            return positive;
        }

        public void setPositive(boolean v) {
            this.positive = v;
        }

        public double getRawPrice() {
            return rawPrice;
        }

        public void setRawPrice(double v) {
            this.rawPrice = v;
        }

        public double getRawChange() {
            return rawChange;
        }

        public void setRawChange(double v) {
            this.rawChange = v;
        }

        public String getOpen() {
            return open;
        }

        public void setOpen(String v) {
            this.open = v;
        }

        public String getHigh() {
            return high;
        }

        public void setHigh(String v) {
            this.high = v;
        }

        public String getLow() {
            return low;
        }

        public void setLow(String v) {
            this.low = v;
        }

        public String getVolume() {
            return volume;
        }

        public void setVolume(String v) {
            this.volume = v;
        }

        public String getWeek52High() {
            return week52High;
        }

        public void setWeek52High(String v) {
            this.week52High = v;
        }

        public String getWeek52Low() {
            return week52Low;
        }

        public void setWeek52Low(String v) {
            this.week52Low = v;
        }

        public String getMarketCap() {
            return marketCap;
        }

        public void setMarketCap(String v) {
            this.marketCap = v;
        }

        public String getPrevClose() {
            return prevClose;
        }

        public void setPrevClose(String v) {
            this.prevClose = v;
        }
    }
}