package edu.RhPro.utils;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Random;

public class CaptchaGenerator {

    private String captchaText;

    public WritableImage generateCaptchaImage() {

        int width = 160;
        int height = 50;

        Canvas canvas = new Canvas(width, height);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Random random = new Random();

        // Fond blanc
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, width, height);

        captchaText = generateRandomText(5);

        gc.setFont(new Font("Arial", 30));

        // Dessiner lettres avec rotation
        for (int i = 0; i < captchaText.length(); i++) {

            gc.setFill(Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble()));

            gc.save();

            double rotation = (random.nextDouble() - 0.5) * 30;
            gc.translate(30 * i + 20, 35);
            gc.rotate(rotation);

            gc.fillText(String.valueOf(captchaText.charAt(i)), 0, 0);

            gc.restore();
        }

        // Bruit
        for (int i = 0; i < 100; i++) {
            gc.setFill(Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble()));
            gc.fillOval(random.nextInt(width), random.nextInt(height), 2, 2);
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);

        return canvas.snapshot(params, null);
    }

    private String generateRandomText(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    public String getCaptchaText() {
        return captchaText;
    }
}
