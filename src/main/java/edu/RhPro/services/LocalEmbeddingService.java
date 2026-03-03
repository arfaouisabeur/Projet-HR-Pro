package edu.RhPro.services;

import java.util.Random;

public class LocalEmbeddingService {

    // Simulation d'embedding local
    public float[] embed(String text) {

        if (text == null || text.isBlank()) {
            return new float[10];
        }

        Random random = new Random(text.hashCode());

        float[] vector = new float[10];

        for (int i = 0; i < vector.length; i++) {
            vector[i] = random.nextFloat();
        }

        return vector;
    }
}