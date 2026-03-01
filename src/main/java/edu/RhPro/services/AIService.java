package edu.RhPro.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service IA — Génère une description professionnelle + suggère une durée
 * en utilisant l'API Groq (GRATUITE, ultra rapide).
 *
 * 🔑 Clé gratuite sur : https://console.groq.com
 *    → "API Keys" → "Create API Key"
 */
public class AIService {

    // 🔐 Remplace par ta clé Groq (commence par "gsk_...")
    private static final String groqApiKey = System.getenv("GROQ_API_KEY");
    private static final String API_URL  = "https://api.groq.com/openai/v1/chat/completions";

    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // =========================================================
    //  Résultat retourné par l'IA (description + durée suggérée)
    // =========================================================
    public static class AiResult {
        public final String description;
        public final int    dureeJoursSuggeree; // 0 = pas de suggestion

        public AiResult(String description, int dureeJoursSuggeree) {
            this.description         = description;
            this.dureeJoursSuggeree  = dureeJoursSuggeree;
        }
    }

    // =========================================================
    //  Méthode principale
    // =========================================================

    /**
     * Génère une description professionnelle ET suggère une durée
     * selon le type de congé.
     *
     * @param typeConge ex: "Congé maladie"
     * @return AiResult contenant la description et la durée suggérée
     */
    public static AiResult genererDescriptionEtDuree(String typeConge) throws Exception {

        String prompt = buildPrompt(typeConge);

        // Format JSON compatible OpenAI (Groq utilise le même format)
        String body = "{"
                + "\"model\": \"llama-3.3-70b-versatile\","
                + "\"messages\": ["
                + "  {\"role\": \"system\", \"content\": \""
                +      "Tu es un assistant RH professionnel. "
                +      "Réponds TOUJOURS en JSON valide avec exactement deux champs : "
                +      "\\\"description\\\" (string, 3-4 lignes formelles en français) "
                +      "et \\\"duree_jours\\\" (integer, durée recommandée en jours). "
                +      "Aucun texte en dehors du JSON.\"},"
                + "  {\"role\": \"user\", \"content\": \"" + escapeJson(prompt) + "\"}"
                + "],"
                + "\"max_tokens\": 300,"
                + "\"temperature\": 0.7"
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Erreur API Groq (code " + response.statusCode() + "): " + response.body());
        }

        return parseGroqResponse(response.body());
    }

    // =========================================================
    //  Prompt adapté au type de congé
    // =========================================================
    private static String buildPrompt(String typeConge) {
        return switch (typeConge) {
            case "Congé annuel" ->
                    "Génère une description formelle pour une demande de congé annuel. "
                            + "L'employé souhaite se reposer après une longue période de travail intensif. "
                            + "Suggère une durée typique en jours pour ce type de congé.";

            case "Congé maladie" ->
                    "Génère une description formelle pour une demande de congé maladie. "
                            + "L'employé est dans l'incapacité de travailler pour raisons de santé. "
                            + "Suggère une durée typique en jours pour ce type de congé.";

            case "Congé maternité" ->
                    "Génère une description formelle pour une demande de congé maternité. "
                            + "L'employée attend un enfant et souhaite exercer son droit légal. "
                            + "Suggère la durée légale standard en jours (Tunisie).";

            case "Congé professionnel" ->
                    "Génère une description formelle pour une demande de congé professionnel. "
                            + "L'employé doit participer à une formation ou un événement professionnel. "
                            + "Suggère une durée typique en jours pour ce type de congé.";

            case "Congé sabbatique" ->
                    "Génère une description formelle pour une demande de congé sabbatique. "
                            + "L'employé souhaite prendre du recul pour un projet personnel ou une reconversion. "
                            + "Suggère une durée typique en jours pour ce type de congé.";

            default ->
                    "Génère une description formelle pour une demande de congé de type : "
                            + typeConge + ". Suggère une durée typique en jours.";
        };
    }

    // =========================================================
    //  Parsing de la réponse Groq → extrait le JSON de l'IA
    // =========================================================
    private static AiResult parseGroqResponse(String groqJson) throws Exception {

        // Extraire le contenu du message retourné par Groq
        String marker = "\"content\": \"";
        int start = groqJson.indexOf(marker);
        if (start == -1) {
            marker = "\"content\":\"";
            start  = groqJson.indexOf(marker);
        }
        if (start == -1) throw new Exception("Réponse Groq inattendue : " + groqJson);

        start += marker.length();

        // Trouver la fin du contenu (guillemet non échappé)
        int end = start;
        while (end < groqJson.length()) {
            if (groqJson.charAt(end) == '"' && groqJson.charAt(end - 1) != '\\') break;
            end++;
        }

        // Désescaper pour obtenir le JSON interne retourné par l'IA
        String innerJson = groqJson.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");

        // Extraire "description"
        String description = extractJsonString(innerJson, "description");

        // Extraire "duree_jours"
        int duree = extractJsonInt(innerJson, "duree_jours");

        return new AiResult(description, duree);
    }

    // =========================================================
    //  Helpers d'extraction JSON (sans librairie externe)
    // =========================================================
    private static String extractJsonString(String json, String key) throws Exception {
        String marker = "\"" + key + "\": \"";
        int start = json.indexOf(marker);
        if (start == -1) {
            marker = "\"" + key + "\":\"";
            start  = json.indexOf(marker);
        }
        if (start == -1) throw new Exception("Champ '" + key + "' introuvable dans : " + json);

        start += marker.length();
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
    }

    private static int extractJsonInt(String json, String key) {
        try {
            String marker = "\"" + key + "\": ";
            int start = json.indexOf(marker);
            if (start == -1) {
                marker = "\"" + key + "\":";
                start  = json.indexOf(marker);
            }
            if (start == -1) return 0;

            start += marker.length();
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)))) end++;
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}