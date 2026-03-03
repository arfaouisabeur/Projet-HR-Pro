package edu.RhPro.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.RhPro.utils.AppConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class GeminiClient {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String summarize(String cvText) {

        try {
            // 1) vérifications
            if (cvText == null) return "CV vide.";
            cvText = cvText.trim();

            if (cvText.length() < 200) {
                return "CV illisible ou trop court (le PDF ne contient pas assez de texte).";
            }

            // 2) lire config
            String apiKey = AppConfig.get("gemini.api.key");
            String model = AppConfig.get("gemini.model");

            if (apiKey == null || apiKey.isBlank()) {
                return "Clé Gemini manquante (gemini.api.key).";
            }
            if (model == null || model.isBlank()) {
                model = "gemini-2.5-flash";
            }

            // 3) limiter taille envoyée (évite erreurs quota/tokens)
            if (cvText.length() > 12000) {
                cvText = cvText.substring(0, 12000);
            }

            // 4) prompt propre (sans markdown)
            String prompt =
                    "Tu es un assistant RH.\n" +
                            "Fais un résumé très court et simple de ce CV.\n\n" +

                            "Format obligatoire (maximum 6 lignes):\n" +

                            "Profil: ...\n" +
                            "Compétences clés: ...\n" +
                            "Expérience principale: ...\n" +
                            "Formation: ...\n" +
                            "Points forts: ...\n\n" +

                            "Réponse courte et claire.\n\n" +

                            "CV:\n" + cvText;

            // 5) construire JSON propre (échappement OK)
            String body = """
            {
              "contents": [
                {
                  "parts": [
                    { "text": %s }
                  ]
                }
              ]
            }
            """.formatted(mapper.writeValueAsString(prompt));

            // 6) requête
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 7) erreurs HTTP
            if (response.statusCode() != 200) {
                return "Erreur Gemini HTTP " + response.statusCode() + " :\n" + response.body();
            }

            // 8) extraire TEXTE (pas JSON brut)
            JsonNode root = mapper.readTree(response.body());
            String summary = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");

            summary = summary.trim();

            if (summary.isBlank()) {
                return "Aucun résumé généré.";
            }

            // 9) nettoyage léger (au cas où)
            summary = summary.replace("**", "");
            return summary;

        } catch (Exception e) {
            throw new RuntimeException("Erreur GeminiClient: " + e.getMessage(), e);
        }
    }
    public String analyzeCv(String cvText) {

        try {

            String apiKey = AppConfig.get("gemini.api.key");
            String model = AppConfig.get("gemini.model");

            String prompt =
                    "Tu es un expert RH.\n\n" +

                            "Analyse ce CV pour un recruteur.\n\n" +

                            "Structure la réponse comme ceci:\n\n" +

                            "Profil du candidat:\n" +
                            "- résumé rapide\n\n" +

                            "Compétences principales:\n" +
                            "- liste courte\n\n" +

                            "Expérience:\n" +
                            "- poste | entreprise | missions principales\n\n" +

                            "Points forts:\n" +
                            "- ...\n\n" +

                            "Manques / Risques:\n" +
                            "- ...\n\n" +

                            "Recommandation RH:\n" +
                            "- recommander ou non le candidat\n\n" +

                            "CV:\n" + cvText;

            String body = """
{
 "contents":[
  {
   "parts":[
    {"text":%s}
   ]
  }
 ]
}
""".formatted(new ObjectMapper().writeValueAsString(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://generativelanguage.googleapis.com/v1beta/models/"
                                    + model +
                                    ":generateContent?key=" + apiKey))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response =
                    client.send(request,HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());

            String text = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");

            return text;

        } catch (Exception e) {

            throw new RuntimeException("Erreur analyse CV Gemini: " + e.getMessage());
        }
    }
}