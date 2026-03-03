package edu.RhPro.services;

import org.json.JSONObject;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SignatureService {

    private final HttpClient client = HttpClient.newHttpClient();

    private String loadDropboxKey() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) throw new RuntimeException("config.properties introuvable dans resources");
            var props = new java.util.Properties();
            props.load(in);
            String key = props.getProperty("dropboxsign.api.key");
            if (key == null || key.isBlank()) throw new RuntimeException("dropboxsign.api.key manquante");
            return key.trim();
        } catch (Exception e) {
            throw new RuntimeException("Erreur chargement clé DropboxSign: " + e.getMessage(), e);
        }
    }

    public String sendContractByEmail(String signerName, String signerEmail) throws Exception {

        String apiKey = loadDropboxKey();

        // Charger le PDF depuis resources/contracts/contrat.pdf
        InputStream pdfStream = getClass().getClassLoader().getResourceAsStream("contracts/contrat.pdf");
        if (pdfStream == null) throw new RuntimeException("contracts/contrat.pdf introuvable");

        byte[] pdfBytes = pdfStream.readAllBytes();

        String boundary = "----RHPRO" + System.currentTimeMillis();

        ByteArrayOutputStream body = new ByteArrayOutputStream();

        // ✅ MODE TEST (important pour projet)
        writeField(body, boundary, "test_mode", "1");

        writeField(body, boundary, "title", "Contrat de travail");
        writeField(body, boundary, "subject", "Veuillez signer votre contrat");
        writeField(body, boundary, "message", "Bonjour, merci de signer le contrat ci-joint.");

        writeField(body, boundary, "signers[0][name]", signerName);
        writeField(body, boundary, "signers[0][email_address]", signerEmail);

        writeFileBytes(body, boundary, "files[0]", "contrat.pdf", "application/pdf", pdfBytes);

        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        String basicAuth = Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.hellosign.com/v3/signature_request/send"))
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur Dropbox Sign (" + response.statusCode() + "): " + response.body());
        }

        // Retourne l'ID pour stockage DB
        JSONObject json = new JSONObject(response.body());
        return json.getJSONObject("signature_request").getString("signature_request_id");
    }

    private static void writeField(OutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void writeFileBytes(OutputStream out, String boundary, String name,
                                       String filename, String contentType, byte[] bytes) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(bytes);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }
    public boolean isContractSigned(String signatureRequestId) throws Exception {

        String apiKey = loadDropboxKey();

        String basicAuth = java.util.Base64.getEncoder()
                .encodeToString((apiKey + ":").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.hellosign.com/v3/signature_request/" + signatureRequestId))
                .header("Authorization", "Basic " + basicAuth)
                .GET()
                .build();

        java.net.http.HttpResponse<String> response =
                java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur statut (" + response.statusCode() + "): " + response.body());
        }

        org.json.JSONObject json = new org.json.JSONObject(response.body());
        org.json.JSONObject sr = json.getJSONObject("signature_request");

        return sr.getBoolean("is_complete"); // ✅ true => signé
    }
}



