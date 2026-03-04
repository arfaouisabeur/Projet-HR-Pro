package edu.RhPro.utils;

import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Properties PROPS = new Properties();
    private static boolean loaded = false;

    private AppConfig() {}

    private static synchronized void loadOnce() {
        if (loaded) return;
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) throw new RuntimeException("config.properties introuvable dans resources");
            PROPS.load(in);
            loaded = true;
        } catch (Exception e) {
            throw new RuntimeException("Erreur chargement config.properties: " + e.getMessage(), e);
        }
    }

    public static String get(String key) {
        loadOnce();
        String v = PROPS.getProperty(key);
        return v == null ? "" : v.trim();
    }
}