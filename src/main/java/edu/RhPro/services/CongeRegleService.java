package edu.RhPro.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Règles métier des congés + calendrier des jours fériés tunisiens.
 * - Limites légales par type de congé (droit tunisien)
 * - Calcul des jours ouvrables (hors weekends + fériés)
 * - Détection des jours fériés dans une période
 * - Validation complète d'une demande
 *
 * ✅ Jours fériés islamiques récupérés DYNAMIQUEMENT via l'API Aladhan
 *    (https://aladhan.com/islamic-calendar-api) — gratuite, sans clé API.
 */
public class CongeRegleService {

    // ══════════════════════════════════════════════
    //  1. LIMITES LÉGALES PAR TYPE (jours calendaires)
    // ══════════════════════════════════════════════

    public static class RegleConge {
        public final String type;
        public final int maxJours;
        public final int minJours;
        public final boolean documentObligatoire;
        public final String description;
        public final String couleur;
        public final String icone;

        public RegleConge(String type, int minJours, int maxJours,
                          boolean documentObligatoire, String description,
                          String couleur, String icone) {
            this.type = type;
            this.minJours = minJours;
            this.maxJours = maxJours;
            this.documentObligatoire = documentObligatoire;
            this.description = description;
            this.couleur = couleur;
            this.icone = icone;
        }
    }

    private static final Map<String, RegleConge> REGLES = new LinkedHashMap<>();

    static {
        REGLES.put("Congé annuel", new RegleConge(
                "Congé annuel", 1, 30, false,
                "Max 30 jours ouvrables par an (Code du travail tunisien Art. 107)",
                "#0369a1", "🏖"));

        REGLES.put("Congé maladie", new RegleConge(
                "Congé maladie", 1, 180, true,
                "Certificat médical obligatoire. Max 6 mois (180 j) avec justificatif",
                "#059669", "🏥"));

        REGLES.put("Congé maternité", new RegleConge(
                "Congé maternité", 30, 112, false,
                "Entre 30 et 112 jours (16 semaines). Protégé par la loi n°2002-32",
                "#ec4899", "👶"));

        REGLES.put("Congé professionnel", new RegleConge(
                "Congé professionnel", 1, 10, false,
                "Max 10 jours par an pour formation ou mission professionnelle",
                "#6d2269", "💼"));

        REGLES.put("Congé sabbatique", new RegleConge(
                "Congé sabbatique", 30, 365, false,
                "Entre 30 jours et 1 an. Accord préalable de l'employeur requis",
                "#d97706", "🌍"));

        REGLES.put("Autre", new RegleConge(
                "Autre", 1, 30, false,
                "Congé exceptionnel, max 30 jours. Justificatif recommandé",
                "#6b7280", "📋"));
    }

    public static RegleConge getRegle(String typeConge) {
        return REGLES.get(typeConge);
    }

    public static Map<String, RegleConge> getToutesLesRegles() {
        return Collections.unmodifiableMap(REGLES);
    }

    // ══════════════════════════════════════════════
    //  2. JOURS FÉRIÉS TUNISIENS
    // ══════════════════════════════════════════════

    public static class JourFerie {
        public final String nom;
        public final String type; // "fixe" ou "islamique"
        public final String emoji;

        public JourFerie(String nom, String type, String emoji) {
            this.nom = nom;
            this.type = type;
            this.emoji = emoji;
        }
    }

    // ── Cache pour éviter d'appeler l'API plusieurs fois pour la même année ──
    private static final Map<Integer, Map<LocalDate, JourFerie>> CACHE_FERIES
            = new ConcurrentHashMap<>();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    /**
     * Retourne tous les jours fériés tunisiens pour une année donnée.
     * Les fêtes islamiques sont récupérées dynamiquement via l'API Aladhan.
     * En cas d'échec réseau, un fallback statique est utilisé.
     */
    public static Map<LocalDate, JourFerie> getJoursFeries(int annee) {

        // Retourner depuis le cache si déjà chargé
        if (CACHE_FERIES.containsKey(annee)) {
            return CACHE_FERIES.get(annee);
        }

        Map<LocalDate, JourFerie> feries = new LinkedHashMap<>();

        // ── Jours fériés FIXES (toujours les mêmes dates) ────────────
        feries.put(LocalDate.of(annee, Month.JANUARY, 1),
                new JourFerie("Nouvel An", "fixe", "🎆"));
        feries.put(LocalDate.of(annee, Month.MARCH, 20),
                new JourFerie("Fête de l'Indépendance", "fixe", "🇹🇳"));
        feries.put(LocalDate.of(annee, Month.APRIL, 9),
                new JourFerie("Journée des Martyrs", "fixe", "🕊"));
        feries.put(LocalDate.of(annee, Month.MAY, 1),
                new JourFerie("Fête du Travail", "fixe", "👷"));
        feries.put(LocalDate.of(annee, Month.JULY, 25),
                new JourFerie("Fête de la République", "fixe", "🏛"));
        feries.put(LocalDate.of(annee, Month.AUGUST, 13),
                new JourFerie("Journée de la Femme", "fixe", "👩"));
        feries.put(LocalDate.of(annee, Month.OCTOBER, 15),
                new JourFerie("Fête de l'Évacuation", "fixe", "🏳"));

        // ── Jours fériés ISLAMIQUES (dynamiques via API Aladhan) ─────
        try {
            Map<LocalDate, JourFerie> islamiques = recupererFeriesIslamiques(annee);
            feries.putAll(islamiques);
            System.out.println("[CongeRegleService] Fériés islamiques " + annee
                    + " chargés dynamiquement (" + islamiques.size() + " fêtes).");
        } catch (Exception e) {
            System.err.println("[CongeRegleService] API Aladhan indisponible ("
                    + e.getMessage() + "). Utilisation du fallback statique.");
            feries.putAll(getFeriesIslamiquesFallback(annee));
        }

        CACHE_FERIES.put(annee, feries);
        return feries;
    }

    /**
     * Vide le cache (utile pour forcer un rechargement).
     */
    public static void viderCache() {
        CACHE_FERIES.clear();
    }

    // ══════════════════════════════════════════════
    //  API ALADHAN — Récupération dynamique
    // ══════════════════════════════════════════════

    /**
     * Récupère les fêtes islamiques d'une année grégorienne via l'API Aladhan.
     *
     * Fêtes récupérées :
     *  - Aïd el-Fitr      : 1 Shawwal  (mois 10)
     *  - Aïd el-Adha      : 10 Dhu al-Hijjah (mois 12)
     *  - Ras el-Am Hijri  : 1 Muharram (mois 1)
     *  - Mouled           : 12 Rabi' al-Awwal (mois 3)
     *  - Début Ramadan    : 1 Ramadan  (mois 9)
     */
    private static Map<LocalDate, JourFerie> recupererFeriesIslamiques(int annee)
            throws Exception {

        Map<LocalDate, JourFerie> islamiques = new LinkedHashMap<>();

        // {mois hégien, jour hégien, nom, emoji, ajouter J+1}
        Object[][] fetes = {
                {1,  1,  "Ras el-Am el-Hijri (Nouvel An islamique)", "☪",  false},
                {3,  12, "Mouled (Naissance du Prophète)",           "☪",  false},
                {9,  1,  "Début Ramadan",                            "🌙", false},
                {10, 1,  "Aïd el-Fitr (Aïd Seghir)",                "🌙", true },
                {12, 10, "Aïd el-Adha (Aïd Kebir)",                 "🐑", true },
        };

        // Une année grégorienne peut couvrir 2 années hégiennes
        // Approximation : année grégorienne - 579 ≈ année hégirienne
        int hijriBase = annee - 579;

        for (int decalage = 0; decalage <= 1; decalage++) {
            int ha = hijriBase + decalage;

            for (Object[] fete : fetes) {
                int    moisH  = (int)    fete[0];
                int    jourH  = (int)    fete[1];
                String nom    = (String) fete[2];
                String emoji  = (String) fete[3];
                boolean j1    = (boolean)fete[4];

                // Appel API Aladhan : convertit date hégirienne → grégorienne
                // Endpoint : GET /v1/hToG?date=DD-MM-YYYY
                String dateH = String.format("%02d-%02d-%04d", jourH, moisH, ha);
                String url   = "https://api.aladhan.com/v1/hToG?date=" + dateH;

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .GET()
                        .build();

                HttpResponse<String> resp = HTTP_CLIENT.send(
                        req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() != 200) continue;

                LocalDate dateGreg = parseAladhanResponse(resp.body());
                if (dateGreg == null || dateGreg.getYear() != annee) continue;

                // Éviter les doublons
                if (islamiques.containsKey(dateGreg)) continue;

                islamiques.put(dateGreg, new JourFerie(nom, "islamique", emoji));

                // Ajouter J+1 pour Aïd el-Fitr et Aïd el-Adha
                if (j1) {
                    LocalDate lendemain = dateGreg.plusDays(1);
                    if (lendemain.getYear() == annee && !islamiques.containsKey(lendemain)) {
                        islamiques.put(lendemain, new JourFerie(
                                nom.replace("(Aïd Seghir)", "(J+1)")
                                        .replace("(Aïd Kebir)",  "(J+1)"),
                                "islamique", emoji));
                    }
                }
            }
        }

        if (islamiques.isEmpty()) {
            throw new Exception("Aucune fête islamique retournée par l'API pour " + annee);
        }

        return islamiques;
    }

    /**
     * Parse la réponse JSON de l'API Aladhan pour extraire la date grégorienne.
     * Exemple de réponse :
     * {"code":200,"status":"OK","data":{"gregorian":{"date":"19-03-2026",...}}}
     */
    private static LocalDate parseAladhanResponse(String json) {
        try {
            // On cherche la date dans la section "gregorian"
            int gIdx = json.indexOf("\"gregorian\"");
            if (gIdx == -1) return null;

            String marker = "\"date\":\"";
            int dIdx  = json.indexOf(marker, gIdx);
            if (dIdx == -1) return null;

            int start = dIdx + marker.length();
            int end   = json.indexOf("\"", start);
            if (end == -1) return null;

            String dateStr = json.substring(start, end); // ex: "19-03-2026"
            String[] parts = dateStr.split("-");
            if (parts.length != 3) return null;

            return LocalDate.of(
                    Integer.parseInt(parts[2]),  // année
                    Integer.parseInt(parts[1]),  // mois
                    Integer.parseInt(parts[0])   // jour
            );
        } catch (Exception e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════
    //  FALLBACK STATIQUE (si API inaccessible)
    // ══════════════════════════════════════════════

    /**
     * Dates islamiques statiques utilisées uniquement si l'API Aladhan
     * est inaccessible (pas de connexion internet, timeout, etc.).
     */
    private static Map<LocalDate, JourFerie> getFeriesIslamiquesFallback(int annee) {
        Map<LocalDate, JourFerie> f = new LinkedHashMap<>();
        if (annee == 2025) {
            f.put(LocalDate.of(2025, 3,  1),  new JourFerie("Début Ramadan",                           "islamique", "🌙"));
            f.put(LocalDate.of(2025, 3, 30),  new JourFerie("Aïd el-Fitr (Aïd Seghir)",               "islamique", "🌙"));
            f.put(LocalDate.of(2025, 3, 31),  new JourFerie("Aïd el-Fitr (J+1)",                      "islamique", "🌙"));
            f.put(LocalDate.of(2025, 6,  6),  new JourFerie("Aïd el-Adha (Aïd Kebir)",                "islamique", "🐑"));
            f.put(LocalDate.of(2025, 6,  7),  new JourFerie("Aïd el-Adha (J+1)",                      "islamique", "🐑"));
            f.put(LocalDate.of(2025, 6, 26),  new JourFerie("Ras el-Am el-Hijri (Nouvel An islamique)","islamique", "☪" ));
            f.put(LocalDate.of(2025, 9,  4),  new JourFerie("Mouled (Naissance du Prophète)",          "islamique", "☪" ));
        } else if (annee == 2026) {
            f.put(LocalDate.of(2026, 2, 17),  new JourFerie("Début Ramadan",                           "islamique", "🌙"));
            f.put(LocalDate.of(2026, 3, 19),  new JourFerie("Aïd el-Fitr (Aïd Seghir)",               "islamique", "🌙"));
            f.put(LocalDate.of(2026, 3, 20),  new JourFerie("Aïd el-Fitr (J+1)",                      "islamique", "🌙"));
            f.put(LocalDate.of(2026, 5, 27),  new JourFerie("Aïd el-Adha (Aïd Kebir)",                "islamique", "🐑"));
            f.put(LocalDate.of(2026, 5, 28),  new JourFerie("Aïd el-Adha (J+1)",                      "islamique", "🐑"));
            f.put(LocalDate.of(2026, 6, 16),  new JourFerie("Ras el-Am el-Hijri (Nouvel An islamique)","islamique", "☪" ));
            f.put(LocalDate.of(2026, 8, 25),  new JourFerie("Mouled (Naissance du Prophète)",          "islamique", "☪" ));
        } else if (annee == 2027) {
            f.put(LocalDate.of(2027, 2,  6),  new JourFerie("Début Ramadan",                           "islamique", "🌙"));
            f.put(LocalDate.of(2027, 3,  8),  new JourFerie("Aïd el-Fitr (Aïd Seghir)",               "islamique", "🌙"));
            f.put(LocalDate.of(2027, 3,  9),  new JourFerie("Aïd el-Fitr (J+1)",                      "islamique", "🌙"));
            f.put(LocalDate.of(2027, 5, 16),  new JourFerie("Aïd el-Adha (Aïd Kebir)",                "islamique", "🐑"));
            f.put(LocalDate.of(2027, 5, 17),  new JourFerie("Aïd el-Adha (J+1)",                      "islamique", "🐑"));
            f.put(LocalDate.of(2027, 6,  5),  new JourFerie("Ras el-Am el-Hijri (Nouvel An islamique)","islamique", "☪" ));
            f.put(LocalDate.of(2027, 8, 14),  new JourFerie("Mouled (Naissance du Prophète)",          "islamique", "☪" ));
        }
        return f;
    }

    // ══════════════════════════════════════════════
    //  3. JOURS FÉRIÉS DANS UNE PÉRIODE
    // ══════════════════════════════════════════════

    public static List<Map.Entry<LocalDate, JourFerie>> getJeriesDansPeriode(
            LocalDate debut, LocalDate fin) {

        List<Map.Entry<LocalDate, JourFerie>> result = new ArrayList<>();
        Set<Integer> annees = new HashSet<>();
        LocalDate d = debut;
        while (!d.isAfter(fin)) { annees.add(d.getYear()); d = d.plusMonths(1); }
        annees.add(fin.getYear());

        for (int annee : annees) {
            for (Map.Entry<LocalDate, JourFerie> entry : getJoursFeries(annee).entrySet()) {
                LocalDate date = entry.getKey();
                if (!date.isBefore(debut) && !date.isAfter(fin))
                    result.add(entry);
            }
        }
        result.sort(Map.Entry.comparingByKey());
        return result;
    }

    // ══════════════════════════════════════════════
    //  4. CALCUL DES JOURS OUVRABLES
    // ══════════════════════════════════════════════

    public static int calculerJoursOuvrables(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) return 0;
        Set<LocalDate> feriesSet = new HashSet<>();
        for (int annee = debut.getYear(); annee <= fin.getYear(); annee++)
            feriesSet.addAll(getJoursFeries(annee).keySet());
        int count = 0;
        LocalDate current = debut;
        while (!current.isAfter(fin)) {
            DayOfWeek dow = current.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY
                    && !feriesSet.contains(current)) count++;
            current = current.plusDays(1);
        }
        return count;
    }

    public static long calculerJoursCalendaires(LocalDate debut, LocalDate fin) {
        if (debut == null || fin == null || fin.isBefore(debut)) return 0;
        return fin.toEpochDay() - debut.toEpochDay() + 1;
    }

    // ══════════════════════════════════════════════
    //  5. VALIDATION D'UNE DEMANDE
    // ══════════════════════════════════════════════

    public static class ResultatValidation {
        public final boolean valide;
        public final List<String> erreurs;
        public final List<String> avertissements;
        public final List<String> infos;
        public final int joursOuvrables;
        public final long joursCalendaires;
        public final List<Map.Entry<LocalDate, JourFerie>> feriesDansPeriode;

        public ResultatValidation(boolean valide, List<String> erreurs,
                                  List<String> avertissements, List<String> infos,
                                  int joursOuvrables, long joursCalendaires,
                                  List<Map.Entry<LocalDate, JourFerie>> feriesDansPeriode) {
            this.valide = valide; this.erreurs = erreurs;
            this.avertissements = avertissements; this.infos = infos;
            this.joursOuvrables = joursOuvrables; this.joursCalendaires = joursCalendaires;
            this.feriesDansPeriode = feriesDansPeriode;
        }
    }

    public static ResultatValidation valider(String typeConge, LocalDate debut,
                                             LocalDate fin, boolean aDocument) {
        List<String> erreurs = new ArrayList<>();
        List<String> avertissements = new ArrayList<>();
        List<String> infos = new ArrayList<>();

        if (typeConge == null || debut == null || fin == null) {
            erreurs.add("Données incomplètes");
            return new ResultatValidation(false, erreurs, avertissements, infos, 0, 0,
                    Collections.emptyList());
        }

        long joursCalendaires = calculerJoursCalendaires(debut, fin);
        int joursOuvrables    = calculerJoursOuvrables(debut, fin);
        List<Map.Entry<LocalDate, JourFerie>> feries = getJeriesDansPeriode(debut, fin);
        RegleConge regle = getRegle(typeConge);

        if (regle != null) {
            if (joursCalendaires < regle.minJours)
                erreurs.add("Durée insuffisante : minimum " + regle.minJours
                        + " jours pour " + typeConge + " (vous avez " + joursCalendaires + " j)");
            if (joursCalendaires > regle.maxJours)
                erreurs.add("Durée dépassée : maximum " + regle.maxJours
                        + " jours pour " + typeConge + " (vous avez " + joursCalendaires + " j)");
            if (regle.maxJours > 0 && joursCalendaires > regle.maxJours * 0.85
                    && joursCalendaires <= regle.maxJours)
                avertissements.add("Attention : vous utilisez "
                        + String.format("%.0f", (joursCalendaires * 100.0 / regle.maxJours))
                        + "% de votre quota maximum autorisé");
            if (regle.documentObligatoire && !aDocument)
                avertissements.add("Un certificat médical est obligatoire pour " + typeConge);
        }

        if (debut.isBefore(LocalDate.now()))
            avertissements.add("La date de début est dans le passé");

        if (!feries.isEmpty()) {
            StringBuilder feriesMsg = new StringBuilder("Jours fériés inclus : ");
            for (int i = 0; i < feries.size(); i++) {
                if (i > 0) feriesMsg.append(", ");
                feriesMsg.append(feries.get(i).getValue().emoji)
                        .append(" ").append(feries.get(i).getValue().nom)
                        .append(" (").append(feries.get(i).getKey()).append(")");
            }
            infos.add(feriesMsg.toString());
        }

        infos.add("Jours ouvrables effectifs : " + joursOuvrables
                + " (sur " + joursCalendaires + " jours calendaires)");

        return new ResultatValidation(erreurs.isEmpty(), erreurs, avertissements,
                infos, joursOuvrables, joursCalendaires, feries);
    }
}