package edu.RhPro.controllers.employe;

import edu.RhPro.entities.Evenement;
import edu.RhPro.entities.Rating;
import edu.RhPro.entities.User;
import edu.RhPro.services.RatingService;
import edu.RhPro.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

import java.sql.SQLException;

public class RatingPopupController {

    @FXML private Label eventTitleLabel;
    @FXML private TextArea commentArea;
    @FXML private Button submitButton;
    @FXML private Button closeButton;
    @FXML private Label messageLabel;
    @FXML private HBox starPreviewBox;
    @FXML private Label starCountLabel;

    private Evenement event;
    private Rating existingRating;
    private RatingService ratingService = new RatingService();
    private Runnable onRatingSubmitted;

    @FXML
    public void initialize() {
        // Add listener to comment area to update star preview in real-time
        commentArea.textProperty().addListener((observable, oldValue, newValue) -> {
            updateStarPreview(newValue);
        });
    }

    public void setEvenement(Evenement event) {
        this.event = event;
        eventTitleLabel.setText("📅 " + event.getTitre());
    }

    public void setExistingRating(Rating rating) {
        this.existingRating = rating;
        if (rating != null) {
            commentArea.setText(rating.getCommentaire());
            submitButton.setText("Mettre à jour l'évaluation");
        }
    }

    public void setOnRatingSubmitted(Runnable callback) {
        this.onRatingSubmitted = callback;
    }

    private void updateStarPreview(String comment) {
        // Analyze sentiment to determine stars
        int stars = analyzeSentiment(comment);

        // Update star preview
        for (int i = 0; i < starPreviewBox.getChildren().size(); i++) {
            Label starLabel = (Label) starPreviewBox.getChildren().get(i);
            if (i < stars) {
                starLabel.setVisible(true);
                starLabel.setText("⭐");
            } else {
                starLabel.setVisible(false);
            }
        }

        // Update star count label with description
        String description = getStarDescription(stars);
        starCountLabel.setText(stars + " étoile" + (stars > 1 ? "s" : "") + " - " + description);

        // Change color based on rating
        switch (stars) {
            case 1:
            case 2:
                starCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #ef4444;");
                break;
            case 3:
                starCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #f59e0b;");
                break;
            case 4:
            case 5:
                starCountLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #10b981;");
                break;
        }
    }

    private String getStarDescription(int stars) {
        switch (stars) {
            case 1: return "Très mauvais";
            case 2: return "Mauvais";
            case 3: return "Moyen";
            case 4: return "Bon";
            case 5: return "Excellent";
            default: return "";
        }
    }

    // Sentiment Analysis (copied from service for real-time preview)
    private int analyzeSentiment(String commentaire) {
        if (commentaire == null || commentaire.trim().isEmpty()) {
            return 3; // Default
        }

        String lowerComment = commentaire.toLowerCase();

        String[] excellentKeywords = {"excellent", "super", "fantastique", "génial", "parfait",
                "incroyable", "merveilleux", "exceptionnel", "top", "👌"};
        String[] goodKeywords = {"bien", "bon", "satisfait", "content", "agréable", "sympa",
                "correct", "pas mal", "👍"};
        String[] averageKeywords = {"moyen", "acceptable", "passable", "ordinaire", "bof",
                "ni bien ni mal", "ok"};
        String[] poorKeywords = {"mauvais", "déçu", "insatisfait", "pas content", "décevant",
                "désagréable", "pas bien", "à améliorer"};
        String[] terribleKeywords = {"terrible", "horrible", "catastrophe", "très mauvais",
                "pire", "inacceptable", "désastre", "honteux"};

        int score = 0;
        int keywordCount = 0;

        for (String keyword : excellentKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 5;
                keywordCount++;
            }
        }
        for (String keyword : goodKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 4;
                keywordCount++;
            }
        }
        for (String keyword : averageKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 3;
                keywordCount++;
            }
        }
        for (String keyword : poorKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 2;
                keywordCount++;
            }
        }
        for (String keyword : terribleKeywords) {
            if (lowerComment.contains(keyword)) {
                score += 1;
                keywordCount++;
            }
        }

        if (keywordCount > 0) {
            return score / keywordCount;
        }

        return 3;
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        String comment = commentArea.getText().trim();

        if (comment.isEmpty()) {
            messageLabel.setText("❌ Veuillez écrire un commentaire");
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
            return;
        }

        try {
            User currentUser = Session.getCurrentUser();
            if (currentUser == null) {
                messageLabel.setText("❌ Utilisateur non connecté");
                return;
            }

            // Analyze sentiment to get stars
            int stars = analyzeSentiment(comment);

            if (existingRating == null) {
                // Create new rating
                Rating rating = new Rating(
                        this.event.getId(),
                        currentUser.getId(),
                        comment,
                        stars
                );
                ratingService.addEntity(rating);
                messageLabel.setText("✅ Évaluation ajoutée avec succès !");
                messageLabel.setStyle("-fx-text-fill: #10b981;");
            } else {
                // Update existing rating
                existingRating.setCommentaire(comment);
                existingRating.setEtoiles(stars);
                ratingService.updateEntity(existingRating);
                messageLabel.setText("✅ Évaluation mise à jour avec succès !");
                messageLabel.setStyle("-fx-text-fill: #10b981;");
            }

            // Disable submit button to prevent double submission
            submitButton.setDisable(true);
            submitButton.setText("✓ Envoyé");

            // Close after 2 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> {
                        Stage stage = (Stage) submitButton.getScene().getWindow();
                        stage.close();
                        if (onRatingSubmitted != null) {
                            onRatingSubmitted.run();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("❌ Erreur lors de l'enregistrement: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: #ef4444;");
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }
}