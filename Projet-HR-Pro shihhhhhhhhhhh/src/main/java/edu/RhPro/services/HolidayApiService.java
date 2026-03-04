package edu.RhPro.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour récupérer les jours fériés tunisiens.
 * Utilise l'API publique Nager.Date (https://date.nager.at) en premier.
 * En cas d'échec réseau, retourne les jours fériés Tunisie codés en dur (fallback).
 */
public class HolidayApiService {

    private static final String API_BASE = "https://date.nager.at/api/v3/PublicHolidays";
    private static final String COUNTRY_CODE = "TN"; // Tunisie

    // ─── Data model ─────────────────────────────────────────────────────────────

    public static class Holiday {
        private final LocalDate date;
        private final String name;
        private final String localName;
        private final boolean isFixed;

        public Holiday(LocalDate date, String name, String localName, boolean isFixed) {
            this.date = date;
            this.name = name;
            this.localName = localName;
            this.isFixed = isFixed;
        }

        public LocalDate getDate()      { return date; }
        public String getName()          { return name; }
        public String getLocalName()     { return localName; }
        public boolean isFixed()         { return isFixed; }

        @Override
        public String toString() {
            return date + " — " + localName + " (" + name + ")";
        }
    }

    // ─── Public API ─────────────────────────────────────────────────────────────

    /**
     * Retourne les jours fériés pour l'année donnée.
     * Essaie d'abord l'API Nager.Date, sinon utilise le fallback statique.
     */
    public List<Holiday> getHolidays(int year) {
        try {
            List<Holiday> fromApi = fetchFromApi(year);
            if (!fromApi.isEmpty()) return fromApi;
        } catch (Exception e) {
            System.out.println("[HolidayApiService] API non disponible, utilisation du fallback: " + e.getMessage());
        }
        return getFallbackHolidays(year);
    }

    /**
     * Vérifie si une date donnée est un jour férié.
     */
    public boolean isHoliday(LocalDate date) {
        List<Holiday> holidays = getHolidays(date.getYear());
        return holidays.stream().anyMatch(h -> h.getDate().equals(date));
    }

    /**
     * Retourne le jour férié pour une date donnée, ou null.
     */
    public Holiday getHolidayForDate(LocalDate date) {
        List<Holiday> holidays = getHolidays(date.getYear());
        return holidays.stream().filter(h -> h.getDate().equals(date)).findFirst().orElse(null);
    }

    // ─── API Fetch ───────────────────────────────────────────────────────────────

    private List<Holiday> fetchFromApi(int year) throws Exception {
        String urlString = API_BASE + "/" + year + "/" + COUNTRY_CODE;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("HTTP " + responseCode);
        }

        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        }
        conn.disconnect();

        return parseJson(json.toString());
    }

    /**
     * Parse JSON minimal sans librairie externe.
     * Format attendu de Nager.Date:
     * [{"date":"2026-01-01","localName":"Jour de l'An","name":"New Year's Day","fixed":true,...}, ...]
     */
    private List<Holiday> parseJson(String json) {
        List<Holiday> result = new ArrayList<>();
        // Remove array brackets
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]"))  json = json.substring(0, json.length() - 1);

        // Split objects
        String[] objects = splitJsonObjects(json);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (String obj : objects) {
            try {
                String date      = extractJsonField(obj, "date");
                String name      = extractJsonField(obj, "name");
                String localName = extractJsonField(obj, "localName");
                String fixed     = extractJsonField(obj, "fixed");

                if (date != null && name != null) {
                    // Use English name as display name (localName from API is Arabic which may not render)
                    result.add(new Holiday(
                            LocalDate.parse(date, fmt),
                            name,
                            name, // use English name to avoid Arabic rendering issues
                            "true".equals(fixed)
                    ));
                }
            } catch (Exception ignored) {}
        }
        return result;
    }

    private String[] splitJsonObjects(String json) {
        List<String> parts = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0) parts.add(json.substring(start, i + 1)); }
        }
        return parts.toArray(new String[0]);
    }

    private String extractJsonField(String obj, String field) {
        String key = "\"" + field + "\"";
        int idx = obj.indexOf(key);
        if (idx < 0) return null;
        int colon = obj.indexOf(':', idx + key.length());
        if (colon < 0) return null;
        int valStart = colon + 1;
        while (valStart < obj.length() && obj.charAt(valStart) == ' ') valStart++;
        if (valStart >= obj.length()) return null;
        char first = obj.charAt(valStart);
        if (first == '"') {
            int end = obj.indexOf('"', valStart + 1);
            return end < 0 ? null : obj.substring(valStart + 1, end);
        } else {
            int end = valStart;
            while (end < obj.length() && obj.charAt(end) != ',' && obj.charAt(end) != '}') end++;
            return obj.substring(valStart, end).trim();
        }
    }

    // ─── Fallback : Jours fériés Tunisie (fixes + approx. pour fêtes islamiques) ─

    /**
     * Jours fériés tunisiens officiels.
     * Les fêtes islamiques sont approximatives (calendrier lunaire).
     */
    public List<Holiday> getFallbackHolidays(int year) {
        List<Holiday> list = new ArrayList<>();

        // ── Fêtes fixes ──────────────────────────────────────────────────────────
        list.add(new Holiday(LocalDate.of(year, 1, 1),   "New Year's Day",          "Jour de l'An",     true));
        list.add(new Holiday(LocalDate.of(year, 3, 20),  "Independence Day",        "Fête de l'Indépendance",           true));
        list.add(new Holiday(LocalDate.of(year, 4, 9),   "Martyrs' Day",            "Journée des Martyrs",             true));
        list.add(new Holiday(LocalDate.of(year, 5, 1),   "Labour Day",              "Fête du Travail",               true));
        list.add(new Holiday(LocalDate.of(year, 7, 25),  "Republic Day",            "Fête de la République",           true));
        list.add(new Holiday(LocalDate.of(year, 8, 13),  "Women's Day",             "Journée de la Femme",              true));
        list.add(new Holiday(LocalDate.of(year, 10, 15), "Evacuation Day",          "Fête de l'Évacuation",              true));

        // ── Fêtes islamiques (approximatives par année) ──────────────────────────
        list.addAll(getIslamicHolidays(year));

        list.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return list;
    }

    /**
     * Dates approximatives des fêtes islamiques pour la Tunisie.
     * Calculées selon la règle de Kuwaiti / algorithme de Butcher-Meeus.
     */
    private List<Holiday> getIslamicHolidays(int year) {
        List<Holiday> list = new ArrayList<>();

        // Approximation basée sur le cycle de 33 ans (décalage ~11 jours/an)
        // Dates 2025 de référence, ajustement annuel
        int diff = year - 2025;
        int shiftDays = diff * (-11); // recule d'environ 11 jours par an

        // Aïd el-Fitr 2025 ≈ 30 mars
        LocalDate aidFitr = LocalDate.of(2025, 3, 30).plusDays(shiftDays);
        // Normaliser dans la même année
        aidFitr = normalizeToYear(aidFitr, year);
        if (aidFitr != null) {
            list.add(new Holiday(aidFitr,          "Eid al-Fitr",      "Aïd el-Fitr",         false));
            list.add(new Holiday(aidFitr.plusDays(1), "Eid al-Fitr (2)", "Aïd el-Fitr (2ème jour)", false));
        }

        // Aïd el-Adha 2025 ≈ 6 juin
        LocalDate aidAdha = LocalDate.of(2025, 6, 6).plusDays(shiftDays);
        aidAdha = normalizeToYear(aidAdha, year);
        if (aidAdha != null) {
            list.add(new Holiday(aidAdha,          "Eid al-Adha",      "Aïd el-Adha",        false));
            list.add(new Holiday(aidAdha.plusDays(1), "Eid al-Adha (2)", "Aïd el-Adha (2ème jour)", false));
        }

        // Nouvel An Hégirien 2025 ≈ 27 juin
        LocalDate hijriNewYear = LocalDate.of(2025, 6, 27).plusDays(shiftDays);
        hijriNewYear = normalizeToYear(hijriNewYear, year);
        if (hijriNewYear != null) {
            list.add(new Holiday(hijriNewYear, "Islamic New Year", "Nouvel An Hégirien", false));
        }

        // Mawlid an-Nabi 2025 ≈ 5 sept
        LocalDate mawlid = LocalDate.of(2025, 9, 5).plusDays(shiftDays);
        mawlid = normalizeToYear(mawlid, year);
        if (mawlid != null) {
            list.add(new Holiday(mawlid, "Prophet's Birthday", "Mawlid an-Nabi", false));
        }

        return list;
    }

    private LocalDate normalizeToYear(LocalDate date, int year) {
        // Try the date as-is in the target year
        try {
            return LocalDate.of(year, date.getMonthValue(), date.getDayOfMonth());
        } catch (Exception e) {
            // Feb 29 etc.
            return null;
        }
    }
}