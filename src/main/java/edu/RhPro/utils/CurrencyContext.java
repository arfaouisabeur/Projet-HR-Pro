package edu.RhPro.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Currency;
import java.util.Locale;

public final class CurrencyContext {

    // ✅ Base currency stored in DB
    public static final String BASE = "TND";

    private static String displayCurrency = BASE;           // CAD, EUR, ...
    private static BigDecimal rateFromTnd = BigDecimal.ONE; // TND -> displayCurrency
    private static boolean loaded = false;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private CurrencyContext() {}

    /** Call this if you changed VPN / network and want to reload without restart */
    public static synchronized void forceReload() {
        loaded = false;
        displayCurrency = BASE;
        rateFromTnd = BigDecimal.ONE;
        ensureLoaded();
    }

    /** Loads display currency + rate once (IP -> currency + rates) */
    public static synchronized void ensureLoaded() {
        if (loaded) return;

        try {
            System.out.println("🔥 CurrencyContext: loading...");

            // 0) Debug IP
            String ip = fetchPublicIp();
            System.out.println("✅ Public IP = " + ip);

            // 1) Detect currency from IP (no key)
            String ipInfoJson = httpGet("https://ipapi.co/json/");
            JsonObject ipObj = JsonParser.parseString(ipInfoJson).getAsJsonObject();

            String country = safeGet(ipObj, "country_code");
            String cur = safeGet(ipObj, "currency");

            System.out.println("✅ ipapi country=" + country + " currency=" + cur);

            if (cur != null && !cur.isBlank()) {
                displayCurrency = cur.toUpperCase(Locale.ROOT);
            } else {
                displayCurrency = BASE;
            }

            // 2) Rates from base TND (free endpoint)
            if (!displayCurrency.equals(BASE)) {
                String ratesJson = httpGet("https://open.er-api.com/v6/latest/" + BASE);
                JsonObject ratesObj = JsonParser.parseString(ratesJson).getAsJsonObject();

                String result = safeGet(ratesObj, "result");
                System.out.println("✅ er-api result=" + result);

                JsonObject rates = ratesObj.getAsJsonObject("rates");
                if (rates != null && rates.has(displayCurrency)) {
                    rateFromTnd = rates.get(displayCurrency).getAsBigDecimal();
                    System.out.println("✅ rate TND->" + displayCurrency + " = " + rateFromTnd);
                } else {
                    System.out.println("⚠️ Currency not found in rates, fallback to TND.");
                    displayCurrency = BASE;
                    rateFromTnd = BigDecimal.ONE;
                }
            } else {
                rateFromTnd = BigDecimal.ONE;
            }

        } catch (Exception e) {
            System.out.println("❌ CurrencyContext failed: " + e.getMessage());
            displayCurrency = BASE;
            rateFromTnd = BigDecimal.ONE;
        } finally {
            loaded = true;
            System.out.println("✅ CurrencyContext loaded. display=" + displayCurrency + " rate=" + rateFromTnd);
        }
    }

    public static String getDisplayCurrency() {
        ensureLoaded();
        return displayCurrency;
    }

    /** TND (DB) -> display currency */
    public static BigDecimal convertFromTnd(BigDecimal amountTnd) {
        ensureLoaded();
        if (amountTnd == null) return null;

        if (getDisplayCurrency().equals(BASE)) {
            return amountTnd.setScale(2, RoundingMode.HALF_UP);
        }

        return amountTnd.multiply(rateFromTnd).setScale(2, RoundingMode.HALF_UP);
    }

    /** display currency -> TND (DB) */
    public static BigDecimal convertDisplayToTnd(BigDecimal amountDisplay) {
        ensureLoaded();
        if (amountDisplay == null) return null;

        if (getDisplayCurrency().equals(BASE)) {
            return amountDisplay.setScale(2, RoundingMode.HALF_UP);
        }

        if (rateFromTnd == null || rateFromTnd.compareTo(BigDecimal.ZERO) == 0) {
            return amountDisplay.setScale(2, RoundingMode.HALF_UP); // safe fallback
        }

        // amountDisplay = amountTnd * rate => amountTnd = amountDisplay / rate
        return amountDisplay
                .divide(rateFromTnd, 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Display only (no "base", no TND text) */
    public static String formatDisplayOnly(BigDecimal amountTnd) {
        ensureLoaded();
        if (amountTnd == null) return "";

        String cur = getDisplayCurrency();
        BigDecimal converted = convertFromTnd(amountTnd);

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        String symbol = getCurrencySymbol(cur);

        if (!cur.equals(BASE)) return nf.format(converted) + " " + symbol;
        return nf.format(amountTnd) + " TND";
    }

    /** Optional: show display + base (if you want debug) */
    public static String formatDisplayWithBase(BigDecimal amountTnd) {
        ensureLoaded();
        if (amountTnd == null) return "";

        String cur = getDisplayCurrency();
        BigDecimal converted = convertFromTnd(amountTnd);

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.FRANCE);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        String symbol = getCurrencySymbol(cur);

        if (!cur.equals(BASE)) {
            return nf.format(converted) + " " + symbol + " (base: " + nf.format(amountTnd) + " TND)";
        }
        return nf.format(amountTnd) + " TND";
    }

    private static String getCurrencySymbol(String code) {
        try {
            Currency c = Currency.getInstance(code);
            return c.getSymbol(Locale.CANADA_FRENCH);
        } catch (Exception ignored) {
            return code;
        }
    }

    private static String httpGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "RhPro/1.0")
                .GET()
                .build();
        return HTTP.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    /** Debug helper: your real public IP */
    public static String fetchPublicIp() {
        try {
            String json = httpGet("https://api.ipify.org?format=json");
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            String ip = safeGet(o, "ip");
            return ip == null ? "unknown" : ip;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String safeGet(JsonObject obj, String key) {
        try {
            if (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        } catch (Exception ignored) {}
        return null;
    }
}