package edu.RhPro.services;

import okhttp3.*;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Service Text-to-Speech via ElevenLabs API.
 * Cle gratuite sur : https://elevenlabs.io  (10 000 caracteres/mois offerts)
 * Voice ID "Rachel" = 21m00Tcm4TlvDq8ikWAM (voix feminine professionnelle FR)
 */
public class ElevenLabsService {

    // Remplace par ta cle API ElevenLabs
    private static final String API_KEY  = "sk_a470686bf8ae87524f4f9926b8e9d12b117efb81a1536771";
    private static final String VOICE_ID = "21m00Tcm4TlvDq8ikWAM"; // Rachel - voix pro
    private static final String API_URL  = "https://api.elevenlabs.io/v1/text-to-speech/" + VOICE_ID;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // ══════════════════════════════════════════════
    //  Construire le texte du resume RH
    // ══════════════════════════════════════════════
    public static String construireResume(
            int total,
            double delaiMoyen,
            double tauxRes,
            long slaDepasses,
            int nbUrgentes,
            java.util.Map<String, Integer> statuts,
            java.util.Map<String, Integer> priorites) {

        String mois = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy",
                java.util.Locale.FRENCH));

        // Trouver le statut dominant
        String statutDominant = statuts.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse("inconnu");

        // Construire le texte naturel
        StringBuilder texte = new StringBuilder();
        texte.append("Bonjour. Voici le rapport des services RH pour ").append(mois).append(". ");
        texte.append("Au total, ").append(total).append(" demandes ont été enregistrées. ");

        if (tauxRes > 0) {
            texte.append("Le taux de résolution est de ")
                    .append(String.format("%.0f", tauxRes))
                    .append(" pourcent. ");
        }

        if (delaiMoyen > 0) {
            texte.append("Le délai moyen de traitement est de ")
                    .append(String.format("%.1f", delaiMoyen))
                    .append(" jours. ");
        }

        if (slaDepasses > 0) {
            texte.append("Attention : ").append(slaDepasses)
                    .append(slaDepasses == 1 ? " demande a dépassé" : " demandes ont dépassé")
                    .append(" le délai SLA. Une action est requise. ");
        } else {
            texte.append("Bonne nouvelle : aucun délai SLA n'a été dépassé. ");
        }

        if (nbUrgentes > 0) {
            texte.append(nbUrgentes)
                    .append(nbUrgentes == 1 ? " demande urgente est" : " demandes urgentes sont")
                    .append(" en attente de traitement. ");
        }

        texte.append("Le statut le plus fréquent est : ").append(statutDominant).append(". ");

        // Priorités
        int urgent = priorites.getOrDefault("URGENT", 0);
        int normal = priorites.getOrDefault("NORMAL", 0);
        int faible = priorites.getOrDefault("FAIBLE", 0);
        texte.append("Répartition par priorité : ")
                .append(urgent).append(" urgentes, ")
                .append(normal).append(" normales, ")
                .append(faible).append(" faibles. ");

        texte.append("Fin du rapport. Bonne journée.");

        return texte.toString();
    }

    // ══════════════════════════════════════════════
    //  Appeler ElevenLabs et retourner le fichier MP3
    // ══════════════════════════════════════════════
    public static File genererAudio(String texte) throws Exception {

        // Corps de la requête JSON
        String json = "{"
                + "\"text\":\"" + escapeJson(texte) + "\","
                + "\"model_id\":\"eleven_multilingual_v2\","
                + "\"voice_settings\":{"
                + "  \"stability\":0.5,"
                + "  \"similarity_boost\":0.8,"
                + "  \"style\":0.2,"
                + "  \"use_speaker_boost\":true"
                + "}"
                + "}";

        Request request = new Request.Builder()
                .url(API_URL)
                .header("xi-api-key", API_KEY)
                .header("Content-Type", "application/json")
                .header("Accept", "audio/mpeg")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("ElevenLabs erreur " + response.code() + " : " + response.body().string());
            }

            // Sauvegarder en fichier MP3 temporaire
            File mp3 = File.createTempFile("rhpro_rapport_", ".mp3");
            mp3.deleteOnExit();
            try (FileOutputStream fos = new FileOutputStream(mp3)) {
                fos.write(response.body().bytes());
            }
            return mp3;
        }
    }

    // ══════════════════════════════════════════════
    //  Jouer le fichier MP3 avec JavaFX Media
    // ══════════════════════════════════════════════
    public static javafx.scene.media.MediaPlayer jouerAudio(File mp3) {
        javafx.scene.media.Media media =
                new javafx.scene.media.Media(mp3.toURI().toString());
        javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
        player.play();
        return player;
    }

    // ── Helper ──────────────────────────────────
    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}