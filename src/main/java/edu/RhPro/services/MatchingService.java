package edu.RhPro.services;

public class MatchingService {

    private final LocalEmbeddingService local = new LocalEmbeddingService();

    // Calcul du score entre CV et Offre
    public int computeScore(String cvText, String offreText) {

        if (cvText == null || offreText == null) {
            return 0;
        }

        float[] cvVector = local.embed(cvText);
        float[] offreVector = local.embed(offreText);

        if (cvVector == null || offreVector == null) {
            return 0;
        }

        double similarity = cosineSimilarity(cvVector, offreVector);

        // transformer similarité (0-1) en score (0-100)
        int score = (int) Math.round(similarity * 100);

        // sécurité
        if (score < 0) score = 0;
        if (score > 100) score = 100;

        return score;
    }

    // Calcul similarité cosinus
    private double cosineSimilarity(float[] a, float[] b) {

        int length = Math.min(a.length, b.length);

        double dot = 0;
        double normA = 0;
        double normB = 0;

        for (int i = 0; i < length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0;
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}