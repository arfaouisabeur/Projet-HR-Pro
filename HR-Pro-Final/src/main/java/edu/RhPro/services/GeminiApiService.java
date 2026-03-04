package edu.RhPro.services;

import com.google.gson.*;
import edu.RhPro.entities.Tache;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Service IA — Groq API (GRATUIT, ultra-rapide, fonctionne partout)
 *
 * ✅ Clé gratuite en 30 secondes :
 *    1. Allez sur https://console.groq.com
 *    2. Sign up avec Google
 *    3. API Keys → Create API Key → Copiez
 *
 * Modèle utilisé : llama-3.1-8b-instant (très rapide, très intelligent)
 */
public class GeminiApiService {  // Nom gardé pour compatibilité avec ChatbotController

    // ─────────────────────────────────────────────────
    //  ⚠️  METTEZ VOTRE CLÉ GROQ ICI  (gratuite)
    //  👉  https://console.groq.com  → API Keys
    // ─────────────────────────────────────────────────
    private static final String API_KEY = "VOTRE_CLE_GROQ_ICI";

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL   = "llama-3.1-8b-instant";  // Rapide et gratuit
    private static final int    TIMEOUT = 15_000;

    private final Gson gson = new Gson();

    // Historique de conversation
    private final List<Map<String, String>> history = new ArrayList<>();

    // ── TEST DE CONNEXION ─────────────────────────────
    public String testConnection() {
        // Test DNS
        try {
            InetAddress.getByName("api.groq.com");
            System.out.println("[Groq] ✅ DNS OK");
        } catch (UnknownHostException e) {
            return "Pas d'accès internet. Vérifiez votre connexion ou utilisez le hotspot de votre téléphone.";
        }

        // Test TCP 443
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress("api.groq.com", 443), 5000);
            s.close();
            System.out.println("[Groq] ✅ Port 443 OK");
        } catch (IOException e) {
            return "Port 443 bloqué par votre réseau. Utilisez le hotspot de votre téléphone.";
        }

        // Test API réel
        try {
            String testBody = buildRequestBody("Réponds juste 'ok'", false);
            String result   = callApi(testBody);
            System.out.println("[Groq] ✅ API OK : " + result);
            return null; // succès
        } catch (IOException e) {
            System.err.println("[Groq] ❌ " + e.getMessage());
            return e.getMessage();
        }
    }

    // ── ENVOI DE MESSAGE ──────────────────────────────
    public String sendMessage(String userMessage, List<Tache> tasks) throws IOException {

        // Injecter le contexte des tâches dans le 1er message
        String msg = userMessage;
        if (history.isEmpty() && tasks != null && !tasks.isEmpty()) {
            msg = buildContextPrefix(tasks) + "\n\nQuestion : " + userMessage;
        }

        // Ajouter à l'historique
        history.add(Map.of("role", "user", "content", msg));
        if (history.size() > 20) history.remove(0);

        try {
            String body  = buildRequestBody(null, true);
            String reply = callApi(body);

            history.add(Map.of("role", "assistant", "content", reply));
            return reply;

        } catch (IOException e) {
            // Retirer le message en cas d'erreur
            history.remove(history.size() - 1);
            throw e;
        }
    }

    // ── CONSTRUCTION DE LA REQUÊTE ────────────────────
    private String buildRequestBody(String singleMessage, boolean useHistory) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System message
        messages.add(Map.of(
                "role", "system",
                "content", "Tu es un assistant RH intelligent intégré dans l'application HR-Pro. " +
                        "Tu aides les employés à gérer leur travail. " +
                        "Tu réponds TOUJOURS en français, de manière concise et professionnelle. " +
                        "Tu peux répondre à n'importe quelle question. " +
                        "Utilise des emojis avec modération."
        ));

        if (useHistory) {
            messages.addAll(history);
        } else {
            messages.add(Map.of("role", "user", "content", singleMessage));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model",       MODEL);
        body.put("messages",    messages);
        body.put("max_tokens",  1024);
        body.put("temperature", 0.7);

        return gson.toJson(body);
    }

    // ── APPEL HTTP ────────────────────────────────────
    private String callApi(String jsonBody) throws IOException {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(API_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",  "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }

            if (code != 200) {
                String errMsg = "Erreur HTTP " + code;
                try {
                    JsonObject err = JsonParser.parseString(sb.toString()).getAsJsonObject();
                    if (err.has("error")) {
                        JsonElement e = err.getAsJsonObject("error").get("message");
                        if (e != null) errMsg = e.getAsString();
                    }
                } catch (Exception ignored) {}

                // Messages d'erreur explicites
                if (code == 401) errMsg = "Clé API invalide. Vérifiez votre clé sur https://console.groq.com";
                if (code == 429) errMsg = "Quota dépassé. Attendez quelques secondes.";
                if (code == 503) errMsg = "Service temporairement indisponible.";

                throw new IOException(errMsg);
            }

            // Parser la réponse OpenAI-compatible
            JsonObject response = JsonParser.parseString(sb.toString()).getAsJsonObject();
            return response
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ── UTILITAIRES ───────────────────────────────────
    public void resetConversation() {
        history.clear();
    }

    public boolean isConfigured() {
        return API_KEY != null
                && !API_KEY.isBlank()
                && !API_KEY.equals("VOTRE_CLE_GROQ_ICI")
                && !API_KEY.equals("VOTRE_CLE_GEMINI_ICI");
    }

    private String buildContextPrefix(List<Tache> tasks) {
        long todo  = tasks.stream().filter(t -> "TODO" .equals(t.getStatut())).count();
        long doing = tasks.stream().filter(t -> "DOING".equals(t.getStatut())).count();
        long done  = tasks.stream().filter(t -> "DONE" .equals(t.getStatut())).count();
        int  rate  = (int)((done * 100) / tasks.size());

        StringBuilder sb = new StringBuilder("=== TÂCHES DE L'EMPLOYÉ ===\n");
        sb.append("Total:").append(tasks.size())
                .append(" | À faire:").append(todo)
                .append(" | En cours:").append(doing)
                .append(" | Terminées:").append(done)
                .append(" | Taux:").append(rate).append("%\n");
        for (Tache t : tasks) {
            sb.append("- [").append(t.getStatut()).append("] #").append(t.getId())
                    .append(": ").append(t.getTitre());
            if (t.getDescription() != null && !t.getDescription().isBlank())
                sb.append(" → ").append(t.getDescription());
            sb.append("\n");
        }
        sb.append("===========================");
        return sb.toString();
    }
}