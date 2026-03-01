package edu.RhPro.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 🔐 OCR Service — Analyse un certificat médical via OCR.space API
 * Clé gratuite sur : https://ocr.space/ocrapi
 */
public class OcrService {

    // 🔑 Clé gratuite — crée ton compte sur ocr.space
    private static final String API_KEY = "K85766506188957"; // remplace par ta clé
    private static final String API_URL = "https://api.ocr.space/parse/image";

    // =====================================================
    //  Résultat retourné après analyse OCR
    // =====================================================
    public static class OcrResult {
        public String texteComplet;
        public String nomMedecin;
        public LocalDate dateDebut;
        public LocalDate dateFin;
        public int dureeJours;
        public boolean estValide; // true si au moins médecin OU date trouvé
        public String messageErreur;
    }

    // =====================================================
    //  Méthode principale — envoie l'image à l'API OCR
    // =====================================================
    public static OcrResult analyserCertificat(File imageFile) throws Exception {

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("apikey", API_KEY)
                .addFormDataPart("language", "fre")        // français
                .addFormDataPart("isOverlayRequired", "false")
                .addFormDataPart("detectOrientation", "true")
                .addFormDataPart("scale", "true")           // améliore la qualité
                .addFormDataPart("file", imageFile.getName(),
                        RequestBody.create(imageFile,
                                MediaType.parse("image/*")))
                .build();

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Erreur OCR API : " + response.code());
            }

            String jsonStr = response.body().string();
            return parseOcrResponse(jsonStr);
        }
    }

    // =====================================================
    //  Parsing de la réponse OCR → extraction intelligente
    // =====================================================
    private static OcrResult parseOcrResponse(String json) {
        OcrResult result = new OcrResult();

        try {
            JSONObject obj = new JSONObject(json);

            // Vérifier si OCR a réussi
            boolean isErroredOnProcessing = obj.optBoolean("IsErroredOnProcessing", false);
            if (isErroredOnProcessing) {
                result.estValide = false;
                result.messageErreur = obj.optString("ErrorMessage", "Erreur OCR inconnue");
                return result;
            }

            // Extraire le texte complet
            JSONArray parsedResults = obj.optJSONArray("ParsedResults");
            if (parsedResults == null || parsedResults.isEmpty()) {
                result.estValide = false;
                result.messageErreur = "Aucun texte détecté dans l'image";
                return result;
            }

            StringBuilder texte = new StringBuilder();
            for (int i = 0; i < parsedResults.length(); i++) {
                texte.append(parsedResults.getJSONObject(i)
                        .optString("ParsedText", ""));
            }

            result.texteComplet = texte.toString();

            // === Extraction intelligente ===
            result.nomMedecin = extraireNomMedecin(result.texteComplet);
            result.dateDebut   = extraireDateDebut(result.texteComplet);
            result.dateFin     = extraireDateFin(result.texteComplet);

            if (result.dateDebut != null && result.dateFin != null) {
                result.dureeJours = (int) (result.dateFin.toEpochDay()
                        - result.dateDebut.toEpochDay());
            }

            // Valide si on a trouvé au moins le médecin ou une date
            result.estValide = (result.nomMedecin != null)
                    || (result.dateDebut != null);

            if (!result.estValide) {
                result.messageErreur =
                        "Certificat non reconnu — aucun médecin ni date détectés";
            }

        } catch (Exception e) {
            result.estValide = false;
            result.messageErreur = "Erreur parsing : " + e.getMessage();
        }

        return result;
    }

    // =====================================================
    //  Extraction du nom du médecin (patterns tunisiens)
    // =====================================================
    private static String extraireNomMedecin(String texte) {
        // Patterns courants sur certificats médicaux tunisiens/français
        String[] patterns = {
                "(?i)Dr\\.?\\s+([A-ZÀ-Ü][a-zà-ü]+(?:\\s+[A-ZÀ-Ü][a-zà-ü]+){0,3})",
                "(?i)Docteur\\s+([A-ZÀ-Ü][a-zà-ü]+(?:\\s+[A-ZÀ-Ü][a-zà-ü]+){0,3})",
                "(?i)Médecin\\s*:\\s*([A-ZÀ-Ü][a-zà-ü]+(?:\\s+[A-ZÀ-Ü][a-zà-ü]+){0,3})",
                "(?i)Praticien\\s*:\\s*([A-ZÀ-Ü][a-zà-ü]+(?:\\s+[A-ZÀ-Ü][a-zà-ü]+){0,3})"
        };

        for (String pat : patterns) {
            Matcher m = Pattern.compile(pat).matcher(texte);
            if (m.find()) return "Dr. " + m.group(1).trim();
        }
        return null;
    }

    // =====================================================
    //  Extraction de la date de début d'arrêt
    // =====================================================
    private static LocalDate extraireDateDebut(String texte) {
        // Cherche "du JJ/MM/AAAA" ou "à partir du" ou "incapable ... du"
        String[] patternsDebut = {
                "(?i)(?:du|à partir du|depuis le)\\s*(\\d{1,2}[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{4}))",
                "(?i)(?:arrêt|repos)\\s*(?:du|de)\\s*(\\d{1,2}[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{4}))",
                "(\\d{1,2}[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{4}))" // fallback : 1ère date trouvée
        };

        for (String pat : patternsDebut) {
            Matcher m = Pattern.compile(pat).matcher(texte);
            if (m.find()) {
                LocalDate d = parseDate(m.group(1));
                if (d != null) return d;
            }
        }
        return null;
    }

    // =====================================================
    //  Extraction de la date de fin d'arrêt
    // =====================================================
    private static LocalDate extraireDateFin(String texte) {
        // Cherche "au JJ/MM/AAAA" ou "jusqu'au" ou "inclus le"
        String[] patternsFin = {
                "(?i)(?:au|jusqu'au|inclus le)\\s*(\\d{1,2}[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{4}))",
                "(?i)(?:fin|expire)\\s*(?:le)?\\s*(\\d{1,2}[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{4}))"
        };

        for (String pat : patternsFin) {
            Matcher m = Pattern.compile(pat).matcher(texte);
            if (m.find()) {
                LocalDate d = parseDate(m.group(1));
                if (d != null) return d;
            }
        }

        // Fallback : 2ème date dans le texte si différente de dateDebut
        Matcher all = Pattern.compile("(\\d{1,2}[/\\-\\.](\\d{1,2})[/\\-\\.](\\d{4}))")
                .matcher(texte);
        LocalDate first = null;
        while (all.find()) {
            LocalDate d = parseDate(all.group(1));
            if (d != null) {
                if (first == null) first = d;
                else if (!d.equals(first)) return d; // 2ème date différente
            }
        }
        return null;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null) return null;
        String normalized = raw.replaceAll("[\\-\\.]", "/");
        String[] formats = {"d/M/yyyy", "dd/MM/yyyy"};
        for (String fmt : formats) {
            try {
                return LocalDate.parse(normalized,
                        DateTimeFormatter.ofPattern(fmt));
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }
}