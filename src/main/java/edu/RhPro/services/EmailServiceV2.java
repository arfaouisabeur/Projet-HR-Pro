package edu.RhPro.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

public class EmailServiceV2 {

    private static final String ENV_API_KEY = "SENDGRID_API_KEY";
    private static final String ENV_FROM_EMAIL = "SENDGRID_FROM_EMAIL";
    private static final String SENDGRID_URL = "https://api.sendgrid.com/v3/mail/send";

    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * ✅ Method expected by your controller.
     * Returns a readable diagnostics string (safe: doesn't print full key).
     */
    public String debugConfiguration() {
        String apiKey = System.getenv(ENV_API_KEY);
        String from = System.getenv(ENV_FROM_EMAIL);

        boolean apiKeyPresent = apiKey != null && !apiKey.isBlank();
        boolean fromPresent = from != null && !from.isBlank();

        String apiKeyTrim = apiKeyPresent ? apiKey.trim() : "";
        String prefix = apiKeyPresent ? apiKeyTrim.substring(0, Math.min(6, apiKeyTrim.length())) : "";

        return "EmailService diagnostics:\n" +
                "- SENDGRID_API_KEY present: " + apiKeyPresent + "\n" +
                "- SENDGRID_FROM_EMAIL present: " + fromPresent + "\n" +
                (fromPresent ? "FROM=" + from.trim() + "\n" : "") +
                (apiKeyPresent ? "API_KEY length=" + apiKeyTrim.length() + "\n" : "") +
                (apiKeyPresent ? "API_KEY starts with: " + prefix + "...\n" : "") +
                "Expected success status: 202\n";
    }

    /**
     * ✅ Method expected by your controller.
     * True only if env vars exist and look valid.
     */
    public boolean isConfigured() {
        String apiKey = System.getenv(ENV_API_KEY);
        String from = System.getenv(ENV_FROM_EMAIL);

        if (apiKey == null || apiKey.isBlank()) return false;
        if (from == null || from.isBlank()) return false;

        apiKey = apiKey.trim();
        from = from.trim();

        // Strong hint: real SendGrid keys usually start with SG.
        // Not mandatory, but helps catch wrong env values.
        if (!apiKey.startsWith("SG.")) return false;

        return true;
    }

    /**
     * ✅ Method expected by your controller.
     * Sends the email and returns SendGrid HTTP status (202 on success).
     * Throws IOException on non-202.
     */
    public int sendSalaryPaidEmail(String toEmail, String subject, String htmlContent) throws IOException {
        String apiKey = requireEnv(ENV_API_KEY).trim();
        String fromEmail = requireEnv(ENV_FROM_EMAIL).trim();

        // basic validations
        if (toEmail == null || toEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is empty.");
        }
        toEmail = toEmail.trim();

        if (subject == null) subject = "";
        if (htmlContent == null) htmlContent = "";

        // Build JSON body for SendGrid
        String jsonBody = buildSendGridJson(fromEmail, toEmail, subject, htmlContent);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SENDGRID_URL))
                .header("Authorization", "Bearer " + apiKey) // ✅ IMPORTANT
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("SendGrid request interrupted", e);
        }

        int status = response.statusCode();
        String body = response.body();

        if (status != 202) {
            throw new IOException("SendGrid error status=" + status + " body=" + body);
        }

        return status;
    }

    private static String requireEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return v;
    }

    private static String buildSendGridJson(String fromEmail, String toEmail, String subject, String htmlContent) {
        String textContent = stripHtmlToText(htmlContent);

        return "{"
                + "\"personalizations\":[{\"to\":[{\"email\":\"" + jsonEscape(toEmail) + "\"}]}],"
                + "\"from\":{\"email\":\"" + jsonEscape(fromEmail) + "\"},"
                + "\"subject\":\"" + jsonEscape(subject) + "\","
                + "\"content\":["
                +   "{\"type\":\"text/plain\",\"value\":\"" + jsonEscape(textContent) + "\"},"
                +   "{\"type\":\"text/html\",\"value\":\"" + jsonEscape(htmlContent) + "\"}"
                + "]"
                + "}";
    }

    /** simple HTML -> text fallback */
    private static String stripHtmlToText(String html) {
        if (html == null) return "";
        return html
                .replaceAll("(?is)<style.*?>.*?</style>", "")
                .replaceAll("(?is)<script.*?>.*?</script>", "")
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}